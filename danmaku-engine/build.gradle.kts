plugins {
    alias(libs.plugins.androidLibrary)
}

android {
    namespace = "com.bytedance.danmaku.render.engine"
    compileSdk = 37
    buildToolsVersion = "37.0.0"

    defaultConfig {
        minSdk = 33
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        buildConfig = true
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.robolectric)
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.collection:collection:1.5.0")
    implementation(libs.coroutines.android)
}

apply(from = rootProject.file("gradle/robolectric-tests.gradle.kts"))
