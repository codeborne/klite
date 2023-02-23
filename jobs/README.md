# klite-jobs

Provides an ExecutorService-based JobRunner that also handles transactions.
Depends on [jdbc module](../jdbc).

By default, [Jobs](src/JobRunner.kt) use DB locks to ensure only one instance is running at a time.
You can enable parallel runs by overriding `allowParallelRun` property.

Usage example:
```kotlin
  use<JobRunner>().apply {
    scheduleDaily(require<SomeJobImplementation>(), at = LocalTime.of(7, 0))
  }
```
