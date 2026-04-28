# EDITH-J

EDITH-J is a Windows-first JavaFX desktop assistant built on Java 17. All typed and voice input flows through a single `AssistantService` pipeline that handles notes, reminders, app launching, utilities, weather, desktop tools, and open-ended AI chat backed by Groq.

## Features

- **Intent routing** — deterministic handlers for notes, reminders, launcher, weather, utilities, and desktop tools
- **AI chat fallback** — open-ended questions are answered by Groq (`llama-3.3-70b-versatile` by default) when no specific intent matches
- **Conversation memory** — recent interactions are stored in SQLite and injected into the system prompt to give the assistant context across sessions
- **Voice input** — microphone capture is implemented; transcription requires a `TranscriptionEngine` to be wired in (falls back to typed input otherwise)
- **Local persistence** — notes, reminders, and memory entries stored in SQLite with JSON fallback
- **Live status badge** — the UI probes Groq on startup and shows online/offline state

## Project Layout

```
src/main/java/com/edithj/
├── app/          Application entry point (EdithApplication, Launcher, Bootstrap)
├── assistant/    Intent routing, orchestration, and Groq status tracking
├── ai/           ChatService interface, GroqChatService adapter, PromptTemplateService
├── commands/     CommandHandler implementations (notes, reminders, launcher, weather, …)
├── config/       AppConfig, ModelConfig, StorageConfig, EnvConfig
├── integration/
│   └── llm/      LlmClient interface, GroqClient, PromptBuilder
├── launcher/     OS app/URL launcher (Windows + cross-platform)
├── memory/       MemoryEntry, MemoryService, SQLite and in-memory repositories
├── notes/        Note domain, NoteService, SQLite and JSON repositories
├── reminders/    Reminder domain, ReminderService, SQLite and JSON repositories
├── speech/       AudioCapture, SpeechRecognizer, TypedFallbackService
├── storage/      DatabaseManager, RepositoryFactory, migration service, StoragePaths
├── ui/           JavaFX controllers, view models, and SceneManager
└── util/         BackgroundTaskRunner, TimeParser, ValidationUtils
src/main/resources/
├── fxml/         FXML layouts (main shell, chat, notes, reminders views)
├── css/          Application stylesheet
└── prompts/      System prompt loaded at runtime
```

## Requirements

- Java 17
- Maven 3.9+
- Windows (JavaFX dependencies are currently packaged with the `win` classifier)
- Optional: `GROQ_API_KEY` for AI chat replies

## Configuration

Secrets must not be committed. Set environment variables before running:

| Variable | Required | Default | Description |
|---|---|---|---|
| `GROQ_API_KEY` | No | — | Enables Groq-backed AI chat; without it, the assistant returns a prompt to set the key |
| `GROQ_MODEL` | No | `llama-3.3-70b-versatile` | Groq model to use |
| `GROQ_BASE_URL` | No | `https://api.groq.com/openai/v1` | Groq API base URL |
| `GROQ_TIMEOUT_SECONDS` | No | `30` | Request timeout in seconds |
| `GROQ_TEMPERATURE` | No | `0.2` | Sampling temperature (0.0–2.0) |
| `EDITH_STORAGE_BACKEND` | No | `sqlite` | Storage backend (`sqlite`) |
| `EDITH_DB_PATH` | No | `~/.edith-j/data/edith.db` | Path to the SQLite database file |

Local property overrides can be placed in `edith.properties` in the working directory (ignored by git). These override `application.properties` defaults but are superseded by environment variables.

## Build and Test

```bash
mvn clean test
```

The CI suite excludes `GroqConnectivityProbeTest` (a live network test requiring `GROQ_API_KEY`). To run it locally:

```bash
GROQ_API_KEY=<your-key> mvn clean test
```

## Package

Produces a runnable fat JAR at `target/edith-j-*-all.jar`:

```bash
mvn clean package
```

## Run

```bash
mvn javafx:run
```

## Storage

Data is stored under `~/.edith-j/data/` by default:

- **Primary backend:** SQLite (`edith.db`) — notes, reminders, memory entries, migration markers
- **Fallback:** JSON files when SQLite is unavailable
- On first launch a one-time migration copies any existing JSON data into SQLite

## Voice Input

Audio capture from the microphone is fully implemented using `javax.sound.sampled` (16 kHz / 16-bit mono WAV). Speech-to-text transcription is not bundled — the `SpeechRecognizer` accepts a pluggable `TranscriptionEngine`. When no engine is wired in, the assistant prompts the user to type their input instead.

## Architecture Notes

- All UI controllers are thin and delegate to domain services or the navigation layer; no business logic lives in controllers.
- `AssistantService` is the single entry point for both typed and voice input; command handlers and the Groq fallback are registered there.
- `FallbackChatService` maintains a rolling conversation window and enriches the system prompt with long-term memory entries from SQLite before each Groq call.
- `RepositoryFactory` selects SQLite or falls back to JSON/in-memory repositories transparently, so services never reference storage directly.
