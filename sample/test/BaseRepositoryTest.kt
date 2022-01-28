package klite.jdbc

import net.oddpoet.expect.expect
import net.oddpoet.expect.extension.beGreaterThanOrEqualTo
import org.junit.jupiter.api.Test

class BaseRepositoryTest: TempTableDBTest() {
  val repository = object: BaseRepository(db, "temp") {}

  @Test fun count() {
    expect(repository.count()).to.beGreaterThanOrEqualTo(0)
  }
}
