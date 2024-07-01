# Unreleased
* core: logger(name) is now accessible without a class instance context
* core: Any.logger() will now take the closest non-anonymous superclass
* server: errors.on<SomeException>(StatusCode) convenience reified function added
* server: AssetsHandler will now allow serving of SPA index.html even if requested directory in assets exists
* jdbc: BaseCrudRepository.list() and by() now both have the suffix parameter
* jdbc: handle Postgres "cached plan must not change result type" exception by enabling autosave=conservative by default

# 1.6.8
* server: ErrorHandler now supports finding of handlers using exception super classes
* jobs: fix deprecated schedule() method implementation

# 1.6.7
* core: Converter can now force initialize type arguments' companion objects
* core: Decimal.absoluteValue and Decimal.sign introduced, like other numeric types in Kotlin
* core: Registry and TypedHttpClient moved from server module, so that JsonHttpClient can be used without the server
* server: HttpExchange.path<>(), query<>(), and session<>() now allow for automatic String conversion into value types
* server: Browser class better detects various iOS browsers that run on Apple WebKit
* jobs: deprecated non-Duration schedule() methods
* oauth: support for missing first or last names or locale
* jdbc: [initial support](jdbc/src/PostgresNotifier.kt) for Postgres listen/notify
* slf4j: fix length optimization of stack traces in StackTraceOptimizingLogger

# 1.6.6
* core: Converter will force initialize companion objects to better support `init { Converter.use {...} }`
* core: Converter now supports inline classes with multiple constructors automatically
* core: Converter now supports any static(String) method automatically, not only parse()
* core: Cache keepAlive function introduced to prolong external cache entries
* server: KeyCipher now uses base64UrlEncode() internally instead of plain base64
* jdbc: db.upsert() now uses `excluded` special table name for update part instead of setting values twice

# 1.6.5
* core: simple Cache with expiration timer implemented
* oauth: AppleOAuthClient updated and tested
* oauth: JWT helper class introduced
* jdbc: introduced db.upsertBatch()
* jobs: runOnce() introduced for convenience
* json: throw more descriptive errors from JsonNode.get() in case of missing keys

# 1.6.4
* core: added common value types for Email, Phone, and Password, also StringValue base class
* jdbc: introduced db.insertBatch()
* jdbc: deprecated Entity interface (UUID-based), which can be replaced with BaseEntity<UUID>
* jdbc: support SqlComputed with other operators, e.g. `where = "date" lte SqlComputed("currrent_date")`
* server: added path to annotated(), to make paths more visible in one place
* server: make a few more Server properties public (sessionStore, notFoundHandler, etc)
* server: bugfix after 1.6.0: wrap notFoundHandler correctly to produce correct 404 response
* server: NotFoundRoute introduced to be easily distinguishable from normal routes
* oauth: new experimental OAuth 2.0 login module

# 1.6.3
* server: added support for SSE (Server-Side Events) to `HttpExchange`
* json: improve parsing of complex types with parameters
* jdbc: introduce `NullableId` for entities with null ids until they are persisted.
* jdbc: introduce `UpdatableEntity` for optimistic locking in `BaseRepository.save`

# 1.6.2
* server: fix check for requested assets being inside of assets directory
* server: introduced FormDataRenderer
* core/json: KClass.createFrom will report missing parameters in a nicer way
* core: conversion of enum values from string is now case-insensitive
* json: can convert number to a custom type using opts.values
* json: TSGenerator: better detection of types of inline classes with several computed properties
* jdbc: upsert() now has skipUpdateFields parameter

# 1.6.1
* jdbc: and() introduced in addition to or() for more convenient composition of where expressions
* Kotlin and other dependency updates

# 1.6.0
* server: notFoundHandler is now decorated separately in each context, so that decorators can intercept and handle missing routes (e.g. CorsHandler)
* server: ErrorHandler will now omit the default message thrown by first() function ("List/Collection is empty.") and generate a standard 404 NotFound response
* server: ThrowableHandler now has HttpExchange as receiver for cleaner code, as it is not used in most handlers anyway
* server: ThrowableHandler returning null will now proceed with next handler, eventually producing error 500 if not handled
* server: Route is now a KAnnotatedElement, so that findAnnotation/hasAnnotation() functions from kotlin-reflect should be used on it (not backwards-compatible)
* jdbc: startDevDB() will now throw an exception if docker-compose returns a non-zero exit code
* jdbc: deprecated 1.4.x functions removed, please upgrade from 1.4.x to 1.5.0 first, fix deprecations, and then to 1.6.x
* openapi: new module that can generate OpenAPI json spec file for all routes in a context

# 1.5.6
* jdbc: DB_READONLY=true env var can be used to make the whole app read-only (e.g. when migrating the DB, instead of Heroku maintenance mode)
* jdbc: CrudRepository.get() now has optional forUpdate parameter
* HttpExchange.fileName() added for setting of Content-Disposition
* json bugfix: json within json (and '\') are now properly escaped
* json bugfix: render Any type as 'any' instead of 'string' in TSGenerator
* csv: new module for CSV parsing/generation

# 1.5.5
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
