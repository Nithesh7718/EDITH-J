# EDITH-J

EDITH-J is a self-healing desktop AI runtime for resilient orchestration of AI, voice, automation, and persistence. It is designed to operate as a fault-aware runtime control plane that detects problems, diagnoses failures, executes bounded recovery actions, and verifies restored stability.

## Runtime Identity

EDITH-J is built as a resilient runtime, not just an assistant app. It maintains continuity by monitoring subsystem health and recovering from failures in packaged AI, voice, storage, automation, and startup paths.

### Core capabilities

- subsystem health monitoring with structured `HealthSignal` events
- incident detection, diagnosis, and persistent logging
- provider failover and degraded-mode execution
- packaged frontend and speech-model validation during startup
- SQLite lock recovery and reconnect handling
- adaptive failure memory for repeated incident patterns
- post-recovery verification and runtime metrics

## Architecture

- `edith-ui/` contains the React frontend source.
- `src/main/java/` contains the backend runtime, API, resilience layer, assistant orchestration, storage, and speech pipeline.
- `models/vosk-model-small-en-us-0.15/` contains the bundled offline speech model used by packaged Windows builds.
- `docs/` contains self-healing architecture and invention documentation.

## Runtime Model

EDITH-J is packaged as a standalone runtime with local storage and asset validation.

- User config: `%APPDATA%\EDITH-J\edith.properties`
- User config template: `%APPDATA%\EDITH-J\edith.properties.example`
- User data: `%LOCALAPPDATA%\EDITH-J\data`
- User logs: `%LOCALAPPDATA%\EDITH-J\logs`
- User temp: `%LOCALAPPDATA%\EDITH-J\temp`
- Default workspace: `%USERPROFILE%\Documents\EDITH-workspace`

When configured without a user file, EDITH-J starts with safe defaults and still applies self-healing runtime behavior.

## Health and Metrics

- `GET /api/health` returns service status and health summary.
- `GET /api/resilience/metrics` returns measurable runtime metrics such as startup success rate, mean time to detect, mean time to recover, provider failover success rate, and degraded-mode continuity.

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

This performs:

- `npm ci` in `edith-ui/`
- Vite production build
- copying the frontend output into backend classpath
- creating the shaded runtime JAR at `target/edith-j-0.1.0-SNAPSHOT-all.jar`

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
- bundled React frontend assets
- bundled Vosk speech model
- `edith.properties.example`

It writes installers into:

```text
target/installer/
```

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

A packaged Windows build should include:

- the shaded application JAR
- packaged React frontend assets
- bundled Vosk model under `models/`
- config template at `conf/edith.properties.example`
- application metadata and icon

## Configuration

Configuration precedence is:

1. Environment variables
2. User config at `%APPDATA%\EDITH-J\edith.properties`
3. Bundled config at `conf/edith.properties`
4. `edith.properties` in the current working directory for development
5. Built-in defaults

Use [`edith.properties.example`](./edith.properties.example) as a reference template.

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

- `storage.db-path` may be relative and resolves under `%LOCALAPPDATA%\EDITH-J\data`.
- `speech.vosk.model-path` may be omitted in packaged builds because the installer bundles the default model.
- Missing API keys are handled gracefully by the runtime.

## Release Artifact Naming

- `EDITH-J-0.1.0-windows-x64.exe`
- `EDITH-J-0.1.0-windows-x64.msi`
- `EDITH-J-0.1.0-windows-x64-app-image.zip`

## Release Readiness

- Windows release checklist: [`docs/windows-release-checklist.md`](./docs/windows-release-checklist.md)
- Installer verification checklist: [`docs/installer-verification-checklist.md`](./docs/installer-verification-checklist.md)

## Documentation

- `docs/invention-note.md`
- `docs/technical-problem.md`
- `docs/system-architecture.md`
- `docs/recovery-lifecycle.md`

## API Overview

- `GET /api/health`
- `GET /api/resilience/metrics`
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
