plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeCompiler)
}

android {
    namespace = "com.aritxonly.teak.common"
    compileSdk = 37
    buildToolsVersion = "37.0.0"

    defaultConfig {
        minSdk = 33
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.androidx.activity.compose)
    testImplementation("junit:junit:4.13.2")
    // Public API exposes Compose / Miuix types (Backdrop, Shape, MiuixTheme colors).
    api(libs.compose.runtime)
    api(libs.compose.foundation)
    api(libs.compose.ui)
    api(libs.compose.animation)
    api(libs.compose.material3)
    api(libs.miuix.ui)
    api(libs.miuix.blur)
    api(libs.miuix.icons)
    api(libs.kyant.shapes)

    implementation(libs.coroutines.android)
}
