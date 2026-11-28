package com.aritxonly.bdfun.infra.playback

/** MPD 内容签名：同一份 manifest 不重复写盘，也用来判断是不是同一个自适应源。 */
internal fun String.playbackSignature(): String = "${length}_${hashCode()}"
