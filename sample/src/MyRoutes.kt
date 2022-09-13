import klite.HttpExchange
import klite.StatusCode
import klite.annotations.*
import klite.i18n.Lang
import kotlinx.coroutines.delay
import users.UserRepository
import java.util.*

@Path("/hello")
class MyRoutes(private val userRepository: UserRepository) {
  @GET fun sayHello() = MyData("Hello")
  @GET("2") fun withExchange(exchange: HttpExchange) = "Hello2 ${exchange.method} ${exchange.path}"
  @GET("3") fun HttpExchange.asContext() = "${Lang.translate(this, "hello")} $method $path"

  @GET("/user/:id") fun user(@PathParam id: UUID) = userRepository.get(id)

  @GET("/suspend") suspend fun suspend(exchange: HttpExchange): String {
    delay(100)
    return "Suspend ${exchange.method} ${exchange.path}"
  }

  @GET("/suspend204") suspend fun suspendNoContent() = delay(50)
  @POST("/201") fun created() = StatusCode.Created
  @GET("/null") fun returnNull() = null

  @GET("/admin") @AdminOnly fun onlyForAdmins() = "Only for admins"

  @GET("/params")
  fun withOptionalParams(@QueryParam optional: Boolean = false, @QueryParam required: String, @QueryParam nullable: String?) = "$optional,$required,$nullable"

  @GET("/broken-render")
  fun brokenRender() = object { lateinit var hello: String }

  @POST("/post")
  fun postExample(body: MyData, @QueryParam optional: Boolean = true) =
    "Received $body as json, optional = $optional"
}

data class MyData(val hello: String, val world: Double = Math.PI)
