package com.smartproject.git;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class GithubAPIManager {

    private static final String GITHUB_API_URL = "https://api.github.com/user/repos";

    /**
     * GitHub üzerinde yeni bir repository oluşturur.
     * @return Oluşturulan repository'nin clone_url'sini döndürür.
     */
    public String createRepository(String repoName, boolean isPrivate, String token) throws Exception {
        URL url = new URL(GITHUB_API_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        
        // GitHub API için gerekli Header'lar
        conn.setRequestProperty("Authorization", "token " + token);
        conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("User-Agent", "SmartProjectManager");
        conn.setDoOutput(true);

        // JSON Body oluştur
        JsonObject body = new JsonObject();
        body.addProperty("name", repoName);
        body.addProperty("private", isPrivate);
        body.addProperty("auto_init", false); // Kendi projemizi göndereceğimiz için boş açılmalı

        Gson gson = new Gson();
        String jsonInputString = gson.toJson(body);

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonInputString.getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        int responseCode = conn.getResponseCode();
        // 201 Created veya 200 OK
        if (responseCode == 201 || responseCode == 200) {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), "utf-8"))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }

                JsonObject jsonResponse = gson.fromJson(response.toString(), JsonObject.class);
                return jsonResponse.get("clone_url").getAsString();
            }
        } else {
            // Hata Durumu (Örn: Repo zaten var, Token yanlış vb.)
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getErrorStream(), "utf-8"))) {
                StringBuilder errorResponse = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    errorResponse.append(responseLine.trim());
                }
                
                JsonObject errObj = gson.fromJson(errorResponse.toString(), JsonObject.class);
                String errMsg = errObj.has("message") ? errObj.get("message").getAsString() : "Bilinmeyen API Hatası";
                
                throw new Exception("GitHub Hatası (" + responseCode + "): " + errMsg);
            }
        }
    }
}
