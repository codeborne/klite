# klite-jackson

Klite json body parser/renderer using Jackson.
If unsure, [klite-json](../json) will provide a more consistent and easily configurable overall experience.

```kotlin
use<JsonBody>()
```
or
```kotlin
useOnly<JsonBody>()
```

This will also add jackson deserializers for all types already registered with [Converter](../core/src/Converter.kt) at the moment of creation.
