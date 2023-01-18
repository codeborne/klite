package klite.jdbc

import klite.jdbc.ChangeSet.On.FAIL
import klite.trimToNull
import org.intellij.lang.annotations.Language
import javax.sql.DataSource

data class ChangeSet(
  override val id: String,
  @Language("SQL") val sql: CharSequence = StringBuilder(),
  val context: String? = null,
  val onChange: On = FAIL,
  val onFail: On = FAIL,
  val separator: String? = ";",
  val filePath: String? = null,
  var checksum: Long? = null
): BaseEntity<String> {
  var rowsAffected = 0
  var statements: List<String> = emptyList()

  fun isNotEmpty() = id.isNotEmpty() || sql.isNotEmpty()

  fun matches(contexts: Set<String>) =
    if (context == null) true
    else if (context.startsWith("!")) context.substring(1) !in contexts
    else context in contexts

  internal fun addLine(line: String) {
    if ((sql as StringBuilder).isNotEmpty()) sql.append("\n")
    sql.append(line)
  }

  internal fun finish() = apply {
    statements = (if (separator != null) sql.split(separator) else listOf(sql.toString())).mapNotNull { it.trimToNull() }
    checksum = statements.fold(0L) { r, s -> r * 89 + s.replace("\\s*\n\\s*".toRegex(), "\n").hashCode() }
  }

  enum class On { FAIL, RUN, SKIP, MARK_RAN }
}

class ChangeSetRepository(db: DataSource): BaseCrudRepository<ChangeSet, String>(db, "db_changelog") {
  private val lock = ChangeSet("klite:lock")
  override val defaultOrder = "$orderAsc for update"
  override fun ChangeSet.persister() = toValuesSkipping(ChangeSet::separator, ChangeSet::sql, ChangeSet::onChange, ChangeSet::onFail)
  fun lock() = db.insert(table, lock.persister())
  fun unlock() = db.delete(table, mapOf("id" to lock.id))
}
