package com.smartproject.scanner;

import com.smartproject.model.Project;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ProjectScanner {

    // İgnore edilecek, taranmayacak klasörler
    private static final List<String> IGNORED_DIRS = Arrays.asList(
            ".git", "node_modules", "target", "build", ".idea", ".spm", "venv", ".venv",
            "env", ".env", "site-packages", "__pycache__", ".settings", ".classpath",
            ".project", "bin", "out", "dist", ".gradle", ".next", ".nuxt", ".turbo",
            ".cache", "coverage", ".output", "static"
    );

    public List<Project> scanDirectory(File rootDir) {
        List<Project> projects = new ArrayList<>();
        if (rootDir == null || !rootDir.exists() || !rootDir.isDirectory()) {
            return projects;
        }

        findProjectsRecursively(rootDir, projects);
        return projects;
    }

    private void findProjectsRecursively(File dir, List<Project> dirProjects) {
        if (dir == null || !dir.isDirectory()) return;
        String dirName = dir.getName().toLowerCase();
        if (IGNORED_DIRS.contains(dirName)) return;

        // 1. Eger alt klasorlerinde acik (explicit) proje manifest dosyalari (package.json, pom.xml vb.) olan projeler varsa
        // bu klasor bir workspace / projeler konteyneridir. Alt klasorleri tara.
        List<File> childProjectDirs = getChildDirsWithManifests(dir);
        if (!childProjectDirs.isEmpty()) {
            File[] subDirs = dir.listFiles(File::isDirectory);
            if (subDirs != null) {
                for (File childDir : subDirs) {
                    findProjectsRecursively(childDir, dirProjects);
                }
            }
            return;
        }

        // 2. Alt klasorlerde ayri manifestli proje yoksa ve bu klasor bir proje ise (manifest'i var veya icinde kod dosyasi var)
        if (isProjectRoot(dir)) {
            Project project = checkAndCreateProject(dir);
            if (project != null) {
                dirProjects.add(project);
            }
            // Proje olarak eklendi, alt klasorlerine (components, src, providers, app vb.) proje aramak icin GIRME!
            return;
        }

        // 3. Proje kok degilse alt klasorleri tara
        File[] subDirs = dir.listFiles(File::isDirectory);
        if (subDirs != null) {
            for (File subDir : subDirs) {
                findProjectsRecursively(subDir, dirProjects);
            }
        }
    }

    /**
     * Klasorun icindeki 1 seviye alt klasorlerden hangilerinin kendine ait manifest dosyasi (package.json, pom.xml vb.) var?
     */
    private List<File> getChildDirsWithManifests(File dir) {
        List<File> list = new ArrayList<>();
        File[] subDirs = dir.listFiles(File::isDirectory);
        if (subDirs == null) return list;

        for (File subDir : subDirs) {
            String name = subDir.getName().toLowerCase();
            if (IGNORED_DIRS.contains(name)) continue;

            if (hasManifestFile(subDir)) {
                list.add(subDir);
            }
        }
        return list;
    }

    /**
     * Bir klasorde acik proje tanimlama dosyasi (package.json, pom.xml, requirements.txt, .spm vb.) var mi?
     */
    private boolean hasManifestFile(File dir) {
        File[] files = dir.listFiles();
        if (files == null) return false;

        for (File f : files) {
            String fname = f.getName().toLowerCase();
            if (f.isDirectory()) {
                if (fname.equals(".spm") || fname.equals(".git")) {
                    return true;
                }
            } else {
                if (fname.equals("pom.xml") || fname.equals("package.json") || fname.equals("build.gradle") ||
                    fname.equals("build.gradle.kts") || fname.equals("requirements.txt") || fname.equals("pyproject.toml") ||
                    fname.equals("pipfile") || fname.equals("go.mod") || fname.equals("cargo.toml") ||
                    fname.equals("dockerfile") || fname.equals("docker-compose.yml") || fname.equals("docker-compose.yaml")) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isProjectRoot(File dir) {
        if (dir == null || !dir.isDirectory()) return false;

        // Eger manifest varsa kesinlikle proje kokudur
        if (hasManifestFile(dir)) {
            return true;
        }

        // Manifest yoksa ama klasorun dogrudan icinde (src haric kendisinde) kaynak kod dosyasi varsa
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (!f.isDirectory() && detectLanguage(f.getName()) != null) {
                    return true;
                }
            }
        }
        return false;
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
