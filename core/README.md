# klite-core

Some core concepts that can be useful without [klite-server](../server). Dependency of other klite modules.

* [Config](src/Config.kt) - simple env/system properties based configuration
* [Converter](src/Converter.kt) - base for http/jdbc/json converision of Strings to type-safe values
* [Logger](src/Logger.kt) - convenient extensions for JDK System.Logger
* [toValues/create](src/Values.kt) - functions for conversion of objects to maps and back; can be used for DTO mapping, but also jdbc/json/etc
* [Decimal](src/Decimal.kt) - an alternative for BigDecimal for monetary amounts
