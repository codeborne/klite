package klite.oauth

import klite.HttpExchange
import klite.Session
import klite.UnauthorizedException
import klite.annotations.AttrParam
import klite.annotations.GET
import java.lang.System.currentTimeMillis

open class AuthRoutes {
  @GET("/user") open fun currentUser(@AttrParam user: User?) = user ?: throw UnauthorizedException()
  @GET("/logout") open fun logout(session: Session) = session.clear()
}

fun HttpExchange.initSession(user: User) {
  session.clear()
  session["userId"] = user.id.toString()
  session["started"] = currentTimeMillis().toString()
  attr("user", user)
}
