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
annotation class XmlPath(val path: String)

class XMLParser(private val factory: SAXParserFactory = SAXParserFactory.newInstance()) {
  fun <T : Any> parse(xml: InputStream, clazz: KClass<T>): T {
    val builderMap = mutableMapOf<String, String>()
    val currentPath = mutableListOf<String>()
    val currentText = StringBuilder()

    val pathToProperty = clazz.publicProperties.values
      .mapNotNull { it.findAnnotation<XmlPath>()?.let { ann -> ann.path to it } }
      .toMap()

    val handler = object : DefaultHandler() {
      override fun startElement(uri: String?, localName: String?, qName: String, attributes: Attributes) {
        currentPath.add(qName)
        currentText.setLength(0)

        for ((xmlPath, prop) in pathToProperty) {
          if (xmlPath.endsWith("/@${attributes.getQName(0)}") && xmlPath.startsWith(currentPath.joinToString("/"))) {
            builderMap[prop.name] = attributes.getValue(attributes.getQName(0))
          }
        }
      }

      override fun characters(ch: CharArray, start: Int, length: Int) {
        currentText.append(ch, start, length)
      }

      override fun endElement(uri: String?, localName: String?, qName: String) {
        val path = currentPath.joinToString("/")

        pathToProperty.forEach { (xmlPath, prop) ->
          if (xmlPath == path) {
            builderMap[prop.name] = currentText.toString().trim()
          }
        }

        currentPath.removeAt(currentPath.size - 1)
        currentText.setLength(0)
      }
    }

    factory.newSAXParser().parse(xml, handler)
    return clazz.createFrom(builderMap)
  }

  inline fun <reified T: Any> parse(xml: InputStream): T = parse(xml, T::class)
}
