package com.aritxonly.bdfun.core.domain.update

data class AppVersion(val name: String, val code: Int) {
    companion object {
        fun parse(raw: String): AppVersion? {
            val name = raw.removePrefix("v")
            val preview = Regex("preview-(0|[1-9][0-9]?)\\.(0|[1-9][0-9]?)").matchEntire(name)
            val stable = Regex("(0|[1-9][0-9]{0,2})\\.(0|[1-9][0-9]?)\\.(0|[1-9][0-9]?)").matchEntire(name)
            val parts = when {
                preview != null -> listOf(0, preview.groupValues[1].toInt(), preview.groupValues[2].toInt())
                stable != null -> stable.groupValues.drop(1).map(String::toInt)
                else -> return null
            }
            return AppVersion(name, parts[0] * 10000 + parts[1] * 100 + parts[2])
        }
    }
}

data class UpdateRelease(val version: AppVersion, val notes: String, val page: String, val apk: String?,
    val prerelease: Boolean = version.name.startsWith("preview-"))

fun releaseNoteLines(markdown: String): List<String> = markdown.lineSequence().map { line ->
    line.trim().removeSuffix("\\").replace(Regex("^[-*+]\\s+"), "")
        .replace(Regex("^#{1,6}\\s+"), "")
        .replace(Regex("\\[([^]]+)]\\([^)]+\\)"), "$1")
        .replace("**", "").replace("__", "").replace("`", "").trim()
}.filter { it.isNotBlank() }.toList()

fun latestRelease(releases: List<UpdateRelease>, includePreview: Boolean): UpdateRelease? =
    releases.filter { it.version.code >= 0 && (includePreview || !it.prerelease) }.maxByOrNull { it.version.code }
