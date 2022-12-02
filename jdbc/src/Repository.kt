package klite.jdbc

import java.sql.ResultSet
import java.util.*
import javax.sql.DataSource
import kotlin.reflect.KClass

interface BaseEntity<ID> {
  val id: ID
}

interface Entity: BaseEntity<UUID>

abstract class BaseRepository(protected val db: DataSource, val table: String) {
  protected open val orderAsc get() = "order by createdAt"
  protected open val orderDesc get() = "$orderAsc desc"

  open fun count(where: Map<String, Any?> = emptyMap()): Int = db.select("select count(*) from $table", where) { getInt(1) }.first()
}

abstract class CrudRepository<E: Entity>(db: DataSource, table: String): BaseCrudRepository<UUID, E>(db, table)

abstract class BaseCrudRepository<ID, E: BaseEntity<ID>>(db: DataSource, table: String): BaseRepository(db, table) {
  @Suppress("UNCHECKED_CAST")
  private val entityClass = this::class.supertypes.first().arguments.first().type!!.classifier as KClass<E>

  protected open fun ResultSet.mapper(): E = fromValues(entityClass)
  protected open fun E.persister() = toValues()

  open fun get(id: ID): E = db.query(table, id) { mapper() }
  open fun save(entity: E) = db.upsert(table, entity.persister())
  open fun list(where: Map<String, Any?> = emptyMap(), order: String = orderDesc): List<E> =
    db.query(table, where, order) { mapper() }
}
