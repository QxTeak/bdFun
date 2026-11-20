package com.aritxonly.bdfun.feature.comment.application

/**
 * 楼中楼里回复「不是楼主」的人时，在正文前面带上「回复 @某人 :」。
 *
 * 服务端只按根楼归档，正文里不写明对象的话，别人和对方都看不出来这条是在回复楼里某个人，
 * 只会像直接回复楼主。官方网页和客户端也是这么拼消息的。
 * 回复楼主（parent 就是根楼）以及没有明确对象时保持原样。
 */
internal fun threadReplyMessage(
    text: String,
    targetName: String?,
    targetRpid: Long,
    rootRpid: Long,
): String {
    if (targetRpid <= 0L || targetRpid == rootRpid) return text
    val name = targetName?.trim().orEmpty()
    if (name.isEmpty()) return text
    return " 回复 @$name : $text"
}
