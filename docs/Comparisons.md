## Klite alternatives

### Ktor

Ktor seems bigger and more complicated than a simple web framework should be.
It has more dependencies and the style of writing routes requires more boilerplate.

Klite annotated routes are just plain classes and are very simple to unit test without complex request/response mocking.
Unfortunately, Ktor documentation advices to write route classes, which are tightly-coupled to the framework and
cannot be unit-tested like plain code.

### SparkJava/Kotlin

SparkJava provides only basic route handles and is tightly coupled with Jetty.
The routes are very difficult to write tests for and most projects invent their own frameworks around it.

### Jooby/Kooby

Jooby is a good modular web framework for Java, and is actually a big inspiration for Klite.
However, Jooby is quite complex inside, making it hard to debug and extend.

Despite having decent Kotlin support (Kooby), it's APIs are too Java-esque with Java-style builders and nullability
workarounds that seem unnatural in Kotlin.

Annotated routes (called `mvc` in Jooby) are compile-time, with annotation processor converting them to regular routes.
This sounds like a good idea at first, but in reality just slows down incremental compilation without any performance benefit, as
reflection is still used in runtime to find precompiled routes. Also, sometimes compilation or recompilation of them fails
and you are left with puzzling error messages.

But the biggest problem is that filters/decorators don't support coroutines properly, meaning it's impossible
to have stable transaction management when coroutines are used. After filters and decorators run before the first
suspension, rendering them useless in async use cases.

### General

All of the above lack logging/jdbc/transaction/migrations that most projects need.
Klite provides these essential parts as separate optional modules.
