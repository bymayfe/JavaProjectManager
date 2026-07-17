package com.smartproject.git;

import com.smartproject.model.Project;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.transport.RemoteConfig;

import java.io.File;
import java.util.List;

public class GitManager {

    public boolean hasRemote(Project project) {
        File repoDir = project.getProjectFolder();
        File gitDir = new File(repoDir, ".git");
        if (!gitDir.exists()) return false;
        
        try (Git git = Git.open(repoDir)) {
            return !git.remoteList().call().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    public String getRemoteUrl(Project project) {
        File repoDir = project.getProjectFolder();
        File gitDir = new File(repoDir, ".git");
        if (!gitDir.exists()) return null;
        
        try (Git git = Git.open(repoDir)) {
            List<RemoteConfig> remotes = git.remoteList().call();
            for (RemoteConfig remote : remotes) {
                if ("origin".equals(remote.getName())) {
                    if (!remote.getURIs().isEmpty()) {
                        return remote.getURIs().get(0).toString();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getOwnerFromUrl(String url) {
        if (url == null) return null;
        url = url.trim();
        if (url.endsWith(".git")) {
            url = url.substring(0, url.length() - 4);
        }
        
        // Match https://github.com/owner/repo
        int index = url.indexOf("github.com/");
        if (index != -1) {
            String parts = url.substring(index + "github.com/".length());
            String[] split = parts.split("/");
            if (split.length > 0) return split[0];
        }
        
        // Match git@github.com:owner/repo
        index = url.indexOf("github.com:");
        if (index != -1) {
            String parts = url.substring(index + "github.com:".length());
            String[] split = parts.split("/");
            if (split.length > 0) return split[0];
        }
        
        return null;
    }

    public String commitAndPush(Project project, String username, String token, String remoteUrl) {
        File repoDir = project.getProjectFolder();

        try {
            Git git;
            File gitDir = new File(repoDir, ".git");
            if (!gitDir.exists()) {
                git = Git.init().setDirectory(repoDir).call();
            } else {
                git = Git.open(repoDir);
            }

            // Tüm değişiklikleri ekle (git add .)
            git.add().addFilepattern(".").call();

            // Commit yap
            git.commit()
               .setMessage("Smart Project Manager AI analizi eklendi")
               .setAuthor("SmartProject", "ai@smartproject.com")
               .call();

            // Remote ayarlama
            if (remoteUrl != null && !remoteUrl.trim().isEmpty()) {
                if (git.remoteList().call().isEmpty()) {
                    git.remoteAdd().setName("origin").setUri(new URIish(remoteUrl.trim())).call();
                } else {
                    // Remote zaten varsa, config üzerinden URL'i güncelle
                    org.eclipse.jgit.lib.StoredConfig config = git.getRepository().getConfig();
                    config.setString("remote", "origin", "url", remoteUrl.trim());
                    config.save();
                }
            }

            // GitHub Push işlemi
            if (username != null && !username.isEmpty() && token != null && !token.isEmpty()) {
                if (!git.remoteList().call().isEmpty()) {
                    UsernamePasswordCredentialsProvider creds = new UsernamePasswordCredentialsProvider(username, token);
                    PushCommand pushCommand = git.push().setCredentialsProvider(creds);
                    pushCommand.call();
                    git.close();
                    return "✅ Başarılı! Değişiklikler commit edildi ve GitHub'a pushlandı.";
                } else {
                    git.close();
                    return "⚠️ Commit başarılı ancak Remote URL (GitHub linki) verilmediği için push yapılamadı.";
                }
            } else {
                git.close();
                return "⚠️ Commit başarılı ancak Ayarlar'da GitHub kullanıcı adı/token olmadığı için push yapılamadı.";
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "❌ Git Hatası: " + e.getMessage();
        }
    }
}
