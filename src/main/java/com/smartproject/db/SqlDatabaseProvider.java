package com.smartproject.db;

import com.smartproject.model.Project;
import java.sql.*;
import java.util.*;

public class SqlDatabaseProvider implements DatabaseProvider {

    private String jdbcUrl;
    private String driverClass;
    private String username;
    private String password;

    public SqlDatabaseProvider(String jdbcUrl, String driverClass, String username, String password) {
        this.jdbcUrl = jdbcUrl;
        this.driverClass = driverClass;
        this.username = username;
        this.password = password;
    }

    @Override
    public void initialize() throws Exception {
        if (jdbcUrl == null || jdbcUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("JDBC URL bos birakilamaz!");
        }
        if (driverClass != null && !driverClass.trim().isEmpty()) {
            Class.forName(driverClass.trim());
        } else {
            if (jdbcUrl.contains("sqlite")) {
                Class.forName("org.xerial.sqlite.JDBC");
            } else if (jdbcUrl.contains("mysql")) {
                Class.forName("com.mysql.cj.jdbc.Driver");
            }
        }

        try (Connection conn = getConnection()) {
            createTableIfNotExists(conn);
        }
    }

    private Connection getConnection() throws SQLException {
        if (username != null && !username.trim().isEmpty()) {
            return DriverManager.getConnection(jdbcUrl, username, password);
        } else {
            return DriverManager.getConnection(jdbcUrl);
        }
    }

    private void createTableIfNotExists(Connection conn) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS projects (" +
                "id VARCHAR(50) PRIMARY KEY, " +
                "displayName VARCHAR(255), " +
                "absolutePath VARCHAR(500), " +
                "source VARCHAR(50), " +
                "scanDate VARCHAR(50), " +
                "languages VARCHAR(1000), " +
                "tags VARCHAR(1000), " +
                "description TEXT, " +
                "fileCount INT, " +
                "sshHost VARCHAR(255), " +
                "sshPort VARCHAR(50), " +
                "sshUser VARCHAR(255), " +
                "sshPass VARCHAR(255), " +
                "sshPemPath VARCHAR(500), " +
                "containerId VARCHAR(255), " +
                "containerName VARCHAR(255), " +
                "uniqueKey VARCHAR(500) UNIQUE" +
                ")";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    @Override
    public void saveProject(Project project) throws Exception {
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

        entry.sshHost       = project.getSshHost();
        entry.sshPort       = project.getSshPort();
        entry.sshUser       = project.getSshUser();
        entry.sshPass       = null;    // Guvenlik: sifre DB'ye yazilmaz
        entry.sshPemPath    = null;    // Guvenlik: PEM yolu DB'ye yazilmaz
        entry.containerId   = project.getContainerId();
        entry.containerName = project.getContainerName();

        String uniqueKey = project.getUniqueKey();
        String languagesStr = String.join(",", entry.languages);
        String tagsStr = String.join(",", entry.tags);

        try (Connection conn = getConnection()) {
            try (PreparedStatement deleteStmt = conn.prepareStatement("DELETE FROM projects WHERE uniqueKey = ?")) {
                deleteStmt.setString(1, uniqueKey);
                deleteStmt.executeUpdate();
            }

            String insertSql = "INSERT INTO projects (id, displayName, absolutePath, source, scanDate, languages, tags, " +
                    "description, fileCount, sshHost, sshPort, sshUser, sshPass, sshPemPath, containerId, containerName, uniqueKey) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                ps.setString(1, entry.id);
                ps.setString(2, entry.displayName);
                ps.setString(3, entry.absolutePath);
                ps.setString(4, entry.source);
                ps.setString(5, entry.scanDate);
                ps.setString(6, languagesStr);
                ps.setString(7, tagsStr);
                ps.setString(8, entry.description);
                ps.setInt(9, entry.fileCount);
                ps.setString(10, entry.sshHost);
                ps.setString(11, entry.sshPort);
                ps.setString(12, entry.sshUser);
                ps.setString(13, entry.sshPass);
                ps.setString(14, entry.sshPemPath);
                ps.setString(15, entry.containerId);
                ps.setString(16, entry.containerName);
                ps.setString(17, uniqueKey);
                ps.executeUpdate();
            }
        }
    }

    @Override
    public void updateTags(String projectId, List<String> newTags) throws Exception {
        String tagsStr = String.join(",", newTags);
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE projects SET tags = ? WHERE id = ?")) {
            ps.setString(1, tagsStr);
            ps.setString(2, projectId);
            ps.executeUpdate();
        }
    }

    @Override
    public void updateDescription(String projectId, String description) throws Exception {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE projects SET description = ? WHERE id = ?")) {
            ps.setString(1, description);
            ps.setString(2, projectId);
            ps.executeUpdate();
        }
    }

    @Override
    public void deleteById(String projectId) throws Exception {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM projects WHERE id = ?")) {
            ps.setString(1, projectId);
            ps.executeUpdate();
        }
    }

    @Override
    public List<ProjectRepository.ProjectEntry> getAllEntries() throws Exception {
        List<ProjectRepository.ProjectEntry> list = new ArrayList<>();
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM projects")) {
            while (rs.next()) {
                ProjectRepository.ProjectEntry entry = new ProjectRepository.ProjectEntry();
                entry.id = rs.getString("id");
                entry.displayName = rs.getString("displayName");
                entry.absolutePath = rs.getString("absolutePath");
                entry.source = rs.getString("source");
                entry.scanDate = rs.getString("scanDate");
                
                String langStr = rs.getString("languages");
                if (langStr != null && !langStr.trim().isEmpty()) {
                    entry.languages = new ArrayList<>(Arrays.asList(langStr.split(",")));
                } else {
                    entry.languages = new ArrayList<>();
                }

                String tagsStr = rs.getString("tags");
                if (tagsStr != null && !tagsStr.trim().isEmpty()) {
                    entry.tags = new ArrayList<>(Arrays.asList(tagsStr.split(",")));
                } else {
                    entry.tags = new ArrayList<>();
                }

                entry.description = rs.getString("description");
                entry.fileCount = rs.getInt("fileCount");
                entry.sshHost = rs.getString("sshHost");
                entry.sshPort = rs.getString("sshPort");
                entry.sshUser = rs.getString("sshUser");
                entry.sshPass    = null; // Guvenlik: DB'den okunmaz
                entry.sshPemPath = null; // Guvenlik: DB'den okunmaz
                entry.containerId = rs.getString("containerId");
                entry.containerName = rs.getString("containerName");
                list.add(entry);
            }
        }
        return list;
    }

    @Override
    public void clear() throws Exception {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM projects");
        }
    }
}
