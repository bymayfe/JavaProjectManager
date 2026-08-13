package com.smartproject.ai;

import com.google.gson.*;
import com.smartproject.db.ProjectRepository;
import com.smartproject.db.ProjectRepository.ProjectEntry;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * Kullanicinin sorusuna gore taranan projelerden oneri yapar.
 * Tum proje listesini baglam olarak Groq API'ye gonderir.
 */
public class ProjectAssistant {

    private ProjectRepository repository;
    private JsonArray historyMessages; // Sohbet gecmisi

    public ProjectAssistant(ProjectRepository repository) {
        this.repository = repository;
        this.historyMessages = new JsonArray();
    }

    /**
     * Kullanicinin sorusunu ve taranan proje listesini secili AI servisine gonderir.
     */
    public String ask(String userQuestion, String service, String apiKey, String geminiModel, String gptApiUrl, String gptModel) throws Exception {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new Exception("API anahtari bos! Lutfen Ayarlar sekmesinden girin.");
        }

        List<ProjectEntry> allProjects = repository.getAllEntries();

        // Proje listesini ozetleyerek baglam olustur
        StringBuilder context = new StringBuilder();
        context.append("Asagida taranan yazilim projelerinin listesi bulunmaktadir:\n\n");

        if (allProjects.isEmpty()) {
            context.append("(Henuz hicbir proje taranmamis. Once 'Projeler' sekmesinden bir klasor tarayin.)\n");
        } else {
            for (int i = 0; i < allProjects.size(); i++) {
                ProjectEntry p = allProjects.get(i);
                context.append((i + 1)).append(". Proje Adi: ").append(p.displayName).append("\n");
                context.append("   Yol: ").append(p.absolutePath).append("\n");
                context.append("   Kaynak Turu: ").append(p.source).append("\n");
                context.append("   Diller: ").append(
                        p.languages != null ? String.join(", ", p.languages) : "bilinmiyor").append("\n");
                context.append("   Dosya Sayisi: ").append(p.fileCount).append("\n");

                // SSH / Docker kaynak detaylari — asistana verelim
                boolean hasHost = p.sshHost != null && !p.sshHost.isEmpty();
                if (hasHost) {
                    context.append("   Uzak Sunucu (SSH Host): ").append(p.sshHost).append("\n");
                    if (p.sshPort != null && !p.sshPort.isEmpty() && !"22".equals(p.sshPort)) {
                        context.append("   SSH Port: ").append(p.sshPort).append("\n");
                    }
                    if (p.sshUser != null && !p.sshUser.isEmpty()) {
                        context.append("   SSH Kullanici: ").append(p.sshUser).append("\n");
                    }
                }
                if (p.containerName != null && !p.containerName.isEmpty()) {
                    context.append("   Docker Container Adi: ").append(p.containerName).append("\n");
                }
                if (p.containerId != null && !p.containerId.isEmpty()) {
                    context.append("   Docker Container ID: ").append(p.containerId.substring(0, Math.min(p.containerId.length(), 12))).append("\n");
                }

                if (p.tags != null && !p.tags.isEmpty()) {
                    context.append("   Etiketler: ").append(String.join(", ", p.tags)).append("\n");
                }

                // --- Zengin Analiz Detaylari ---
                if (p.thirdPartyLibs != null && !p.thirdPartyLibs.isEmpty()) {
                    context.append("   3. Parti Kutuphaneler: ").append(String.join(", ", p.thirdPartyLibs)).append("\n");
                }
                if (p.csvColumns != null && !p.csvColumns.isEmpty()) {
                    for (Map.Entry<String, List<String>> csvEntry : p.csvColumns.entrySet()) {
                        context.append("   CSV Sutunlari [").append(csvEntry.getKey()).append("]: ")
                               .append(String.join(", ", csvEntry.getValue())).append("\n");
                    }
                }
                if (p.runCommands != null && !p.runCommands.isEmpty()) {
                    context.append("   Calistirma Komutlari:\n");
                    for (Map.Entry<String, String> cmdEntry : p.runCommands.entrySet()) {
                        context.append("     ").append(cmdEntry.getKey()).append(" -> ").append(cmdEntry.getValue()).append("\n");
                    }
                }
                if (p.hasHardcodedSecrets) {
                    context.append("   ⚠ Guvenlik: Bu projede hardcoded (sabit kodlanmis) kimlik bilgisi tespit edildi.\n");
                }
                if (p.readmeContent != null && !p.readmeContent.isEmpty()) {
                    int limit = Math.min(p.readmeContent.length(), 3500);
                    context.append("   README (Tam veya Geniş Özet): \n").append(p.readmeContent, 0, limit);
                    if (p.readmeContent.length() > limit) context.append("... (devamı kesildi)\n");
                    context.append("\n");
                } else if (p.description != null && !p.description.isEmpty()) {
                    context.append("   Aciklama: ").append(p.description, 0,
                            Math.min(p.description.length(), 1500)).append("...\n");
                }
                context.append("\n");
            }
        }


