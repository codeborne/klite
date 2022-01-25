## Klite alternatives

### Ktor

Ktor seems bigger and more complicated than a simple web framework should be.
It has more dependencies and the style of writing routes requires more boilerplate.

Klite annotated routes are just plain classes and are very simple to unit test without complex request/response mocking.

### SparkJava/Kotlin

SparkJava provides only basic route handles and is tightly coupled with Jetty.
The routes are very difficult to write tests for and most projects invent their own frameworks around it.

### Jooby/Kooby

Jooby is a good modular web framework for Java, but is quite complex inside, making it hard to debug and extend.

Despite having decent Kotlin support (Kooby), it's APIs are too Java-esque with Java-style builders and nullability
workarounds that seem unnatural in Kotlin.

But the biggest problem is that filters/decorators don't support coroutines properly, meaning it's impossible
to have stable transaction management when coroutines are used. After filters and decorators run before the first
suspension, rendering them useless in async use cases.

### General

All of the above lack logging/jdbc/transaction/migrations that most projects need.
Klite provides these essential parts as separate optional modules.
