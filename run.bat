@echo off
cd /d "%~dp0"
java -jar LudoKoala.jar
if errorlevel 1 pause
