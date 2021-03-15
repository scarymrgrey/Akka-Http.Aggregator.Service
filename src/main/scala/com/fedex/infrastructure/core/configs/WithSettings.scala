package com.fedex.infrastructure.core.configs

import com.typesafe.config.ConfigFactory
import pureconfig.ConfigSource

trait WithSettings {
  import pureconfig.generic.auto._
  private val conf = ConfigFactory.defaultApplication().resolve()
  val xyzConfs = ConfigSource
    .fromConfig(conf)
    .at("xyz-backend")
    .loadOrThrow[XyzHttpServiceBusConfigs]
}
