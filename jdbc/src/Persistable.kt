package klite.jdbc

import java.util.*

/**
 * Base class for data model classes.
 * Excludes id from equality checks, thus making it easier to check for equality in tests.
 *
 * Calling [setId] is possible only once, it will fail if id is already set (enforcing immutability)
 * [copy] method can be called to make a new instance without an id.
 * Note: you need to call [setId] if you want to update the model.
 */
abstract class Persistable<out T>: Entity {
  override lateinit var id: UUID

  fun hasId() = this::id.isInitialized
  fun setId(id: UUID = UUID.randomUUID()): T {
    require(!hasId()) { "id already initialized: $id" }
    this.id = id
    @Suppress("UNCHECKED_CAST") return this as T
  }
}
