# EDITH-J

EDITH-J is an intelligent desktop assistant with a React frontend and a Java 21 (Javalin) backend. It routes typed and voice input through a unified intent pipeline supporting notes, reminders, desktop automation, and multi-provider AI chat.

## Architecture

- **Frontend**: React + Vite (located in `edith-ui/`). Built and served by the Java backend.
- **Backend**: Java 21 + Javalin. Handles logic, persistence (SQLite), and AI orchestration.
- **Communication**: REST API.

## What it does

- **Unified Intent Routing**: Notes, reminders, launcher actions, weather, and fallback chat.
- **Desktop Automation**: Open apps, play music, web search, and file management (create/rename/move).
- **AI Integration**: Support for Groq, Gemini, OpenAI, and Sarvam.
- **Persistence**: SQLite-backed storage for chat history, notes, and reminders.
- **Voice Support**: Integrated STT/TTS pipeline through `AssistantService`.

## Project Layout

- `edith-ui/` - React frontend source code.
- `src/main/java/com/edithj/api` - Javalin REST API server.
- `src/main/java/com/edithj/app` - Application entry point (`Launcher.java`).
- `src/main/java/com/edithj/assistant` - Intent classification and orchestration logic.
- `src/main/java/com/edithj/commands` - Command handlers for specific tools.
- `src/main/java/com/edithj/ai` - LLM provider integrations.
- `src/main/java/com/edithj/storage` - SQLite persistence layer.
- `src/main/resources/public` - Static assets for the React UI.

## Requirements

- Java 21 LTS
- Node.js & npm (for building the frontend)
- Maven 3.9+

## How to Build & Run

### 1. Build the UI
```bash
cd edith-ui
npm install
npm run build
cd ..
```

### 2. Build and Package
```bash
mvn clean package
```
This builds the Java backend and bundles the React assets into the JAR.

### 3. Run
```bash
java -jar target/edith-j-0.1.0-SNAPSHOT-all.jar
```
Or use the development runner:
```bash
mvn exec:java
```
Upon launch, the application will start the backend server and automatically open the UI in your default system browser at `http://localhost:8080`.

## API Overview

### Chat
- `GET  /api/chat/history` - Retrieve recent chat messages.
- `POST /api/chat`         - Send a message to the assistant.

### Notes & Reminders
- `GET  /api/notes` / `POST /api/notes`
- `GET  /api/reminders` / `POST /api/reminders`

### Automation
- `POST /api/automation/open-app`   - `{ "app": "Notepad" }`
- `POST /api/automation/web-search` - `{ "query": "Weather today" }`
- `POST /api/automation/file`       - `{ "action": "open", "path": "notes.txt" }`

## Configuration

Secrets are managed via environment variables:
- `GROQ_API_KEY`, `GEMINI_API_KEY`, `OPENAI_API_KEY`, `SARVAM_API_KEY`

Optional overrides can be placed in `edith.properties` at the root.

## Release

To produce a self-contained Windows `.exe` installer:
```bash
mvn package -P windows-installer
```
The installer will be generated in `target/installer/`.
