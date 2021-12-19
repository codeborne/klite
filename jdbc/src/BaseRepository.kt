package klite.jdbc

import javax.sql.DataSource

abstract class BaseRepository(val db: DataSource, protected val table: String) {
  fun count(): Int = db.select("select count(*) from $table", emptyMap()) { getInt(1) }.first()
}
