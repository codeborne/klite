package klite.jdbc

import klite.PropValue
import klite.publicProperties
import klite.toValues
import org.intellij.lang.annotations.Language
import java.sql.ResultSet
import java.time.Instant
import java.util.*
import javax.sql.DataSource
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

interface BaseEntity<ID> {
  val id: ID
}

interface NullableId<ID>: BaseEntity<ID?> {
  override var id: ID?
}

/** Implement this for optimistic locking support in [BaseCrudRepository.save] */
interface UpdatableEntity {
  var updatedAt: Instant?
}

interface Entity: BaseEntity<UUID>

abstract class BaseRepository(protected val db: DataSource, val table: String) {
  protected open val orderAsc get() = "order by createdAt"
  protected open val orderDesc get() = "$orderAsc desc"
}

abstract class CrudRepository<E: Entity>(db: DataSource, table: String): BaseCrudRepository<E, UUID>(db, table) {
  override fun generateId() = UUID.randomUUID()
}

abstract class BaseCrudRepository<E: BaseEntity<ID>, ID>(db: DataSource, table: String): BaseRepository(db, table) {
  @Suppress("UNCHECKED_CAST")
  private val entityClass = this::class.supertypes.first().arguments.first().type!!.classifier as KClass<E>
  override val orderAsc get() = "order by $table.createdAt"
  open val defaultOrder get() = orderDesc
  open val selectFrom @Language("SQL", prefix = "select * from ") get() = table

  protected open fun ResultSet.mapper(): E = create(entityClass)
  protected open fun E.persister() = toValues()

  open fun get(id: ID, forUpdate: Boolean = false): E = db.select(selectFrom, id, "$table.id", if (forUpdate) "for update" else "") { mapper() }
  open fun list(vararg where: PropValue<E>?, order: String = defaultOrder): List<E> =
    db.select(selectFrom, where.filterNotNull(), order) { mapper() }
  open fun by(vararg where: PropValue<E>?): E? = list(*where).firstOrNull()
  open fun count(vararg where: PropValue<E>?): Long = db.count(selectFrom, where.filterNotNull())

  open fun save(entity: E): Int {
    var useInsert = false
    if (entity is NullableId<*> && entity.id == null) {
      (entity as NullableId<ID>).id = generateId()
      useInsert = true
    }
    if (entity is UpdatableEntity) {
      useInsert = useInsert || entity.updatedAt == null
      val now = now()
      if (!useInsert) {
        val numUpdated = db.update(table, entity.persister() + ("updatedAt" to now), where = listOf("id" to entity.id, "updatedAt" to entity.updatedAt))
        if (numUpdated == 0) throw StaleEntityException()
        entity.updatedAt = now
        return numUpdated
      }
      entity.updatedAt = now
    }
    if (useInsert) db.insert(table, entity.persister())
    return db.upsert(table, entity.persister())
  }

  /** Recommended to override if used with [NullableId] */
  open fun generateId(): ID {
    val idClass = entityClass.publicProperties.first { it.name == "id" }.returnType.classifier as KClass<*>
    return (idClass.constructors.find { it.parameters.isEmpty() } ?: idClass.primaryConstructor!!).callBy(emptyMap()) as ID
  }
}

fun now(): Instant = Instant.ofEpochMilli(System.currentTimeMillis())
