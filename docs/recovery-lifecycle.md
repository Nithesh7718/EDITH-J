# Recovery Lifecycle

## 1. Detection

The runtime identifies failures when subsystems emit `HealthSignal` events. Signals include:

- component identity
- failure type
- severity
- timestamp
- metadata

## 2. Diagnosis

Each serious signal becomes an `Incident` with a root-cause diagnosis. Diagnosis captures:

- subsystem context
- likely recovery path
- evidence from runtime metadata

## 3. Policy and Recovery

`RecoveryPolicyEngine` creates a `RecoveryPlan` with one or more `RecoveryAction` objects. Example actions include:

- provider failover
- retrying database connection
- validating packaged assets
- entering degraded mode for automation or voice

`RecoveryActionExecutor` runs the plan and persists action outcomes.

## 4. Verification

After recovery, `PostRecoveryVerifier` confirms that actions succeeded and the component health score is restored. Recovery latency is measured and recorded.

## 5. Adaptive Memory

`AdaptiveFailureMemory` tracks repeated failure signatures over time and updates recovery recommendations to reduce recurrence. This enables the runtime to prioritize stronger recovery strategies for repeated incidents.
