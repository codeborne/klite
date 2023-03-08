# 1.5.0-unreleased
* core: toValues() functions moved here from klite-jdbc
* core: TSID introduced as an alternative to UUID
* json: new lightweight json parser
* i18n: now uses the lightweight klite-json, not jackson
* jackson: package changed to klite.jackson to avoid conflicts with klite-json
* jdbc: fromValues() was renamed to create()
* jdbc: switched db.select() <-> db.query(), taking "where" as a list or varargs, to allow for duplicated columns

The release is not backwards-compatible. This will migrate the most important parts (you may need to change -E to -r on a Mac):
`find -name '*.kt' -exec sed -Ei 's/klite.json./klite.jackson./; s/mapOfNotNull/notNullValues/; /db\.(query|select)/{N; s/db\.query/db.xxxselect/g; s/db\.select/db.query/g; s/mapOf/listOf/g; s/emptyMap/emptyList/g}; /db\.delete/s/mapOf//' {} \; -exec sed -Ei 's/db\.xxxselect/db\.select/; s/(db.update\(.*, )mapOf\((.*?\)), (mapOf\(.*?\))\)/\1\3, \2/; ' {} \;`

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
