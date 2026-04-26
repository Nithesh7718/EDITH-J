@echo off
setlocal EnableDelayedExpansion

set "APP_HOME=%~dp0"
set "JAR_NAME=edith-j-0.1.0-SNAPSHOT-all.jar"
set "JAR=%APP_HOME%target\%JAR_NAME%"

:: ── Verify JAR exists ────────────────────────────────────────────────────────
if not exist "%JAR%" (
    echo [EDITH-J] JAR not found: %JAR%
    echo [EDITH-J] Run  mvn clean package  first, then try again.
    pause
    exit /b 1
)

:: ── JVM options ──────────────────────────────────────────────────────────────
set "JAVA_OPTS=--enable-native-access=ALL-UNNAMED -Xms128m -Xmx512m"

:: ── Launch ───────────────────────────────────────────────────────────────────
echo [EDITH-J] Starting...
start "EDITH-J" javaw %JAVA_OPTS% -jar "%JAR%"
