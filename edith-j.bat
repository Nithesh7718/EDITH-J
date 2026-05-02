@echo off
setlocal EnableExtensions EnableDelayedExpansion

set "APP_HOME=%~dp0"
set "TARGET_DIR=%APP_HOME%target"
set "JAR_PATH="

for %%F in ("%TARGET_DIR%\edith-j-*-all.jar") do (
    set "JAR_PATH=%%~fF"
)

if not defined JAR_PATH (
    echo [EDITH-J] Shaded JAR not found in %TARGET_DIR%.
    echo [EDITH-J] Run mvn clean package first, then try again.
    exit /b 1
)

set "JAVA_OPTS=--enable-native-access=ALL-UNNAMED -Xms128m -Xmx512m"

echo [EDITH-J] Starting from !JAR_PATH!
start "" javaw %JAVA_OPTS% -jar "!JAR_PATH!"
