plugins {
    id("kothagolp.android.library")
}

android {
    namespace = "com.kmhmubin.kothagolp.source.api"
}

dependencies {
    api(project(":core:domain"))
    api(project(":core:common"))

    // HTML Parsing (providers use Jsoup directly)
    implementation("org.jsoup:jsoup:1.18.1")

    // Coroutines (provider registry uses StateFlow)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")

    // Android annotations (@DrawableRes)
    implementation("androidx.annotation:annotation:1.9.1")
}
