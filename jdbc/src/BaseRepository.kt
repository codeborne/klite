package klite.jdbc

import javax.sql.DataSource

abstract class BaseRepository(protected val db: DataSource, val table: String) {
  protected val orderAsc = "order by createdAt"
  protected val orderDesc = "$orderAsc desc"

  fun count(where: Map<String, Any?> = emptyMap()): Int = db.select("select count(*) from $table", where) { getInt(1) }.first()
}
