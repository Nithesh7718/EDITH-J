# EDITH-J Master Build Plan

A complete execution blueprint for turning EDITH-J into a stable desktop AI assistant with a strong AI core, modern JavaFX UI, embedded database, secure configuration, test coverage, and release workflow. The current repository already includes JavaFX app structure, domain packages for assistant features, Maven build setup, FXML/CSS resources, local persistence, and Groq-related fallback behavior, so this plan focuses on refactoring and completing the existing codebase rather than rewriting it from scratch. [cite:294][cite:255][cite:296]

## Product goal

EDITH-J should become a Windows-first desktop AI assistant with these core capabilities: chat-first assistant experience, deterministic notes/reminders/launcher tools, Groq-powered open-ended reasoning, optional voice input, persistent memory, and a polished UI shell. The repository already has modules for assistant flow, notes, reminders, speech, UI, and storage, which makes this target realistic if the architecture is tightened and the project is narrowed to a stronger v1. [cite:294]

## Locked tech stack

| Layer | Final choice | Notes |
|---|---|---|
| Primary OS | Windows-first | Cross-platform can come later after launcher and desktop integration stabilize. [cite:294] |
| JDK | Java 21 LTS | Standardize one runtime to reduce ecosystem drift. The repo currently documents Java 17, so this should be updated consistently. [cite:294] |
| UI | JavaFX + FXML + CSS | Matches the current codebase and avoids unnecessary rewrite cost. [cite:294] |
| Build tool | Maven | Already used by the project. [cite:294] |
| AI chat | Groq text chat API | Groq supports chat/text generation and streaming-style assistant use cases. [cite:255][cite:295] |
| STT | Groq Speech-to-Text first | Groq documents Whisper-based speech-to-text support. [cite:296] |
| Wake word | openWakeWord later | Optional enhancement for voice mode. [cite:274] |
| TTS | System TTS first | Keep v1 simple, then upgrade later. [cite:273] |
| DB | SQLite | Best fit for embedded desktop persistence. [cite:297][cite:299] |
| Config | Env vars + placeholder example file | Prevent secret leaks and match best practice. [cite:294] |
| Tests | JUnit 5 + Mockito + integration smoke | Good balance for release readiness. [cite:294] |
| CI | GitHub Actions | Needed for build/test discipline. [cite:294] |

## Architecture target

EDITH-J should be organized into five layers so features stop leaking into each other:

1. Presentation layer: JavaFX views, controllers, UI state, navigation, theming. [cite:294]
2. Application layer: assistant orchestration, conversation flow, workflow coordinator. [cite:294]
3. Intelligence layer: intent routing, tool planning, Groq interaction, memory retrieval, response composition. [cite:255][cite:294]
4. Domain layer: notes, reminders, launcher, settings, profile, conversations, memory. [cite:294]
5. Infrastructure layer: SQLite repositories, config loading, Groq client, speech adapters, logging, OS adapters. [cite:294][cite:297]

This layered split is required because the current repository already spans multiple concerns and will become harder to scale if the assistant logic, UI logic, storage, and AI calls remain mixed. [cite:294]

## Current repo understanding

The repository currently exposes a JavaFX desktop application with packages and features around assistant flow, commands, launcher, notes, reminders, speech, UI, and storage. It also includes FXML, CSS, prompts, data resources, Maven build files, and tracked config such as `edith.properties`, while the README describes typed and voice input, notes, reminders, weather, utilities, and fallback AI behavior. [cite:294]

This means the project is already beyond the "toy app" stage, but it still behaves more like a broad prototype than a tightly engineered desktop assistant. The next phase should focus on hardening the core loop rather than adding more feature breadth. [cite:294]

## Non-negotiable decisions

- Real secrets must never be committed; runtime secrets should come from environment variables like `GROQ_API_KEY`. [cite:294]
- `edith.properties` in the repository must contain placeholders only, or be replaced by `edith.properties.example`. [cite:294]
- JDK 21 LTS becomes the single standard runtime. [cite:294]
- JavaFX remains the UI stack for v1. [cite:294]
- SQLite becomes the primary persistence layer; JSON/file storage may remain only for import/export or temporary migration. [cite:297][cite:299]
- Deterministic tools handle notes/reminders/launcher first; Groq is used for open-ended reasoning and ambiguity resolution, not as the default handler for everything. [cite:255][cite:294]

## Copilot operating model

