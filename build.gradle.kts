import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    base
    `maven-publish`
    `version-catalog`

    alias(libs.plugins.kotlinPlugin)
    alias(libs.plugins.kotlinterPlugin)
    alias(libs.plugins.detektPlugin)
    alias(libs.plugins.gradleVersionsPluign)
    alias(libs.plugins.gradleVersionsFilterPlugin)
    alias(libs.plugins.gradleUpdateVersionsPlugin)
}

allprojects {
    apply(plugin = "kotlin")
    apply(plugin = "org.jmailen.kotlinter")
    apply(plugin = "io.gitlab.arturbosch.detekt")
    apply(plugin = "maven-publish")

    repositories {
        mavenLocal()
        mavenCentral()
    }

    dependencies {
        implementation(rootProject.libs.kotlinLogging)

        testImplementation(rootProject.libs.junit.jupiter)
        testRuntimeOnly(rootProject.libs.junit.launcher)
    }

    tasks {
        withType<KotlinCompile>().configureEach {
            compilerOptions {
                jvmTarget.set(
                    JvmTarget.valueOf("JVM_" + rootProject.libs.versions.jdk.get().replace('.', '_'))
                )
                freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
            }
        }

        withType<JavaCompile> {
            options.encoding = "UTF-8"
        }

        withType<Test> {
            useJUnitPlatform()

            jvmArgs = listOf("-Xshare:off")
        }


    }

    java {
        withJavadocJar()
        withSourcesJar()

        toolchain {
            languageVersion.set(JavaLanguageVersion.of(rootProject.libs.versions.jdk.get()))
        }
    }

    detekt {
        buildUponDefaultConfig = true
        config.setFrom(files("${project.rootDir}/detekt-config.yml"))
    }

    publishing {
        group = "com.github.atholbro"
        version = System.getenv("VERSION") ?: "0.0.1-SNAPSHOT"

        repositories {
            maven {
                val publishName: String by project
                val publishUrl: String by project
                val publishUsername: String by project
                val publishPassword: String by project

                name = publishName
                url = uri(publishUrl)

                credentials {
                    username = publishUsername
                    password = publishPassword
                }
            }
        }

        publications {
            create<MavenPublication>("maven") {
                from(components["java"])
            }
        }
    }
}

tasks.test {
    useJUnitPlatform()
}

versionsFilter {
    gradleReleaseChannel.set("release")
}
