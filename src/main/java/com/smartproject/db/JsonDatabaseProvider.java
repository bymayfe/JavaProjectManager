package com.smartproject.db;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.smartproject.model.Project;
import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

public class JsonDatabaseProvider implements DatabaseProvider {

    private static final String DB_FOLDER = System.getProperty("user.home") + File.separator + ".smartproject";
    private static final String DB_FILE   = DB_FOLDER + File.separator + "projects_db.json";

    private List<ProjectRepository.ProjectEntry> entries = new ArrayList<>();
    private Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Override
    public void initialize() throws Exception {
        ensureDbFolder();
        load();
    }

    private void ensureDbFolder() {
        File folder = new File(DB_FOLDER);
        if (!folder.exists()) folder.mkdirs();
    }

    private void load() {
        File dbFile = new File(DB_FILE);
        if (!dbFile.exists()) return;

        try (Reader reader = new FileReader(dbFile)) {
            Type listType = new TypeToken<List<ProjectRepository.ProjectEntry>>() {}.getType();
            List<ProjectRepository.ProjectEntry> loaded = gson.fromJson(reader, listType);
            if (loaded != null) entries = loaded;
        } catch (Exception e) {
            System.err.println("JSON DB yuklenirken hata: " + e.getMessage());
            entries = new ArrayList<>();
        }
    }

    private void save() {
        try (Writer writer = new FileWriter(DB_FILE)) {
            gson.toJson(entries, writer);
        } catch (Exception e) {
            System.err.println("JSON DB kaydedilirken hata: " + e.getMessage());
        }
    }

    private String getUniqueKeyOfEntry(ProjectRepository.ProjectEntry e) {
        if ("LOCAL".equals(e.source)) {
            return "LOCAL:" + e.absolutePath;
        } else if ("SSH".equals(e.source)) {
            return "SSH:" + e.sshHost + ":" + e.sshPort + ":" + e.absolutePath;
        } else {
            return "DOCKER:" + e.sshHost + ":" + e.sshPort + ":" + e.containerId + ":" + e.absolutePath;
        }
    }

    @Override
    public void saveProject(Project project) throws Exception {
        entries.removeIf(e -> getUniqueKeyOfEntry(e).equals(project.getUniqueKey()));

        ProjectRepository.ProjectEntry entry = new ProjectRepository.ProjectEntry();
        entry.id          = project.getId();
        entry.displayName = project.getDisplayName();
        entry.absolutePath= project.getAbsolutePath();
        entry.source      = project.getSource().name();
        entry.scanDate    = project.getScanDate();
        entry.languages   = new ArrayList<>(project.getLanguagesUsed());
        entry.tags        = new ArrayList<>(project.getTags());
        entry.description = project.getDescription();
        entry.fileCount   = project.getSourceFiles().size();

        // Baglanti kimlik bilgileri: sadece host, port ve kullanici adi saklanir.
        // Sifre ve PEM yolu guvenlk nedeniyle DB'ye yazilmaz.
        entry.sshHost       = project.getSshHost();
        entry.sshPort       = project.getSshPort();
        entry.sshUser       = project.getSshUser();
        entry.sshPass       = null;       // Guvenlik: sifre DB'ye yazilmaz
        entry.sshPemPath    = null;       // Guvenlik: PEM yolu DB'ye yazilmaz
        entry.containerId   = project.getContainerId();
        entry.containerName = project.getContainerName();

        entries.add(entry);
        save();
    }

    @Override
    public void updateTags(String projectId, List<String> newTags) throws Exception {
        for (ProjectRepository.ProjectEntry e : entries) {
            if (projectId.equals(e.id)) {
                e.tags = new ArrayList<>(newTags);
                break;
            }
        }
        save();
    }

    @Override
    public void updateDescription(String projectId, String description) throws Exception {
        for (ProjectRepository.ProjectEntry e : entries) {
            if (projectId.equals(e.id)) {
                e.description = description;
                break;
            }
        }
        save();
    }

    @Override
    public void deleteById(String projectId) throws Exception {
        entries.removeIf(e -> projectId.equals(e.id));
        save();
    }

    @Override
    public List<ProjectRepository.ProjectEntry> getAllEntries() throws Exception {
        return new ArrayList<>(entries);
    }

    @Override
    public void clear() throws Exception {
        entries.clear();
        save();
    }
}
