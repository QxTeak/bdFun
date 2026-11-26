plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeCompiler)
}
android {
    namespace = "com.aritxonly.bdfun.feature.video"
    compileSdk {
        version = release(37) {
            minorApiLevel = 2
        }
    }
    buildToolsVersion = "37.0.0"
    defaultConfig { minSdk = 33 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures { compose = true }
}
kotlin { jvmToolchain(21) }
dependencies {
    api(project(":core:model"))
    api(project(":core:domain"))
    api(project(":core:common"))
    api(project(":core:application"))
    api(project(":TeakUI"))
    api(libs.coroutines.core)
    implementation(libs.coroutines.android)
    implementation(libs.androidx.core)
    implementation(libs.androidx.activity.compose)
    testImplementation(libs.junit)
}
