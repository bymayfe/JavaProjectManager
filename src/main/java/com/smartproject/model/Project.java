package com.smartproject.model;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class Project {

    // Kaynak tipi: LOCAL, DOCKER, SSH
    public enum SourceType { LOCAL, DOCKER, SSH }

    private String id;                   // Benzersiz kimlik (UUID)
    private File projectFolder;
    private String displayName;
    private Set<String> languagesUsed;
    private List<File> sourceFiles;
    private List<String> tags;           // Kullanici etiketleri
    private String scanDate;             // Taranma tarihi
    private SourceType source;           // Kaynak tipi
    private String description;          // AI ozeti
    private String remotePath;           // Docker/SSH icin uzak yol
    
    // Uzak baglanti detaylari
    private String sshHost;
    private String sshPort;
    private String sshUser;
    private String sshPass;
    private String sshPemPath;
    private String containerId;
    private String containerName;

    // Local proje icin constructor
    public Project(File projectFolder) {
        this.id = UUID.randomUUID().toString();
        this.projectFolder = projectFolder;
        this.languagesUsed = new HashSet<>();
        this.sourceFiles = new ArrayList<>();
        this.tags = new ArrayList<>();
        this.source = SourceType.LOCAL;
        this.scanDate = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date());

        String parentName = projectFolder.getParentFile() != null
                ? projectFolder.getParentFile().getName() : "";
        this.displayName = parentName.isEmpty()
                ? projectFolder.getName()
                : parentName + " / " + projectFolder.getName();
    }

    // Docker / SSH icin constructor
    public Project(String displayName, String remotePath, SourceType source,
                   String sshHost, String sshPort, String sshUser, String sshPass, String sshPemPath,
                   String containerId, String containerName) {
        this.id = UUID.randomUUID().toString();
        this.displayName = displayName;
        this.remotePath = remotePath;
        this.source = source;
        this.projectFolder = null;
        this.languagesUsed = new HashSet<>();
        this.sourceFiles = new ArrayList<>();
        this.tags = new ArrayList<>();
        this.scanDate = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date());
        
        this.sshHost = sshHost;
        this.sshPort = sshPort;
        this.sshUser = sshUser;
        this.sshPass = sshPass;
        this.sshPemPath = sshPemPath;
        this.containerId = containerId;
        this.containerName = containerName;
    }

    // Database'den geri yuklemek icin constructor
    public Project(com.smartproject.db.ProjectRepository.ProjectEntry entry) {
        this.id = entry.id;
        this.displayName = entry.displayName;
        this.remotePath = entry.absolutePath;
        this.source = SourceType.valueOf(entry.source);
        this.scanDate = entry.scanDate;
        this.languagesUsed = new HashSet<>(entry.languages);
        this.tags = new ArrayList<>(entry.tags);
        this.description = entry.description;
        
        this.sshHost = entry.sshHost;
        this.sshPort = entry.sshPort;
        this.sshUser = entry.sshUser;
        this.sshPass = entry.sshPass;
        this.sshPemPath = entry.sshPemPath;
        this.containerId = entry.containerId;
        this.containerName = entry.containerName;

        if (this.source == SourceType.LOCAL) {
            this.projectFolder = new File(entry.absolutePath);
            repopulateLocalSourceFiles();
        } else {
            this.projectFolder = null;
            this.sourceFiles = new ArrayList<>();
            for (int i = 0; i < entry.fileCount; i++) {
                this.sourceFiles.add(new File("remote_placeholder_" + i));
            }
        }
    }

    public File getSpmFolder() {
        if (projectFolder != null) {
            return new File(projectFolder, ".spm");
        } else {
            String dbFolder = System.getProperty("user.home") + File.separator + ".smartproject";
            File remoteMetaDir = new File(dbFolder + File.separator + "projects" + File.separator + id);
            if (!remoteMetaDir.exists()) remoteMetaDir.mkdirs();
            return new File(remoteMetaDir, ".spm");
        }
    }

    public String getUniqueKey() {
        if (source == SourceType.LOCAL) {
            return "LOCAL:" + getAbsolutePath();
        } else if (source == SourceType.SSH) {
            return "SSH:" + sshHost + ":" + sshPort + ":" + remotePath;
        } else {
            return "DOCKER:" + sshHost + ":" + sshPort + ":" + containerId + ":" + remotePath;
        }
    }

    public void repopulateLocalSourceFiles() {
        if (projectFolder == null || !projectFolder.exists() || !projectFolder.isDirectory()) return;
        this.sourceFiles = new ArrayList<>();
        scanLocalFiles(projectFolder);
    }

    private void scanLocalFiles(File dir) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                String name = f.getName().toLowerCase();
                if (name.equals(".git") || name.equals("node_modules") || name.equals("target") ||
                    name.equals("build") || name.equals(".idea") || name.equals(".spm") || name.equals("venv")) continue;
                scanLocalFiles(f);
            } else {
                String name = f.getName().toLowerCase();
                if (name.endsWith(".java") || name.endsWith(".py") || name.endsWith(".js") || name.endsWith(".ts") ||
                    name.endsWith(".html") || name.endsWith(".css") || name.endsWith(".cs") || name.endsWith(".cpp") ||
                    name.endsWith(".c") || name.endsWith(".h") || name.endsWith(".rs") || name.endsWith(".go") ||
                    name.endsWith(".php") || name.endsWith(".rb") || name.endsWith(".sh") || name.endsWith("dockerfile") ||
                    name.endsWith(".yml") || name.endsWith(".yaml")) {
                    this.sourceFiles.add(f);
                }
            }
        }
    }

    // --- Getter / Setter ---

    public String getId() { return id; }

    public File getProjectFolder() { return projectFolder; }

    public String getDisplayName() { return displayName; }

    public Set<String> getLanguagesUsed() { return languagesUsed; }

    public void addLanguage(String language) { this.languagesUsed.add(language); }

    public List<File> getSourceFiles() { return sourceFiles; }

    public void addSourceFile(File file) { this.sourceFiles.add(file); }

    public List<String> getTags() { return tags; }

    public void addTag(String tag) {
        if (!tags.contains(tag)) tags.add(tag);
    }

    public void removeTag(String tag) { tags.remove(tag); }

    public String getScanDate() { return scanDate; }

    public SourceType getSource() { return source; }

    public String getDescription() { return description; }

    public void setDescription(String description) { this.description = description; }

    public String getRemotePath() { return remotePath; }

    public String getSshHost() { return sshHost; }
    public String getSshPort() { return sshPort; }
    public String getSshUser() { return sshUser; }
    public String getSshPass() { return sshPass; }
    public String getSshPemPath() { return sshPemPath; }
    public String getContainerId() { return containerId; }
    public String getContainerName() { return containerName; }

    public String getAbsolutePath() {
        if (projectFolder != null) return projectFolder.getAbsolutePath();
        return remotePath != null ? remotePath : "";
    }

    @Override
    public String toString() {
        String sourceTag = source == SourceType.LOCAL ? "" : " [" + source.name() + "]";
        return displayName + sourceTag + " (" + String.join(", ", languagesUsed) + ")";
    }
}
