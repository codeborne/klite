package users

import klite.Converter
import klite.jdbc.BaseCrudRepository
import klite.jdbc.BaseEntity
import klite.uuid
import java.sql.ResultSet
import java.util.*

/**
 * A sample type-safe Id class.
 * Beware: Mockk's any() matcher has [trouble with inline classes](https://github.com/mockk/mockk/issues/847).
 * Workaround: `fun <T> MockKMatcherScope.anyId(): Id<T> = Id(ofType(UUID::class))`
 */
@JvmInline value class Id<T>(val uuid: UUID = UUID.randomUUID()) {
  constructor(uuid: String): this(uuid.uuid)
  override fun toString() = uuid.toString()
  companion object {
    init { Converter.use { Id<Any>(it) } }
  }
}

fun <T> String.toId(): Id<T> = Id(uuid)

fun <T> ResultSet.getId(column: String = "id") = getString(column).toId<T>()
fun <T> ResultSet.getIdOrNull(column: String) = getString(column)?.toId<T>()

typealias Entity<T> = BaseEntity<Id<T>>
typealias CrudRepository<T> = BaseCrudRepository<T, Id<T>>
