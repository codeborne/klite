package klite.jdbc

import ch.tutteli.atrium.api.fluent.en_GB.toBeGreaterThanOrEqualTo
import ch.tutteli.atrium.api.verbs.expect
import klite.sample.TempTableDBTest
import org.junit.jupiter.api.Test

class BaseRepositoryTest: TempTableDBTest() {
  val repository = object: BaseCrudRepository<BaseEntity<String>, String>(db, "temp") {}

  @Test fun count() {
    expect(repository.count()).toBeGreaterThanOrEqualTo(0)
  }
}