        int projectCount = allProjects.size();
        String systemPrompt = "Sen akilli, samimi ve yetenekli bir yazilim proje asistanisin.\n" +
                "Kullanicinin analiz edilmis projelerini bilirsin; asagida verilen projeleri bulmak, aciklamak ve sorularini yanitlamakla gorevlisin.\n\n" +
                "<veritabani_projeleri>\n" +
                context.toString() +
                "</veritabani_projeleri>\n\n" +
                "KIRMIZI CIZGI KURALLARI (ASAGIDAKI KURALLARA KESINLIKLE UYACAKSIN):\n" +
                "1. Eger kullanici SADECE 'Merhaba', 'Selam' gibi selamlama kelimeleri yazarsa, projeleri listelemeden samimi bir selam ver ve 'Merhaba! Sistemimde " + projectCount + " adet projeniz bulunuyor, nasil bir sey ariyorsunuz?' de.\n" +
                "2. Kullanici genel olarak 'benim projem var mi', 'neler var', 'projeleri listele' diye sorarsa, KESINLIKLE 'Sistemimde " + projectCount + " adet projeniz var' de ve <veritabani_projeleri> icindeki projelerin isimlerini kisaca listele. ASLA 'projeniz bulunmamaktadir' seklinde yalan soyleme!\n" +
                "3. Kullanici bir teknoloji, kütüphane, arac veya kavram ariyorsa (orn: shap, xai, python, react, sql, makine ogrenmesi vb.):\n" +
                "   - <veritabani_projeleri> icindeki her bir projenin 'Diller', 'Etiketler', '3. Parti Kutuphaneler' ve 'README' kisimlarini cok dikkatli tara.\n" +
                "   - Eger aranan kelime VEYA esanlamlisi/iliskilisi (orn: XAI ariyorsa SHAP, LIME da gecerlidir) bu alanlarda geciyorsa, o projeyi MUTLAKA listele.\n" +
                "   - Sadece eger tum projeleri taradiktan sonra HICBIR iliski bulamazsan 'Maalesef bulamadim' de.\n" +
                "4. Listeleme yaparken: 'X projesinde bu teknoloji kullanilmistir' diyerek projeyi ve nasil kullanildigini (README'ye dayanarak) kisaca acikla.\n" +
                "5. Eger kullanici konusuna dair bir soru sorarsa (orn: 'bu ne projesi', 'ne ise yarar'):\n" +
                "   - Sohbet gecmisinde konusulan projeyi veya veritabanindaki projeyi bulup README detaylarina dayanarak samimi bir sekilde anlat.\n" +
                "6. KAYNAK BİLGİSİ (DOCKER / SSH):\n" +
                "   - Projenin nerede calistigini (Uzak Sunucu IP, Docker Container Adi vb.) biliyorsan ve kullanici sorarsa ('vds ipi ne', 'nerede calisiyor'), bu bilgiyi dogrudan ver.\n" +
                "7. Cevaplarin samimi, kisa, net ve profesyonel olsun. Robottik ifadelerden kacinin ve yalnizca gercekten asistan gibi yanit verin.\n";

        // Sohbet gecmisini ve sistem talimatini yapılandırılmış mesaj dizisi olarak olustur
        JsonArray messages = new JsonArray();

        // 1. Sistem Talimati
        JsonObject sysMsg = new JsonObject();
        sysMsg.addProperty("role", "system");
        sysMsg.addProperty("content", systemPrompt);
        messages.add(sysMsg);

        // 2. Sohbet Gecmisi
        for (JsonElement el : historyMessages) {
            JsonObject obj = el.getAsJsonObject();
            String role = obj.get("role").getAsString();
            String content = obj.get("content").getAsString();
            
            JsonObject histMsg = new JsonObject();
            histMsg.addProperty("role", role);
            histMsg.addProperty("content", content);
            messages.add(histMsg);
        }

        // 3. Mevcut Soru
        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", userQuestion);
        messages.add(userMsg);

        AIAnalyzer analyzer = new AIAnalyzer();
        String assistantReply = analyzer.sendRequest(messages, service, apiKey, geminiModel, gptApiUrl, gptModel);

        // Sohbet gecmisine kaydet
        JsonObject savedUserMsg = new JsonObject();
        savedUserMsg.addProperty("role", "user");
        savedUserMsg.addProperty("content", userQuestion);
        historyMessages.add(savedUserMsg);

        JsonObject savedAstMsg = new JsonObject();
        savedAstMsg.addProperty("role", "assistant");
        savedAstMsg.addProperty("content", assistantReply);
        historyMessages.add(savedAstMsg);

        // Sohbet gecmisini max 20 mesajla (10 tur) sinirla
        // Context window tasmasini ve API hatalarini onler
        final int MAX_HISTORY = 20;
        while (historyMessages.size() > MAX_HISTORY) {
            // En eski mesaji (index 0) sil
            JsonArray trimmed = new JsonArray();
            for (int i = 1; i < historyMessages.size(); i++) {
                trimmed.add(historyMessages.get(i));
            }
            historyMessages = trimmed;
        }

        return assistantReply;
    }
}
