@echo off
title Smart Project Manager Derleyici ve Baslatici
echo ===================================================
echo   Smart Project Manager - Hizli Derleme ve Calistirma
echo ===================================================
echo.

:: JAVA_HOME otomatik bulma - ayarlama
if exist "%JAVA_HOME%\bin\java.exe" goto java_home_ok
if not exist "C:\Program Files\Java" goto java_home_ok
for /d %%i in ("C:\Program Files\Java\jdk-*") do set "JAVA_HOME=%%i"
:java_home_ok

:: Java kontrolu
echo [1/3] Java kontrol ediliyor...
where java >nul 2>nul
if errorlevel 1 goto no_java
goto java_ok

:no_java
echo [HATA] Bilgisayarinizda Java JRE veya JDK bulunamadi.
echo Lutfen Java yukleyin - En az Java 11: https://adoptium.net/
pause
exit /b 1

:java_ok
echo.

:: Derlenmis JAR kontrolu
if not exist target\SmartProjectManager-1.0-SNAPSHOT-jar-with-dependencies.jar goto compile_project

echo [BILGI] Daha once derlenmis bir surum mevcut.
set /p secim="Bu surum calistirilsin mi? [E/H]: "

if /i "%secim%"=="E" goto run_jar
if /i "%secim%"=="Y" goto run_jar
if /i "%secim%"=="H" goto confirm_recompile
if /i "%secim%"=="N" goto confirm_recompile
echo Gecersiz secim. Varsayilan olarak calistiriliyor...
goto run_jar

:confirm_recompile
echo.
echo [UYARI] Sisteminizde 'mvn' (Maven) komutu bulunamadi.
set /p secim_recompile="Maven Wrapper ile otomatik derleme yapilsin mi? [E/H]: "

if /i "%secim_recompile%"=="E" goto compile_project
if /i "%secim_recompile%"=="Y" goto compile_project
if /i "%secim_recompile%"=="H" goto cancel_build
if /i "%secim_recompile%"=="N" goto cancel_build
echo Gecersiz secim. Cikiliyor...
pause
exit /b 1

:compile_project
:: Maven veya Maven Wrapper kontrolu
where mvn >nul 2>nul
if errorlevel 1 goto check_wrapper

echo [BILGI] Sisteminizde Maven kurulu. Proje derleniyor...
call mvn clean package
if errorlevel 1 goto try_mvn_package
goto run_jar

:try_mvn_package
call mvn package
if errorlevel 1 goto compile_error
goto run_jar

:check_wrapper
if not exist mvnw.cmd goto no_wrapper

echo [BILGI] Maven Wrapper kullanilarak proje derleniyor...
call .\mvnw.cmd clean package
if errorlevel 1 goto try_package_only
goto run_jar

:try_package_only
echo [BILGI] 'clean' yapilamadi (uygulama acik olabilir). Direkt paketleme deneniyor...
call .\mvnw.cmd package
if errorlevel 1 goto compile_error
goto run_jar

:no_wrapper
echo [UYARI] Sisteminizde 'mvn' komutu veya 'mvnw.cmd' bulunamadi.
pause
exit /b 1

:cancel_build
echo.
echo [BILGI] Derleme islemi iptal edildi.
pause
exit /b 0

:compile_error
echo.
echo =======================================================
echo [HATA] Derleme basarisiz oldu!
echo =======================================================
echo Eger arka planda 'Smart Project Manager' aciksa,
echo lutfen acik olan Java penceresini KAPATIP tekrar deneyin.
echo.
pause
exit /b 1

:run_jar
echo.
echo [INFO] Uygulama baslatiliyor...
if exist target\SmartProjectManager-1.0-SNAPSHOT-jar-with-dependencies.jar (
    java -jar target\SmartProjectManager-1.0-SNAPSHOT-jar-with-dependencies.jar
) else (
    echo [HATA] Derlenmis JAR dosyasi bulunamadi!
    pause
)
