package com.smartproject.scanner;

import com.smartproject.model.Project;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ProjectScanner {

    // İgnore edilecek, taranmayacak klasörler
    private static final List<String> IGNORED_DIRS = Arrays.asList(".git", "node_modules", "target", "build", ".idea", ".spm", "venv");

    public List<Project> scanDirectory(File rootDir) {
        List<Project> projects = new ArrayList<>();
        if (rootDir == null || !rootDir.exists() || !rootDir.isDirectory()) {
            return projects;
        }

        // Kök dizinden başlayarak projeleri derinlemesine (recursive) ara
        findProjectsRecursively(rootDir, projects);
        return projects;
    }

    private void findProjectsRecursively(File dir, List<Project> projects) {
        if (dir == null || !dir.isDirectory() || IGNORED_DIRS.contains(dir.getName())) {
            return;
        }

        if (isProjectRoot(dir)) {
            // Eğer burası bir projenin ana klasörüyse, projeyi oluştur ve alt klasörlerde başka proje arama.
            Project project = checkAndCreateProject(dir);
            if (project != null) {
                projects.add(project);
            }
        } else {
            // Eğer proje kökü değilse (örn: Masaüstü veya Period 8 klasörü), içindeki klasörleri tek tek gez.
            File[] subDirs = dir.listFiles(File::isDirectory);
            if (subDirs != null) {
                for (File subDir : subDirs) {
                    findProjectsRecursively(subDir, projects);
                }
            }
        }
    }

    private boolean isProjectRoot(File dir) {
        File[] files = dir.listFiles();
        if (files == null) return false;

        boolean hasCodeFile = false;
        for (File f : files) {
            String name = f.getName().toLowerCase();
            if (f.isDirectory()) {
                // Yaygın proje klasörleri
                if (name.equals(".git") || name.equals("src") || name.equals(".idea") || name.equals("node_modules")) {
                    return true;
                }
            } else {
                // Yaygın proje dosyaları
                if (name.equals("pom.xml") || name.equals("package.json") || name.equals("build.gradle") || name.equals("requirements.txt") || name.equals(".gitignore") || name.equals("docker-compose.yml") || name.equals("docker-compose.yaml") || name.equals("dockerfile")) {
                    return true;
                }
                // Veya klasörün doğrudan içinde kod dosyası varsa
                if (detectLanguage(name) != null) {
                    hasCodeFile = true;
                }
            }
        }
        return hasCodeFile;
    }

    private Project checkAndCreateProject(File dir) {
        Project project = new Project(dir);
        scanFilesRecursively(dir, project);
        
        // Eğer hiç kaynak kodu dosyası bulunmadıysa ama güçlü bir gösterge dosyası (package.json vb.) varsa yine de kabul et
        if (project.getSourceFiles().isEmpty()) {
            boolean hasMarker = false;
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    String name = f.getName().toLowerCase();
                    if (name.equals("pom.xml") || name.equals("package.json") || name.equals("build.gradle") ||
                            name.equals("requirements.txt") || name.equals("go.mod") || name.equals("cargo.toml") ||
                            name.equals("dockerfile")) {
                        hasMarker = true;
                        project.addSourceFile(f);
                        project.addLanguage("Config");
                        break;
                    }
                }
            }
            if (!hasMarker) return null;
        }
        return project;
    }

    private void scanFilesRecursively(File dir, Project project) {
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                if (!IGNORED_DIRS.contains(file.getName())) {
                    scanFilesRecursively(file, project);
                }
            } else {
                String language = detectLanguage(file.getName());
                if (language != null) {
                    project.addSourceFile(file);
                    project.addLanguage(language);
                }
            }
        }
    }

    private String detectLanguage(String fileName) {
        String lowerName = fileName.toLowerCase();
        if (lowerName.endsWith(".java")) return "Java";
        if (lowerName.endsWith(".py")) return "Python";
        if (lowerName.endsWith(".js") || lowerName.endsWith(".ts")) return "JavaScript/TypeScript";
        if (lowerName.endsWith(".html") || lowerName.endsWith(".css")) return "Web";
        if (lowerName.endsWith(".cpp") || lowerName.endsWith(".h")) return "C++";
        if (lowerName.endsWith(".c")) return "C";
        if (lowerName.endsWith(".cs")) return "C#";
        if (lowerName.endsWith(".php")) return "PHP";
        if (lowerName.endsWith(".go")) return "Go";
        if (lowerName.endsWith(".rb")) return "Ruby";
        if (lowerName.endsWith(".rs")) return "Rust";
        if (lowerName.endsWith(".yml") || lowerName.endsWith(".yaml")) return "YAML/Docker";
        if (lowerName.endsWith(".json")) return "JSON";
        if (lowerName.endsWith(".sh")) return "Shell";
        if (lowerName.equals("dockerfile")) return "Dockerfile";
        
        // Sadece kod dosyalarını dikkate alıyoruz, resim/txt vs atlansın
        return null;
    }
}
