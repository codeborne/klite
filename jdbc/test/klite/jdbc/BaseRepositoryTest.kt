package klite.jdbc

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BaseRepositoryTest: DBTest() {
  val repository = object: BaseRepository(db, "users") {}

  @Test
  fun count() {
    assertThat(repository.count()).isGreaterThanOrEqualTo(0)
  }
}
