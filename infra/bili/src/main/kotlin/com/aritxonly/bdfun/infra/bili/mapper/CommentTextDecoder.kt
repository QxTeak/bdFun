package com.aritxonly.bdfun.infra.bili.mapper

private val commentEntity = Regex("&(#(?:[xX][0-9a-fA-F]{1,8}|[0-9]{1,10})|[A-Za-z][A-Za-z0-9]{1,31});")

/** 仅解码一层，保留未知/损坏实体及原始空白，避免把用户正文当作 HTML 文档。 */
internal fun decodeCommentText(text: String): String {
    if ('&' !in text) return text
    return commentEntity.replace(text) { match ->
        val entity = match.groupValues[1]
        val codePoint = when {
            entity.startsWith("#x", ignoreCase = true) -> entity.drop(2).toLongOrNull(16)
            entity.startsWith("#") -> entity.drop(1).toLongOrNull()
            else -> commentHtmlEntities[entity]?.toLong()
        }
        if (codePoint == null || codePoint !in 1L..0x10FFFFL || codePoint in 0xD800L..0xDFFFL) {
            match.value
        } else {
            String(Character.toChars(codePoint.toInt()))
        }
    }
}
