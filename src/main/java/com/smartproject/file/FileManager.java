package com.smartproject.file;

import com.smartproject.model.Project;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class FileManager {

    private static final String MARKER_FOLDER_NAME = ".spm";

    public void saveAnalysisResult(Project project, String markdownContent) throws IOException {
        File markerFolder = project.getSpmFolder();

        // Eğer klasör yoksa oluştur
        if (!markerFolder.exists()) {
            markerFolder.mkdirs();
        }

        // README.md dosyasını yaz
        File readmeFile = new File(markerFolder, "README.md");
        try (FileWriter writer = new FileWriter(readmeFile)) {
            writer.write(markdownContent);
        }
    }
}
