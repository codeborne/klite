# klite-json

Klite lightweight json body parser/renderer without external dependencies.

No magic type coersion by default (e.g. empty string into 0), but can be overridden.

Supports Kotlin data and inline classes, reuses type conversion from Klite's core [Converter](../core/src/Converter.kt).

```kotlin
use<JsonBody>()
```
or
```kotlin
useOnly<JsonBody>()
```
