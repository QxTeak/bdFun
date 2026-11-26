pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenLocal()
        google()
        mavenCentral()
        maven("https://maven.aliyun.com/repository/public")
    }
}

rootProject.name = "bdFun"
include(":app")
include(":core:model")
include(":TeakUI")
include(":danmaku-engine")

// Miuix 使用本地源码构建（Apache-2.0）。
// 仓库不包含该源码；请把 Miuix 源码放在项目旁边的 miuix-src/ 目录，
// 或通过环境变量 MIUIX_SOURCE=/path/to/miuix 指定。
// 找不到源码时，会使用 dependencyResolutionManagement 里的 Maven 依赖。
val miuixSource = file(
    System.getenv("MIUIX_SOURCE")
        ?: gradle.startParameter.projectProperties["miuixSource"]
        ?: file(".local/miuix-bc1024d78a154f5167efff262ae5ed66264c481e")
            .takeIf { it.resolve("settings.gradle.kts").isFile }?.path
        ?: "../miuix-src/miuix-bc1024d78a154f5167efff262ae5ed66264c481e",
)
if (miuixSource.resolve("settings.gradle.kts").isFile) {
    includeBuild(miuixSource) {
        dependencySubstitution {
            listOf(
                "miuix-core" to ":miuix-core",
                "miuix-material" to ":miuix-material",
                "miuix-squircle" to ":miuix-squircle",
                "miuix-ui" to ":miuix-ui",
                "miuix-blur" to ":miuix-blur",
                "miuix-preference" to ":miuix-preference",
                "miuix-icons" to ":miuix-icons",
                "miuix-glass" to ":miuix-glass",
                "miuix-glass-icons" to ":miuix-glass-icons",
                "miuix-nav" to ":miuix-nav",
                "miuix-window" to ":miuix-window",
            ).forEach { (artifact, projectPath) ->
                substitute(module("top.yukonga.miuix.kmp:$artifact"))
                    .using(project(projectPath))
                substitute(module("top.yukonga.miuix.kmp:${artifact}-android"))
                    .using(project(projectPath))
            }
        }
    }
}

include(":core:common")

include(":core:domain")

include(":core:application")

include(":core:navigation")

include(":infra:bili")

include(":infra:storage")

include(":infra:playback")

include(":feature:home")
include(":tools:kotlin-audit")

include(":feature:video")
