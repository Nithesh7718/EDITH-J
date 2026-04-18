# EDITH-J

EDITH-J is a Java 17 JavaFX desktop assistant. It routes typed and voice input through a single assistant service so notes, reminders, launch commands, utilities, weather, and fallback chat all use the same intent pipeline.

## What it does

- Intent routing for notes, reminders, launcher actions, weather, utilities, desktop tools, and fallback chat
- Typed and voice input through `AssistantService`
- Local persistence for notes and reminders
- Groq-backed chat fallback when no specific command intent matches

## Project Layout

- `src/main/java/com/edithj/app` - application startup
- `src/main/java/com/edithj/assistant` - intent routing and orchestration
- `src/main/java/com/edithj/ai` - chat abstractions and Groq chat service wrapper
- `src/main/java/com/edithj/commands` - command handlers
- `src/main/java/com/edithj/launcher` - OS launch helpers
- `src/main/java/com/edithj/memory` - lightweight assistant memory models and service
- `src/main/java/com/edithj/notes` - note domain and repositories
- `src/main/java/com/edithj/reminders` - reminder domain and repositories
- `src/main/java/com/edithj/speech` - speech capture and typed fallback
- `src/main/java/com/edithj/ui` - JavaFX views, controllers, and navigation
- `src/main/java/com/edithj/storage` - SQLite and JSON storage helpers
- `src/main/resources` - FXML, CSS, prompts, and seeded data

## Requirements

- Java 17
- Maven 3.9+
- Optional: `GROQ_API_KEY` for chat fallback

## Configuration

Secrets should stay out of tracked files.

- Set `GROQ_API_KEY` in your environment for Groq chat replies.
- Optional overrides can be placed in `edith.properties`, which is ignored by git.

## Build And Test

```bash
mvn clean test
```

## Run

```bash
mvn javafx:run
```

## Storage

Notes and reminders are stored under `~/.edith-j/data/` by default.

- Primary backend: SQLite (`~/.edith-j/data/edith.db`)
- Fallback backend: JSON files when SQLite is unavailable

## Notes

- The chat view now routes through `AssistantService` instead of talking to Groq directly.
- The UI controllers are thin and delegate to services or the navigation layer.
