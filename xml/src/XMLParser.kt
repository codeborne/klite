package klite.xml

import klite.createFrom
import klite.publicProperties
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler
import java.io.InputStream
import javax.xml.parsers.SAXParserFactory
import javax.xml.stream.XMLEventReader
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.events.Attribute
import javax.xml.stream.events.StartElement
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

  // TODO: handle repeating and nested elements
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

  fun parsePathMap(xml: InputStream): Map<String, String> =
    parse(xml, Map::class as KClass<Map<String, String>>, pathToProperty = emptyMap(), creator = { it })

  fun parseNestedMap(xml: InputStream): Map<String, Any> {
    val reader = XMLInputFactory.newInstance().createXMLEventReader(xml)

    fun parseElement(reader: XMLEventReader, start: StartElement): Any {
      val map = mutableMapOf<String, Any>()

      val attrs = start.attributes
      while (attrs.hasNext()) {
        val attr = attrs.next() as Attribute
        map[attr.name.localPart] = attr.value
      }

      val children = mutableMapOf<String, Any>()
      val childLists = mutableMapOf<String, MutableList<Any>>()
      var textContent: String? = null

      while (reader.hasNext()) {
        val event = reader.nextEvent()
        when {
          event.isStartElement -> {
            val childStart = event.asStartElement()
            val childName = childStart.name.localPart
            val childValue = parseElement(reader, childStart)

            if (children.containsKey(childName)) {
              val list = childLists.getOrPut(childName) {
                val existing = children.remove(childName)!!
                mutableListOf(existing)
              }
              list.add(childValue)
              children[childName] = list
            } else {
              children[childName] = childValue
            }
          }
          event.isCharacters -> {
            val text = event.asCharacters().data.trim()
            if (text.isNotEmpty()) textContent = text
          }
          event.isEndElement -> {
            if (event.asEndElement().name == start.name) {
              // Decide return type
              if (map.isEmpty() && children.isEmpty()) {
                // Text-only element → return text
                return textContent ?: ""
              } else {
                // Merge children into map
                map.putAll(children)
                // If there is also text along with attributes/children, optionally store under key
                if (textContent != null) map["text"] = textContent
                return map
              }
            }
          }
        }
      }

      return map
    }

    // Skip any events until the root element
    while (reader.hasNext()) {
      val event = reader.nextEvent()
      if (event.isStartElement) {
        return mapOf(event.asStartElement().name.localPart to parseElement(reader, event.asStartElement()))
      }
    }

    return emptyMap()
  }
}
