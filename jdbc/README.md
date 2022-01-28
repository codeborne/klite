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
  // with boilerplate removed
  db.insert("table", mapOf("column" to value))
  db.query("table", mapOf("column" to value) { MyEntity(getString("column")) }
  // or you can write full sql manually using db.exec() and db.select()
```

In query mappers you can either use ResultSet methods and extensions to build your entities or use the [ResultSet.fromValues](src/BaseModel.kt): `{ fromValues<MyEntity>() }`

Also, [Any.toValues](src/BaseModel.kt) is provided to simplify conversion of entities to Maps for use with insert/update/upsert.

[JdbcConverter](src/JdbcConverter.kt) can be used to register conversion of custom types to be sent to the DB.
