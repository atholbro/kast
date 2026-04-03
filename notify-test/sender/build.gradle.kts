plugins {
    kotlin("jvm")
    application

    // jib + javaagent for otel
    alias(libs.plugins.jib)
    alias(libs.plugins.jib.javaagent)
}

val version = (System.getenv("VERSION") ?: "").ifEmpty { "latest" }

application {
    mainClass.set("net.aholbrook.kast.notifytest.MainKt")
}

dependencies {
    with (rootProject) {
        implementation(project(":kast"))
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
        to { image = "${System.getenv("REGISTRY")}/notify-test-sender:$version" }
        container {
            ports = listOf("8080")
        }
    }
}
