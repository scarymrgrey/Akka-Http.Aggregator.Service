package com.fedex.infrastructure.core.configs

import scala.concurrent.duration.FiniteDuration

case class XyzHttpServiceBusConfigs(maxElements: Int, queueSize: Int, finiteDurationSeconds: Int, backendHost: String, backendPort: Int)