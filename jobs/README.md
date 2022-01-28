# klite-jobs

Provides an ExecutorService-based JobRunner that also handles transactions.
Depends on [jdbc module](../jdbc).

Usage example:
```kotlin
  use<JobRunner>().apply {
    scheduleDaily(require<SomeJobImplementation>(), at = LocalTime.of(7, 0))
  }
```
