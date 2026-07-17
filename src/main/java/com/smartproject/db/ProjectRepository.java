package com.smartproject.db;

import com.smartproject.config.ConfigManager;
import com.smartproject.model.Project;

import java.io.File;
import java.util.*;

/**
 * Projeleri veritabaninda (JSON, MongoDB veya SQL) saklar ve yonetir.
 * Aktif veritabani saglayicisi ConfigManager'dan okunur ve dynamic olarak degistirilir.
 */
public class ProjectRepository {

    // Her entry JSON'da saklanacak basit bir kayit sinifi
    public static class ProjectEntry {
        public String id;
        public String displayName;
        public String absolutePath;
        public String source;        // LOCAL, DOCKER, SSH
        public String scanDate;
        public List<String> languages;
        public List<String> tags;
        public String description;
        public int fileCount;

        // Uzak baglanti detaylari
        public String sshHost;
        public String sshPort;
        public String sshUser;
        public String sshPass;
        public String sshPemPath;
        public String containerId;
        public String containerName;

        public ProjectEntry() {
            languages = new ArrayList<>();
            tags = new ArrayList<>();
        }
    }

    private ConfigManager configManager;
    private DatabaseProvider provider;

    // Geriye uyumluluk icin bos constructor
    public ProjectRepository() {
        this(new ConfigManager());
    }

    public ProjectRepository(ConfigManager configManager) {
        this.configManager = configManager;
        refreshProvider();
    }

    /**
     * Ayarlar veya profil degistiginde veritabani baglantisini tazelemek icin kullanilir.
     */
    public synchronized void refreshProvider() {
        String dbType = configManager.getDbType();
        try {
            if ("mongodb".equalsIgnoreCase(dbType)) {
                String uri = configManager.getDbUrl();
                String dbName = configManager.getDbName();
                String collection = configManager.getDbTable();
                provider = new MongoDatabaseProvider(uri, dbName, collection);
            } else if ("sql".equalsIgnoreCase(dbType)) {
                String jdbcUrl = configManager.getDbUrl();
                String driver = configManager.getDbDriver();
                String user = configManager.getDbUser();
                String pass = configManager.getDbPass();
                provider = new SqlDatabaseProvider(jdbcUrl, driver, user, pass);
            } else {
                provider = new JsonDatabaseProvider();
            }
            provider.initialize();
        } catch (Exception e) {
            System.err.println("Veritabani saglayicisi baslatilamadi (" + dbType + "): " + e.getMessage());
            e.printStackTrace();
            // Hata durumunda local JSON'a geri don ki uygulama cokmesin
            try {
                provider = new JsonDatabaseProvider();
                provider.initialize();
            } catch (Exception ex) {
                // ignore
            }
        }
    }

    // --- Project nesnesini DB'ye ekle veya guncelle ---
    public void saveProject(Project project) {
        try {
            provider.saveProject(project);
        } catch (Exception e) {
            System.err.println("Proje kaydedilirken hata: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // --- Etiketi guncelle ve kaydet ---
    public void updateTags(String projectId, List<String> newTags) {
        try {
            provider.updateTags(projectId, newTags);
        } catch (Exception e) {
            System.err.println("Etiketler guncellenirken hata: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // --- Aciklamayi guncelle ---
    public void updateDescription(String projectId, String description) {
        try {
            provider.updateDescription(projectId, description);
        } catch (Exception e) {
            System.err.println("Aciklama guncellenirken hata: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // --- Tum DB'yi temizle ---
    public void clear() {
        try {
            provider.clear();
        } catch (Exception e) {
            System.err.println("Veritabani temizlenirken hata: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // --- Tek kaydi sil ---
    public void deleteById(String projectId) {
        try {
            provider.deleteById(projectId);
        } catch (Exception e) {
            System.err.println("Proje silinirken hata: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // --- Tum kayitlari getir ---
    public List<ProjectEntry> getAllEntries() {
        try {
            return provider.getAllEntries();
        } catch (Exception e) {
            System.err.println("Tum kayitlar getirilirken hata: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    // --- Dile gore filtrele ---
    public List<ProjectEntry> findByLanguage(String lang) {
        List<ProjectEntry> result = new ArrayList<>();
        try {
            for (ProjectEntry e : provider.getAllEntries()) {
                if (e.languages != null) {
                    for (String l : e.languages) {
                        if (l.equalsIgnoreCase(lang)) {
                            result.add(e);
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    // --- Anahtar kelimeye gore ara (isim veya aciklama) ---
    public List<ProjectEntry> findByKeyword(String keyword) {
        List<ProjectEntry> result = new ArrayList<>();
        String kw = keyword.toLowerCase();
        try {
            for (ProjectEntry e : provider.getAllEntries()) {
                boolean nameMatch = e.displayName != null && e.displayName.toLowerCase().contains(kw);
                boolean descMatch = e.description != null && e.description.toLowerCase().contains(kw);
                boolean tagMatch  = e.tags != null && e.tags.stream().anyMatch(t -> t.toLowerCase().contains(kw));
                if (nameMatch || descMatch || tagMatch) {
                    result.add(e);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    // --- DB dosyasinin veya saglayicinin turunu dondur ---
    public String getDbPath() {
        if (provider instanceof JsonDatabaseProvider) {
            return System.getProperty("user.home") + File.separator + ".smartproject" + File.separator + "projects_db.json";
        }
        return "Uzak/Sunucu Veritabani: " + configManager.getDbType().toUpperCase();
    }

    // --- Kayit sayisi ---
    public int count() {
        try {
            return provider.getAllEntries().size();
        } catch (Exception e) {
            return 0;
        }
    }
}