Use GitHub Copilot in a controlled way instead of asking it to rewrite the entire repository blindly.

### Recommended Copilot mode sequence

1. Ask mode for repo analysis and understanding.
2. Edit mode for file-by-file refactors.
3. Inline Chat for local targeted changes.
4. Agent mode only for bounded multi-file tasks. [cite:294]

### Rules for Copilot

- Never let Copilot generate or commit secrets.
- Require Copilot to explain every change before applying it.
- Refactor one package at a time.
- Run tests after each package-level change.
- Keep commits small and reversible.
- Prefer extraction and restructuring over large rewrites.

## File-by-file implementation plan

## Root level

### `pom.xml`

**What it likely has now:** Maven setup, JavaFX dependencies/plugins, current runtime settings. [cite:294]

**What it should implement:**
- Java 21 compiler target.
- Explicit JavaFX plugin config.
- Shade plugin for a runnable fat JAR.
- Surefire and, if needed later, Failsafe separation.
- Dependencies for SQLite JDBC, logging, JUnit 5, Mockito, Jackson if needed. [cite:294][cite:297]

**Copilot prompt:**
```text
Refactor pom.xml for a Windows-first JavaFX desktop assistant on JDK 21 LTS. Keep Maven. Add or clean up JavaFX plugin config, SQLite JDBC, SLF4J + Logback, JUnit 5, Mockito, and a shaded JAR build path. Do not add unnecessary frameworks. Show the final dependency and plugin rationale before editing.
```

### `.gitignore`

**What it likely has now:** general ignores, but not fully aligned with secret and runtime hygiene because `edith.properties` is visible in the repo. [cite:294]

**What it should implement:**
- ignore `edith.properties`
- ignore `.env` and `.env.*`
- ignore `*.db`, `logs/`, `recordings/`, `target/`, IDE files, temp exports

**Copilot prompt:**
```text
Audit .gitignore for a JavaFX desktop AI assistant. Add ignores for local secrets, SQLite db files, logs, audio recordings, temp exports, target output, and IDE files. Keep tracked example config files only.
```

### `edith.properties` / `edith.properties.example`

**What it likely has now:** tracked configuration, possibly including live-key format exposure risk. [cite:294]

**What it should implement:**
- placeholders only
- no live secrets
- move real config loading to env vars and optional local override file outside git

**Copilot prompt:**
```text
Replace any real or risky values in edith.properties with placeholders only. If appropriate, rename tracked config to edith.properties.example and adjust config loading so runtime secrets come from environment variables like GROQ_API_KEY.
```

### `README.md`

**What it likely has now:** feature overview, package listing, run/test instructions. [cite:294]

**What it should implement:**
- secure setup steps
- JDK 21 standardization
- architecture overview
- runbook for `mvn javafx:run`, tests, package
- screenshots section placeholder
- known limitations
- troubleshooting
- top user flows

**Copilot prompt:**
```text
Rewrite README.md into a release-quality runbook for EDITH-J. Keep it concise but complete. Include architecture overview, secure setup, GROQ_API_KEY usage, JDK 21, run/test/package commands, top user flows, troubleshooting, and known limitations.
```

## `src/main/java/.../app`

### App bootstrap

**What it likely has now:** JavaFX application entrypoint and startup. [cite:294]

**What it should implement:**
- centralized bootstrap
- config loading
- logging initialization
- SQLite initialization and migrations
- service registry or lightweight app context
- graceful shutdown hooks

**Copilot prompt:**
```text
Refactor the application bootstrap so startup is clean and explicit. Create or improve classes for config initialization, logging startup, database initialization, service wiring, and graceful shutdown. Keep the design simple and suitable for a JavaFX desktop app.
```

## `src/main/java/.../assistant`

### Assistant orchestration

**What it likely has now:** `AssistantService` as the main pipeline. [cite:294]

**What it should implement:**
- `AssistantService` as orchestrator only
- `IntentRouter`
- `ToolPlanner`
- `ToolExecutionService`
- `ConversationService`
- `ResponseComposer`
- unified `AssistantResponse`

**Copilot prompt:**
```text
Refactor the assistant package so AssistantService becomes a thin orchestrator. Extract intent routing, tool planning, tool execution, conversation management, and response composition into separate classes. Add a unified AssistantResponse model used by UI and services.
```

### Unified response model

