plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeCompiler)
}
android {
    namespace = "com.aritxonly.bdfun.feature.home"
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
    implementation(project(":core:application"))
    api(project(":core:model"))
    api(project(":core:domain"))
    api(project(":TeakUI"))
    implementation(project(":core:common"))
    implementation(libs.coroutines.android)
    implementation(libs.coil.compose)
    testImplementation(libs.junit)
}
