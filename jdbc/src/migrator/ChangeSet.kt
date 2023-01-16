package klite.jdbc

import klite.jdbc.ChangeSet.On.FAIL
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
  val statements = mutableListOf<String>()
  var rowsAffected = 0

  private var lastPos = 0
  internal fun addLine(line: String) {
    sql as StringBuilder
    if (sql.isNotEmpty()) sql.append("\n")
    sql.append(line)
    if (separator != null) addNextStatement()
  }

  private fun addNextStatement(pos: Int = sql.indexOf(separator ?: "", lastPos)) {
    if (pos <= lastPos) return
    val stmt = sql.substring(lastPos, pos)
    statements += stmt
    checksum = (checksum ?: 0) * 89 + stmt.replace("\\s*\n\\s*".toRegex(), "\n").hashCode()
    lastPos = pos + (separator ?: "").length
  }

  internal fun finish() = addNextStatement(sql.length)

  fun matches(contexts: Set<String>) =
    if (context == null) true
    else if (context.startsWith("!")) context.substring(1) !in contexts
    else context in contexts

  enum class On { FAIL, RUN, SKIP, MARK_RAN }
}

class ChangeSetRepository(db: DataSource): BaseCrudRepository<ChangeSet, String>(db, "db_changelog") {
  override val defaultOrder = "$orderAsc for update"
  override fun ChangeSet.persister() = toValuesSkipping(ChangeSet::separator, ChangeSet::sql, ChangeSet::onChange, ChangeSet::onFail)
  fun lock(id: String) = db.query(table, mapOf("id" to id), " for update", ArrayList(1)) { mapper() }.firstOrNull()
}
