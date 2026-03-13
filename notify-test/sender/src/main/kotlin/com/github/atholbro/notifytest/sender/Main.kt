package com.github.atholbro.notifytest.sender

import com.github.atholbro.notifytest.sender.http.installHealthChecks
import com.github.atholbro.notifytest.sender.http.installMicrometerMetrics
import com.github.atholbro.udpkt.Notification
import com.github.atholbro.udpkt.NotificationEncoder
import com.github.atholbro.udpkt.Sender
import com.github.atholbro.udpkt.StringEncoding
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import mu.KotlinLogging
import java.time.Duration
import java.time.OffsetDateTime

private val logger = KotlinLogging.logger {}

fun main() {
    val micrometerRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    val targets = System.getenv("TARGETS")
    val timePort = System.getenv("TIME_PORT").toInt()

    val sender = Sender(
        hosts = arrayOf(targets),
        port = timePort,
        encoder = NotificationEncoder(StringEncoding),
        refreshInterval = Duration.ofSeconds(15),
    )

    sender.start()
    CoroutineScope(Dispatchers.Default).launch {
        while (isActive) {
            delay(1000)
            val notification = Notification("time", OffsetDateTime.now().toString())
            logger.info { "sending time ($targets:$timePort): $notification" }
            sender.notifyAll(notification)
        }
    }

    // Start KTOR server
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        installMicrometerMetrics(micrometerRegistry)
        installHealthChecks(micrometerRegistry)
    }.apply { start(wait = true) }

    sender.stop()
}
