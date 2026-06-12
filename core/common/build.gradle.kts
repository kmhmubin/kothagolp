plugins {
    id("kothagolp.android.library")
}

android {
    namespace = "com.kmhmubin.kothagolp.core.common"
}

dependencies {
    implementation(project(":core:domain"))

    // Networking
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // HTML Parsing
    implementation("org.jsoup:jsoup:1.18.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
}
