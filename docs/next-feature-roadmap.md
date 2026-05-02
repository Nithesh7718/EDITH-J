# Next Feature Roadmap

This roadmap translates the strongest next EDITH-J features into an implementation order that fits the current codebase.

## Why this order

EDITH-J already has:

- assistant orchestration in `AssistantService`
- deterministic command handlers for notes, reminders, launcher, and desktop actions
- persistent notes, reminders, chat history, and memory repositories
- a React shell with sidebar panels and a chat view

The main gap is not raw feature count. The main gap is the response and execution contract between the assistant, tools, and UI. Planning, approvals, recovery, source badges, and proactive surfaces all depend on richer structured responses.

## Recommended build order

1. Source-aware response envelope
2. Action approvals and execution policy
3. Recovery behavior for failed tool runs
4. Task planner
5. Memory extraction
6. Daily brief and proactive panel
7. Semantic search for notes and conversations

## Phase 1: Source-aware response envelope

Build this first because it unlocks the UI and backend contract needed by almost every later feature.

### Backend changes

- Expand `AssistantResponse` beyond `intentType`, `userInput`, `answer`, and `channel`
- Add fields such as:
  - `source`
  - `success`
  - `requiresApproval`
  - `approvalType`
  - `explanation`
  - `actions`
  - `artifacts`
  - `recoveryOptions`
  - `metadata`
- Return the full `AssistantResponse` from `/api/chat` instead of flattening chat replies into a minimal DTO
- Persist source metadata in conversation history so chat replay can still show badges

### UI changes

- Add source badges to assistant messages: `AI`, `Notes`, `Reminders`, `Launcher`, `Automation`, `Memory`, `Web`
- Add space in the message card for follow-up actions like `Approve`, `Retry`, `Open`, `Use alternative`
- Preserve graceful rendering for old history items that only contain plain text

### Why first

- The current chat API throws away structured response data
- Source badges are already called for in the master plan
- Approvals, planner output, and recovery actions all need a richer message model

## Phase 2: Action approvals and execution policy

Build this second because EDITH-J already performs desktop and file actions, and some current paths execute immediately.

### Scope

- Require approval before:
  - file rename or move outside low-risk cases
  - file delete
  - bulk file operations
  - app install
  - arbitrary shell-like or generated automation actions
- Introduce a policy layer that classifies requests as:
  - `safe_auto`
  - `confirm_once`
  - `always_confirm`
  - `blocked`

### Code areas

- `AssistantService`
- `DesktopAutomationCommandHandler`
- launcher services
- API route handlers that currently dispatch automation directly

### Important design choice

Do not scatter approval prompts inside every handler. Add a shared action-execution policy so all risky actions behave consistently.

## Phase 3: Recovery behavior

Build this before the planner so EDITH-J becomes more trustworthy when tools fail.

### Expected behavior

- retry transient failures automatically when safe
- return exact blocked reason for policy, missing config, missing file, unsupported app, or provider outage
- suggest alternatives such as:
  - web fallback instead of app launch
  - create instead of open
  - manual clarification when the target is ambiguous

### Code areas

- `AssistantResponse` recovery fields
- `AssistantService` orchestration
- launcher and desktop handlers
- LLM-backed generation paths

### Outcome

This turns failures from dead ends into guided next steps, which matters for every future capability.

## Phase 4: Task planner

Build once approvals and recovery exist, otherwise the planner can generate steps the system cannot safely execute.

### Target behavior

- Convert one user request into:
  - goal
  - ordered sub-steps
  - tool per step
  - execution status
  - approval requirements
  - fallback path

### Suggested shape

- Add a `TaskPlan` model with `TaskStep` items
- Add a planner service between intent classification and tool execution
- Start with hybrid planning:
  - deterministic planning for known workflows
  - LLM planning only for open-ended multi-step requests

### Good first planner use cases

- “Prepare a meeting follow-up”
- “Research this topic and save notes”
- “Create a reminder, draft an email, and open the calendar”

## Phase 5: Memory extraction

The repository already includes a memory package, so this is more of an upgrade than a greenfield build.

### Extract and save

- user preferences
- recurring contacts
- work context
- favorite apps
- repeated routines

### Recommended approach

- start with explicit or high-confidence extraction only
- store memory with category, confidence, provenance, and last-confirmed timestamp
- allow correction or dismissal in the UI later

### Why after planner

Memory improves planning and personalization, but it is less urgent than fixing the core action contract.

## Phase 6: Daily brief and proactive panel

This is a strong UX feature and fits the current shell naturally once the backend can produce structured summaries.

### Panel contents

- today’s reminders
- unfinished tasks
- recent notes or threads
- suggested next actions
- pending approvals

### Backend inputs

- reminders
- recent chat history
- memory entries
- planner tasks
- recent failures needing retry

### UI fit

The current sidebar plus panel layout already supports this without a redesign.

## Phase 7: Semantic search

This is valuable, but it should come after the assistant can explain source, recover cleanly, and act safely.

### Search targets

- notes
- conversation history
- memory entries

### Suggested rollout

- first: better lexical plus metadata search
- second: local embeddings or chunked semantic retrieval
- third: cross-source search results with source labels and timestamps

### Why later

Semantic retrieval is powerful, but trust and execution quality will move EDITH-J forward faster right now.

## Best next implementation slice

If only one slice should be built next, make it:

`structured AssistantResponse + source badges + approval-ready chat actions`

That slice gives EDITH-J:

- visible source awareness
- a stable contract for approvals
- the response shape needed for recovery options
- a foundation for planner output and proactive UI cards

## Concrete first files to touch

- `src/main/java/com/edithj/assistant/AssistantResponse.java`
- `src/main/java/com/edithj/assistant/AssistantService.java`
- `src/main/java/com/edithj/chat/ChatMessage.java`
- `src/main/java/com/edithj/chat/ConversationHistoryService.java`
- `src/main/java/com/edithj/api/EdithApiServer.java`
- `edith-ui/src/api.ts`
- `edith-ui/src/App.tsx`

## Things to avoid

- adding semantic search first without source and recovery UX
- embedding approval logic separately inside each command handler
- making the planner fully LLM-driven for deterministic tasks
- storing memory without provenance or confidence
- adding proactive UI before the backend can explain why a suggestion exists
