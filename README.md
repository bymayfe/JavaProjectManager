# 🚀 Smart Project Manager

[![Java Version](https://img.shields.io/badge/Java-11%2B-orange?logo=openjdk&logoColor=white)](https://adoptium.net/)
[![Maven Build](https://img.shields.io/badge/Maven-3.x-blue?logo=apachemaven&logoColor=white)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![Platform Compatibility](https://img.shields.io/badge/Platform-Windows%20%7C%20macOS%20%7C%20Linux-lightgrey)](https://github.com/bymayfe)

Smart Project Manager, yazılım geliştirme projelerinin yaşam döngüsünü desteklemek üzere tasarlanmış, kapsamlı ve modern bir **Java tabanlı masaüstü uygulamasıdır**. Bu proje, geliştiricinin **Java Programlama Dersi** kapsamında geliştirdiği dönem projesidir. Projenin fikir sahibi ve baş geliştiricisi **Seyfettin Budak**'tır.

Geliştiricilere ve proje yöneticilerine; projelerini etkin bir şekilde yönetme, derinlemesine kod analizi yapma, Git/GitHub entegrasyonu sağlama ve Docker ortamlarını tarama gibi kritik süreçleri tek bir platform üzerinden yönetme imkanı sunar.

---

## ✨ Temel Özellikler

*   📂 **Kapsamlı Proje Yönetimi:** Proje oluşturma, düzenleme, listeleme ve kalıcı olarak depolama süreçlerini yöneten gelişmiş arayüz ve veri modeli.
*   🧠 **Yapay Zeka Destekli Analiz (`AIAnalyzer`):** Yapay zeka motoru (Gemini, Groq, GPT) entegrasyonu ile kod tabanınızı analiz eder, performans darboğazlarını ve güvenlik açıklarını raporlar.
*   🐳 **Docker Entegrasyonu ve Tarama:** Yerel Docker ortamındaki imajları, container'larını ve ağları tarayarak grafiksel arayüz üzerinden detaylı durum bilgisi sunar.
*   🐙 **Git & GitHub Entegrasyonu:** JGit entegrasyonu sayesinde Git komutlarına gerek kalmadan değişiklikleri commit etme, depolara pushlama ve GitHub API üzerinden depo yönetimi yapabilme.
*   🔒 **Uzak Bağlantı & SSH Yönetimi:** Uzak sunuculara veya Docker ana bilgisayarlarına güvenli SSH (JSch) bağlantısı kurarak oradaki dosyalara erişebilme, onları yerleşik konsol üzerinden kontrol edebilme ve uzak sunucudaki projeleri analiz ettirebilme desteği.
*   💾 **Esnek Veritabanı Seçimi:** Uygulama verilerinin nerede saklanacağını (MongoDB, MySQL veya gömülü SQLite) dinamik olarak seçebilme ve yönetebilme desteği.
*   🎨 **Modern Kullanıcı Arayüzü:** FlatLaf teması ile modern, göze hoş gelen ve yüksek çözünürlüklü (HiDPI) ekranlarla uyumlu kullanıcı dostu grafik arayüz.

---

## 🛠️ Teknoloji Yığını

*   **Dil:** Java 11+
*   **Grafik Arayüz:** Java Swing & AWT (FlatLaf Modern Teması ile)
*   **Veritabanı Desteği:** SQLite, MySQL ve MongoDB
*   **Sürüm Kontrolü:** Eclipse JGit
*   **Ağ & Bağlantı:** JSch (SSH kütüphanesi)
*   **Yapay Zeka:** Google GenAI SDK & OpenAI API standardı
*   **Build Sistemi:** Maven 3.x

---

## ⚙️ Proje Yapısı

```text
javaProje/
├── .mvn/                  # Maven Wrapper yapılandırması
├── src/
│   ├── main/
│   │   └── java/
│   │       └── com/
│   │           └── smartproject/
│   │               ├── ai/         # Yapay zeka analiz motoru
│   │               ├── config/     # Uygulama ayarları yönetimi
│   │               ├── db/         # Veritabanı bağlantıları (SQL/NoSQL)
│   │               ├── file/       # Dosya/Dizin yöneticileri
│   │               ├── git/        # Git ve GitHub API entegrasyonu
│   │               ├── gui/        # Swing arayüz bileşenleri ve pencereler
│   │               ├── model/      # Temel veri modelleri (Project vb.)
│   │               └── scanner/    # Docker tarama modülleri
├── pom.xml                # Maven bağımlılıkları tanımları
├── run.bat                # Windows için tek tıkla başlatıcı
├── run.sh                 # Mac/Linux için tek tıkla başlatıcı
└── README.md              # Proje dokümantasyonu
```

---

## 🚀 Nasıl Çalıştırılır?

Smart Project Manager uygulamasını çalıştırmak için işletim sisteminize göre aşağıdaki yollardan birini seçebilirsiniz:

### Yöntem A: Hızlı Başlatıcı Scriptleri (Tavsiye Edilen)
Bilgisayarınıza Maven kurmanıza gerek kalmadan, tek tıkla projeyi derleyip çalıştırabilirsiniz.

1.  **Windows için:**
    *   Klasördeki [run.bat](file:///C:/Users/seyfo/Desktop/Period%208/javaProje/run.bat) dosyasına çift tıklayın veya terminalden `.\run.bat` komutunu çalıştırın.
2.  **Mac / Linux için:**
    *   Terminalden sırayla şu komutları çalıştırın:
        ```bash
        chmod +x run.sh
        ./run.sh
        ```

> [!NOTE]
> Bu scriptler bilgisayarınızda daha önceden derlenmiş bir sürüm (`target`) bulursa doğrudan çalıştırır. Eğer derlenmiş sürüm yoksa, internetten **Maven Wrapper** aracılığıyla gerekli bağımlılıkları otomatik olarak indirip derlemeyi başlatır.

---

### Yöntem B: Manuel Derleme ve Çalıştırma
Eğer komutları elle çalıştırmayı tercih ediyorsanız:

1.  **Projeyi derleyin (Maven Wrapper ile):**
    *   **Windows:** `mvnw.cmd clean package`
    *   **Mac / Linux:** `./mvnw clean package`
2.  **Oluşan JAR dosyasını çalıştırın:**
    ```bash
    java -jar target/SmartProjectManager-1.0-SNAPSHOT-jar-with-dependencies.jar
    ```

---

## 👥 Yazarlar ve Katkıda Bulunanlar

*   **Seyfettin Budak** - *Proje Fikri, Tasarım ve Baş Geliştirici (Lead Developer)* - [bymayfe](https://github.com/bymayfe)

> [!IMPORTANT]
> Bu proje, yazarın **Java Programlama Dersi** için dönem projesi olarak geliştirilmiştir. Projenin tüm fikir hakları, özgün tasarımı ve kod geliştirmesi tamamen **Seyfettin Budak**'a aittir.

---

## 📄 Lisans

Bu proje **MIT Lisansı** altında lisanslanmıştır. Detaylar için `LICENSE` dosyasına göz atabilirsiniz.
