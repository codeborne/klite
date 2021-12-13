package klite

import klite.Cookie.SameSite.None
import klite.crypto.KeyCipher
import klite.crypto.KeyGenerator

class Session(
  val params: MutableMap<String, String?> = mutableMapOf(),
  val isNew: Boolean = true
) {
  var changed = false; private set

  operator fun get(key: String) = params[key]
  operator fun set(key: String, value: String?) = params.put(key, value).also { changed = true }
  fun clear() = params.clear().also { changed = true }
}

interface SessionStore {
  fun load(exchange: HttpExchange): Session
  fun save(exchange: HttpExchange, session: Session)
}

class CookieSessionStore(
  sessionSecret: String = Config.required("SESSION_SECRET"),
  val cookie: Cookie = Cookie("S", "", httpOnly = true, sameSite = None),
  keyGenerator: KeyGenerator = KeyGenerator()
): SessionStore {
  private val keyCipher = KeyCipher(keyGenerator.keyFromSecret(sessionSecret))

  override fun load(exchange: HttpExchange) = exchange.cookie(cookie.name)?.let {
    Session(urlDecodeParams(keyCipher.decrypt(it)) as MutableMap<String, String?>, isNew = false)
  } ?: Session()

  override fun save(exchange: HttpExchange, session: Session) {
    if (session.changed)
      exchange += cookie.copy(value = keyCipher.encrypt(urlEncodeParams(session.params)), secure = exchange.isSecure)
  }
}
