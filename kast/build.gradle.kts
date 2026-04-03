import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-library`
    `maven-publish`
    signing
    jacoco

    alias(libs.plugins.kotlin.jvm)

    alias(libs.plugins.nmcp)
    alias(libs.plugins.nmcp.aggregation)
}

dependencies {
    with (rootProject) {
        implementation(libs.kotlin.coroutines.core)
        implementation(libs.dnsjava)

        testImplementation(libs.mockk)

        nmcpAggregation(project(":kast"))
    }
}

java {
    withJavadocJar()
    withSourcesJar()
}

jacoco {
    toolVersion = libs.versions.jacoco.get()
}

tasks {
    jar {
        archiveBaseName.set("kast")
    }

    withType<JavaCompile>().configureEach {
        options.release.set(17)
    }

    withType<KotlinCompile>().configureEach {
        compilerOptions {
            freeCompilerArgs.add("-opt-in=net.aholbrook.kast.InternalApi")
        }
    }


    withType<AbstractPublishToMaven>().configureEach {
        dependsOn(rootProject.tasks.named("check"))
    }

    withType<AbstractArchiveTask>().configureEach {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
    }

    withType<Javadoc>().configureEach {
        options {
            (this as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
        }
    }

    jacocoTestReport {
        dependsOn(test)

        reports {
            xml.required = true
            csv.required = true
            html.required = true
        }
    }
}

publishing {
    publications {
        group = "net.aholbrook.kast"
        version = System.getenv("VERSION") ?: ""

        create<MavenPublication>("maven") {
            artifactId = "kast"
            from(components["java"])

            pom {
                name.set("Kast")
                description.set("Kubernetes Asynchronous Signaling Transport")
                url.set("https://github.com/atholbro/kast")

                licenses {
                    license {
                        name.set("The MIT License (MIT)")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }

                developers {
                    developer {
                        id.set("aholbrook")
                        name.set("Andrew Holbrook")
                        email.set("atholbro@gmail.com")
                    }
                }

                scm {
                    connection.set("scm:git:git@github.com:atholbro/kast.git")
                    developerConnection.set("scm:git:git@github.com:atholbro/kast.git")
                    url.set("https://github.com/atholbro/kast")
                }
            }
        }
    }

    repositories {
        maven {
            url = if (version.toString().endsWith("SNAPSHOT")) {
                uri("https://central.sonatype.com/repository/maven-snapshots/")
            } else {
                uri("https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/")
            }

            credentials {
                username = System.getenv("PUBLISH_USER")
                password = System.getenv("PUBLISH_PASS")
            }
        }
    }
}

signing {
    useInMemoryPgpKeys(
        System.getenv("GPG_KEY"),
        System.getenv("GPG_PASS"),
    )
    sign(publishing.publications)
}

nmcpAggregation {
    centralPortal {
        username = System.getenv("PUBLISH_USER")
        password = System.getenv("PUBLISH_PASS")

        publishingType = "USER_MANAGED"
    }
}
