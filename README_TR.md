# 🚀 Smart Project Manager

🌐 Bu dökümanı diğer dillerde okuyun: [English (İngilizce)](README.md)

[![Java Version](https://img.shields.io/badge/Java-11%2B-orange?logo=openjdk&logoColor=white)](https://adoptium.net/)
[![Maven Build](https://img.shields.io/badge/Maven-3.x-blue?logo=apachemaven&logoColor=white)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![Version](https://img.shields.io/badge/version-1.1.1-informational)](CHANGELOG.md)
[![Platform Compatibility](https://img.shields.io/badge/Platform-Windows%20%7C%20macOS%20%7C%20Linux-lightgrey)](https://github.com/bymayfe)

Smart Project Manager, yazılım geliştirme projelerinin yaşam döngüsünü desteklemek üzere tasarlanmış, kapsamlı ve modern bir **Java tabanlı masaüstü uygulamasıdır**. Bu proje, geliştiricinin **Java Programlama Dersi** kapsamında geliştirdiği dönem projesidir. Projenin fikir sahibi ve baş geliştiricisi **Seyfettin Budak**'tır.

Geliştiricilere ve proje yöneticilerine; projelerini etkin bir şekilde yönetme, derinlemesine kod analizi yapma, Git/GitHub entegrasyonu sağlama ve Docker ortamlarını tarama gibi kritik süreçleri tek bir platform üzerinden yönetme imkanı sunar.

---

## ✨ Temel Özellikler

*   📂 **Kapsamlı Proje Yönetimi:** Proje oluşturma, düzenleme, listeleme ve kalıcı olarak depolama süreçlerini yöneten gelişmiş arayüz ve veri modeli.
*   🧠 **Yapay Zeka Destekli Analiz (`AIAnalyzer`):** **Gemini**, **Groq** ve **GPT/OpenAI-uyumlu** servislerini destekleyen yapay zeka motoru. Kod tabanınızı analiz eder, performans darboğazlarını ve güvenlik açıklarını raporlar; ayrıca otomatik olarak profesyonel README dosyaları ve proje etiketleri üretir. Desteklenen modeller için [Yapay Zeka Servisleri](#-yapay-zeka-servisleri--desteklenen-modeller) bölümüne bakın.
*   🐳 **Docker Entegrasyonu ve Tarama:** Yerel Docker ortamındaki imajları, container'larını ve ağları tarayarak grafiksel arayüz üzerinden detaylı durum bilgisi sunar.
*   🐙 **Git & GitHub Entegrasyonu:** JGit entegrasyonu sayesinde Git komutlarına gerek kalmadan değişiklikleri commit etme, depolara pushlama ve GitHub API üzerinden depo yönetimi yapabilme.
*   🔒 **Uzak Bağlantı & SSH Yönetimi:** Uzak sunuculara güvenli SSH (JSch) bağlantısı kurarak dosyalara erişebilme, yerleşik konsol üzerinden komut çalıştırabilme ve uzak projeleri analiz ettirebilme. **Rate limit koruması** (bağlantı debounce + exponential backoff retry) ile SSH sunucusunun tekrarlı bağlantı denemelerini engellemesi önlenir.
*   💾 **Esnek Veritabanı Seçimi:** Uygulama verilerinin nerede saklanacağını (MongoDB, MySQL veya gömülü SQLite) dinamik olarak seçebilme ve yönetebilme desteği.
*   👤 **Bağlantı Profil Sistemi:** `ConfigManager` üzerinden birden fazla SSH/AI/veritabanı yapılandırma profilini kaydedip geri yükleyebilme — ortamlar arasında tek tıkla geçiş.
*   🎨 **Modern Kullanıcı Arayüzü:** FlatLaf teması ile modern, göze hoş gelen ve yüksek çözünürlüklü (HiDPI) ekranlarla uyumlu kullanıcı dostu grafik arayüz.

---

## 🤖 Yapay Zeka Servisleri & Desteklenen Modeller

Tüm yapay zeka özellikleri (kod analizi, README üretimi, proje etiketleme, asistan sohbeti) `AIAnalyzer` motoru üzerinden çalışır ve üç sağlayıcı ailesini destekler:

| Sağlayıcı | Servis Kodu | Varsayılan Model | Notlar |
|-----------|-------------|-------------------|--------|
| **Google Gemini** | `gemini` | `gemini-2.5-flash` | Ayarlardan değiştirilebilir |
| **Groq** | `groq` | `llama-3.3-70b-versatile` | Ücretsiz tier'in en iyisi; hızlı çıkarım |
| **OpenAI / GPT** | `gpt` | `gpt-4o-mini` | Tam OpenAI API desteği |
| **Özel (OpenAI-Uyumlu)** | `custom` | Yapılandırılabilir | Ollama, LM Studio gibi yerel endpoint'lerle çalışır |

> [!TIP]
> Ücretli kullanım istemiyorsanız **Groq** + `llama-3.3-70b-versatile` kombinasyonu en iyi hız/kalite dengesini sunar. Ücretsiz API anahtarınızı [console.groq.com](https://console.groq.com) adresinden alabilirsiniz.

---

## 🛠️ Teknoloji Yığını

*   **Dil:** Java 11+
*   **Grafik Arayüz:** Java Swing & AWT (FlatLaf Modern Teması ile)
*   **Veritabanı Desteği:** SQLite, MySQL ve MongoDB
*   **Sürüm Kontrolü:** Eclipse JGit
*   **Ağ & Bağlantı:** JSch (keepAlive, debounce ve exponential backoff retry ile)
*   **Yapay Zeka:** Google GenAI SDK (Gemini) & OpenAI-uyumlu REST API (Groq, GPT, Özel)
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

### 📦 Yöntem C: Otomatik Sürüm (Release) Dosyaları Oluşturma (Windows)

GitHub Sürümlerine (Releases) yükleyeceğiniz dosyaları hazırlamak için tam otomatik bir script oluşturduk.

1. Proje ana klasöründeki **`build_release.bat`** dosyasına çift tıklayın.
2. Script sizin için her şeyi otomatik yapacaktır:
   - Kodu Maven ile derler.
   - Ortak **Cross-Platform JAR** dosyasını oluşturur.
   - `jpackage` kullanarak **Windows Portable EXE** (gömülü Java içerir) paketini ayarlar.
   - Tüm çıktıları sıkıştırarak `dist/SmartProjectManager-Windows-Portable.zip` dosyasını hazırlar.
   - GitHub Releases için gereken SHA256 şifrelerini ve Markdown metnini ekrana yazdırır!

> [!NOTE]
> Her işletim sistemi (Windows, macOS, Linux) için sadece ortak **JAR** dosyasını (`SmartProjectManager-CrossPlatform.jar`) paylaşmak veya kullanmak da tamamen yeterlidir (bu durumda hedef bilgisayarda en az Java 11 kurulu olması gerekir).

---
*(Mac/Linux sistemlerinde jpackage ile manuel paketleme adımları için bu belgenin önceki sürümlerine göz atabilirsiniz).*

---

## 👥 Yazarlar ve Katkıda Bulunanlar

*   **Seyfettin Budak** - *Proje Fikri, Tasarım ve Baş Geliştirici (Lead Developer)* - [bymayfe](https://github.com/bymayfe)

> [!IMPORTANT]
> Bu proje, yazarın **Java Programlama Dersi** için dönem projesi olarak geliştirilmiştir. Projenin tüm fikir hakları, özgün tasarımı ve kod geliştirmesi tamamen **Seyfettin Budak**'a aittir.

---

## 📄 Lisans

Bu proje **MIT Lisansı** altında lisanslanmıştır. Detaylar için `LICENSE` dosyasına göz atabilirsiniz.

---

## 📋 Değişiklik Günlüğü

Tüm sürüm geçmişi ve yayın notları için [CHANGELOG.md](CHANGELOG.md) dosyasına bakın.
