$ErrorActionPreference = "Stop"
Write-Host "Starting Smart Project Manager Release Build..."
$version = "1.2.0"
Write-Host "Version: $version"

Write-Host "Compiling with Maven..."
.\mvnw.cmd clean package -DskipTests

Write-Host "Preparing dist/ directory..."
if (Test-Path "dist") { Remove-Item -Recurse -Force "dist" }
New-Item -ItemType Directory -Path "dist" | Out-Null

$fatJar = "target\SmartProjectManager-1.2.0-jar-with-dependencies.jar"
$crossJar = "dist\SmartProjectManager-CrossPlatform.jar"
Copy-Item $fatJar -Destination $crossJar

Write-Host "Building Windows Portable App Image (jpackage)..."
$jpackage = "C:\Program Files\Java\jdk-26.0.1\bin\jpackage.exe"
& $jpackage --input target\ --dest dist\ --name SmartProjectManager --main-jar "SmartProjectManager-1.2.0-jar-with-dependencies.jar" --main-class com.smartproject.Main --type app-image

$portableZip = "dist\SmartProjectManager-Windows-Portable.zip"
Write-Host "Compressing Windows Portable ZIP..."
Compress-Archive -Path "dist\SmartProjectManager" -DestinationPath $portableZip -Force

function Get-FileInfoMd($path, $name) {
    $fileInfo = Get-Item $path
    $hash = (Get-FileHash -Path $path -Algorithm SHA256).Hash.ToLower()
    $sizeMb = [math]::Round($fileInfo.Length / 1MB, 1)
    return "[$name](https://github.com/bymayfe/JavaProjectManager/releases/download/v$version/$name)`nsha256:$hash`n$sizeMb MB"
}

$jarMarkdown = Get-FileInfoMd $crossJar "SmartProjectManager-CrossPlatform.jar"
$zipMarkdown = Get-FileInfoMd $portableZip "SmartProjectManager-Windows-Portable.zip"

Write-Host "`n=== RELEASE ASSETS MARKDOWN ==="
Write-Host $jarMarkdown
Write-Host ""
Write-Host $zipMarkdown
Write-Host "===============================`n"

Write-Host "Press Enter to close this window..." -ForegroundColor Yellow
Read-Host
