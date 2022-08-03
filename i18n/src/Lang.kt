package klite.i18n

import com.fasterxml.jackson.databind.json.JsonMapper
import klite.HttpExchange
import klite.json.parse
import java.util.Locale

typealias Translations = Map<String, Any>
private typealias MutableTranslations = MutableMap<String, Any>

object Lang {
  const val COOKIE = "LANG"

  val available: List<String> = load("langs")
  private val translations = loadTranslations()

  fun lang(e: HttpExchange) = ensureAvailable(e.cookie(COOKIE))
  fun takeIfAvailable(lang: String?) = lang?.takeIf { available.contains(it) }
  fun ensureAvailable(requestedLang: String?) = takeIfAvailable(requestedLang) ?: available.first()

  fun remember(e: HttpExchange, lang: String) = e.cookie(COOKIE, lang)

  fun translations(e: HttpExchange): Translations = translations[lang(e)]!!
  fun translations(requestedLang: String?): Translations = translations[ensureAvailable(requestedLang)]!!

  fun translate(e: HttpExchange, key: String) = translations(e).invoke(key)
  fun translate(lang: String, key: String, substitutions: Map<String, String> = emptyMap()) =
    translations(lang).invoke(key, substitutions)

  fun loadTranslations(): Map<String, Translations> {
    val loaded = available.associateWith { lang ->
      load<MutableTranslations>(lang)
    }

    val default = loaded[available[0]]!!
    available.drop(1).forEach { lang -> merge(loaded[lang] as MutableTranslations, default) }
    return loaded
  }

  @Suppress("UNCHECKED_CAST")
  private fun merge(dest: MutableTranslations, src: Translations) {
    src.forEach { (key, value) ->
      if (value is Map<*, *>) {
        if (dest[key] == null) dest[key] = mutableMapOf<String, Any>()
        merge(dest[key] as MutableTranslations, value as Translations)
      }
      else if (dest[key] == null) dest[key] = value
    }
  }

  @Suppress("UNCHECKED_CAST")
  private inline fun <reified T: Any> load(lang: String): T = JsonMapper().parse(
    javaClass.getResourceAsStream("/$lang.json") ?: error("/$lang.json not found in classpath"), T::class)
}

private fun Translations.resolve(key: String) =
  key.split('.').fold(this) { more: Any?, k -> (more as? Map<*, *>)?.get(k) }

@Suppress("UNCHECKED_CAST")
fun Translations.getMany(key: String) = resolve(key) as? Map<String, String> ?: emptyMap()
operator fun Translations.invoke(key: String) = resolve(key) as? String ?: key
operator fun Translations.invoke(key: String, substitutions: Map<String, String> = emptyMap()): String {
  var result = invoke(key)
  substitutions.forEach { result = result.replace("{${it.key}}", it.value) }
  return result
}

val HttpExchange.lang: String get() = Lang.lang(this)
