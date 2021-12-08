package klite.jdbc

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BaseRepositoryTest: TempTableDBTest() {
  val repository = object: BaseRepository(db, "temp") {}

  @Test
  fun count() {
    assertThat(repository.count()).isGreaterThanOrEqualTo(0)
  }
}
