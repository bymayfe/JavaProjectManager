package com.smartproject.scanner;

import com.google.gson.*;
import com.smartproject.model.Project;
import com.smartproject.model.Project.SourceType;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

/**
 * Docker container'larindaki ve uzak sunuculardaki projeleri tarar.
 * Desteklenen modlar:
 *   - CLI Modu     : 'docker' komutu ile (yerel Docker Desktop gerekir)
 *   - API Modu     : Docker REST API (localhost:2375)
 *   - SSH - Docker : SSH ile VDS'ye baglanip docker komutlarini uzaktan calistir
 *   - SSH - Klasor : SSH ile VDS'ye baglanip dizinleri tara
 */
public class DockerScanner {

    private static final String DOCKER_API_BASE = "http://localhost:2375";

    private Session activeSession = null;
    private String lastHost = null;
    private int lastPort = -1;
    private String lastUser = null;
    private String lastPass = null;
    private String lastPemPath = null;

    // Tamamen goz ardi edilecek klasorler (DirNode agaci kurulurken de atlanir)
    private static final Set<String> IGNORED_DIRS = new HashSet<>(Arrays.asList(
        ".git", "node_modules", "target", "build", ".idea", ".spm",
        "venv", ".venv", "__pycache__", "dist", "out", ".next",
        "__mocks__", ".cache", "vendor", "bower_components", ".gradle",
        "coverage", ".nyc_output", "tmp", ".tmp", "logs", "log"
    ));

    // Guclu proje gostergesi dosyalar
    private static final Set<String> STRONG_PROJECT_FILES = new HashSet<>(Arrays.asList(
        "pom.xml", "package.json", "build.gradle", "requirements.txt",
        "go.mod", "cargo.toml", "makefile", ".gitignore", "dockerfile",
        "docker-compose.yml", "docker-compose.yaml", "setup.py",
        "pyproject.toml", "composer.json", "gemfile", "build.sbt",
        "project.clj", "mix.exs", "cmakelists.txt"
    ));

    // Guclu proje gostergesi alt klasorler
    private static final Set<String> STRONG_PROJECT_DIRS = new HashSet<>(Arrays.asList(
        ".git", "src", ".idea", "node_modules"
    ));

    // ====================================================
    // SSH YARDIMCI METODLAR
    // ====================================================

    /**
     * SSH oturumu ac. Sifre veya .pem dosyasi ile kimlik dogrulamasi desteklenir.
     * @param pemPath null veya bos ise sifre kullanilir; aksi halde .pem dosyasi kullanilir.
     */
    private Session openSshSession(String host, int port, String user, String pass, String pemPath) throws Exception {
        JSch jsch = new JSch();

        if (pemPath != null && !pemPath.trim().isEmpty()) {
            if (pass != null && !pass.trim().isEmpty()) {
                jsch.addIdentity(pemPath.trim(), pass.trim());
            } else {
                jsch.addIdentity(pemPath.trim());
            }
        }

        Session session = jsch.getSession(user, host, port);

        if (pemPath == null || pemPath.trim().isEmpty()) {
            session.setPassword(pass);
        }

        java.util.Properties config = new java.util.Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);
        
        // Keep-alive to prevent timeout and reduce reconnects
        session.setServerAliveInterval(15000); // 15 seconds
        session.setServerAliveCountMax(3);
        
