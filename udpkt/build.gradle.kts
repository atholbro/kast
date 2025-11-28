dependencies {
    with (rootProject) {
        implementation(libs.kotlin.coroutines.core)
        implementation(libs.dnsjava)

        testImplementation(libs.kotest.assertions)
        testImplementation(libs.mockk)
    }
}