**What it should implement:**
- response text
- source type (`groq`, `notes`, `reminders`, `launcher`, etc.)
- success/failure
- metadata map
- optional structured payload

**Copilot prompt:**
```text
Create a unified AssistantResponse model that supports normal replies, tool results, errors, and metadata. Use it across assistant-related services to reduce UI coupling.
```

## `src/main/java/.../ai` (new package)

This package should be created if it does not exist.

**What it should implement:**
- `GroqClient`
- `GroqChatService`
- `PromptTemplateService`
- `SystemPromptBuilder`
- `StreamingResponseHandler`
- `ChatHistoryWindow`

Groq’s docs explicitly support text chat and API usage suitable for assistant-style calls, and this should become a first-class subsystem rather than a fallback side feature. [cite:255][cite:258][cite:295]

**Copilot prompt:**
```text
Create a dedicated ai package for Groq integration. Implement clean separation between raw API client, chat service, prompt building, and response parsing. Prepare for streaming responses in the UI even if the first version still returns fully assembled text. Handle missing GROQ_API_KEY, HTTP errors, and malformed responses explicitly.
```

## `src/main/java/.../config` (new package)

**What it should implement:**
- `AppConfig`
- `EnvConfigLoader`
- `PropertiesConfigLoader`
- `SecretValidator`
- `ModelConfig`

**Copilot prompt:**
```text
Create a config package that centralizes app settings, environment-variable loading, optional placeholder property loading, and secret validation. Remove scattered config access from other classes and keep runtime secret handling explicit.
```

## `src/main/java/.../commands`

### Command contracts

**What it likely has now:** feature handlers for notes, reminders, utilities, weather, and other assistant commands. [cite:294]

**What it should implement:**
- standard command interface
- command context
- command result mapped to `AssistantResponse`
- registry-based command lookup

**Copilot prompt:**
```text
Refactor the commands package around a consistent AssistantCommand interface and CommandContext model. Make command execution deterministic, testable, and easy to register. Return results through AssistantResponse.
```

## `src/main/java/.../launcher`

### Desktop integrations

**What it likely has now:** launcher helpers. [cite:294]

**What it should implement:**
- allowlisted applications only
- Windows-specific adapter
- registry of supported launch targets
- safety validation
- clear errors for unsupported commands

**Copilot prompt:**
```text
Refactor launcher support into a Windows-first allowlisted service. Add a registry of supported apps and block unsafe arbitrary command execution. Return user-friendly errors for unknown app names.
```

## `src/main/java/.../notes`

### Notes domain

**What it likely has now:** notes model/service/repository using local persistence. [cite:294]

**What it should implement:**
- SQLite repository
- note CRUD
- tags
- search
- pinning
- optional summary field
- import/export compatibility if JSON exists today

**Copilot prompt:**
```text
Refactor the notes package into a stronger domain module with Note model, NotesService, repository abstraction, and SQLite implementation. Preserve current note functionality, add search/tag/pin support, and keep migration from existing storage practical.
```

## `src/main/java/.../reminders`

### Reminders domain

**What it likely has now:** reminders model/service/repository using local persistence. [cite:294]

**What it should implement:**
- SQLite-backed reminders
- active/completed/snoozed states
- due-time parsing normalization
- reminder scheduler
- notification trigger service
- recurrence-ready schema

**Copilot prompt:**
```text
Refactor reminders into a robust module with repository abstraction, SQLite implementation, normalized schedule fields, and statuses like active/completed/snoozed. Keep the first pass simple but architect it for recurrence and notifications later.
```

## `src/main/java/.../speech`

### Voice stack

**What it likely has now:** speech input and typed fallback. [cite:294]

**What it should implement:**
- microphone capture service
- STT adapter
- voice session state machine
- optional TTS adapter
- optional wake-word bridge

Groq’s speech-to-text docs provide a good first remote STT path, while openWakeWord is suitable as a later wake-word enhancement. [cite:296][cite:274]

**Copilot prompt:**
```text
Refactor the speech package into clear services for microphone capture, speech-to-text, optional text-to-speech, and voice session state. Keep typed input working. Prepare extension points for wake-word support later.
```

## `src/main/java/.../storage`

### Persistence and migration

**What it likely has now:** local storage helpers, possibly JSON/file persistence. [cite:294]

**What it should implement:**
- `DatabaseManager`
- migration runner
- backup/export service
- JSON import/export compatibility
- storage health checks

