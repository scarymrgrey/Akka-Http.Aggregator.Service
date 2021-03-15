package com.fedex.infrastructure.core.configs

case class XyzHttpServiceBusConfigs(maxElements: Int, queueSize: Int, finiteDurationSeconds: Int, backendHost: String, backendPort: Int)