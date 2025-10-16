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
import kotlin.reflect.full.findAnnotation

@Target(PROPERTY) @Retention(RUNTIME)
annotation class XmlPath(
  /** start with /, attributes with /@ */
  val path: String
)

class XMLParser(
  private val factory: SAXParserFactory = SAXParserFactory.newInstance().apply { isNamespaceAware = true }
) {
  fun <T : Any> parse(xml: InputStream, clazz: KClass<T>): T {
    val values = mutableMapOf<String, String>()
    var currentPath = ""
    val currentText = StringBuilder()

    val pathToProperty = clazz.publicProperties.values
      .mapNotNull { it.findAnnotation<XmlPath>()?.let { ann -> ann.path to it } }.toMap()

    val handler = object : DefaultHandler() {
      override fun startElement(uri: String?, localName: String?, qName: String, attributes: Attributes) {
        currentPath += "/" + (localName ?: qName)
        currentText.setLength(0)

        for (i in 0 ..< attributes.length) {
          val path = currentPath + "/@" + (attributes.getLocalName(i) ?: attributes.getQName(i))
          pathToProperty[path]?.let { prop ->
            values[prop.name] = attributes.getValue(i)
          }
        }
      }

      override fun characters(ch: CharArray, start: Int, length: Int) {
        currentText.append(ch, start, length)
      }

      override fun endElement(uri: String?, localName: String?, qName: String) {
        pathToProperty[currentPath]?.let { prop ->
          values[prop.name] = currentText.toString().trim()
        }

        currentPath = currentPath.substringBeforeLast("/")
        currentText.setLength(0)
      }
    }

    factory.newSAXParser().parse(xml, handler)
    return clazz.createFrom(values)
  }

  inline fun <reified T: Any> parse(xml: InputStream): T = parse(xml, T::class)
}
