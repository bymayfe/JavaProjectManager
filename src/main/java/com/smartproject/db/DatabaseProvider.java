package com.smartproject.db;

import com.smartproject.model.Project;
import java.util.List;

public interface DatabaseProvider {
    void initialize() throws Exception;
    void saveProject(Project project) throws Exception;
    void updateTags(String projectId, List<String> newTags) throws Exception;
    void updateDescription(String projectId, String description) throws Exception;
    void deleteById(String projectId) throws Exception;
    List<ProjectRepository.ProjectEntry> getAllEntries() throws Exception;
    void clear() throws Exception;
}
