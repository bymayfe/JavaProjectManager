package com.smartproject.ai;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.smartproject.model.Project;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.google.genai.Client;

public class AIAnalyzer {

    // README.md olustur
    public String generateReadme(Project project, String service, String apiKey, String geminiModel, String gptApiUrl, String gptModel, String githubUser) throws Exception {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("API Key bos olamaz!");
        }

        String author = (githubUser == null || githubUser.trim().isEmpty()) ? "Gelistirici" : githubUser;

        StringBuilder fileNamesList = new StringBuilder();
        for (File f : project.getSourceFiles()) {
            fileNamesList.append("- ").append(f.getName()).append("\n");
        }

        String prompt =
            "Sen uzman bir yazilimcisin. Sana bir projenin kullandigi programlama dillerini " +
            "ve icindeki dosyalari verecegim. Bu proje icin detayli, profesyonel bir README.md " +
            "olustur. Turkce yaz.\n\n" +
            "Proje Adi: " + project.getDisplayName() + "\n" +
            "Kullanilan Diller: " + String.join(", ", project.getLanguagesUsed()) + "\n" +
            "Dosyalar:\n" + fileNamesList + "\n\n" +
            "Lutfen asagidaki KURALLARA KESINLIKLE UYARAK Markdown formatinda bir README yaz:\n" +
            "1. Giris, Ozellikler ve Nasil Calistirilir kisimlarini icersin.\n" +
            "2. Projeyi gelistiren kisi (Yazar/Gelistirici) olarak SADECE '" + author + "' ismini kullan. Baska hicbir sahte isim (Ayse, Ahmet vb.) uydurma!\n" +
            "3. Kafandan sahte Github linkleri veya sahte Lisanslar (GNU, MIT vb.) uydurma. Bilmiyorsan o kisimlari hic ekleme.\n" +
            "4. Sadece sana verilen dosya isimlerinden mantikli sonuclar cikarip aciklamalar yaz.";

