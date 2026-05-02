# System Architecture

## Runtime Layers

### 1. Runtime Orchestration

- `HealthMonitorRegistry` collects structured health signals from subsystems.
- `RecoveryPolicyEngine` generates bounded recovery plans based on incident severity.
- `RecoveryActionExecutor` executes recovery actions and records success.
- `PostRecoveryVerifier` confirms that recovery actions restored component health.
- `AdaptiveFailureMemory` stores repeated failure signatures and recovery context.

### 2. Core Subsystems

- AI provider layer: provider selection, health scoring, and failover.
- Voice pipeline: offline speech model validation, typed fallback, and pipeline health signals.
- SQLite storage: schema initialization, lock retry, and storage health reporting.
- Automation commands: execution failures are recorded and fallback behavior is surfaced.
- Startup flow: packaged frontend and model assets are validated before runtime readiness.

### 3. Observability and Metrics

- Health signals are exposed through backend health endpoints.
- Metrics include startup success rate, mean time to detect, mean time to recover, provider failover success rate, repeated-failure reduction, and degraded-mode continuity.

## Component Interaction

- The backend startup sequence initializes health monitoring and migrates storage.
- Subsystems emit health signals into the registry whenever failures or degraded behavior are observed.
- Recovery policies generate plans that are persisted for later analysis.
- The runtime can maintain a degraded mode when optional capabilities such as voice transcription become unavailable.
