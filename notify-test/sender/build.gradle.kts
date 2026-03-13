plugins {
    kotlin("jvm")
    application

    // jib + javaagent for otel
    alias(libs.plugins.jib.jibPlugin)
    alias(libs.plugins.jib.javaagentPlugin)
}

val version = (System.getenv("VERSION") ?: "").ifEmpty { "latest" }

application {
    mainClass.set("com.github.atholbrook.notifytest.MainKt")
}

dependencies {
    with (rootProject) {
        implementation(project(":udpkt"))
        implementation(libs.bundles.opentelemetry)
        implementation(libs.bundles.ktor.server)
        implementation(libs.bundles.ktor.micrometer)

        runtimeOnly(libs.logbackClassic)

        javaagent(libs.opentelemetry.javaagent)
    }
}

tasks {
    jib {
        from { image = "library/eclipse-temurin:${rootProject.libs.versions.eclipseTemurin.get()}" }
        to { image = "harbor.holbrook.casa/library/notify-test-sender:$version" }
        container {
            ports = listOf("8080")
        }
    }
}
