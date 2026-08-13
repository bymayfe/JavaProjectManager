@echo off
echo Starting Release Build...
powershell -ExecutionPolicy Bypass -File "%~dp0build_release.ps1"
pause
