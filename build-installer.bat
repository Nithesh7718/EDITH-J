@echo off
setlocal EnableDelayedExpansion

:: ════════════════════════════════════════════════════════════════════
::  EDITH-J  –  Windows Installer Builder
::  Produces either:
::    • app-image  (portable folder, no WiX needed)  ← default
::    • exe/msi    (requires WiX Toolset v3 installed)
::
::  Usage:
::    build-installer.bat            → portable app-image
::    build-installer.bat exe        → .exe installer  (needs WiX)
::    build-installer.bat msi        → .msi installer  (needs WiX)
:: ════════════════════════════════════════════════════════════════════

set "APP_HOME=%~dp0"
set "TYPE=app-image"
if /i "%~1"=="exe" set "TYPE=exe"
if /i "%~1"=="msi" set "TYPE=msi"

set "APP_NAME=EDITH-J"
set "APP_VERSION=0.1.0"
set "JAR_NAME=edith-j-0.1.0-SNAPSHOT-all.jar"
set "MAIN_CLASS=com.edithj.app.Launcher"
set "ICON=%APP_HOME%src\main\packaging\edith-j.ico"

set "INPUT_DIR=%APP_HOME%target\jpackage-input"
set "OUTPUT_DIR=%APP_HOME%target\installer"

echo.
echo ╔══════════════════════════════════════════════════════╗
echo ║            E·D·I·T·H-J  Installer Builder           ║
echo ╠══════════════════════════════════════════════════════╣
echo ║  Type   : %TYPE%
echo ║  Output : %OUTPUT_DIR%
echo ╚══════════════════════════════════════════════════════╝
echo.

:: ── Step 1: Maven build ──────────────────────────────────────────────────────
echo [1/4] Building shaded JAR with Maven...
call mvn clean package -DskipTests -q
if errorlevel 1 (
    echo [ERROR] Maven build failed. Aborting.
    pause
    exit /b 1
)
echo       Done.

:: ── Step 2: Prepare jpackage input directory ─────────────────────────────────
echo [2/4] Preparing jpackage input...
if exist "%INPUT_DIR%" rmdir /s /q "%INPUT_DIR%"
mkdir "%INPUT_DIR%"
copy /y "%APP_HOME%target\%JAR_NAME%" "%INPUT_DIR%\%JAR_NAME%" >nul
if errorlevel 1 (
    echo [ERROR] Could not copy JAR. Aborting.
    pause
    exit /b 1
)
echo       Done.

:: ── Step 3: Clean previous installer output ──────────────────────────────────
echo [3/4] Cleaning previous output...
if exist "%OUTPUT_DIR%" rmdir /s /q "%OUTPUT_DIR%"
echo       Done.

:: ── Step 4: Run jpackage ─────────────────────────────────────────────────────
echo [4/4] Running jpackage (type=%TYPE%)...

set "JPACKAGE_ARGS=--input "%INPUT_DIR%" ^
  --main-jar "%JAR_NAME%" ^
  --main-class "%MAIN_CLASS%" ^
  --name "%APP_NAME%" ^
  --app-version %APP_VERSION% ^
  --dest "%OUTPUT_DIR%" ^
  --java-options "--enable-native-access=ALL-UNNAMED" ^
  --java-options "-Xms128m" ^
  --java-options "-Xmx512m" ^
  --type %TYPE%"

if exist "%ICON%" (
    set "JPACKAGE_ARGS=%JPACKAGE_ARGS% --icon "%ICON%""
)

if /i "%TYPE%"=="exe" (
    set "JPACKAGE_ARGS=%JPACKAGE_ARGS% --win-menu --win-shortcut --win-dir-chooser --win-menu-group "EDITH-J" --win-upgrade-uuid 7f3e2c1a-4b8d-4f9e-a2c3-1d5e6f7a8b9c"
    set "PATH=%ProgramFiles(x86)%\WiX Toolset v3.14\bin;%PATH%"
)
if /i "%TYPE%"=="msi" (
    set "JPACKAGE_ARGS=%JPACKAGE_ARGS% --win-menu --win-shortcut --win-dir-chooser --win-menu-group "EDITH-J" --win-upgrade-uuid 7f3e2c1a-4b8d-4f9e-a2c3-1d5e6f7a8b9c"
    set "PATH=%ProgramFiles(x86)%\WiX Toolset v3.14\bin;%PATH%"
)

jpackage %JPACKAGE_ARGS%
if errorlevel 1 (
    echo.
    echo [ERROR] jpackage failed.
    if /i "%TYPE%"=="exe" echo        EXE/MSI installers require WiX Toolset v3:
    if /i "%TYPE%"=="msi" echo        EXE/MSI installers require WiX Toolset v3:
    if /i "%TYPE%"=="exe" echo        https://github.com/wixtoolset/wix3/releases
    if /i "%TYPE%"=="msi" echo        https://github.com/wixtoolset/wix3/releases
    if /i "%TYPE%"=="exe" echo        Or run without arguments for a portable app-image instead.
    if /i "%TYPE%"=="msi" echo        Or run without arguments for a portable app-image instead.
    pause
    exit /b 1
)

echo.
echo ════════════════════════════════════════════════════════
echo   Build complete!
echo   Output → %OUTPUT_DIR%
if /i "%TYPE%"=="app-image" echo   Run    → %OUTPUT_DIR%\%APP_NAME%\%APP_NAME%.exe
echo ════════════════════════════════════════════════════════
echo.
pause
