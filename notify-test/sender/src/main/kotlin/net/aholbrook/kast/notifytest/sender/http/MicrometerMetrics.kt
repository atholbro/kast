package net.aholbrook.kast.notifytest.sender.http

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmHeapPressureMetrics
import io.micrometer.core.instrument.binder.jvm.JvmInfoMetrics
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.prometheus.metrics.instrumentation.jvm.JvmMetrics

fun Application.installMicrometerMetrics(prometheusMeterRegistry: PrometheusMeterRegistry): PrometheusMeterRegistry {
    // Register Prometheus JVM metrics (req. for JVM dashboards)
    JvmMetrics.builder().register(prometheusMeterRegistry.prometheusRegistry)

    install(MicrometerMetrics) {
        registry = prometheusMeterRegistry

        // Micrometer JVM metrics (less useful)
        meterBinders =
            listOf(
                // note: JvmMemoryMetrics conflicts with Prometheus JvmMetrics, which are superior
                JvmHeapPressureMetrics(),
                JvmGcMetrics(),
                JvmThreadMetrics(),
                JvmInfoMetrics(),
                ProcessorMetrics(),
            )
    }

    routing {
        get("/metrics") {
            call.respond(prometheusMeterRegistry.scrape())
        }
    }

    return prometheusMeterRegistry
}
