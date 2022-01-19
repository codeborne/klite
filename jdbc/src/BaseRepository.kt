package klite.jdbc

import javax.sql.DataSource

abstract class BaseRepository(protected val db: DataSource, protected val table: String) {
  protected val orderAsc = "order by createdAt"
  protected val orderDesc = "$orderAsc desc"

  fun count(): Int = db.select("select count(*) from $table", emptyMap()) { getInt(1) }.first()
}
