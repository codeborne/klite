# klite-serialization

Allows using kotlinx-serialization to read and write objects.
At first, json is provided by default, but other formats may be added easily.

Please [include kotlinx-serialization](build.gradle.kts), it's plugin and optionally kotlinx-datetime as
dependencies of your own project.

Note: a disadvantage of Kotlin Serialization is that you cannot register custom serializers globally, e.g.
for java.util.UUID, etc and need to annotate them in every class or file.