**Copilot prompt:**
```text
Introduce a proper storage layer centered on SQLite. Add DatabaseManager, migration support, and backup/export hooks. If the current project uses JSON persistence, keep it only for import/export or migration support, not as the main long-term store.
```

## `src/main/java/.../memory` (new package)

### Assistant memory

A JARVIS-style assistant needs memory beyond raw chat history.

**What it should implement:**
- user preferences
- recent conversation summaries
- pinned context
- assistant memory entries
- retrieval methods for prompt enrichment

**Copilot prompt:**
```text
Create a memory package for EDITH-J with models and services for user preferences, recent conversation summaries, pinned context, and memory retrieval for prompt enrichment. Keep it lightweight and local-first.
```

## `src/main/java/.../ui`

### JavaFX shell and views

**What it likely has now:** controllers, views, CSS, navigation. [cite:294]

**What it should implement:**
- shell layout with sidebar, main chat, right-side context panel
- bottom input bar with send and mic
- UI state store
- conversation list/history
- notes/reminders/settings panels
- dark premium theme
- visible states: listening, thinking, typing, speaking, offline

**Copilot prompt:**
```text
Refactor the JavaFX UI into a modern assistant shell. Keep FXML and CSS. Build a sidebar, main chat area, optional right context panel, and bottom input bar with send/mic actions. Add visible assistant states such as listening, thinking, and offline. Keep controllers thin and move logic into services.
```

### Streaming response support

**What it should implement:**
- append partial model output into the chat UI
- support cancellation/interruption later
- show source badges like `AI`, `Notes`, `Reminder`, `Launcher`

**Copilot prompt:**
```text
Prepare the chat UI and assistant pipeline for streaming responses. Even if the current implementation remains synchronous, define interfaces and UI hooks that can append partial output later.
```

## `src/main/resources`

### `fxml/`

**What it likely has now:** JavaFX layouts. [cite:294]

**What it should implement:**
- `main-shell.fxml`
- `chat-view.fxml`
- `notes-panel.fxml`
- `reminders-panel.fxml`
- `settings-view.fxml`
- `voice-overlay.fxml`

**Copilot prompt:**
```text
Reorganize FXML views into a modular shell-based structure with separate views for main shell, chat, notes, reminders, settings, and optional voice overlay.
```

### `css/`

**What it likely has now:** styling files. [cite:294]

**What it should implement:**
- theme tokens
- dark theme first
- reusable panel and message styles
- mic/listening indicator styles
- status badges

**Copilot prompt:**
```text
Refactor JavaFX CSS into a reusable design system with theme variables, dark theme defaults, message bubble styles, panel styles, input bar styles, and assistant state visuals.
```

### `prompts/`

**What it likely has now:** prompt resources. [cite:294]

**What it should implement:**
- system prompt for EDITH persona
- tool-routing prompt
- summarization prompt
- memory extraction prompt
- fallback clarification prompt

**Copilot prompt:**
```text
Create a structured prompts folder for EDITH-J with separate prompt templates for system persona, tool routing, summarization, memory extraction, and fallback clarification. Avoid hardcoding long prompts inside Java classes.
```

### `application.properties`

**What it likely has now:** base app settings. [cite:294]

**What it should implement:**
- non-secret defaults only
- storage path
- theme
- feature flags
- debug logging level
- default model name

**Copilot prompt:**
```text
Clean application.properties so it contains only non-secret defaults such as theme, storage path, debug flags, and default model settings. Remove any sensitive values from resource config.
```

## Database design

Use SQLite as the primary embedded data store for desktop persistence. This is a better fit than pure JSON/file persistence because it supports relationships, search, state tracking, and clean growth while still being local and lightweight. [cite:297][cite:299]

### Minimum schema

- `users`
- `preferences`
- `conversations`
- `messages`
- `notes`
- `note_tags`
- `reminders`
- `tasks`
- `tool_runs`
- `memory_entries`
- `app_settings`

### Copilot prompt
```text
Design a lightweight SQLite schema for EDITH-J covering users, preferences, conversations, messages, notes, note tags, reminders, tasks, tool runs, memory entries, and app settings. Keep it simple, normalized, and suitable for a local desktop assistant.
```

## Testing plan

The project needs QA level B: unit tests plus integration smoke. The repo already documents Maven test usage, but the next step is to make testing meaningful enough to protect refactors. [cite:294]

