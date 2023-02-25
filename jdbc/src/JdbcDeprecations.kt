package klite.jdbc

import klite.toValues
import klite.toValuesSkipping
import java.sql.ResultSet
import kotlin.reflect.KProperty1

@Deprecated("use type from klite-core", ReplaceWith("klite.PropValue<T>", "klite.PropValue"))
typealias PropValue<T> = klite.PropValue<T>

@Deprecated("use function from klite-core", ReplaceWith("this.toValues<T>(*provided)", "klite.toValues"))
inline fun <T: Any> T.toValues(vararg provided: PropValue<T>) = toValues(*provided)
@Deprecated("use function from klite-core", ReplaceWith("this.toValuesSkipping<T>(*skip)", "klite.toValuesSkipping"))
inline fun <T: Any> T.toValuesSkipping(vararg skip: KProperty1<T, *>) = toValuesSkipping(*skip)

@Deprecated("was renamed to create()", ReplaceWith("this.create<T>(*provided)"))
inline fun <reified T: Any> ResultSet.fromValues(vararg provided: PropValue<T>) = create(T::class, *provided)
