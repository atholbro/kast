package com.github.atholbro.notifytest.receiver

import com.github.atholbro.notifytest.receiver.http.installHealthChecks
import com.github.atholbro.notifytest.receiver.http.installMicrometerMetrics
import com.github.atholbro.udpkt.NotificationDecoder
import com.github.atholbro.udpkt.Receiver
import com.github.atholbro.udpkt.StringEncoding
import installMicrometerMetrics
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import mu.KotlinLogging
import java.time.Duration
import java.time.OffsetDateTime

private val logger = KotlinLogging.logger {}

fun main() {
    val micrometerRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    val timePort = System.getenv("TIME_PORT").toInt()
    val receiver = Receiver(timePort, NotificationDecoder(StringEncoding))

    logger.info { "listen on: $timePort" }
    CoroutineScope(Dispatchers.Default).launch {
        receiver.start()

        while (isActive) {
            receiver.whenNotified("time") {
                val now = OffsetDateTime.now()
                val received = OffsetDateTime.parse(it)
                logger.info { "Received time: $received, delta: ${Duration.between(received, now).toNanos()}ns" }
            }
        }
    }

    // Start KTOR server
    embeddedServer(Netty, port = 8081, host = "0.0.0.0") {
        installMicrometerMetrics(micrometerRegistry)
        installHealthChecks(micrometerRegistry)
    }.apply { start(wait = true) }
}
