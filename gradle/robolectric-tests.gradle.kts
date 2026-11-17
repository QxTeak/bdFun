import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.tasks.testing.Test

// 测试 SDK 交给 Gradle 解析和缓存，避免 Robolectric 在测试 JVM 内临时联网遇到 429。
val catalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
val robolectricTestSdk = configurations.create("robolectricTestSdk") {
    isCanBeResolved = true
    isCanBeConsumed = false
    isTransitive = false
}
dependencies.add(robolectricTestSdk.name, catalog.findLibrary("robolectric-android-sdk").get())
val sdkDirectory = layout.buildDirectory.dir("robolectric-sdk")
val prepareRobolectricTestSdk = tasks.register<Sync>("prepareRobolectricTestSdk") {
    from(robolectricTestSdk)
    into(sdkDirectory)
}
tasks.withType<Test>().configureEach {
    dependsOn(prepareRobolectricTestSdk)
    inputs.files(robolectricTestSdk).withPropertyName("robolectricSdk").withPathSensitivity(PathSensitivity.NONE)
    systemProperty("robolectric.dependency.dir", sdkDirectory.get().asFile.absolutePath)
}
