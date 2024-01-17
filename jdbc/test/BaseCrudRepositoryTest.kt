package klite.jdbc

import ch.tutteli.atrium.api.fluent.en_GB.toBeAnInstanceOf
import ch.tutteli.atrium.api.verbs.expect
import io.mockk.mockk
import klite.TSID
import org.junit.jupiter.api.Test
import java.util.*
import javax.sql.DataSource

class BaseCrudRepositoryTest {
  @Test fun `generateId for TSID`() {
    class TSIDEntity(override val id: TSID<TSIDEntity>): BaseEntity<TSID<TSIDEntity>>
    class Repo(db: DataSource): BaseCrudRepository<TSIDEntity, TSID<TSIDEntity>>(db, "")
    expect(Repo(mockk()).generateId()).toBeAnInstanceOf<TSID<*>>()
  }

  @Test fun `generateId for UUID`() {
    class Repo(db: DataSource): CrudRepository<Entity>(db, "")
    expect(Repo(mockk()).generateId()).toBeAnInstanceOf<UUID>()
  }
}
