package net.aholbrook.kast.notifytest.receiver.http

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.micrometer.core.instrument.MeterRegistry

fun Application.installHealthChecks(micrometerRegistry: MeterRegistry) {
    val healthCheckCounter = micrometerRegistry.counter("health_check_counter")
    val readyCheckCounter = micrometerRegistry.counter("ready_check_counter")

    routing {
        get("/health") {
            healthCheckCounter.increment()
            call.respond(HttpStatusCode.OK, "")
        }

        get("/ready") {
            readyCheckCounter.increment()
            call.respond(HttpStatusCode.OK, "")
        }
    }
}
