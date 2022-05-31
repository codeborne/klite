package org.slf4j.impl

import klite.slf4j.KliteLoggerFactory
import org.slf4j.spi.LoggerFactoryBinder

object StaticLoggerBinder: LoggerFactoryBinder {
  @JvmField val REQUESTED_API_VERSION = "1.7.36"
  @JvmStatic fun getSingleton() = this
  private val factory = KliteLoggerFactory()
  override fun getLoggerFactory() = factory
  override fun getLoggerFactoryClassStr() = factory.javaClass.name
}
