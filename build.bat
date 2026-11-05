@echo off
rem Builds LudoKoala.jar from the src folder (requires JDK 17 or newer).
setlocal
cd /d "%~dp0"
if exist build rmdir /s /q build
mkdir build\classes
dir /s /b src\*.java > build\sources.txt
javac --release 17 -encoding UTF-8 -d build\classes @build\sources.txt
if errorlevel 1 (
  echo Build failed.
  exit /b 1
)
jar --create --file LudoKoala.jar --main-class Controller.LudoApplication -C build\classes .
if errorlevel 1 exit /b 1
echo Built LudoKoala.jar
