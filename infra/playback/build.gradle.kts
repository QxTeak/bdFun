plugins { alias(libs.plugins.androidLibrary) }
android {
    namespace = "com.aritxonly.bdfun.infra.playback"
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
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    implementation(project(":core:application"))
    implementation(project(":core:domain"))
    implementation(project(":core:model"))
    implementation(libs.media3.exoplayer)
    // 本地 MPD（多档视频 + 单音轨）播放：换档只改轨道选择，音频不断。
    implementation(libs.media3.exoplayer.dash)
    implementation(libs.media3.ui)
    implementation(libs.androidx.media)
    implementation(libs.androidx.core)
    implementation(libs.coil.core)
    // 新模块也按 app 的已解析版本约束旧 media-compat 的传递依赖，不重新引入旧 AndroidX。
    constraints {
        implementation(libs.androidx.lifecycle.runtime)
        implementation(libs.androidx.lifecycle.common)
        implementation(libs.androidx.arch.core.common)
        implementation(libs.androidx.collection)
        implementation(libs.androidx.annotation)
    }
    constraints { implementation(libs.androidx.annotation.experimental) { because("与 app 已解析的 Media3 注解版本一致") } }
    implementation(libs.coroutines.android)
}

apply(from = rootProject.file("gradle/robolectric-tests.gradle.kts"))
