@echo off
setlocal EnableExtensions EnableDelayedExpansion

set "APP_HOME=%~dp0"
set "TYPE=app-image"
if /i "%~1"=="exe" set "TYPE=exe"
if /i "%~1"=="msi" set "TYPE=msi"

set "OUTPUT_DIR=%APP_HOME%target\installer"
set "ICON=%APP_HOME%src\main\packaging\edith-j.ico"
set "MAIN_CLASS=com.edithj.app.Launcher"
set "APP_NAME=EDITH-J"
set "APP_VERSION=0.1.0"
set "INPUT_DIR=%APP_HOME%target\jpackage-input"

echo.
echo ╔══════════════════════════════════════════════════════╗
echo ║            E·D·I·T·H-J  Installer Builder           ║
echo ╠══════════════════════════════════════════════════════╣
echo ║  Type   : %TYPE%
echo ║  Output : %OUTPUT_DIR%
echo ╚══════════════════════════════════════════════════════╝
echo.

if /i "%TYPE%"=="app-image" goto APP_IMAGE

echo [1/1] Building Windows installer through Maven profile...
call mvn clean package -P windows-installer -Djpackage.type=%TYPE%
if errorlevel 1 (
    echo [ERROR] Maven packaging failed.
    exit /b 1
)

echo.
echo Build complete.
echo Output: %OUTPUT_DIR%
exit /b 0

:APP_IMAGE
echo [1/3] Building shaded JAR...
call mvn clean package -DskipTests
if errorlevel 1 (
    echo [ERROR] Maven build failed.
    exit /b 1
)

echo [2/3] Preparing jpackage input...
if exist "%INPUT_DIR%" rmdir /s /q "%INPUT_DIR%"
mkdir "%INPUT_DIR%"
mkdir "%INPUT_DIR%\models\vosk-model-small-en-us-0.15"
mkdir "%INPUT_DIR%\conf"

set "JAR_PATH="
for %%F in ("%APP_HOME%target\edith-j-*-all.jar") do (
    set "JAR_PATH=%%~fF"
)

if not defined JAR_PATH (
    echo [ERROR] Shaded JAR not found in target\.
    exit /b 1
)

copy /y "!JAR_PATH!" "%INPUT_DIR%\" >nul
xcopy /e /i /y "%APP_HOME%models\vosk-model-small-en-us-0.15" "%INPUT_DIR%\models\vosk-model-small-en-us-0.15" >nul
copy /y "%APP_HOME%edith.properties.example" "%INPUT_DIR%\conf\" >nul

echo [3/3] Running jpackage (portable app-image)...
if exist "%OUTPUT_DIR%" rmdir /s /q "%OUTPUT_DIR%"

set "JAR_NAME="
for %%F in ("!JAR_PATH!") do set "JAR_NAME=%%~nxF"

if exist "%ICON%" (
    jpackage ^
      --input "%INPUT_DIR%" ^
      --main-jar "!JAR_NAME!" ^
      --main-class "%MAIN_CLASS%" ^
      --name "%APP_NAME%" ^
      --app-version %APP_VERSION% ^
      --dest "%OUTPUT_DIR%" ^
      --type app-image ^
      --java-options "--enable-native-access=ALL-UNNAMED" ^
      --java-options "-Xms128m" ^
      --java-options "-Xmx512m" ^
      --add-modules java.base,java.desktop,java.logging,java.net.http,java.prefs,java.sql,jdk.crypto.ec,jdk.unsupported ^
      --jlink-options "--no-header-files --no-man-pages --strip-debug" ^
      --vendor "EDITH-J Project" ^
      --description "Enhanced Desktop Intelligent Task Helper" ^
      --icon "%ICON%"
) else (
    jpackage ^
      --input "%INPUT_DIR%" ^
      --main-jar "!JAR_NAME!" ^
      --main-class "%MAIN_CLASS%" ^
      --name "%APP_NAME%" ^
      --app-version %APP_VERSION% ^
      --dest "%OUTPUT_DIR%" ^
      --type app-image ^
      --java-options "--enable-native-access=ALL-UNNAMED" ^
      --java-options "-Xms128m" ^
      --java-options "-Xmx512m" ^
      --add-modules java.base,java.desktop,java.logging,java.net.http,java.prefs,java.sql,jdk.crypto.ec,jdk.unsupported ^
      --jlink-options "--no-header-files --no-man-pages --strip-debug" ^
      --vendor "EDITH-J Project" ^
      --description "Enhanced Desktop Intelligent Task Helper"
)

if errorlevel 1 (
    echo [ERROR] jpackage failed.
    exit /b 1
)

echo.
echo Build complete.
echo Output: %OUTPUT_DIR%
echo Run:    %OUTPUT_DIR%\%APP_NAME%\%APP_NAME%.exe
exit /b 0
