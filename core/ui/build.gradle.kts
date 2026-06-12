plugins {
    id("kothagolp.android.compose")
}

android {
    namespace = "com.kmhmubin.kothagolp.core.ui"
}

dependencies {
    api(project(":core:domain"))
    implementation("androidx.core:core-ktx:1.17.0")
    implementation(project(":core:common"))

    // Compose
    implementation("androidx.compose.material3:material3:1.4.0")
    implementation("androidx.compose.foundation:foundation:1.10.0")
    implementation("androidx.compose.ui:ui:1.10.0")
    implementation("androidx.compose.material:material-icons-extended:1.7.5")

    // Window size class for adaptive UI
    implementation("androidx.compose.material3:material3-window-size-class:1.3.2")

    // Image loading
    implementation("io.coil-kt:coil-compose:2.7.0")
}
