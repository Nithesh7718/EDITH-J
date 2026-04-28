# EDITH-J

EDITH-J is an intelligent desktop assistant with a React frontend and a Java 25 backend powered by Javalin. Typed and voice input flow through a shared intent pipeline for notes, reminders, desktop automation, and AI-assisted chat.

## Features

- **Unified intent routing** for notes, reminders, launcher actions, weather, and fallback chat
- **Desktop automation** for app launch, web search, music, and basic file operations
- **AI integration** with Groq, Gemini, OpenAI, and Sarvam providers
- **Persistence** through SQLite-backed chat history, notes, and reminders
- **Voice support** through the assistant speech pipeline with Vosk model integration

## Project Layout

- `edith-ui/` - React frontend source code
- `src/main/java/com/edithj/api` - Javalin REST API server
- `src/main/java/com/edithj/app` - Application entry point
- `src/main/java/com/edithj/assistant` - Intent classification and orchestration
- `src/main/java/com/edithj/commands` - Command handlers for assistant tools
- `src/main/java/com/edithj/ai` - LLM provider integrations
- `src/main/java/com/edithj/storage` - Persistence and repository wiring
- `src/main/resources/public` - Built frontend assets served by the backend

## Requirements

- Java 25 LTS
- Node.js and npm
- Maven 3.9+

## How to Build and Run

### 1. Build the UI

```bash
cd edith-ui
npm install
npm run build
cd ..
```

### 2. Build the app

```bash
mvn clean package
```

### 3. Run

```bash
java -jar target/edith-j-0.1.0-SNAPSHOT-all.jar
```

For development you can also use:

```bash
mvn exec:java
```

By default the backend opens the UI at `http://localhost:8080`.

## API Overview

- `GET /api/chat/history`
- `POST /api/chat`
- `GET /api/notes`
- `POST /api/notes`
- `GET /api/reminders`
- `POST /api/reminders`
- `POST /api/automation/open-app`
- `POST /api/automation/web-search`
- `POST /api/automation/file`

## Configuration

Configuration is resolved in this order:

1. Environment variables
2. `edith.properties` in the project root
3. Built-in defaults

Example `edith.properties`:

```properties
# AI Configuration
edith.ai.provider=groq
edith.ai.workspaceDir=C:/path/to/workspace

# Provider API Keys (or set as env vars)
edith.ai.groq.apiKey=gsk_...
edith.ai.gemini.apiKey=
edith.ai.openai.apiKey=
edith.ai.sarvam.apiKey=

# Automation
edith.automation.musicUrl=https://music.youtube.com

# Desktop Automation Toggles
edith.desktop.fileOpenEnabled=true
edith.desktop.clipboardWriteEnabled=true

# Launcher Overrides
edith.launch.notepad=C:/Windows/System32/notepad.exe

# Voice Support
speech.vosk.model-path=models/vosk-model-small-en-us-0.15
```

## Release

To produce a Windows installer:

```bash
mvn package -P windows-installer
```

The installer will be generated in `target/installer/`.
