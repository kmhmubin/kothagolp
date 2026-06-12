plugins {
    id("kothagolp.android.library")
    id("com.google.devtools.ksp") version "2.2.10-2.0.2"
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.kmhmubin.kothagolp.core.data"
}

dependencies {
    api(project(":source-api"))
    api(project(":core:common"))

    // Room Database
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // JSON
    implementation("com.google.code.gson:gson:2.11.0")

    // Background sync
    implementation("androidx.work:work-runtime-ktx:2.10.3")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Google Drive sync
    implementation("com.google.apis:google-api-services-drive:v3-rev20251210-2.0.0")

    // EPUB Generation
    implementation("org.redundent:kotlin-xml-builder:1.9.1")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Notifications (SyncNotifier)
    implementation("androidx.core:core-ktx:1.17.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:core-ktx:1.6.1")
    androidTestImplementation("androidx.room:room-testing:2.8.4")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
}
