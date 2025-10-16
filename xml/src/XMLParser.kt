package klite.xml

import klite.createFrom
import klite.publicProperties
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler
import java.io.InputStream
import javax.xml.parsers.SAXParserFactory
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.PROPERTY
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation

@Target(PROPERTY) @Retention(RUNTIME)
annotation class XmlPath(
  /** root element starts with /, path suffix without /, attributes with /@ */
  val path: String
)

class XMLParser(
  private val factory: SAXParserFactory = SAXParserFactory.newInstance().apply { isNamespaceAware = true }
) {
  inline fun <reified T: Any> parse(xml: InputStream): T = parse(xml, T::class)

  fun parsePathMap(xml: InputStream): Map<String, String> =
    parse(xml, Map::class as KClass<Map<String, String>>, pathToProperty = emptyMap(), creator = { it })

  fun parse(xml: InputStream, callback: (parentPath: String, name: String, text: String) -> Unit) {
    var currentPath = ""
    val currentText = StringBuilder()

    val handler = object : DefaultHandler() {
      override fun startElement(uri: String?, localName: String?, qName: String, attributes: Attributes) {
        val elementName = localName ?: qName
        val parentPath = currentPath
        currentPath += "/$elementName"
        currentText.setLength(0)

        for (i in 0 until attributes.length) {
          val attrName = attributes.getLocalName(i) ?: attributes.getQName(i)
          callback(currentPath, "@$attrName", attributes.getValue(i))
        }
      }

      override fun characters(ch: CharArray, start: Int, length: Int) {
        currentText.append(ch, start, length)
      }

      override fun endElement(uri: String?, localName: String?, qName: String) {
        val parentPath = currentPath.substringBeforeLast("/", "")
        val text = currentText.toString().trim()
        if (text.isNotEmpty()) {
          val elementName = localName ?: qName
          callback(parentPath, elementName, text)
        }

        currentPath = parentPath
        currentText.setLength(0)
      }
    }

    factory.newSAXParser().parse(xml, handler)
  }

  fun <T : Any> parse(xml: InputStream, type: KClass<T>,
                      pathToProperty: Map<String, KProperty1<T, *>> = readAnnotations(type),
                      creator: (Map<String, String>) -> T = { type.createFrom(it) }
  ): T {
    val values = mutableMapOf<String, String>()
    parse(xml) { parentPath, name, text ->
      val prop = pathToProperty.find(parentPath, name)
      values[prop] = text
    }
    return creator(values)
  }

  private fun <T: Any> readAnnotations(type: KClass<T>) = type.publicProperties.values
    .mapNotNull { it.findAnnotation<XmlPath>()?.let { ann -> ann.path to it } }.toMap()

  // TODO: separate non-root path map may be pre-created for better performance
  private fun <T> Map<String, KProperty1<T, *>>.find(parentPath: String, name: String): String {
    val fullPath = "$parentPath/$name"
    return (this[fullPath] ?: this[name] ?: entries.firstOrNull { !it.key.startsWith("/") && fullPath.endsWith(it.key) }?.value)?.name ?: fullPath
  }
}