### Priority tests

- config loading and missing secret handling
- intent routing
- notes CRUD
- reminders CRUD
- launcher safety behavior
- assistant fallback and Groq path selection
- storage initialization
- app bootstrap smoke path

### Copilot prompt
```text
Add or improve tests for config loading, intent routing, notes CRUD, reminders CRUD, launcher safety, assistant fallback logic, and database initialization. Use JUnit 5 and Mockito. Keep tests focused and runnable in Maven without external secrets.
```

## CI plan

The repository should add a GitHub Actions workflow that runs on push and pull request to keep the codebase stable. The current GitHub project view shows the Actions area but not a mature release process yet. [cite:294]

### Workflow should do

- checkout
- setup JDK 21
- cache Maven
- run `mvn -B clean test`
- optionally package the app

### Copilot prompt
```text
Create a GitHub Actions CI workflow for EDITH-J that runs on push and pull request, sets up JDK 21, caches Maven dependencies, runs Maven tests, and optionally packages the application artifact.
```

## Release deliverables

v1 should ship with:
- stable runnable app via `javafx:run`
- shaded runnable JAR
- clean test suite
- updated README runbook
- release notes and known limitations
- CI workflow

These deliverables align with the current structure of the repository and the changes needed to make it release-ready rather than just feature-rich. [cite:294]

## Recommended execution order

1. Security cleanup and config refactor.
2. `pom.xml` runtime/build cleanup.
3. Config package creation.
4. AI package creation and Groq extraction.
5. Assistant pipeline refactor.
6. SQLite storage foundation.
7. Notes and reminders migration.
8. Launcher hardening.
9. UI shell redesign.
10. Speech cleanup.
11. Memory package.
12. Tests.
13. CI.
14. Release polish.

## What to do while Copilot is working

While Copilot is making code changes, the human side should control quality and risk.

### Always do these in parallel

- Review every diff before accepting it.
- Run the app after every major package refactor.
- Run `mvn clean test` frequently.
- Keep a notes file of what broke and what improved.
- Commit in small steps with clear messages.
- Reject overengineering and surprise framework additions.
- Watch for secret leaks or accidental config regressions.

### Human checklist per task

1. Read Copilot’s proposed explanation.
2. Inspect file diff carefully.
3. Reject anything that adds secrets, magic values, or giant rewrites.
4. Run the app.
5. Test the relevant user flow manually.
6. Run tests.
7. Commit only when stable.

### Manual validation flows

- Create, search, update, and delete a note.
- Create, list, complete, and delete a reminder.
- Open an allowlisted app safely.
- Send an open-ended chat prompt and confirm Groq path is used.
- Start app with missing `GROQ_API_KEY` and confirm graceful error handling.
- Launch UI and verify no crash in a 30+ minute session.

## Copilot session strategy

Do not feed the entire repository to Copilot with "fix everything". Instead, use this sequence:

1. Ask Copilot to analyze one package.
2. Ask for a refactor plan.
3. Approve only the smallest safe implementation step.
4. Test.
5. Commit.
6. Move to the next package.

### Good command sequence

- `pom.xml`
- config
- ai
- assistant
- storage
- notes
- reminders
- launcher
- ui
- speech
- tests
- CI

## Master prompt for Copilot

Use this at the start of a session:

```text
You are helping refactor EDITH-J into a Windows-first JavaFX desktop AI assistant with Java 21 LTS, Groq chat integration, SQLite persistence, secure env-based configuration, modular assistant orchestration, and a modern chat-first UI. Follow this plan exactly:
- Do not rewrite the entire repo at once.
- Work package by package.
- Keep JavaFX, Maven, and the current app foundation.
- Move secrets to env vars only.
- Standardize on JDK 21.
- Introduce clean packages for config, ai, memory, and SQLite storage.
- Keep notes, reminders, and launcher deterministic.
- Use Groq for open-ended reasoning only.
- Keep diffs small and explain changes before editing.
- Add tests for each package you touch.
Start by analyzing the current package I point to and propose the smallest safe refactor.
```

## Final principle

EDITH-J does not need a rewrite to become impressive; it needs disciplined completion. The repository already contains the right conceptual pieces for a desktop assistant, and the path to a real EDITH/JARVIS-style product is to harden architecture, centralize AI, modernize persistence, secure config, and polish the JavaFX experience step by step. [cite:294][cite:255][cite:297]
