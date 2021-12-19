package klite.json

import com.fasterxml.jackson.databind.ObjectMapper
import java.io.InputStream
import java.io.OutputStream
import kotlin.reflect.KClass

inline fun <reified T: Any> ObjectMapper.parse(json: String): T = readValue(json, T::class.java)
inline fun <reified T: Any> ObjectMapper.parse(json: InputStream): T = readValue(json, T::class.java)
inline fun <T: Any> ObjectMapper.parse(json: InputStream, type: KClass<T>): T = readValue(json, type.java)
inline fun <T: Any> ObjectMapper.parse(json: String, type: KClass<T>): T = readValue(json, type.java)
inline fun ObjectMapper.parse(json: InputStream) = readTree(json)

inline fun ObjectMapper.stringify(o: Any?): String = writeValueAsString(o)
inline fun ObjectMapper.streamify(o: Any, out: OutputStream) = writeValue(out, o)
