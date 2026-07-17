package com.smartproject.ai;

import com.google.gson.*;
import com.smartproject.db.ProjectRepository;
import com.smartproject.db.ProjectRepository.ProjectEntry;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

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
                if (p.description != null && !p.description.isEmpty()) {
                    context.append("   Aciklama: ").append(p.description, 0,
                            Math.min(p.description.length(), 300)).append("...\n");
                }
                context.append("\n");
            }
        }

        int projectCount = allProjects.size();
        String systemPrompt = "Sen akilli, samimi ve yetenekli bir yazilim proje asistanisin.\n" +
                "Kullanicinin analiz edilmis projelerini bilirsin; projeleri bulmak, aciklamak ve kaynak bilgilerini vermek konusunda yardimci olursun.\n\n" +
                "KIRMIZI CIZGI KURALLARI (BUNLARA KESINLIKLE UY):\n" +
                "1. Eger kullanici SADECE 'Merhaba', 'Selam', 'Naber' gibi selamlama kelimeleri yazarsa, " +
                "projeleri listelemeden samimi bir selam ver ve 'Merhaba! Veritabaninizda " + projectCount + " adet proje var, nasil bir sey ariyorsunuz?' de.\n" +
                "2. Kullanici genel veya konusuna dair bir soru sorarsa (orn: 'ne ile ilgili proje', 'bu ne projesi', 'konusu ne', 'ne ise yarar' vb.):\n" +
                "   - Eger veritabaninda SADECE 1 adet proje varsa veya sohbet gecmisinde bir projeden bahsedilmisse, " +
                "dogrudan o projenin aciklamasini, konusunu ve amacini samimi bir sekilde acikla.\n" +
                "   - ASLA kullanicinin cumlesindeki 'ne', 'ile', 'ilgili' gibi kelimeleri ayri ayri arama terimi olarak algilama! Cumleyi dogal dilde butunsel olarak anla.\n" +
                "3. Eger kullanici net bir arama veya kelime eslesmesi yapiyorsa (orn: 'python projesi var mi', 'X adinda proje var mi' vb.), " +
                "veritabanindaki projeleri tarayarak eslesenleri bul ve detaylarini (adi, yolu) ver.\n" +
                "4. Eger kullanici 'veritabaninda ne var', 'hangi projeler var', 'projeleri listele' derse, " +
                "SADECE asagida verilen listedeki projeleri (adlari ve yollariyla) listele.\n" +
                "5. DIKKAT: Veritabaninda TOPLAM " + projectCount + " adet proje var! SADECE bu " + projectCount + " adet projeyi listele. ASLA KENDINDEN proje uydurma!\n" +
                "6. Sordugu seyle ilgili gercek bir proje yoksa 'Maalesef buna uygun proje bulamadim' de.\n" +
                "7. Cevaplarin samimi, kisa, net ve okunabilir olsun. ASLA sohbetin devamini (Kullanici:, Asistan:, Sohbet Gecmisi: vb.) kendi cevabina dahil etme! SADECE kendi cevabini ver.\n" +
                "8. KAYNAK BİLGİSİ KURALLARI — COKEN ONEMLI:\n" +
                "   - Her projenin 'Kaynak Turu' alani DOCKER veya SSH olabilir. Bu projelerin nerede calistigini (sunucu IP, container adi) biliyorsun!\n" +
                "   - Eger proje bilgisinde 'Uzak Sunucu (SSH Host)' alani varsa, kullanici VDS IP'sini veya baglanti bilgisini sorarsa BU BİLGİYİ KULLAN ve soyle!\n" +
                "   - Eger kullanici 'vdsnin ipi ne', 'sunucu adresi ne', 'host nedir', 'ip adresi' gibi bir sey sorarsa ve bende o projenin host bilgisi varsa, dogrudan soyle!\n" +
                "   - Docker kaynakli projeleri aciklarkern 'Bu proje, [sunucu IP] adresindeki VDS uzerinde [container adi] Docker container'i icinde calisiyor.' gibi detayli ac.\n" +
                "   - Eger bilgi gercekten yoksa (sshHost alani bos ya da yok ise) o zaman 'Bu bilgiye sahip degilim' de.\n" +
                "9. Projeyi detayli anlatirken: dil, dosya sayisi, container/sunucu bilgisi, tahmini amaci — hepsini bir arada ver. Robottik degil, samimi bir gelistirici gibi yaz.\n\n" +
                "--- VERITABANINDAKI PROJELERIN BASI ---\n" +
                context.toString() +
                "--- VERITABANINDAKI PROJELERIN SONU ---\n";

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
