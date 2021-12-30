# klite-serialization

Allows using `kotlinx-serialization` to read and write objects.
Json format is provided using [JsonBody](src/JsonBody.kt), but other formats may be added easily in future.

Please [include kotlinx-serialization](build.gradle.kts), it's plugin and optionally kotlinx-datetime as
dependencies of your own project.

Note: a disadvantage of Kotlin Serialization is that you cannot register custom serializers globally, e.g.
for java.util.UUID, etc and need to annotate them in every class or file.
See https://github.com/Kotlin/kotlinx.serialization/issues/507
