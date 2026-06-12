plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp") version "2.2.10-2.0.2"
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.kmhmubin.kothagolp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.kmhmubin.kothagolp"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs += "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/INDEX.LIST",
                "META-INF/DEPENDENCIES",
                "META-INF/io.netty.versions.properties"
            )
        }
    }
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:common"))
    implementation(project(":source-api"))
    implementation(project(":core:data"))
    implementation(project(":core:ui"))

    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // DataStore (for preferences/settings)
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Kotlin Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // Compose
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation("androidx.compose.material3:material3:1.4.0")
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.34.0")

    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.palette.ktx)

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.4")

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    // Room Database
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    implementation(libs.androidx.room.common.jvm)
    implementation(libs.androidx.adapters)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.animation)
    ksp("androidx.room:room-compiler:2.8.4")

    // Networking
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // HTML Parsing
    implementation("org.jsoup:jsoup:1.18.1")

    // Image Loading
    implementation("io.coil-kt:coil-compose:2.7.0")

    // EPUB Generation
    implementation("org.redundent:kotlin-xml-builder:1.9.1")

    // JSON
    implementation("com.google.code.gson:gson:2.11.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Background sync
    implementation("androidx.work:work-runtime-ktx:2.10.3")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Google Drive sync
    implementation("com.google.apis:google-api-services-drive:v3-rev20251210-2.0.0")

    // For MediaStyle notifications
    implementation("androidx.media:media:1.7.0")

    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("sh.calvin.reorderable:reorderable:2.4.0")

    // Icons
    implementation("androidx.compose.material:material-icons-extended:1.7.5")

    implementation("androidx.core:core-splashscreen:1.0.1")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
