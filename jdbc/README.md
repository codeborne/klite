# klite-jdbc

Provides simple extension functions for JDBC standard classes for a simple way to query a DB. [Transaction](src/Transaction.kt) management is also supported.

Tested mostly with PostgreSQL.

Usage:

```kotlin
  use<DBModule>() // to register a DataSource and connection pool
  use<RequestTransactionHandler>() // to enable per-request transactions
```

DB access:

```kotlin
  val db = require<DataSource>()
  db.insert("table", mapOf("column" to value))
  db.insert("table", entity.toValues())
  db.query("table", mapOf("column" to value)) { MyEntity(getId(), getString("column")) } // mapper runs in context of ResultSet
  db.query("table", mapOf("col1" to notNull, "col2" to SqlOp(">=", value)), "order by col3 limit 10") { fromValues<MyEntity>() }
  // or you can write full sql manually using db.exec() and db.select()
```

See [all available functions](src/JdbcExtensions.kt).

In query mappers you can either use ResultSet methods and extensions to build your entities or use the
[ResultSet.fromValues](src/BaseModel.kt), like shown above.

Also, [Any.toValues](src/BaseModel.kt) is provided to simplify conversion of entities to Maps for use with insert/update/upsert.

[JdbcConverter](src/JdbcConverter.kt) can be used to register conversion of custom types to be sent to the DB.
