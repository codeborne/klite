package klite.slf4j

class Slf4jRedirectorCreator: System.LoggerFinder() {
  override fun getLogger(name: String, module: Module) = Slf4jRedirector(name)
}