        session.connect(15000);
        return session;
    }

    private synchronized Session getOrCreateSession(String host, int port, String user, String pass, String pemPath) throws Exception {
        if (activeSession != null && activeSession.isConnected() 
                && Objects.equals(host, lastHost) 
                && port == lastPort 
                && Objects.equals(user, lastUser) 
                && Objects.equals(pass, lastPass) 
                && Objects.equals(pemPath, lastPemPath)) {
            return activeSession;
        }

        if (activeSession != null) {
            try {
                activeSession.disconnect();
            } catch (Exception e) {
                // Ignore
            }
            activeSession = null;
        }

        activeSession = openSshSession(host, port, user, pass, pemPath);
        lastHost = host;
        lastPort = port;
        lastUser = user;
        lastPass = pass;
        lastPemPath = pemPath;
        return activeSession;
    }

    /**
     * SSH uzerinden tek bir komut calistir ve sonucu dondur.
     * Hem stdout hem de stderr yakalanir; stderr varsa sonuna [STDERR]... eklenir.
     */
    public String executeSshCommand(String host, int port, String user, String pass, String pemPath,
                                    String command, boolean useSudo) throws Exception {
        Session session = getOrCreateSession(host, port, user, pass, pemPath);
        ChannelExec channel = null;
        try {
            channel = (ChannelExec) session.openChannel("exec");

            boolean hasPass = pass != null && !pass.isEmpty();
            String finalCmd = useSudo ? (hasPass ? "sudo -S " + command : "sudo " + command) : command;
            channel.setCommand(finalCmd);

            if (useSudo && hasPass) {
                channel.setInputStream(new ByteArrayInputStream((pass + "\n").getBytes("UTF-8")));
            } else {
                channel.setInputStream(null);
            }

            // stderr'i de yakala
            ByteArrayOutputStream errBuffer = new ByteArrayOutputStream();
            channel.setErrStream(errBuffer);

            InputStream in = channel.getInputStream();
            channel.connect();

            StringBuilder outputBuffer = new StringBuilder();
            byte[] tmp = new byte[4096];
            while (true) {
                while (in.available() > 0) {
                    int i = in.read(tmp, 0, 4096);
                    if (i < 0) break;
                    outputBuffer.append(new String(tmp, 0, i, "UTF-8"));
                }
                if (channel.isClosed()) {
                    if (in.available() > 0) continue;
                    break;
                }
                Thread.sleep(100);
            }
            channel.disconnect();

            // stderr varsa ve stdout bosse ya da anlamsizsa ekle
            String errOutput = errBuffer.toString("UTF-8").trim();
            // sudo sifre prompt satırlarini filtrele (sudo: password required gibi mesajlar)
            if (!errOutput.isEmpty()) {
                // sudo'nun kendi mesajlarini (sudo: ...) goster ama sifre promptunu gizle
                String filteredErr = errOutput
                        .replaceAll("(?m)^\\[sudo\\] .*\n?", "")
                        .replaceAll("(?m)^sudo: .*\n?", "")  // sudo internal msgs
                        .trim();
                if (!filteredErr.isEmpty()) {
                    if (outputBuffer.length() > 0) {
                        outputBuffer.append("\n");
                    }
                    outputBuffer.append("[HATA/UYARI] ").append(filteredErr);
                }
            }

            return outputBuffer.toString();
        } catch (Exception ex) {
            if (activeSession != null) {
                try {
                    activeSession.disconnect();
                } catch (Exception e) {
                    // Ignore
                }
                activeSession = null;
            }
            throw ex;
        }
    }

    /**
     * SSH baglanti testi. Basarili olursa kullanicinin ev dizinini dondurur.
     */
    public String testSshConnection(String host, int port, String user, String pass, String pemPath) throws Exception {
        String homeDir = executeSshCommand(host, port, user, pass, pemPath, "echo $HOME", false).trim();
        if (homeDir.isEmpty()) homeDir = "/home/" + user;
        return homeDir;
    }

    /**
     * Verilen SSH dizinindeki klasor ve dosyalari listeler.
     * Her entry: { "name", "path", "isDir", "isHidden", "projectHint" }
     */
    public List<Map<String, String>> listSshDirectory(String host, int port, String user, String pass,
                                                       String pemPath, String remotePath, boolean useSudo) throws Exception {
        String command = "ls -1aF " + remotePath;
        String output = executeSshCommand(host, port, user, pass, pemPath, command, useSudo);

        // Once sudo olmadan dene, hata/bos gelirse ve useSudo false ise sudo ile tekrar dene
        if (!useSudo && (output.isEmpty() || output.contains("[HATA/UYARI]"))) {
            String sudoOutput = executeSshCommand(host, port, user, pass, pemPath, command, true);
            if (!sudoOutput.contains("[HATA/UYARI]")) {
                output = sudoOutput;
            }
        }

        if (output.contains("[HATA/UYARI]")) {
            throw new Exception(output.substring(output.indexOf("[HATA/UYARI]") + 12).trim());
        }

        List<String> names = new ArrayList<>();
        for (String line : output.split("\n")) {
            String name = line.trim();
            if (name.isEmpty() || name.equals(".") || name.equals("..") ||
                    name.equals("./") || name.equals("../")) continue;
            names.add(name);
        }

        Set<String> lowerNameSet = new HashSet<>();
        List<Map<String, String>> result = new ArrayList<>();

        for (String rawName : names) {
            boolean isDir = rawName.endsWith("/");
            String name = rawName;
            if (name.endsWith("/") || name.endsWith("*") || name.endsWith("@")) {
                name = name.substring(0, name.length() - 1);
            }
            lowerNameSet.add(name.toLowerCase());

            Map<String, String> entry = new LinkedHashMap<>();
            entry.put("name", name);
            String fullPath = remotePath.endsWith("/") ? remotePath + name : remotePath + "/" + name;
            entry.put("path", fullPath);
            entry.put("isDir", isDir ? "true" : "false");
            entry.put("isHidden", name.startsWith(".") ? "true" : "false");
            entry.put("projectHint", "");
            result.add(entry);
        }

        // Bu dizinin proje ipucunu bul
        String hint = detectHintFromFileList(lowerNameSet);

        List<Map<String, String>> visible = new ArrayList<>();
        for (Map<String, String> e : result) {
            if (!e.get("isHidden").equals("true")) {
                e.put("currentDirHint", hint);
                visible.add(e);
            }
        }
        return visible;
    }

    private String detectHintFromFileList(Set<String> names) {
        if (names.contains(".git"))                 return "git";
        if (names.contains("pom.xml"))              return "maven";
        if (names.contains("package.json"))         return "node";
        if (names.contains("requirements.txt") ||
            names.contains("setup.py") ||
            names.contains("pyproject.toml"))       return "python";
        if (names.contains("docker-compose.yml") ||
            names.contains("docker-compose.yaml") ||
            names.contains("dockerfile"))           return "docker";
        if (names.contains("build.gradle"))         return "gradle";
        if (names.contains("go.mod"))               return "go";
        if (names.contains("cargo.toml"))           return "rust";
        return "";
    }

    private String getHintForFile(String fileName) {
        switch (fileName.toLowerCase()) {
            case "pom.xml":             return "maven";
            case "package.json":        return "node";
            case "requirements.txt":
            case "setup.py":
            case "pyproject.toml":      return "python";
            case "build.gradle":        return "gradle";
            case "go.mod":              return "go";
            case "cargo.toml":          return "rust";
            case "dockerfile":          return "docker";
            case "docker-compose.yml":
            case "docker-compose.yaml": return "docker";
            case ".gitignore":          return "git";
            default:                    return "";
        }
    }

    // ====================================================
    // CLI MODU (Yerel Docker)
    // ====================================================

    public List<Map<String, String>> listContainersCli() throws Exception {
        List<Map<String, String>> result = new ArrayList<>();
        ProcessBuilder pb = new ProcessBuilder("docker", "ps", "--format", "{{.ID}}|{{.Names}}|{{.Image}}");
        pb.redirectErrorStream(true);
        Process proc = pb.start();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length >= 2) {
                    Map<String, String> c = new LinkedHashMap<>();
                    c.put("id",    parts[0].trim());
                    c.put("name",  parts[1].trim());
                    c.put("image", parts.length > 2 ? parts[2].trim() : "");
                    result.add(c);
                }
            }
        }
        proc.waitFor();
        return result;
    }

    public List<Project> scanContainerCli(String containerId, String containerName, String remotePath) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(
                "docker", "exec", containerId,
                "find", remotePath, "-type", "f", "-not", "-path", "*/.*",
                "-not", "-path", "*/node_modules/*", "-not", "-path", "*/vendor/*",
                "-not", "-path", "*/__pycache__/*"
        );
        pb.redirectErrorStream(true);
        Process proc = pb.start();
        StringBuilder out = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) out.append(line).append("\n");
        }
        proc.waitFor();
        return parseProjectsFromOutput(out.toString(), remotePath, "Docker CLI: " + containerName, SourceType.DOCKER);
    }

    // ====================================================
    // REST API MODU (Yerel Docker)
    // ====================================================

    public List<Map<String, String>> listContainersApi() throws Exception {
        String response = httpGet(DOCKER_API_BASE + "/containers/json");
        List<Map<String, String>> result = new ArrayList<>();
        JsonArray arr = JsonParser.parseString(response).getAsJsonArray();
        for (JsonElement el : arr) {
            JsonObject obj = el.getAsJsonObject();
            Map<String, String> c = new LinkedHashMap<>();
            c.put("id",    obj.get("Id").getAsString().substring(0, 12));
            c.put("name",  obj.getAsJsonArray("Names").get(0).getAsString().replace("/", ""));
            c.put("image", obj.get("Image").getAsString());
            result.add(c);
        }
        return result;
    }

    public List<Project> scanContainerApi(String containerId, String containerName, String remotePath) throws Exception {
        String execCreateUrl = DOCKER_API_BASE + "/containers/" + containerId + "/exec";
        JsonObject execBody = new JsonObject();
        execBody.addProperty("AttachStdout", true);
        execBody.addProperty("AttachStderr", true);
        JsonArray cmd = new JsonArray();
        cmd.add("find"); cmd.add(remotePath); cmd.add("-type"); cmd.add("f");
        cmd.add("-not"); cmd.add("-path"); cmd.add("*/.*");
        cmd.add("-not"); cmd.add("-path"); cmd.add("*/node_modules/*");
        cmd.add("-not"); cmd.add("-path"); cmd.add("*/vendor/*");
        execBody.add("Cmd", cmd);
        String execResp = httpPost(execCreateUrl, execBody.toString());
        String execId = new Gson().fromJson(execResp, JsonObject.class).get("Id").getAsString();
        String output = httpPost(DOCKER_API_BASE + "/exec/" + execId + "/start", "{\"Detach\":false,\"Tty\":false}");
        return parseProjectsFromOutput(output, remotePath, "Docker API: " + containerName, SourceType.DOCKER);
    }

    // ====================================================
    // SSH - DOCKER MODU (VDS uzerindeki Docker)
    // ====================================================

    public List<Map<String, String>> listContainersSsh(String host, int port, String user, String pass,
                                                        String pemPath, boolean useSudo) throws Exception {
        List<Map<String, String>> result = new ArrayList<>();

        // Once sudo olmadan dene, bos/hata gelirse sudo ile tekrar dene
        String output = executeSshCommand(host, port, user, pass, pemPath,
                "docker ps --format \"{{.ID}}|{{.Names}}|{{.Image}}\"", useSudo);

        boolean hasRealContent = output.lines().anyMatch(l -> l.contains("|"));
        if (!hasRealContent && !useSudo) {
            String sudoOutput = executeSshCommand(host, port, user, pass, pemPath,
                    "docker ps --format \"{{.ID}}|{{.Names}}|{{.Image}}\"", true);
            if (sudoOutput.lines().anyMatch(l -> l.contains("|"))) {
                output = sudoOutput;
            }
        }

        if (output.contains("[HATA/UYARI]")) {
            boolean hasReal = output.lines().anyMatch(l -> l.contains("|"));
            if (!hasReal) {
                throw new Exception(output.substring(output.indexOf("[HATA/UYARI]") + 12).trim());
            }
        }

        for (String line : output.split("\n")) {
            // HATA/UYARI satirlarini atla
            if (line.startsWith("[HATA") || line.startsWith("[UYARI")) continue;
            String[] parts = line.split("\\|");
            if (parts.length >= 2 && !parts[0].trim().isEmpty()) {
                Map<String, String> c = new LinkedHashMap<>();
                c.put("id",    parts[0].trim());
                c.put("name",  parts[1].trim());
                c.put("image", parts.length > 2 ? parts[2].trim() : "");
                result.add(c);
            }
        }
        return result;
    }

    /**
     * Bir container icindeki belirli bir dizini SSH uzerinden listeler.
     */
    public List<Map<String, String>> listContainerDirectory(String host, int port, String user, String pass,
                                                             String pemPath, boolean useSudo,
                                                             String containerId, String containerPath) throws Exception {
        String command = "docker exec " + containerId + " ls -1aF " + containerPath;
        String output  = executeSshCommand(host, port, user, pass, pemPath, command, useSudo);

        // Once sudo olmadan dene, bos/hata gelirse ve useSudo false ise sudo ile tekrar dene
        if (!useSudo && (output.isEmpty() || output.contains("[HATA/UYARI]"))) {
            String sudoOutput = executeSshCommand(host, port, user, pass, pemPath, command, true);
            if (!sudoOutput.contains("[HATA/UYARI]")) {
                output = sudoOutput;
            }
        }

        if (output.contains("[HATA/UYARI]")) {
            throw new Exception(output.substring(output.indexOf("[HATA/UYARI]") + 12).trim());
        }

        Set<String> lowerNames = new HashSet<>();
        List<Map<String, String>> result = new ArrayList<>();

        for (String rawName : output.split("\n")) {
            String name = rawName.trim();
            if (name.isEmpty() || name.equals(".") || name.equals("..") ||
                    name.equals("./") || name.equals("../")) continue;

            boolean isDir = name.endsWith("/");
            if (name.endsWith("/") || name.endsWith("*") || name.endsWith("@")) {
                name = name.substring(0, name.length() - 1);
            }
            lowerNames.add(name.toLowerCase());

            if (name.startsWith(".")) continue; // gizli dosyalari gosterme

            Map<String, String> entry = new LinkedHashMap<>();
            entry.put("name", name);
            String fullPath = containerPath.endsWith("/") ? containerPath + name : containerPath + "/" + name;
            entry.put("path", fullPath);
            entry.put("isDir", isDir ? "true" : "false");
            result.add(entry);
        }

        String hint = detectHintFromFileList(lowerNames);
        for (Map<String, String> e : result) {
            e.put("currentDirHint", hint);
        }
        return result;
    }

    /**
     * VDS'deki bir Docker container'ini SSH uzerinden tarar.
     */
    public List<Project> scanContainerSsh(String host, int port, String user, String pass, String pemPath,
                                           String containerId, String containerName, String remotePath,
                                           boolean useSudo) throws Exception {
        String command = "docker exec " + containerId + " find " + remotePath +
                " -type f -not -path \"*/.*\" -not -path \"*/node_modules/*\"" +
                " -not -path \"*/vendor/*\" -not -path \"*/__pycache__/*\"" +
                " -not -path \"*/dist/*\" -not -path \"*/target/*\" -not -path \"*/build/*\"";
        String output = executeSshCommand(host, port, user, pass, pemPath, command, useSudo);

        if (output.contains("[HATA/UYARI]")) {
            boolean hasFiles = output.lines().anyMatch(l -> l.trim().startsWith("/"));
            if (!hasFiles) {
                throw new Exception(output.substring(output.indexOf("[HATA/UYARI]") + 12).trim());
            }
        }
        return parseProjectsFromOutput(output, remotePath, "Docker SSH: " + containerName, SourceType.DOCKER,
                host, String.valueOf(port), user, pass, pemPath, containerId, containerName);
    }

    // ====================================================
    // SSH - KLASOR MODU (VDS uzerinde direkt klasor)
    // ====================================================

    public List<Map<String, String>> listRemoteFoldersSsh(String host, int port, String user, String pass,
                                                           String pemPath, String remotePath, boolean useSudo) throws Exception {
        List<Map<String, String>> result = new ArrayList<>();
        String command = "find " + remotePath + " -mindepth 1 -maxdepth 1 -type d";
        String output = executeSshCommand(host, port, user, pass, pemPath, command, useSudo);

        if (output.contains("[HATA/UYARI]")) {
            throw new Exception(output.substring(output.indexOf("[HATA/UYARI]") + 12).trim());
        }
        for (String line : output.split("\n")) {
            String folderPath = line.trim();
            if (!folderPath.isEmpty() && folderPath.startsWith("/")) {
                Map<String, String> c = new LinkedHashMap<>();
                c.put("id",    folderPath);
                c.put("name",  folderPath.substring(folderPath.lastIndexOf('/') + 1));
                c.put("image", "Klasor");
                result.add(c);
            }
        }
        return result;
    }

    public List<Project> scanRemoteFolderSsh(String host, int port, String user, String pass, String pemPath,
                                               String remotePath, boolean useSudo) throws Exception {
        String command = "find " + remotePath +
                " -type f -not -path \"*/.*\" -not -path \"*/node_modules/*\"" +
                " -not -path \"*/vendor/*\" -not -path \"*/__pycache__/*\"" +
                " -not -path \"*/dist/*\" -not -path \"*/target/*\" -not -path \"*/build/*\"" +
                " -not -path \"*/venv/*\" -not -path \"*/.venv/*\" -not -path \"*/.next/*\"";
        String output = executeSshCommand(host, port, user, pass, pemPath, command, useSudo);

        if (output.contains("[HATA/UYARI]")) {
            boolean hasFiles = output.lines().anyMatch(l -> l.trim().startsWith("/"));
            if (!hasFiles) {
                throw new Exception(output.substring(output.indexOf("[HATA/UYARI]") + 12).trim());
            }
        }
        return parseProjectsFromOutput(output, remotePath, "SSH: " + host, SourceType.SSH,
                host, String.valueOf(port), user, pass, pemPath, null, null);
    }

    // ====================================================
    // PROJE TESPIT MOTORU
    // ====================================================

    private static class DirNode {
        String name;
        String fullPath;
        List<String> files = new ArrayList<>();
        Map<String, DirNode> subDirs = new LinkedHashMap<>();

        DirNode(String name, String fullPath) {
            this.name     = name;
            this.fullPath = fullPath;
        }
    }

    private List<Project> parseProjectsFromOutput(String output, String remotePath, String prefix, SourceType sourceType) {
        return parseProjectsFromOutput(output, remotePath, prefix, sourceType, null, null, null, null, null, null, null);
    }

    private List<Project> parseProjectsFromOutput(String output, String remotePath, String prefix, SourceType sourceType,
                                                  String sshHost, String sshPort, String sshUser, String sshPass, String sshPemPath,
                                                  String containerId, String containerName) {
        DirNode rootNode = new DirNode("root", remotePath);

        for (String line : output.split("\n")) {
            String filePath = line.trim();
            if (filePath.isEmpty() || !filePath.startsWith("/")) continue;
            if (!filePath.startsWith(remotePath)) continue;

            String relPath = filePath.substring(remotePath.length());
            if (relPath.startsWith("/")) relPath = relPath.substring(1);

            String[] parts = relPath.split("/");
            DirNode current = rootNode;
            boolean skip = false;

            for (int i = 0; i < parts.length - 1; i++) {
                String part = parts[i];
                if (part.isEmpty()) continue;
                if (IGNORED_DIRS.contains(part.toLowerCase())) { skip = true; break; }
                current.subDirs.putIfAbsent(part, new DirNode(part,
                        current.fullPath + (current.fullPath.endsWith("/") ? "" : "/") + part));
                current = current.subDirs.get(part);
            }

            if (skip) continue;
            String fileName = parts[parts.length - 1];
            if (!fileName.isEmpty()) current.files.add(fileName);
        }

        List<Project> projects = new ArrayList<>();
        findProjectsInTree(rootNode, projects, prefix, sourceType,
                sshHost, sshPort, sshUser, sshPass, sshPemPath, containerId, containerName);
        return projects;
    }

    private void findProjectsInTree(DirNode node, List<Project> projects, String prefix, SourceType sourceType,
                                     String sshHost, String sshPort, String sshUser, String sshPass, String sshPemPath,
                                     String containerId, String containerName) {
        if (node == null || IGNORED_DIRS.contains(node.name.toLowerCase())) return;

        boolean hasStrong = false;
        int codeFileCount = 0;

        for (String file : node.files) {
            String lower = file.toLowerCase();
            if (STRONG_PROJECT_FILES.contains(lower) ||
                lower.endsWith(".csproj") || lower.endsWith(".sln") || lower.endsWith(".xcodeproj")) {
                hasStrong = true;
            }
            if (getLanguageForFile(lower) != null) codeFileCount++;
        }

        for (String subDir : node.subDirs.keySet()) {
            if (STRONG_PROJECT_DIRS.contains(subDir.toLowerCase())) hasStrong = true;
        }

        if (hasStrong) {
            Project p = new Project(prefix + " / " + node.fullPath, node.fullPath, sourceType,
                    sshHost, sshPort, sshUser, sshPass, sshPemPath, containerId, containerName);
            collectAllFiles(node, p);
            if (p.getSourceFiles().isEmpty()) {
                for (String file : node.files) {
                    String lower = file.toLowerCase();
                    if (STRONG_PROJECT_FILES.contains(lower)) {
                        p.addSourceFile(new java.io.File(
                                node.fullPath + (node.fullPath.endsWith("/") ? "" : "/") + file));
                        p.addLanguage("Config");
                        break;
                    }
                }
            }
            if (!p.getSourceFiles().isEmpty()) projects.add(p);
            return; // Bu dalda baska proje arama
        }

        // Zayif gosterge: en az 3 kod dosyasi, ama alt klasorde guclu gosterge yoksa
        if (codeFileCount >= 3 && !anySubHasStrong(node)) {
            Project p = new Project(prefix + " / " + node.fullPath, node.fullPath, sourceType,
                    sshHost, sshPort, sshUser, sshPass, sshPemPath, containerId, containerName);
            collectAllFiles(node, p);
            if (!p.getSourceFiles().isEmpty()) { projects.add(p); return; }
        }

        for (DirNode child : node.subDirs.values()) {
            findProjectsInTree(child, projects, prefix, sourceType,
                    sshHost, sshPort, sshUser, sshPass, sshPemPath, containerId, containerName);
        }
    }

    private boolean anySubHasStrong(DirNode node) {
        for (DirNode child : node.subDirs.values()) {
            if (IGNORED_DIRS.contains(child.name.toLowerCase())) continue;
            for (String file : child.files) {
                String lower = file.toLowerCase();
                if (STRONG_PROJECT_FILES.contains(lower) ||
                    lower.endsWith(".csproj") || lower.endsWith(".sln")) return true;
            }
            for (String sub : child.subDirs.keySet()) {
                if (STRONG_PROJECT_DIRS.contains(sub.toLowerCase())) return true;
            }
            if (anySubHasStrong(child)) return true;
        }
        return false;
    }

    private void collectAllFiles(DirNode node, Project project) {
        if (IGNORED_DIRS.contains(node.name.toLowerCase())) return;
        for (String file : node.files) {
            String lang = getLanguageForFile(file.toLowerCase());
            if (lang != null) {
                project.addLanguage(lang);
                project.addSourceFile(new java.io.File(
                        node.fullPath + (node.fullPath.endsWith("/") ? "" : "/") + file));
            }
        }
        for (DirNode child : node.subDirs.values()) collectAllFiles(child, project);
    }

    // ====================================================
    // DIL TESPITI
    // ====================================================

    private String getLanguageForFile(String lower) {
        if (lower.endsWith(".java"))   return "Java";
        if (lower.endsWith(".py"))     return "Python";
        if (lower.endsWith(".js"))     return "JavaScript";
        if (lower.endsWith(".ts"))     return "TypeScript";
        if (lower.endsWith(".go"))     return "Go";
        if (lower.endsWith(".cs"))     return "C#";
        if (lower.endsWith(".cpp") || lower.endsWith(".cc")) return "C++";
        if (lower.endsWith(".c") || lower.endsWith(".h")) return "C";
        if (lower.endsWith(".rs"))     return "Rust";
        if (lower.endsWith(".kt"))     return "Kotlin";
        if (lower.endsWith(".rb"))     return "Ruby";
        if (lower.endsWith(".php"))    return "PHP";
        if (lower.endsWith(".swift"))  return "Swift";
        if (lower.endsWith(".yml") || lower.endsWith(".yaml")) return "YAML/Docker";
        if (lower.endsWith(".html") || lower.endsWith(".css") || lower.endsWith(".scss")) return "Web (HTML/CSS)";
        if (lower.endsWith(".sh"))     return "Shell";
        if (lower.equals("dockerfile")) return "Dockerfile";
        return null;
    }

    // ====================================================
    // HTTP YARDIMCI METODLAR
    // ====================================================

    private String httpGet(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(10000);
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }

    private String httpPost(String urlStr, String body) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(15000);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes("utf-8"));
        }
        InputStream stream = conn.getResponseCode() < 300 ? conn.getInputStream() : conn.getErrorStream();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(stream, "utf-8"))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append("\n");
            return sb.toString();
        }
    }
}
