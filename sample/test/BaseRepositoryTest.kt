package klite.jdbc

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isGreaterThanOrEqualTo

class BaseRepositoryTest: TempTableDBTest() {
  val repository = object: BaseRepository(db, "temp") {}

  @Test fun count() {
    expectThat(repository.count()).isGreaterThanOrEqualTo(0)
  }
}
