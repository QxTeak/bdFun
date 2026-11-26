plugins {
    alias(libs.plugins.androidLibrary)
}

android {
    namespace = "com.aritxonly.bdfun.infra.bili"
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
}

kotlin { jvmToolchain(21) }

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:model"))
    implementation(project(":core:domain"))
    implementation(project(":danmaku-engine"))
    testImplementation(libs.junit)
    testImplementation(libs.json.jvm)
    implementation(libs.okhttp)
    implementation(libs.brotli.dec)
    implementation(libs.coroutines.android)
}
