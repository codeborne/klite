package org.slf4j.impl

import org.slf4j.ILoggerFactory
import org.slf4j.spi.LoggerFactoryBinder
import java.util.*

object StaticLoggerBinder: LoggerFactoryBinder {
  @JvmField val REQUESTED_API_VERSION = "1.7.36"
  @JvmStatic fun getSingleton() = this
  private val factory = ServiceLoader.load(ILoggerFactory::class.java).findFirst().get()
  override fun getLoggerFactory() = factory
  override fun getLoggerFactoryClassStr() = factory.javaClass.name
}
