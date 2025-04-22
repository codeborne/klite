package klite.jdbc

import kotlin.annotation.AnnotationTarget.PROPERTY
import kotlin.annotation.AnnotationTarget.VALUE_PARAMETER

/** Use to override the column name in the database */
@Target(PROPERTY, VALUE_PARAMETER) annotation class Column(val name: String)
