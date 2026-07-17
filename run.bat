@echo off
title Smart Project Manager Derleyici ve Baslatici
echo ===================================================
echo   Smart Project Manager - Hizli Derleme ve Calistirma
echo ===================================================
echo.

:: JAVA_HOME otomatik bulma - ayarlama (Gecersiz veya bos ise tespit et)
if exist "%JAVA_HOME%\bin\java.exe" goto java_home_ok
if not exist "C:\Program Files\Java" goto java_home_ok
for /d %%i in ("C:\Program Files\Java\jdk-*") do set "JAVA_HOME=%%i"
:java_home_ok

:: Java kontrolu
echo [1/3] Java kontrol ediliyor...
where java >nul 2>nul
if %errorlevel% neq 0 (
    echo [HATA] Bilgisayarinizda Java JRE veya JDK bulunamadi.
    echo Lutfen Java yukleyin - En az Java 11: https://adoptium.net/
    pause
    exit /b 1
)
echo.

:: Derlenmis JAR kontrolu
if not exist target\SmartProjectManager-1.0-SNAPSHOT-jar-with-dependencies.jar goto compile_project

echo [BILGI] Daha once derlenmis bir surum mevcut.
set /p secim="Bu surum calistirilsin mi? [E/H]: "

if /i "%secim%"=="E" goto run_jar
if /i "%secim%"=="Y" goto run_jar
if /i "%secim%"=="H" goto compile_project
if /i "%secim%"=="N" goto compile_project
echo Gecersiz secim. Varsayilan olarak calistiriliyor...
goto run_jar

:compile_project
:: Maven kontrolu
where mvn >nul 2>nul
if %errorlevel% equ 0 (
    echo [BILGI] Sisteminizde Maven kurulu. Proje derleniyor...
    call mvn clean package
    if %errorlevel% neq 0 goto compile_error
    goto run_jar
)

:: Sistemde Maven yoksa Maven Wrapper secenekleri
echo [UYARI] Sisteminizde 'mvn' - Maven - komutu bulunamadi.
echo.
set /p secim="Maven Wrapper ile otomatik derleme yapilsin mi? [E/H]: "

if /i "%secim%"=="E" goto download_and_compile
if /i "%secim%"=="Y" goto download_and_compile
if /i "%secim%"=="H" exit /b 0
if /i "%secim%"=="N" exit /b 0
echo Gecersiz secim. Cikiliyor...
pause
exit /b 1

:download_and_compile
echo.
echo [BILGI] Eger daha once indirilmediyse, derleme icin Maven araci internetten indirilecektir (yaklasik 10MB).
set /p onay="Bu islemi onayliyor musunuz? [E/H]: "
if /i "%onay%"=="E" goto proceed_download
if /i "%onay%"=="Y" goto proceed_download
goto cancel_build

:proceed_download
:: Eger wrapper dosyalari klasorde yoksa indir
if not exist mvnw.cmd (
    echo.
    echo [INFO] Maven Wrapper dosyalari indiriliyor, lutfen bekleyin...
    powershell -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; New-Item -ItemType Directory -Force -Path .mvn/wrapper; Invoke-WebRequest -Uri 'https://raw.githubusercontent.com/takari/maven-wrapper/master/mvnw' -OutFile 'mvnw'; Invoke-WebRequest -Uri 'https://raw.githubusercontent.com/takari/maven-wrapper/master/mvnw.cmd' -OutFile 'mvnw.cmd'; Invoke-WebRequest -Uri 'https://raw.githubusercontent.com/takari/maven-wrapper/master/.mvn/wrapper/maven-wrapper.properties' -OutFile '.mvn/wrapper/maven-wrapper.properties'; Invoke-WebRequest -Uri 'https://raw.githubusercontent.com/takari/maven-wrapper/master/.mvn/wrapper/maven-wrapper.jar' -OutFile '.mvn/wrapper/maven-wrapper.jar'"
)

echo.
echo Proje Maven Wrapper ile derleniyor...
call .\mvnw.cmd clean package
if %errorlevel% neq 0 goto compile_error
goto run_jar

:compile_error
echo.
echo [HATA] Derleme sirasinda bir hata olustu!
echo Lutfen kodlarinizi veya Java/Maven ayarlarini kontrol edin.
pause
exit /b 1

:cancel_build
echo.
echo [BILGI] Derleme islemi iptal edildi.
pause
exit /b 0

:run_jar
echo.
echo [INFO] Uygulama baslatiliyor...
if exist target\SmartProjectManager-1.0-SNAPSHOT-jar-with-dependencies.jar (
    java -jar target\SmartProjectManager-1.0-SNAPSHOT-jar-with-dependencies.jar
) else (
    echo [HATA] Derlenmis JAR dosyasi bulunamadi!
    pause
)
