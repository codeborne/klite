package klite.jdbc

import klite.PropValue
import klite.toValues
import org.intellij.lang.annotations.Language
import java.sql.ResultSet
import java.util.*
import javax.sql.DataSource
import kotlin.reflect.KClass

interface BaseEntity<ID> {
  val id: ID
}

interface Entity: BaseEntity<UUID>

abstract class BaseRepository(protected val db: DataSource, val table: String) {
  protected open val orderAsc get() = "order by $table.createdAt"
  protected open val orderDesc get() = "$orderAsc desc"
}

abstract class CrudRepository<E: Entity>(db: DataSource, table: String): BaseCrudRepository<E, UUID>(db, table)

abstract class BaseCrudRepository<E: BaseEntity<ID>, ID>(db: DataSource, table: String): BaseRepository(db, table) {
  @Suppress("UNCHECKED_CAST")
  private val entityClass = this::class.supertypes.first().arguments.first().type!!.classifier as KClass<E>
  open val defaultOrder get() = orderDesc
  open val selectFrom @Language("SQL", prefix = "select * from ") get() = table

  protected open fun ResultSet.mapper(): E = create(entityClass)
  protected open fun E.persister() = toValues()

  open fun get(id: ID): E = db.select(selectFrom, id, "$table.id") { mapper() }
  open fun list(vararg where: PropValue<E>?, order: String = defaultOrder): List<E> =
    db.select(selectFrom, where.filterNotNull(), order) { mapper() }
  open fun by(vararg where: PropValue<E>?): E? = list(*where).firstOrNull()
  open fun count(vararg where: PropValue<E>?): Long = db.count(selectFrom, where.filterNotNull())

  open fun save(entity: E) = db.upsert(table, entity.persister())
}
