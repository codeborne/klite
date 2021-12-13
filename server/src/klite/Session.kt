package klite

import klite.Cookie.SameSite.None
import klite.crypto.KeyCipher
import klite.crypto.KeyGenerator

abstract class Session {
  protected val params: MutableMap<String, String?> = mutableMapOf()
  protected var changed = false
  var isNew = true

  fun clear() = params.clear()
  operator fun get(key: String) = params[key]
  operator fun set(key: String, value: String?) {
    params[key] = value
    changed = true
  }

  protected abstract fun load(exchange: HttpExchange)
  protected abstract fun save(exchange: HttpExchange)
}

class CookieSession(
  sessionSecret: String = Config.required("SESSION_SECRET"),
  val cookie: Cookie = Cookie("S", "", httpOnly = true, sameSite = None),
  keyGenerator: KeyGenerator = KeyGenerator()
): Session() {
  val keyCipher = KeyCipher(keyGenerator.keyFromSecret(sessionSecret))

  override fun load(exchange: HttpExchange) {
    exchange.cookie(cookie.name)?.let {
      params += urlDecodeParams(keyCipher.decrypt(it))
      isNew = false
    }
  }

  override fun save(exchange: HttpExchange) {
    if (changed) exchange += cookie.copy(value = keyCipher.encrypt(urlEncodeParams(params)), secure = exchange.isSecure)
  }
}
