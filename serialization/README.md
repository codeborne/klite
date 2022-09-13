# klite-serialization

Allows using `kotlinx-serialization` to read and write objects.
Json format is provided using [JsonBody](src/JsonBody.kt), but other formats may be added easily in the future.

Please [add kotlinx-serialization and enable it's compiler plugin](build.gradle.kts), and optionally kotlinx-datetime as
dependencies of your own project.

## Status

I rather recommend using [jackson](../jackson) module instead.

A disadvantage of Kotlin Serialization is that you cannot register custom serializers globally, e.g. for java.util.UUID, etc and need to annotate them in every class or file.

See https://github.com/Kotlin/kotlinx.serialization/issues/507
