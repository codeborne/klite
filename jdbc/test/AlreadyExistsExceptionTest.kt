import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import klite.jdbc.AlreadyExistsException
import org.junit.jupiter.api.Test
import java.sql.SQLException

class AlreadyExistsExceptionTest {
  @Test fun `take detail from Postgres exception`() {
    val e = SQLException("ERROR: duplicate key value violates unique constraint \"people_personalcode_key\"\n" +
      "  Detail: Key (personalcode)=(38202032213) already exists.")
    expect(AlreadyExistsException(e).message).toEqual("Key (personalcode)=(38202032213) already exists.")
  }

  @Test fun `no detail in SQL exception`() {
    val e = SQLException("ERROR: duplicate key value violates unique constraint \"people_personalcode_key\"")
    expect(AlreadyExistsException(e).message).toEqual("errors.alreadyExists")
  }
}
