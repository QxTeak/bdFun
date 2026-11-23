package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.model.comment.CommentItem

/**
 * 评论区 AI 人格账号（「大肥鱼」类）屏蔽。
 *
 * 规则移植自 ybyz2/bilibili-dafeiyu-blocker 的判定内核（MIT，见
 * docs/DAFEIYU_BLOCK_PROVENANCE.md），只取「按关键词判定」这一半：
 * 删除/拉黑接口、计数横幅、Shadow DOM 面板都不接入，判定结果只用于过滤列表数据。
 *
 * 与上游保持一致的判定顺序（命中即返回）：
 * 0 账号名含「肥鱼」 → 1 品牌词 → 2 @ 到人 → 3 单字「鱼/鲸」 → 4 全角标点/破折号/直角引号。
 * 档位把顺序切成几段：只屏蔽 @ 只看 2，小杯到 1 为止，标准到 2 为止，杀戮全跑。
 */
data class CommentAiBlockConfig(
    val enabled: Boolean = false,
    /** 强度：1 小杯 / 2 标准 / 3 杀戮 / 4 只屏蔽 @；越界按关闭处理。 */
    val strength: Int = STRENGTH_STANDARD,
    /** 楼中楼：0 只删命中的楼中楼 / 1 整楼删除 / 2 不管楼中楼。 */
    val replyMode: Int = REPLY_MODE_ONLY,
)

/** 档位取值与上游 tier 一致，方便和脚本里的设置对读。 */
const val STRENGTH_CUP = 1
const val STRENGTH_STANDARD = 2
const val STRENGTH_KILL = 3
const val STRENGTH_AT_ONLY = 4

const val REPLY_MODE_ONLY = 0
const val REPLY_MODE_WHOLE = 1
const val REPLY_MODE_NONE = 2

/**
 * 预编译好的判定器。正则只在这里构建一次，一页评论共用同一份；
 * 不要在每条评论上重建（上游为同样的原因做了三态缓存）。
 */
class CommentAiBlockPolicy(config: CommentAiBlockConfig) {
    private val strength: Int = config.strength.takeIf { config.enabled } ?: 0
    private val replyMode: Int = config.replyMode

    /** 关闭时不改任何数据，调用方拿到的就是原列表。 */
    val active: Boolean get() = strength in STRENGTH_CUP..STRENGTH_AT_ONLY

    /**
     * 过滤一页评论。置顶（含 UP 主置顶）一律保留且不进楼中楼判定，
     * 与上游一致：置顶若被误删，用户会以为整个评论区坏了。
     */
    fun filter(items: List<CommentItem>): List<CommentItem> {
        if (!active) return items
        val kept = ArrayList<CommentItem>(items.size)
        for (item in items) {
            if (item.isTop) {
                kept += item
                continue
            }
            if (isBlocked(item)) continue
            kept += keepWithReplies(item) ?: continue
        }
        return kept
    }

    fun isBlocked(item: CommentItem): Boolean {
        if (!active) return false
        if (item.uname.isNotEmpty() && NAME_WORDS.containsMatchIn(item.uname)) return true
        val full = item.message
        if (strength == STRENGTH_AT_ONLY) return hasRealMention(full)
        if (BRAND_WORDS.containsMatchIn(full)) return true
        if (strength == STRENGTH_CUP) return false
        if (hasRealMention(full)) return true
        if (strength == STRENGTH_STANDARD) return false
        if (CHAR_WORDS.containsMatchIn(full)) return true
        return KILL_PUNCT.containsMatchIn(full)
    }

    /**
     * 主楼没命中时的楼中楼处理。`only` 只摘掉命中的预览回复并把
     * 「共 N 条回复」同步减掉，避免点开是空的（上游同样会减 rcount）；
     * 返回 null 表示这一楼按 `whole` 设置整楼丢弃。
     */
    private fun keepWithReplies(item: CommentItem): CommentItem? {
        val previews = item.previewReplies
        if (previews.isEmpty() || replyMode == REPLY_MODE_NONE) return item
        val keptPreviews = previews.filterNot { isBlocked(it) }
        if (keptPreviews.size == previews.size) return item
        if (replyMode == REPLY_MODE_WHOLE) return null
        val removed = previews.size - keptPreviews.size
        return item.copy(
            replyCount = (item.replyCount - removed).coerceAtLeast(0),
            extras = item.extras.copy(previewReplies = keptPreviews),
        )
    }

    /** 「回复 @xxx ：」是 B 站自动拼的引用前缀，判 @ 前先剥掉；关键词仍看完整原文。 */
    private fun hasRealMention(text: String): Boolean {
        val body = if (text.startsWith("回复")) QUOTE_PREFIX.find(text)?.let { text.substring(it.range.last + 1) } ?: text else text
        return body.indexOf('@') >= 0 && REAL_MENTION.containsMatchIn(body)
    }

    private companion object {
        /** 简繁不做文本转换，只把字扩成字符类：`鱼` → `[鱼魚]`。必须排在使用它的正则之前。 */
        val TRADITIONAL = mapOf('鱼' to "魚", '鲸' to "鯨")

        /**
         * 只在账号名里认「肥鱼」，判断优先级最高（命中就不看档位），所以宁可窄：
         * 名字只含「鲸」的账号靠正文品牌词兜底。
         */
        val NAME_WORDS = wordsRegex("肥鱼")

        val BRAND_WORDS = wordsRegex("大肥鱼", "肥鱼", "鲸鱼", "大肥鲸", "鲸鱼娘", "本鲸", "本鱼", "本肥鱼", "主人")

        val CHAR_WORDS = wordsRegex("鱼", "鲸")

        /** 全角标点 / 破折号 / 直角引号：杀戮档的兜底特征，误伤面最大。 */
        val KILL_PUNCT = Regex("[：；「」]|[\u2014\u2015\u2500]|--")

        /** 必须真的圈到人：`12345@qq.com` 这类正文里的裸 @ 不算（上游修过的误判）。 */
        val REAL_MENTION = Regex("(?:^|[^\\w.+-])@[\\w\u4e00-\u9fa5\u3040-\u30ff\uac00-\ud7af-]")

        val QUOTE_PREFIX = Regex("^回复\\s*@[\\s\\S]{0,40}?\\s*[:：]\\s*")

        private fun wordsRegex(vararg words: String): Regex =
            Regex(words.joinToString("|") { expand(it) }, RegexOption.IGNORE_CASE)

        private fun expand(word: String): String = buildString {
            word.forEach { char -> TRADITIONAL[char]?.let { append("[$char$it]") } ?: append(char) }
        }
    }
}

/** 当前设置是否真的会过滤数据；UI 和状态容器只关心这一个问句。 */
fun CommentAiBlockConfig.isActive(): Boolean = enabled && strength in STRENGTH_CUP..STRENGTH_AT_ONLY
