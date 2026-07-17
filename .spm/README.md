# Period 8 / javaProje

## Giriş

Period 8 / javaProje, yazılım geliştirme projelerinin yaşam döngüsünü desteklemek üzere tasarlanmış kapsamlı bir Java tabanlı masaüstü uygulamasıdır. Bu proje, geliştiricilere ve proje yöneticilerine, projelerini daha etkin bir şekilde yönetmeleri, derinlemesine analiz etmeleri ve sektördeki popüler araçlarla (Git, GitHub, Docker gibi) sorunsuz bir şekilde entegre etmeleri için güçlü ve kullanıcı dostu bir platform sunar. Uygulama, proje oluşturmadan kod analizine, versiyon kontrolü entegrasyonundan Docker ortamlarının taranmasına kadar geniş bir yelpazede işlevsellik sağlayarak geliştirme süreçlerini optimize etmeyi hedefler.

## Özellikler

Period 8 / javaProje, aşağıdaki temel ve gelişmiş özellikleri sunmaktadır:

*   **Kapsamlı Proje Yönetimi:**
    *   **Proje Tanımlama (`Project.java`):** Projelerinizi yapılandırmak için temel veri modelini sağlar.
    *   **Proje Asistanı (`ProjectAssistant.java`):** Proje yaşam döngüsü boyunca yardımcı olan ana iş akışlarını ve süreçleri yönetir.
    *   **Proje Deposu (`ProjectRepository.java`):** Proje verilerinin kalıcı olarak depolanması ve kolayca erişilebilir olmasını sağlar.
    *   **Proje Düzenleme Arayüzü (`ProjectEditDialog.java`):** Kullanıcıların proje detaylarını kolayca oluşturmasına ve güncellemesine olanak tanıyan grafiksel bir arayüz sunar.

*   **Akıllı Proje Analizi (`AIAnalyzer.java`):**
    *   Yapay zeka destekli bir analiz motoru kullanarak projelerinizin kod tabanını, yapısını ve potansiyel sorunlarını derinlemesine inceler. Kod kalitesi, olası güvenlik açıkları, performans darboğazları veya diğer özelleştirilebilir metrikler hakkında değerli içgörüler ve öneriler sunar.

*   **Dosya ve Dizin İşlemleri:**
    *   **Dosya Yöneticisi (`FileManager.java`):** Proje dosyaları ve dizinleri üzerinde temel okuma, yazma ve gezinme işlemleri için yardımcı fonksiyonlar sağlar.
    *   **Proje Tarayıcı (`ProjectScanner.java`):** Belirli kriterlere göre proje dizinlerini tarar, dosyaları indeksler ve projenin yapısı hakkında bilgi toplar.

*   **Git ve GitHub Entegrasyonu:**
    *   **Git Yöneticisi (`GitManager.java`):** Yerel Git depoları ile etkileşimi yönetir; commit, branch işlemleri ve depo durumu takibi gibi temel versiyon kontrol operasyonlarını destekler.
    *   **GitHub API Yöneticisi (`GithubAPIManager.java`):** GitHub API ile güvenli ve etkin bir şekilde iletişim kurarak GitHub üzerindeki depolarla, issue'larla ve pull request'lerle etkileşimi mümkün kılar.
    *   **GitHub Arayüzü (`GithubOverlayDialog.java`):** Kullanıcıların GitHub ile ilgili bilgilere hızlıca erişmesini ve uygulama içinden belirli GitHub işlemlerini gerçekleştirmesini sağlayan görsel bir bileşendir.

*   **Docker Entegrasyonu ve Tarama:**
    *   **Docker Tarayıcı (`DockerScanner.java`):** Projelerinizle ilişkili Docker imajlarını, container'larını ve ağlarını tarayarak Docker ortamınız hakkında detaylı bilgi toplar.
    *   **Docker Tarama Diyaloğu (`DockerScanDialog.java`):** Docker tarama sonuçlarını ve yapılandırma seçeneklerini görsel olarak sunar.

*   **Uzak Bağlantı Yönetimi (`RemoteConnectionDialog.java`):**
    *   Uzak sunucular, depolar veya diğer kaynaklarla güvenli ve yapılandırılabilir bağlantıların yönetimini sağlar. Bu özellik, uzaktan dağıtım, test veya izleme senaryoları için kritik öneme sahiptir.

*   **Kullanıcı Arayüzü ve Sistem Konfigürasyonu:**
    *   **Ana Grafik Arayüz (`MainGUI.java`, `AssistantPanel.java`):** Tüm bu özelliklere kolay ve sezgisel erişim sağlayan, modern ve kullanıcı dostu ana ekran ve yardımcı panelleri barındırır.
    *   **Konfigürasyon Yöneticisi (`ConfigManager.java`):** Uygulama ayarlarını, kullanıcı tercihlerini ve harici servis bağlantı bilgilerini yönetir, böylece uygulamanın davranışını kişiselleştirebilirsiniz.

## Nasıl Çalıştırılır

Period 8 / javaProje uygulamasını yerel makinenizde çalıştırmak için aşağıdaki adımları izlemeniz gerekmektedir:

1.  **Gereksinimler:**
    *   **Java Development Kit (JDK):** Uygulamanın derlenip çalışabilmesi için en az JDK 11 veya üzeri kurulu olmalıdır.
    *   **Build Aracı:** Projenin bağımlılıklarını yönetmek ve derlemek için Maven veya Gradle gibi bir build aracına ihtiyacınız olacaktır.

2.  **Projeyi Klonlayın:**
    Öncelikle, projenin kaynak kodunu bilgisayarınıza klonlayın.
    ```bash
    git clone [PROJE_GITHUB_ADRESI_BURAYA] # Projenin GitHub adresini buraya ekleyin
    cd javaProje
    ```
    *(Not: Projenin GitHub adresi sağlanmadığı için, yukarıdaki komutta `[PROJE_GITHUB_ADRESI_BURAYA]` kısmını kendi projenizin geçerli GitHub klonlama adresi ile değiştirmeniz gerekmektedir.)*

3.  **Bağımlılıkları Yükleyin ve Derleyin:**
    Klonlama işleminden sonra, projenin bağımlılıklarını yüklemek ve projeyi derlemek için kullanılan build aracına göre aşağıdaki komutlardan birini çalıştırın:

    *   **Maven Kullanıyorsanız:**
        ```bash
        mvn clean install
        ```
    *   **Gradle Kullanıyorsanız:**
        ```bash
        gradle build
        ```

4.  **Uygulamayı Başlatın:**
    Derleme işlemi başarıyla tamamlandıktan sonra, uygulamayı çalıştırmak için iki ana yöntem bulunmaktadır:

    *   **JAR Dosyasından Çalıştırma:**
        Derleme işlemi sonucunda `target/` (Maven) veya `build/libs/` (Gradle) dizininde oluşan JAR dosyasını doğrudan çalıştırabilirsiniz:
        ```bash
        java -jar target/javaProje.jar # Veya uygun JAR dosya yolunuzu kullanın
        ```
    *   **IDE Üzerinden Çalıştırma:**
        Favori Java Geliştirme Ortamınızı (IDE) (örneğin IntelliJ IDEA, Eclipse, VS Code) kullanarak projeyi açın ve `Main.java` dosyasını çalıştırın.

## Yazar

*   **bymayfe**