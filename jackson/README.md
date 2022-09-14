# klite-jackson

Klite json body parser/renderer using Jackson.

```kotlin
use<JsonBody>()
```
or
```kotlin
useOnly<JsonBody>()
```

This will also add jackson deserializers for all types already registered with [Converter](../server/src/klite/Converter.kt) at the moment of creation.
