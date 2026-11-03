import java.text.SimpleDateFormat
import java.util.Date

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

/** 构建时刻，写进 BuildConfig 用于核对设备上装的是哪一次构建。 */
val buildStamp: String = SimpleDateFormat("MM-dd HH:mm").format(Date())

// 自动构建通过环境变量传入固定的发布签名密钥。
val releaseStoreFile = providers.environmentVariable("ANDROID_KEYSTORE_PATH").orNull

android {
    namespace = "com.aritxonly.bdfun"
    compileSdk = 37
    buildToolsVersion = "37.0.0"

    defaultConfig {
        applicationId = "com.aritxonly.bdfun"
        // miuix-blur 鐨?RuntimeShader 璺緞鍩虹嚎
        minSdk = 33
        targetSdk = 37
        versionCode = 125
        versionName = "preview-3.9.4"
        // 每次配置都刷新，打进 BuildConfig 后可以直接在「设置 → 关于 → 版本」里核对装的是哪一版。
        buildConfigField("String", "BUILD_STAMP", "\"$buildStamp\"")
        vectorDrawables.useSupportLibrary = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    signingConfigs {
        if (!releaseStoreFile.isNullOrBlank()) {
            create("release") {
                storeFile = file(releaseStoreFile)
                storePassword = providers.environmentVariable("ANDROID_KEYSTORE_PASSWORD").get()
                keyAlias = providers.environmentVariable("ANDROID_KEY_ALIAS").get()
                keyPassword = providers.environmentVariable("ANDROID_KEY_PASSWORD").get()
            }
        }
    }

    buildTypes {
        debug {
            // 与 release 分开安装，避免相互覆盖
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            // 本地预览构建沿用调试签名；自动发布必须配置发布签名密钥。
            signingConfig = signingConfigs.getByName(
                if (releaseStoreFile.isNullOrBlank()) "debug" else "release"
            )
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    packaging {
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/DEPENDENCIES",
                "META-INF/*.kotlin_module",
            )
        }
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":TeakUI"))

    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
    implementation(libs.compose.animation)
    implementation(libs.compose.material3)

    implementation(libs.miuix.ui)
    implementation(libs.miuix.blur)
    implementation(libs.miuix.icons)

    implementation(libs.kyant.shapes)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.media)
    implementation(libs.coroutines.android)

    implementation(libs.okhttp)
    implementation(libs.coil.compose)

    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)
    implementation(libs.zxing.core)
}
