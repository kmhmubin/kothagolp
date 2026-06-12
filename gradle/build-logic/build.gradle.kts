plugins {
    `kotlin-dsl`
}

group = "com.kmhmubin.kothagolp.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    compileOnly("com.android.tools.build:gradle:9.2.1")
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.10")
    compileOnly("org.jetbrains.kotlin:compose-compiler-gradle-plugin:2.2.10")
}
