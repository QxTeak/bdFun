package com.aritxonly.bdfun.infra.bili

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val liveSendBase = "https://api.live.bilibili.com"

/**
 * 直播弹幕发送（和 PiliPlus 同一套网页端参数）。
 *
 * 表单里除消息本身，还必须带 csrf / csrf_token 和 web_location；接口按 WBI 校验。
 * 失败把服务端 message 原样抛出，页面只负责 toast，不再猜含义。
 */
internal suspend fun BiliApiContext.sendLiveDanmaku(roomId: Long, text: String): Unit =
    withContext(Dispatchers.IO) {
        if (!isLoggedIn) throw BiliException(-101, "请先登录")
        val token = csrf
        if (token.isBlank()) throw BiliException(-101, "登录状态已失效，请重新登录")
        val nowSec = System.currentTimeMillis() / 1000L
        val root = rawRequest(
            "/msg/send",
            mapOf(
                "bubble" to "0",
                "msg" to text,
                "color" to "16777215",
                "mode" to "1",
                "room_type" to "0",
                "jumpfrom" to "0",
                "reply_mid" to "0",
                "reply_attr" to "0",
                "replay_dmid" to "",
                "statistics" to "{\"appId\":100,\"platform\":5}",
                "reply_type" to "0",
                "reply_uname" to "",
                "fontsize" to "25",
                "rnd" to nowSec.toString(),
                "roomid" to roomId.toString(),
                "csrf" to token,
                "csrf_token" to token,
                "web_location" to "444.8",
            ),
            signed = true,
            base = liveSendBase,
            method = "POST",
            referer = "https://live.bilibili.com/$roomId",
            tolerateEmptyBody = false,
        )
        val code = root.optInt("code", 0)
        if (code != 0) throw BiliException(code, apiMessage(code, root.optString("message")))
    }
