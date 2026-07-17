package com.smartproject.db;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import com.smartproject.model.Project;
import org.bson.Document;
import java.util.*;

public class MongoDatabaseProvider implements DatabaseProvider {

    private String connectionUri;
    private String dbName;
    private String collectionName;
    private MongoClient mongoClient;
    private MongoCollection<Document> collection;

    public MongoDatabaseProvider(String connectionUri, String dbName, String collectionName) {
        this.connectionUri = connectionUri;
        this.dbName = dbName != null && !dbName.trim().isEmpty() ? dbName : "smartproject";
        this.collectionName = collectionName != null && !collectionName.trim().isEmpty() ? collectionName : "projects";
    }

    @Override
    public void initialize() throws Exception {
        if (connectionUri == null || connectionUri.trim().isEmpty()) {
            throw new IllegalArgumentException("MongoDB URI bos birakilamaz!");
        }
        mongoClient = MongoClients.create(connectionUri.trim());
        MongoDatabase database = mongoClient.getDatabase(dbName.trim());
        collection = database.getCollection(collectionName.trim());
    }

    private Document toDocument(ProjectRepository.ProjectEntry entry) {
        Document doc = new Document();
        doc.append("_id", entry.id);
        doc.append("displayName", entry.displayName);
        doc.append("absolutePath", entry.absolutePath);
        doc.append("source", entry.source);
        doc.append("scanDate", entry.scanDate);
        doc.append("languages", entry.languages);
        doc.append("tags", entry.tags);
        doc.append("description", entry.description);
        doc.append("fileCount", entry.fileCount);

        doc.append("sshHost", entry.sshHost);
        doc.append("sshPort", entry.sshPort);
        doc.append("sshUser", entry.sshUser);
        doc.append("sshPass", entry.sshPass);
        doc.append("sshPemPath", entry.sshPemPath);
        doc.append("containerId", entry.containerId);
        doc.append("containerName", entry.containerName);

        doc.append("uniqueKey", getUniqueKeyOfEntry(entry));
        return doc;
    }

    private ProjectRepository.ProjectEntry fromDocument(Document doc) {
        if (doc == null) return null;
        ProjectRepository.ProjectEntry entry = new ProjectRepository.ProjectEntry();
        entry.id = doc.getString("_id");
        entry.displayName = doc.getString("displayName");
        entry.absolutePath = doc.getString("absolutePath");
        entry.source = doc.getString("source");
        entry.scanDate = doc.getString("scanDate");
        entry.languages = doc.getList("languages", String.class);
        entry.tags = doc.getList("tags", String.class);
        entry.description = doc.getString("description");
        
        Integer fc = doc.getInteger("fileCount");
        entry.fileCount = fc != null ? fc : 0;

        entry.sshHost = doc.getString("sshHost");
        entry.sshPort = doc.getString("sshPort");
        entry.sshUser = doc.getString("sshUser");
        entry.sshPass = doc.getString("sshPass");
        entry.sshPemPath = doc.getString("sshPemPath");
        entry.containerId = doc.getString("containerId");
        entry.containerName = doc.getString("containerName");
        return entry;
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
        entry.sshPass       = project.getSshPass();
        entry.sshPemPath    = project.getSshPemPath();
        entry.containerId   = project.getContainerId();
        entry.containerName = project.getContainerName();

        Document doc = toDocument(entry);
        String uniqueKey = project.getUniqueKey();
        collection.replaceOne(Filters.eq("uniqueKey", uniqueKey), doc, new ReplaceOptions().upsert(true));
    }

    @Override
    public void updateTags(String projectId, List<String> newTags) throws Exception {
        collection.updateOne(Filters.eq("_id", projectId), new Document("$set", new Document("tags", newTags)));
    }

    @Override
    public void updateDescription(String projectId, String description) throws Exception {
        collection.updateOne(Filters.eq("_id", projectId), new Document("$set", new Document("description", description)));
    }

    @Override
    public void deleteById(String projectId) throws Exception {
        collection.deleteOne(Filters.eq("_id", projectId));
    }

    @Override
    public List<ProjectRepository.ProjectEntry> getAllEntries() throws Exception {
        List<ProjectRepository.ProjectEntry> list = new ArrayList<>();
        for (Document doc : collection.find()) {
            list.add(fromDocument(doc));
        }
        return list;
    }

    @Override
    public void clear() throws Exception {
        collection.deleteMany(new Document());
    }
}
