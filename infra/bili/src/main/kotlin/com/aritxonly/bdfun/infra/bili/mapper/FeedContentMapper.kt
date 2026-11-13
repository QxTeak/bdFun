package com.aritxonly.bdfun.infra.bili.mapper

import org.json.JSONObject
import com.aritxonly.bdfun.core.model.feed.*
import com.aritxonly.bdfun.core.model.video.*
import com.aritxonly.bdfun.infra.bili.biliImage as image
import com.aritxonly.bdfun.infra.bili.biliAvatar as avatar
import com.aritxonly.bdfun.infra.bili.parseBiliCountText as parseCountText
import com.aritxonly.bdfun.infra.bili.parseBiliDurationText as parseDurationText
import com.aritxonly.bdfun.infra.bili.cancellableCatching

internal data class FeedContent(val title: String, val text: String, val images: List<String>)

internal fun parseFeedContent(major: FeedMajor, desc: String): FeedContent {
    val opus = major.opus
    val article = major.article
    val common = major.common
    val pgc = major.pgc
    val live = major.live
    val medialist = major.medialist
    val courses = major.courses
    val music = major.music
    // 图文 opus：feed 在 itemOpusStyle 下把标题 / 正文放在 major.opus 里，
    // 只读 module_dynamic.desc 会出现“很多内容显示不出来”。
    val opusSummary = opus?.optJSONObject("summary")?.s("text").orEmpty()
    val title = opus?.s("title").orEmpty()
        .ifBlank { article?.s("title").orEmpty() }
        .ifBlank { common?.s("title").orEmpty() }
        .ifBlank { live?.s("title").orEmpty() }
        .ifBlank { medialist?.s("title").orEmpty() }
        .ifBlank { courses?.s("title").orEmpty() }
        .ifBlank { music?.s("title").orEmpty() }
    val text = desc
        .ifBlank { opusSummary }
        .ifBlank { article?.s("desc").orEmpty() }
        .ifBlank { common?.s("desc").orEmpty() }
        .ifBlank { common?.s("summary").orEmpty() }
        .ifBlank { pgc?.s("desc").orEmpty() }
        .ifBlank { medialist?.s("desc").orEmpty() }
        .ifBlank { music?.s("desc").orEmpty() }
    val images = buildList {
        major.value?.optJSONObject("draw")?.optJSONArray("items")?.objects()?.forEach {
            it.s("src").takeIf(String::isNotBlank)?.let { src -> add(image(src, 720)) }
        }
        opus?.optJSONArray("pics")?.objects()?.forEach {
            it.s("url").takeIf(String::isNotBlank)?.let { url -> add(image(url, 720)) }
        }
        article?.optJSONArray("covers")?.let { covers ->
            for (i in 0 until covers.length()) {
                val value = covers.opt(i)
                val url = when (value) {
                    is String -> value
                    is JSONObject -> value.s("url")
                    else -> ""
                }
                if (url.isNotBlank()) add(image(url, 720))
            }
        }
        common?.s("cover").orEmpty().takeIf { it.isNotBlank() }?.let { add(image(it, 720)) }
        live?.s("cover").orEmpty().takeIf { it.isNotBlank() }?.let { add(image(it, 720)) }
        medialist?.s("cover").orEmpty().takeIf { it.isNotBlank() }?.let { add(image(it, 720)) }
        courses?.s("cover").orEmpty().takeIf { it.isNotBlank() }?.let { add(image(it, 720)) }
        music?.s("cover").orEmpty().takeIf { it.isNotBlank() }?.let { add(image(it, 720)) }
    }.filter { it.isNotBlank() }.distinct()
    return FeedContent(title, text, images)
}
