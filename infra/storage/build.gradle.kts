plugins { alias(libs.plugins.androidLibrary) }
android {
    namespace = "com.aritxonly.bdfun.infra.storage"
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
    implementation(project(":core:domain"))
    implementation(project(":core:common"))
    implementation(libs.coroutines.core)
    testImplementation(libs.junit)
    testImplementation(libs.json.jvm)
}

dependencies {
    implementation(project(":core:navigation"))
    implementation(project(":core:model"))
}
