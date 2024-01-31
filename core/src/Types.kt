package klite

/** Base class for String-based normalized value types */
abstract class StringValue(val value: String) {
  override fun toString() = value
  override fun equals(other: Any?) = other != null && javaClass == other.javaClass && value == (other as StringValue).value
  override fun hashCode() = value.hashCode()
}

class Email(email: String): StringValue(email.trim().lowercase()) {
  companion object {}
  init { require(value.length > 3 && value.contains("@")) { "Invalid email: $email" } }
  val domain get() = value.substringAfter("@")
}

class Phone(phone: String): StringValue(phone.replace(removeChars, "")) {
  companion object {
    private val removeChars = "[\\s-()]+".toRegex()
    private val valid = "\\+[0-9]{9,}".toRegex()
  }
  init { require(valid.matches(value)) {
    "International phone number should start with + and have at least 9 digits: $value" }
  }
}

class Password(value: String): StringValue(value) {
  override fun toString() = "Password<***>"
}
