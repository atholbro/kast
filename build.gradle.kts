import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    base
    `version-catalog`

    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlinter)
    alias(libs.plugins.detekt)
    alias(libs.plugins.gradleVersions)
    alias(libs.plugins.gradleVersions.filter)
    alias(libs.plugins.gradleVersions.update)
    alias(libs.plugins.binaryCompatibilityValidator)
}

apiValidation {
    apiDumpDirectory = ".api-validation"
    nonPublicMarkers += "net.aholbrook.kast.InternalApi"
    ignoredProjects.addAll(listOf("notify-test", "receiver", "sender"))
}

allprojects {
    val isNotifyTestProject = path == ":notify-test" || path.startsWith(":notify-test:")

    apply(plugin = "kotlin")
    apply(plugin = "org.jmailen.kotlinter")
    if (!isNotifyTestProject) {
        apply(plugin = "io.gitlab.arturbosch.detekt")
    }

    repositories {
        mavenLocal()
        mavenCentral()
    }

    dependencies {
        with (rootProject) {
            implementation(libs.kotlinLogging)

            testImplementation(libs.kotest.assertions.core)
            testImplementation(libs.junit.jupiter.api)
            testImplementation(libs.junit.jupiter.params)

            testRuntimeOnly(libs.junit.jupiter.engine)
            testRuntimeOnly(libs.junit.platform.launcher)
        }
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(rootProject.libs.versions.jvm.get()))
        }
    }

    tasks {
        withType<KotlinCompile>().configureEach {
            compilerOptions {
                jvmTarget.set(
                    JvmTarget.valueOf(
                        "JVM_" + rootProject.libs.versions.jvm.get().replace('.', '_')
                    )
                )
            }
        }

        withType<JavaCompile> { options.encoding = "UTF-8" }

        withType<Test>().configureEach {
            useJUnitPlatform()
            jvmArgs = listOf("-Xshare:off")

            testLogging {
                events("failed", "skipped")
                showExceptions = true
                exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
                showCauses = true
                showStackTraces = true
                showStandardStreams = true
            }
        }
    }

    if (!isNotifyTestProject) {
        detekt {
            buildUponDefaultConfig = true
            config.setFrom(files("${project.rootDir}/detekt-config.yml"))
        }
    }
}

tasks.check {
    dependsOn(tasks.apiCheck)
}
