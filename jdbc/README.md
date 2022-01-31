# klite-jdbc

Provides simple extension functions for JDBC standard classes for a simple way to query a DB. [Transaction](src/Transaction.kt) management is also supported.

Tested mostly with PostgreSQL.

Registration with Server instance:

```kotlin
  use<DBModule>() // to register a DataSource and connection pool using Config variables.
  use<RequestTransactionHandler>() // to enable per-request transactions
```

Usage:

```kotlin
  // obtain the registered DataSource (or declare it as a constructor parameter in your Repository class)
  val db = require<DataSource>()

  // basic insert into a table
  db.insert("table", mapOf("col1" to value, "col2" to value2))

  // insert of all fields of an entity
  db.insert("table", entity.toValues())
  // redefine some entity field value before inserting
  db.upsert("table", entity.toValues(Entity::field to "another value"))

  // basic query from a table (mapper runs in context of ResultSet)
  db.query("table", mapOf("column" to value)) { MyEntity(getId(), getString("column")) }

  // more advanced query with suffix and fromValues() auto-mapper
  db.query("table", mapOf("col1" to notNull, "col2" to SqlOp(">=", value)), "order by col3 limit 10") { fromValues<MyEntity>() }
  // single row, with joins, etc
  db.query("table1 left join table2 on table1.id = table2.megaId", mapOf("table2.field" to value), "limit 1") {
    fromValues<MyEntity>(MyEntity::other to fromValues<OtherEntity>())
  }.first()

  // or you can write full sql manually using db.exec() and db.select()
```

See [all available functions](src/JdbcExtensions.kt).

All the above can run without a transaction: in this case, every query will obtain and release its own connection from the pool,
in autocommit mode. If [Transaction](src/Transaction.kt) is active, then it will obtain a connection on first use,
set autoCommit=false and reuse it until the transaction is closed with either commit or rollback.

In query mappers you can either use ResultSet methods and extensions to build your entities or use the
[ResultSet.fromValues](src/BaseModel.kt). Likewise, [Any.toValues](src/BaseModel.kt) is provided to simplify
conversion of entities to Maps for use with insert/update/upsert.

[JdbcConverter](src/JdbcConverter.kt) can be used to register conversion of custom types to be sent to the DB.
