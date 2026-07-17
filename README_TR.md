# 🚀 Smart Project Manager

🌐 Bu dökümanı diğer dillerde okuyun: [English (İngilizce)](README.md)

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
└── README.md              # İngilizce proje dokümantasyonu
```

---

## ⚙️ Nasıl Çalıştırılır ve Derlenir?

Smart Project Manager uygulamasını derlemek ve çalıştırmak için iki farklı yol (Arayüz Tıklamaları veya Komut Satırı) kullanabilirsiniz.

---

### 🎮 Yöntem A: Arayüz (UI) Üzerinden Tıklayarak
Eğer terminal kullanmak istemiyorsanız, geliştirme ortamınızın (IDE) grafik arayüzündeki hazır butonları kullanarak derleme ve çalıştırma yapabilirsiniz.

<details>
<summary><b>👁️ VS Code ile Tıklayarak Derleme & Çalıştırma (Açmak için tıklayın)</b></summary>

1. **Projeyi Derleme (JAR Üretme):**
   * Sol menüdeki **MAVEN** panelini genişletin.
   * `SmartProjectManager` > `Lifecycle` klasörünü açın.
   * Önce **`clean`** seçeneğinin, ardından **`package`** seçeneğinin yanındaki **Oynat (Run)** butonuna tıklayan çalıştırın.
2. **Projeyi Çalıştırma:**
   * Proje dosyaları arasından `src/main/java/com/smartproject/Main.java` dosyasını bulun.
   * Dosyayı açıp sağ üst köşedeki **Oynat (Run Java)** butonuna basarak programı başlatın.
</details>

<details>
<summary><b>👁️ Apache NetBeans ile Tıklayarak Derleme & Çalıştırma (Açmak için tıklayın)</b></summary>

1. **Projeyi Derleme (JAR Üretme):**
   * Soldaki **Projects** panelinde projenizin adına (`SmartProjectManager`) sağ tıklayın.
   * Menüden **"Clean and Build"** seçeneğini seçin. Maven projenizi otomatik derleyecektir.
2. **Projeyi Çalıştırma:**
   * Projenize sağ tıklayıp **"Run"** seçeneğini seçin veya klavyeden **F6** tuşuna basarak uygulamayı çalıştırın.
</details>

---

### 💻 Yöntem B: Komut Satırı (Terminal) ile
Terminal veya komut satırı kullanarak daha hızlı işlemler yapabilirsiniz.

#### 1. Hızlı Başlatıcı Scriptleri (Maven Gerektirmez):
Proje kök dizininde hazır bulunan scriptleri kullanarak tek tıkla çalıştırabilirsiniz:
* **Windows için:** `run.bat` dosyasına çift tıklayın veya terminalden çalıştırın:
  ```cmd
  .\run.bat
  ```
* **Mac / Linux için:** Terminale sırasıyla şu komutları girin:
  ```bash
  chmod +x run.sh
  ./run.sh
  ```
*Bu scriptler bilgisayarınızda daha önceden derlenmiş bir sürüm (`target/` altında) bulursa doğrudan çalıştırır. Eğer yoksa, Maven Wrapper aracılığıyla otomatik olarak gerekli tüm bağımlılıkları indirip projeyi derler.*

#### 2. Manuel Derleme ve Çalıştırma (Maven Wrapper ile):
* **Windows:**
  ```cmd
  mvnw.cmd clean package
  java -jar target/SmartProjectManager-1.0-SNAPSHOT-jar-with-dependencies.jar
  ```
* **Mac / Linux:**
  ```bash
  ./mvnw clean package
  java -jar target/SmartProjectManager-1.0-SNAPSHOT-jar-with-dependencies.jar
  ```

---

### 📦 Yöntem C: Manuel Sürüm (Release) Dosyaları Oluşturma
Hocanıza göndereceğiniz veya GitHub Sürümlerine (Releases) yükleyeceğiniz dosyaları hazırlama adımları:

> [!NOTE]
> Her işletim sistemi (Windows, macOS, Linux) için sadece ortak **JAR** dosyasını (`SmartProjectManager-CrossPlatform.jar`) paylaşmak veya kullanmak da tamamen yeterlidir (bu durumda hedef bilgisayarda en az Java 11 kurulu olması gerekir). İşletim sistemlerine özel yerel paketler (EXE, APP, DEB) oluşturmak isteğe bağlıdır.

1. **JAR Dosyasını Derleyin:**
   * **Arayüzden:** VS Code Maven panelinden `package` veya NetBeans'ten `Clean and Build` yapın.
   * **Terminalden:** `mvnw.cmd clean package` (Windows) veya `./mvnw clean package` (Mac/Linux) çalıştırın.
2. **Mac ve Linux Sürümünü Hazırlayın:**
   * `target/` içinde oluşan JAR dosyasını kopyalayıp `dist/` klasörünün içerisine yapıştırın ve adını **`SmartProjectManager-CrossPlatform.jar`** yapın.
3. **Sistem Kurulum & Çalıştırılabilir Paketlerini Derleyin (jpackage):**
   *jpackage aracı çapraz derlemeyi desteklemez; hangi sistem için paket üretecekseniz o işletim sisteminde bu komutu çalıştırmalısınız:*
   * **Windows (.exe - Taşınabilir Klasör):**
     Terminalden şu komutla yerel Windows uygulaması oluşturun:
     ```cmd
     "C:\Program Files\Java\jdk-26.0.1\bin\jpackage.exe" --input target\ --dest dist\ --name SmartProjectManager --main-jar SmartProjectManager-1.0-SNAPSHOT-jar-with-dependencies.jar --main-class com.smartproject.Main --type app-image
     ```
   * **macOS (.app Uygulaması):**
     Mac terminalinde şu komutla taşınabilir uygulama paketi üretebilirsiniz:
     ```bash
     jpackage --input target/ --dest dist/ --name SmartProjectManager --main-jar SmartProjectManager-1.0-SNAPSHOT-jar-with-dependencies.jar --main-class com.smartproject.Main --type app-image
     ```
     *(Eğer disk resmi kurulum paketi üretmek isterseniz `--type app-image` yerine `--type dmg` parametresini kullanabilirsiniz).*
   * **Linux (.deb / Kurulum Paketi):**
     Linux terminalinde şu komutla Debian paketi üretebilirsiniz:
     ```bash
     jpackage --input target/ --dest dist/ --name smartprojectmanager --main-jar SmartProjectManager-1.0-SNAPSHOT-jar-with-dependencies.jar --main-class com.smartproject.Main --type deb
     ```
4. **Taşınabilir Sürümü Sıkıştırın (ZIP / TAR.GZ):**
   * **Windows'ta (Sağ Tıkla):** `dist/` klasöründeki `SmartProjectManager` klasörüne **sağ tıklayarak ZIP dosyasına sıkıştırın** ve adını `SmartProjectManager-Windows-Portable.zip` yapın.
   * **Mac / Linux'ta (Terminalden):** Terminalden şu komutla sıkıştırın:
     ```bash
     tar -czvf dist/SmartProjectManager-Portable.tar.gz -C dist SmartProjectManager
     ```

---

## 👥 Yazarlar ve Katkıda Bulunanlar

*   **Seyfettin Budak** - *Proje Fikri, Tasarım ve Baş Geliştirici (Lead Developer)* - [bymayfe](https://github.com/bymayfe)

> [!IMPORTANT]
> Bu proje, yazarın **Java Programlama Dersi** için dönem projesi olarak geliştirilmiştir. Projenin tüm fikir hakları, özgün tasarımı ve kod geliştirmesi tamamen **Seyfettin Budak**'a aittir.

---

## 📄 Lisans

Bu proje **MIT Lisansı** altında lisanslanmıştır. Detaylar için `LICENSE` dosyasına göz atabilirsiniz.
