package klite.jdbc

import java.sql.ResultSet
import java.util.*
import javax.sql.DataSource
import kotlin.reflect.KClass

abstract class BaseRepository(protected val db: DataSource, val table: String) {
  protected val orderAsc = "order by createdAt"
  protected val orderDesc = "$orderAsc desc"

  fun count(where: Map<String, Any?> = emptyMap()): Int = db.select("select count(*) from $table", where) { getInt(1) }.first()
}

abstract class CRUDRepository<E: Entity>(db: DataSource, table: String): BaseRepository(db, table) {
  @Suppress("UNCHECKED_CAST")
  private val entityClass = this::class.supertypes.first().arguments.first().type!!.classifier as KClass<E>

  protected open fun ResultSet.createEntity(): E = fromValues(entityClass)
  protected open fun E.writeEntity() = toValues()

  fun get(id: UUID): E = db.query(table, id) { createEntity() }
  fun save(entity: E) = db.upsert(table, entity.writeEntity())
  fun list(where: Map<String, Any?> = emptyMap(), order: String = orderDesc): List<E> =
    db.query(table, where, order) { createEntity() }
}
