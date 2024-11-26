## Klite alternatives

Klite is Kotlin-first framework without magic. You may first check [Why Kotlin?](Kotlin.md)

### Ktor

Ktor seems bigger and more complicated than a simple web framework should be.
It has more dependencies and the style of writing routes requires more boilerplate.

Klite annotated routes are just plain classes and are very simple to unit test without complex request/response mocking.
Unfortunately, Ktor documentation advices to write route classes that are tightly-coupled to the framework and
cannot be unit-tested like plain code.

### SparkJava/Kotlin

SparkJava provides only basic route handlers and is tightly coupled with Jetty.
The routes are very difficult to write tests for and most projects invent their own frameworks around it.

### Jooby/Kooby

Jooby is a good modular web framework for Java, and is actually a big inspiration for Klite.
However, Jooby is quite complex inside, making it hard to debug and extend.

Despite having decent Kotlin support (Kooby), it's APIs are too Java-esque with Java-style builders and nullability
workarounds that seem unnatural in Kotlin.

Annotated routes (called `mvc` in Jooby) are compile-time, with annotation processor converting them to regular routes.
This sounds like a good idea at first, but in reality just slows down incremental compilation without any performance benefit, as
reflection is still used in runtime to find precompiled routes. Also, sometimes compilation or recompilation of them fails,
and you are left with puzzling error messages.

But the biggest problem is that filters/decorators don't support coroutines properly, meaning it's impossible
to have stable transaction management when coroutines are used. "After" filters and decorators run before the first
suspension, rendering them useless in async use cases.

### Javalin

Javalin also strives to be simple, but contains 7000 lines of code vs Klite's less than 1000.
Also, it is blocking by design with no easy way to write coroutines.

### http4k

Very function-based, supports JDK built-in http server, but the core jar is 1Mb compared to 200k of Klite.
Many integrations, but no JDBC/DB helpers. Also blocking, no coroutine support.

### Spring (Boot) / Hibernate

These are for Annotation-Driven Development lovers. In Kotlin (and actually Java 8+), it is not necessary to add behavior with annotations,
which are hard to debug and test. Often annotations are present, but not work as expected.

Both Spring and Hibernate have become very huge and "enterprise".
Hibernate does a lot of unexpected unnecessary things and requires you to debug your annotations to get the SQL right.
In contrast, klite-jdbc greatly simplifies mapping of classes to queries, but doesn't get in your way, allowing for full power of SQL if necessary.

### General

All of the above lack logging/jdbc/transaction/migrations that most projects need.
Klite provides these essential parts as separate optional modules.
