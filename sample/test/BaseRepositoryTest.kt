package klite.sample

import ch.tutteli.atrium.api.fluent.en_GB.toBeGreaterThanOrEqualTo
import ch.tutteli.atrium.api.verbs.expect
import klite.jdbc.BaseCrudRepository
import klite.jdbc.BaseEntity
import org.junit.jupiter.api.Test

class BaseRepositoryTest: TempTableDBTest() {
  val repository = object: BaseCrudRepository<BaseEntity<String>, String>(db, "temp") {}

  @Test fun count() {
    expect(repository.count()).toBeGreaterThanOrEqualTo(0)
  }
}
