package klite.jdbc

import klite.toValues
import klite.toValuesSkipping
import java.sql.ResultSet
import java.util.*
import javax.sql.DataSource
import kotlin.reflect.KProperty1

@Deprecated("use .uuid instead", ReplaceWith("this.uuid", "klite.uuid"))
fun String.toId(): UUID = UUID.fromString(this)

@Deprecated("use .getUuid() instead", ReplaceWith("this.getUuid()"))
fun ResultSet.getId(column: String = "id") = getUuid(column)
@Deprecated("use .getUuidOrNull() instead", ReplaceWith("this.getUuidOrNull()"))
fun ResultSet.getIdOrNull(column: String = "id") = getUuidOrNull(column)

@Deprecated("use type from klite-core", ReplaceWith("klite.PropValue<T>", "klite.PropValue"))
typealias PropValue<T> = klite.PropValue<T>

@Deprecated("use function from klite-core", ReplaceWith("this.toValues<T>(*provided)", "klite.toValues"))
inline fun <T: Any> T.toValues(vararg provided: PropValue<T>) = toValues(*provided)
@Deprecated("use function from klite-core", ReplaceWith("this.toValuesSkipping<T>(*skip)", "klite.toValuesSkipping"))
inline fun <T: Any> T.toValuesSkipping(vararg skip: KProperty1<T, *>) = toValuesSkipping(*skip)

@Deprecated("was renamed to create()", ReplaceWith("this.create<T>(*provided)"))
inline fun <reified T: Any> ResultSet.fromValues(vararg provided: PropValue<T>) = create(T::class, *provided)

@Deprecated("pass values first and then optional where", ReplaceWith("this.update(table, values, where.map { it.key to it.value })"))
inline fun DataSource.update(table: String, where: Map<Column, Any?>, values: Values): Int = update(table, values, where.map { it.key to it.value })

@Deprecated("pass where as varargs", ReplaceWith("this.delete(table, where.map { it.key to it.value })"))
inline fun DataSource.delete(table: String, where: Map<Column, Any?>): Int = delete(table, where.map { it.key to it.value })

@Deprecated("use select() instead of old query()", ReplaceWith("this.select(table, id, mapper)"))
inline fun <R, ID> DataSource.query(table: String, id: ID, noinline mapper: Mapper<R>): R = select(table, id, mapper)

@Deprecated("use select() instead of old query(), pass where as varargs", ReplaceWith("this.select(table, where.map { it.key to it.value }, suffix, mapper)"))
inline fun <R> DataSource.query(table: String, where: Map<Column, Any?>, suffix: String = "", noinline mapper: Mapper<R>) =
  select(table, where.map { it.key to it.value }, suffix, mapper)

@Deprecated("use query() instead of old select(), pass where as varargs", ReplaceWith("this.query(table, where.map { it.key to it.value }, suffix, mapper)"))
inline fun <R> DataSource.select(table: String, where: Map<Column, Any?>, suffix: String = "", noinline mapper: Mapper<R>) =
  query(table, where.map { it.key to it.value }, suffix, mapper)
