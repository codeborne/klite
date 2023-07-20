# Unreleased
* jdbc: support reading of collections of Decimal from DB array columns
* jdbc: db.upsert() now has an optional "where" parameter
* server: AssetsHandler.headerModifier now gets file parameter to make decisions based on it
* server: allow having annotations on AssetsHandler for e.g. access checks

# 1.5.4
* json: ValueConverter can now be used to transform strings into types
* json: JsonNode.getList() signature fixed
* json: improve error message when trying to parse an empty stream
* json: JsonParser.readArray can now be used to stream json arrays without loading them into memory
* jdbc: close connection (return to pool) even if autoCommit/commit/rollback fails
* server: support for boolean query parameters without values, e.g. ?flag
* server: support for anonymous handler annotations (fixed in Kotlin 1.8)

# 1.5.3
* server: TypedHttpClient/JsonHttpClient uses a better logger name (from nearest user class)
* json/jackson: TypedHttpClient/JsonHttpClient now have overridable trimToLog property in case you need to process how requests/responses are logged
* jobs: JobRunner.schedule() now takes kotlin.time.Duration values
* jdbc: PooledDataSource - a simple and easy to configure connection pool (used in DBModule by default)
* jdbc: use HikariModule instead of DBModule if you still want to use Hikari (also add dependency on com.zaxxer:HikariCP)
* liquibase/serialization - dependencies updated

# 1.5.2
* core: experimental Decimal class to be used for monetary values, with numerical equality (unlike BigDecimal)
* core/json: use default values for explicitly passed nulls if property is not nullable
* core/json: unwrap InvocationTargetException, so that any validation exceptions thrown from data class constructors is propagated properly
* core/json: workaround for a bug in kotlin-reflect, which boxes null values even if nullable inline type is used: https://youtrack.jetbrains.com/issue/KT-57590
* json: TSGenerator can now receive additional library types to generate from command-line, e.g. klite.TSID
* i18n: do not trim translations by default (keep all whitespace)
* jdbc: support binding of Int values to data classes (DB usually returns integers as Long)
* jdbc: possibility to use table aliases when getting of columns from ResultSet with joins/using create (Postgres only)
* jdbc: allow whitespace between -- and keywords in changeset sql files
* server: default RequestLogFormatter will not log StatusCodeExceptions anymore
* server: useHashCodeAsETag() introduced to avoid sending of same responses
* server: run onStop handlers in reverse order of registration, so that e.g. connection pool is stopped after jobs are stopped
* jobs: do not start new jobs on shutdown while waiting for running jobs to finish

# 1.5.1
* json: ValueConverter.from() can now have access to the expected KType
* json: TSGenerator to generate TypeScript types for data classes/enums
* server: AppScope.async now is the standard async function that returns a Deferred. Use AppScope.launch if you want exceptions to be logged

# 1.5.0
* core: toValues() functions moved here from klite-jdbc
* core: TSID introduced as an alternative to UUID
* json: new lightweight json parser
* i18n: now uses the lightweight klite-json, not jackson
* jackson: package changed to klite.jackson to avoid conflicts with klite-json
* jackson: .parse<T> extension function now passes type parameters to Jackson, not only the main class
* jdbc: fromValues() was renamed to create()
* jdbc: switched db.select() <-> db.query(), taking "where" as a list or varargs, to allow for duplicated columns

The release is **not fully backwards-compatible**, however most old functions are provided as @Deprecated.

This will migrate the most important parts:
`find -name '*.kt' -exec sed -ri 's/klite.json./klite.jackson./; s/mapOfNotNull/notNullValues/; /db\.(query|select)/{N; s/db\.query/db.xxxselect/g; s/db\.select/db.query/g; s/mapOf/listOf/g; s/emptyMap/emptyList/g};' {} \; -exec sed -ri 's/db\.xxxselect/db\.select/; s/(db.update\(.*, )mapOf\((.*?\)), (mapOf\(.*?\))\)/\1\3, \2/; s/(db.delete\(.*, )mapOf\((.*?)\)/\1\2/' {} \;`

**Beware**: if you use the replacement above, then make sure that no deprecated query/select usages are left, i.e. check that all "where" maps are replaced with lists/varargs, which is done automatically only if on the same or next line.

You may also use IDEA migration of deprecations, but it will most likely [break code formatting, etc](https://youtrack.jetbrains.com/issue/KTIJ-24870).
Also, you may need to add some imports manually.

# 1.4.5
* jdbc: allow using $${json}$$ in migration scripts without treating it as substitutions
* jobs: do not unlock already running jobs after failing to lock (a problem for 3 instances with jobs or more)

# 1.4.4
* jobs: locked jobs did not release DB connection (bug introduced in 1.4.2)

# 1.4.3
* core: the new core module, to make it possible to use jdbc module without the server
* core: mapOfNotNull() now accepts keys of any type
* core: Converter.supports() now finds supported converters automatically, even if not used previously
* jdbc: db.count() introduced
* jdbc: box @JvmInline classes when loading from DB
* jdbc: allow same column multiple times in or() and deprecate NullOrOp()
* server: do not wrap exceptions already containing parameter name in annotated routes
* server: TypedHttpClient introduced as a more generic foundation for JsonHttpClient
* jackson: JsonHttpClient now accepts KType as non-reified parameters instead of KClass<*> for better type parameter support

# 1.4.2
* server: fallback to `docker compose` without dash
* server: shorten json stack traces by cutting repeating class/package names
* jdbc: emptyArray literal support '{}' - replaces previous EmptyOf() handling in toValues()
* jdbc: jsonb() and stuff that should work for both insert and update must use SqlComputed() now
* jdbc: CrudRepository now skips null where tokens for easier conditionals
* jobs: do not allow running of same jobs in parallel by default - useful in case of multiple instances

# 1.4.1
* server: skip "broken pipe" and "connection reset" exceptions by default
* server: request log now contains exception name that caused the status code to change
* jdbc: orExpr() and or() functions for where expressions
* jackson: be able to override JavaTimeModule settings more easily

# 1.4.0
* server: MimeTypes is now a singleton object
* jdbc: DBMigrator introduced to replace Liquibase in most cases
* jdbc: ClosedRange and OpenRange support
* jdbc: SqlExpr implements equals for easier parameter-based mocking
