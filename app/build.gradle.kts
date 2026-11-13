import java.text.SimpleDateFormat
import java.util.Date
import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

/** 构建时刻，写进 BuildConfig 用于核对设备上装的是哪一次构建。 */
val buildStamp: String = SimpleDateFormat("MM-dd HH:mm").format(Date())

/**
 * 允许命令行临时改包名，用来做实验（比如伪装成系统名单里的某个包）：
 *   gradlew :app:assembleDebug -Ppkg=com.miui.videoplayer
 * 不传就保持原来的 com.aritxonly.bdfun。
 */
val pkgOverride: String? = (findProperty("pkg") as String?)?.takeIf { it.isNotBlank() }

// 正式签名从本机根目录的 keystore.properties 读取；该文件不进仓库。
// 没有配置时退回 debug 签名，方便本地调试，但正式发布必须配置 release 签名。
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.isFile) {
        keystorePropsFile.inputStream().use { load(it) }
    }
}

val releaseStoreFile = providers.environmentVariable("ANDROID_KEYSTORE_PATH").orNull

android {
    namespace = "com.aritxonly.bdfun"
    compileSdk = 37
    buildToolsVersion = "37.0.0"

    defaultConfig {
        applicationId = pkgOverride ?: "com.aritxonly.bdfun"
        // miuix-blur 鐨?RuntimeShader 璺緞鍩虹嚎
        minSdk = 33
        targetSdk = 37
        versionCode = 500
        versionName = "preview-5.0"
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
                storePassword = providers.environmentVariable("KEYSTORE_PASSWORD").get()
                keyAlias = providers.environmentVariable("KEY_ALIAS").get()
                keyPassword = providers.environmentVariable("KEY_PASSWORD").get()
                enableV1Signing = false
                enableV2Signing = true
                enableV3Signing = false
                enableV4Signing = false
            }
        } else if (keystorePropsFile.isFile) {
            create("release") {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            // 与 release 分开安装，避免相互覆盖；临时改包名做实验时不加后缀
            if (pkgOverride == null) applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            // 正式包使用 release keystore；没配置时退回 debug 签名仅用于本地调试。
            signingConfig = if (!releaseStoreFile.isNullOrBlank() || keystorePropsFile.isFile) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
            isDebuggable = false
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
    implementation(project(":feature:home"))
    implementation(project(":feature:video"))
    implementation(project(":infra:storage"))
    implementation(project(":infra:playback"))
    implementation(project(":infra:bili"))
    implementation(project(":core:navigation"))
    implementation(project(":core:application"))
    implementation(project(":core:domain"))
    implementation(project(":core:common"))
    testImplementation("junit:junit:4.13.2")
    implementation(project(":core:model"))
    implementation(project(":TeakUI"))
    implementation(project(":danmaku-engine"))

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
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.media)
    implementation(libs.coroutines.android)

    implementation(libs.okhttp)
    implementation(libs.coil.compose)

    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)
    implementation(libs.zxing.core)
}
