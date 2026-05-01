# EDITH-J

EDITH-J is a Java 25 + Javalin desktop assistant with a React + Vite frontend, SQLite persistence, provider-backed AI chat, desktop automation, and optional offline speech recognition through Vosk.

## Architecture

- `edith-ui/` contains the React frontend source.
- `src/main/java/` contains the backend, API, assistant logic, storage, speech, and launcher code.
- `models/vosk-model-small-en-us-0.15/` contains the bundled default offline speech model used by packaged Windows builds.
- `src/main/packaging/edith-j.ico` is the Windows installer icon.

## Runtime Model

EDITH-J no longer depends on the repository root after packaging.

- User config: `%APPDATA%\EDITH-J\edith.properties`
- User config template: `%APPDATA%\EDITH-J\edith.properties.example`
- User data: `%LOCALAPPDATA%\EDITH-J\data`
- User logs: `%LOCALAPPDATA%\EDITH-J\logs`
- User temp: `%LOCALAPPDATA%\EDITH-J\temp`
- Default workspace: `%USERPROFILE%\Documents\EDITH-workspace`

If no user config exists, the app still starts with safe defaults and writes a local `edith.properties.example` template for reference.

## Requirements

- Java 25
- Maven 3.9+
- Node.js 20+ and npm
- Windows packaging only:
  - `jpackage` available from the JDK
  - WiX Toolset v3.14 installed for `exe` or `msi` installers

## Build The App

### Build the frontend and shaded JAR

```bash
mvn clean package
```

This does all of the following:

- runs `npm ci` in `edith-ui/`
- runs the Vite production build
- copies the frontend build into the backend classpath at packaging time
- creates the shaded runtime JAR at `target/edith-j-0.1.0-SNAPSHOT-all.jar`

### Run from the shaded JAR

```bash
java -jar target/edith-j-0.1.0-SNAPSHOT-all.jar
```

Or on Windows:

```bat
edith-j.bat
```

## Build A Windows Installer

### Maven path

```bash
mvn clean package -P windows-installer
```

That profile stages:

- the shaded JAR
- the bundled Vosk model directory
- `edith.properties.example`

Then it runs `jpackage` and writes the installer into:

```text
target/installer/
```

Default output type is `exe`.

To produce `msi` instead:

```bash
mvn clean package -P windows-installer -Djpackage.type=msi
```

### Batch helper

Portable app-image:

```bat
build-installer.bat
```

Windows `.exe` installer:

```bat
build-installer.bat exe
```

Windows `.msi` installer:

```bat
build-installer.bat msi
```

## Installer Contents

A packaged Windows build is expected to include:

- the shaded application JAR
- the bundled React frontend assets
- the bundled Vosk model under `models/`
- a config template under `conf/edith.properties.example`
- the application icon and Windows metadata

## Configuration

Configuration precedence is:

1. Environment variables
2. User config at `%APPDATA%\EDITH-J\edith.properties`
3. Bundled config at `conf/edith.properties` if present in an installed app
4. `edith.properties` in the current working directory for development
5. Built-in defaults

Use [`edith.properties.example`](./edith.properties.example) as the reference template.

Important active keys:

```properties
app.name=EDITH-J
app.host=127.0.0.1
app.port=8080
app.auto-open-browser=true

edith.ai.provider=groq
edith.ai.workspaceDir=C:/Users/your-user/Documents/EDITH-workspace

storage.backend=sqlite
storage.db-path=edith.db

speech.vosk.model-path=models/vosk-model-small-en-us-0.15
speech.audio.input-device-name=
```

Notes:

- `storage.db-path` may be relative; relative paths resolve under `%LOCALAPPDATA%\EDITH-J\data`.
- `speech.vosk.model-path` may be omitted for packaged builds because the installer bundles the default model.
- API keys remain optional. When missing, EDITH-J starts normally and returns clear provider-specific guidance instead of crashing.

## Release Artifact Naming

Use this naming convention for Windows release uploads:

- `EDITH-J-0.1.0-windows-x64.exe`
- `EDITH-J-0.1.0-windows-x64.msi`
- `EDITH-J-0.1.0-windows-x64-app-image.zip`

## Release Readiness

- Windows release checklist: [`docs/windows-release-checklist.md`](./docs/windows-release-checklist.md)
- Installer verification checklist: [`docs/installer-verification-checklist.md`](./docs/installer-verification-checklist.md)

## API Overview

- `GET /api/health`
- `GET /api/chat/history`
- `POST /api/chat`
- `GET /api/notes`
- `POST /api/notes`
- `GET /api/reminders`
- `POST /api/reminders`
- `POST /api/automation/open-app`
- `POST /api/automation/web-search`
- `POST /api/automation/file`
- `GET /api/voice/status`
- `POST /api/voice/start`
- `POST /api/voice/stop`
