package com.aritxonly.bdfun.infra.bili.update

import com.aritxonly.bdfun.core.domain.update.*
import org.json.JSONArray
import org.json.JSONObject

internal fun githubRelease(item: JSONObject): UpdateRelease? {
    if (item.optBoolean("draft")) return null
    val tag = item.optString("tag_name")
    // 历史标签（例如 preview-3.9.4）仍可阅读，但不能参与更新比较。
    val version = AppVersion.parse(tag) ?: AppVersion(tag.removePrefix("v"), -1)
    val assets = item.optJSONArray("assets") ?: JSONArray()
    val apks = (0 until assets.length()).map { assets.getJSONObject(it) }
        .filter { it.optString("name").endsWith(".apk") && !it.optString("name").contains("debug", true) }
    val apk = apks.singleOrNull()?.optString("browser_download_url")?.takeIf {
        it.startsWith("https://github.com/QxTeak/bdFun/releases/download/")
    }
    return UpdateRelease(version, item.optString("body"), item.getString("html_url"), apk,
        item.optBoolean("prerelease") || version.name.startsWith("preview-"))
}
