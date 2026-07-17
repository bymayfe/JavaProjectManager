#!/bin/bash
clear
echo "==================================================="
echo "  Smart Project Manager - Hızlı Derleme ve Çalıştırma"
echo "==================================================="
echo

# JAVA_HOME otomatik bulma (macOS için)
if [ -z "$JAVA_HOME" ] && [ "$(uname)" == "Darwin" ]; then
    export JAVA_HOME=$(/usr/libexec/java_home 2>/dev/null)
fi

# Java kontrolü
echo "[1/3] Java kontrol ediliyor..."
if ! command -v java &> /dev/null
then
    echo "[HATA] Bilgisayarınızda Java (JRE/JDK) bulunamadı."
    echo "Lütfen Java yükleyin (En az Java 11): https://adoptium.net/"
    exit 1
fi

JAR_PATH="target/SmartProjectManager-1.0-SNAPSHOT-jar-with-dependencies.jar"

# Derlenmiş JAR kontrolü
if [ -f "$JAR_PATH" ]; then
    echo "[BİLGİ] Daha önce derlenmiş bir sürüm mevcut."
    read -p "Bu sürüm çalıştırılsın mı? [E/H]: " secim
    echo
    case "$secim" in
        [EeYy]*) exec java -jar "$JAR_PATH" ;;
        [HhNn]*) ;; # Derleme adımına geç
        *) echo "Geçersiz seçim. Varsayılan olarak çalıştırılıyor..."
           exec java -jar "$JAR_PATH" ;;
    esac
fi

# Maven kontrolü (Sistemde yüklü mü?)
if command -v mvn &> /dev/null; then
    echo "[BİLGİ] Sisteminizde Maven kurulu. Proje derleniyor..."
    mvn clean package
    if [ $? -eq 0 ]; then
        exec java -jar "$JAR_PATH"
    else
        echo "[HATA] Derleme sırasında bir hata oluştu!"
        exit 1
    fi
fi

# Sistemde Maven yoksa Maven Wrapper kontrolü ve seçenekler
echo "[UYARI] Sisteminizde 'mvn' (Maven) komutu bulunamadı."
read -p "Maven Wrapper ile otomatik derleme yapılsın mı? [E/H]: " secim
echo

case "$secim" in
    [EeYy]*)
        echo "[BİLGİ] Eğer daha önce indirilmediyse, derleme için Maven aracı internetten indirilecektir (yaklaşık 10MB)."
        read -p "Bu işlemi onaylıyor musunuz? [E/H]: " onay
        echo
        case "$onay" in
            [EeYy]*) ;;
            *) echo "[BİLGİ] Derleme işlemi iptal edildi."; exit 0 ;;
        esac

        # Eğer wrapper dosyaları klasörde yoksa indir
        if [ ! -f "mvnw" ]; then
            echo "[INFO] Maven Wrapper dosyaları indiriliyor, lütfen bekleyin..."
            mkdir -p .mvn/wrapper
            
            # Curl veya Wget ile indir
            if command -v curl &> /dev/null; then
                curl -sL https://raw.githubusercontent.com/takari/maven-wrapper/master/mvnw -o mvnw
                curl -sL https://raw.githubusercontent.com/takari/maven-wrapper/master/mvnw.cmd -o mvnw.cmd
                curl -sL https://raw.githubusercontent.com/takari/maven-wrapper/master/.mvn/wrapper/maven-wrapper.properties -o .mvn/wrapper/maven-wrapper.properties
                curl -sL https://raw.githubusercontent.com/takari/maven-wrapper/master/.mvn/wrapper/maven-wrapper.jar -o .mvn/wrapper/maven-wrapper.jar
            elif command -v wget &> /dev/null; then
                wget -q https://raw.githubusercontent.com/takari/maven-wrapper/master/mvnw -O mvnw
                wget -q https://raw.githubusercontent.com/takari/maven-wrapper/master/mvnw.cmd -O mvnw.cmd
                wget -q https://raw.githubusercontent.com/takari/maven-wrapper/master/.mvn/wrapper/maven-wrapper.properties -O .mvn/wrapper/maven-wrapper.properties
                wget -q https://raw.githubusercontent.com/takari/maven-wrapper/master/.mvn/wrapper/maven-wrapper.jar -O .mvn/wrapper/maven-wrapper.jar
            else
                echo "[HATA] Sisteminizde 'curl' veya 'wget' bulunamadı. Lütfen dosyaları manuel indirin."
                exit 1
            fi
            chmod +x mvnw
        fi
        
        echo "Proje Maven Wrapper ile derleniyor..."
        ./mvnw clean package
        if [ $? -eq 0 ]; then
            exec java -jar "$JAR_PATH"
        else
            echo "[HATA] Derleme sırasında bir hata oluştu!"
            exit 1
        fi
        ;;
    *)
        exit 0
        ;;
esac