        return sendRequest(prompt, service, apiKey, geminiModel, gptApiUrl, gptModel);
    }

    /**
     * Projeyi analiz edip 3-5 adet kisa etiket (tag) uretir.
     * Donus: ["web", "java", "gui", "proje-yonetimi"] gibi liste.
     */
    public List<String> generateTags(Project project, String service, String apiKey, String geminiModel, String gptApiUrl, String gptModel) throws Exception {
        if (apiKey == null || apiKey.trim().isEmpty()) return new ArrayList<>();

        StringBuilder fileNamesList = new StringBuilder();
        for (File f : project.getSourceFiles()) {
            fileNamesList.append(f.getName()).append(", ");
        }

        String prompt =
            "Asagidaki yazilim projesini incele ve projeyi en iyi tanımlayan 3 ila 5 adet kisa etiket (tag) olustur.\n" +
            "Sadece etiketleri virgülle ayrilmis sekilde yaz, baska hicbir sey yazma.\n" +
            "Ornek: web, java, gui, proje-yonetimi\n\n" +
            "Proje Adi: " + project.getDisplayName() + "\n" +
            "Kullanilan Diller: " + String.join(", ", project.getLanguagesUsed()) + "\n" +
            "Dosyalar: " + fileNamesList;

        String raw = sendRequest(prompt, service, apiKey, geminiModel, gptApiUrl, gptModel);

        // "web, java, gui" satirini parse et
        List<String> tags = new ArrayList<>();
        for (String tag : raw.split(",")) {
            String t = tag.trim()
                          .toLowerCase()
                          .replaceAll("[^a-z0-9çğışöü\\-]", ""); // Duzeltilmis regex
            if (!t.isEmpty() && t.length() <= 30) tags.add(t);
        }
        return tags;
    }

    public String sendRequest(final String prompt, String service, final String apiKey, final String geminiModel, final String gptApiUrl, final String gptModel) throws Exception {
        JsonArray messages = new JsonArray();
        JsonObject msg = new JsonObject();
        msg.addProperty("role", "user");
        msg.addProperty("content", prompt);
        messages.add(msg);
        return sendRequest(messages, service, apiKey, geminiModel, gptApiUrl, gptModel);
    }

    public String sendRequest(final JsonArray messages, String service, final String apiKey, final String geminiModel, final String gptApiUrl, final String gptModel) throws Exception {
        if ("gemini".equalsIgnoreCase(service)) {
            // Gemini icin flat text prompt olustur
            StringBuilder flatPrompt = new StringBuilder();
            for (com.google.gson.JsonElement el : messages) {
                JsonObject obj = el.getAsJsonObject();
                String role = obj.get("role").getAsString();
                String content = obj.get("content").getAsString();
                if ("system".equalsIgnoreCase(role)) {
                    flatPrompt.append("Sistem Talimati:\n").append(content).append("\n\n");
                } else if ("user".equalsIgnoreCase(role)) {
                    flatPrompt.append("Kullanici: ").append(content).append("\n");
                } else {
                    flatPrompt.append("Asistan: ").append(content).append("\n");
                }
            }
            flatPrompt.append("Asistan: ");
            
            final String model = (geminiModel == null || geminiModel.trim().isEmpty()) ? "gemini-2.5-flash" : geminiModel.trim();
            return callWithRetry(() -> sendGeminiWithSdk(flatPrompt.toString(), model, apiKey));
        } else {
            String apiUrl;
            String model;
            if ("gpt".equalsIgnoreCase(service)) {
                apiUrl = "https://api.openai.com/v1/chat/completions";
                model = "gpt-4o-mini";
            } else if ("custom".equalsIgnoreCase(service)) {
                apiUrl = (gptApiUrl == null || gptApiUrl.trim().isEmpty()) 
                        ? "https://api.openai.com/v1/chat/completions" : gptApiUrl.trim();
                model = (gptModel == null || gptModel.trim().isEmpty()) 
                        ? "gpt-4o-mini" : gptModel.trim();
            } else {
                // Groq — ucretlsiz tier'in en iyi modeli
                apiUrl = "https://api.groq.com/openai/v1/chat/completions";
                model = "llama-3.3-70b-versatile";
            }
            return callWithRetry(() -> sendOpenAiCompatibleRequest(apiUrl, model, messages, apiKey));
        }
    }

    private String sendGeminiWithSdk(String prompt, String model, String apiKey) throws Exception {
        Client client = Client.builder().apiKey(apiKey).build();
        com.google.genai.types.GenerateContentResponse response = client.models.generateContent(model, prompt, null);
        String result = response.text();
        if (result == null || result.isEmpty()) {
            throw new Exception("Gemini API bos cevap dondu!");
        }
        return result;
    }

    private String sendOpenAiCompatibleRequest(String apiUrl, String model, JsonArray messages, String apiKey) throws Exception {
        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(30000);

        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        body.add("messages", messages);

        Gson gson = new Gson();
        try (OutputStream os = conn.getOutputStream()) {
            os.write(gson.toJson(body).getBytes("utf-8"));
        }

        int responseCode = conn.getResponseCode();
        if (responseCode == 200) {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), "utf-8"))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) response.append(line.trim());
                JsonObject jsonResponse = gson.fromJson(response.toString(), JsonObject.class);
                return jsonResponse.getAsJsonArray("choices")
                        .get(0).getAsJsonObject()
                        .getAsJsonObject("message")
                        .get("content").getAsString();
            }
        } else {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getErrorStream(), "utf-8"))) {
                StringBuilder err = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) err.append(line.trim());
                throw new Exception("API Hatasi: " + responseCode + " - " + err);
            }
        }
    }

    private interface ApiCallable {
        String call() throws Exception;
    }

    private String callWithRetry(ApiCallable callable) throws Exception {
        int maxRetries = 3;
        long delayMs = 2000;
        double backoffFactor = 2.0;

        Exception lastException = null;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return callable.call();
            } catch (Exception e) {
                lastException = e;
                e.printStackTrace();

                String errorClass = e.getClass().getName().toLowerCase();
                String errorMsg   = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
                // Cause'u da kontrol et (Gemini SDK wrapped exception fırlatabilir)
                String causeMsg   = "";
                if (e.getCause() != null) {
                    causeMsg = e.getCause().toString().toLowerCase();
                    if (e.getCause().getMessage() != null) {
                        causeMsg += " " + e.getCause().getMessage().toLowerCase();
                    }
                }
                String allMsg = errorMsg + " " + causeMsg;

                boolean isRateLimit = allMsg.contains("429") ||
                                     allMsg.contains("rate limit") ||
                                     allMsg.contains("rate_limit") ||
                                     allMsg.contains("quota") ||
                                     allMsg.contains("too many requests") ||
                                     allMsg.contains("resource_exhausted"); // Gemini SDK kodu

                boolean isNetworkError = errorClass.contains("io") ||
                                         errorClass.contains("timeout") ||
                                         errorClass.contains("connect") ||
                                         errorClass.contains("runtime") || // Gemini SDK RuntimeException
                                         allMsg.contains("failed to execute http request") ||
                                         allMsg.contains("timeout") ||
                                         allMsg.contains("connection") ||
                                         allMsg.contains("socket");

                boolean shouldRetry = isRateLimit || isNetworkError;

                if (attempt == maxRetries || !shouldRetry) {
                    throw e;
                }

                long jitter     = (long) (Math.random() * 500);
                long sleepTime  = delayMs + jitter;
                String errorType = isRateLimit ? "Rate Limit (429/Quota/ResourceExhausted)" : "Gecici Ag/IO Hatasi";
                System.out.println("API Hatasi [" + errorType + "] algilandi. "
                        + sleepTime + "ms sonra tekrar denenecek (Deneme " + attempt + "/" + maxRetries + ").");
                Thread.sleep(sleepTime);
                delayMs = (long) (delayMs * backoffFactor);
            }
        }
        throw lastException != null ? lastException
                : new Exception("Bilinmeyen bir hata nedeniyle API cagrisi basarisiz oldu.");
    }
}
