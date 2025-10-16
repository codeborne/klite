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

  fun <T : Any> parse(xml: InputStream, type: KClass<T>, pathToProperty: Map<String, KProperty1<T, *>> = readAnnotations(type)): T {
    val values = mutableMapOf<String, String>()
    var currentPath = ""
    val currentText = StringBuilder()

    val handler = object : DefaultHandler() {
      override fun startElement(uri: String?, localName: String?, qName: String, attributes: Attributes) {
        currentPath += "/" + (localName ?: qName)
        currentText.setLength(0)

        for (i in 0 ..< attributes.length) {
          val path = currentPath + "/@" + (attributes.getLocalName(i) ?: attributes.getQName(i))
          pathToProperty.find(path)?.let { prop ->
            values[prop.name] = attributes.getValue(i)
          }
        }
      }

      override fun characters(ch: CharArray, start: Int, length: Int) {
        currentText.append(ch, start, length)
      }

      override fun endElement(uri: String?, localName: String?, qName: String) {
        pathToProperty.find(currentPath)?.let { prop ->
          values[prop.name] = currentText.toString().trim()
        }

        currentPath = currentPath.substringBeforeLast("/")
        currentText.setLength(0)
      }
    }

    factory.newSAXParser().parse(xml, handler)
    return type.createFrom(values)
  }

  private fun <T: Any> readAnnotations(type: KClass<T>) = type.publicProperties.values
    .mapNotNull { it.findAnnotation<XmlPath>()?.let { ann -> ann.path to it } }.toMap()

  // TODO: separate non-root path map may be pre-created for better performance
  private fun <T> Map<String, KProperty1<T, *>>.find(path: String) =
    this[path] ?: this.entries.firstOrNull { !it.key.startsWith("/") && path.endsWith(it.key) }?.value
}
