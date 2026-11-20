package com.aritxonly.bdfun.infra.bili

import android.content.Context
import android.net.Uri
import android.text.Html
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.Dispatcher
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream
import java.util.zip.InflaterInputStream
import kotlin.math.roundToInt
import com.aritxonly.bdfun.core.domain.*
import com.aritxonly.bdfun.core.model.video.*
import com.aritxonly.bdfun.core.model.comment.*
import com.aritxonly.bdfun.core.model.user.*
import com.aritxonly.bdfun.core.model.feed.*
import com.aritxonly.bdfun.core.model.message.*
import com.aritxonly.bdfun.core.model.library.*
import com.aritxonly.bdfun.infra.bili.mapper.*
import com.aritxonly.bdfun.infra.bili.BILI_API as API
import com.aritxonly.bdfun.infra.bili.BILI_APP_API as APP_API
import com.aritxonly.bdfun.infra.bili.BILI_APP_UA as APP_UA
import com.aritxonly.bdfun.infra.bili.BILI_APP as APP
import com.aritxonly.bdfun.infra.bili.BILI_APP_KEY as APP_KEY
import com.aritxonly.bdfun.infra.bili.BILI_APP_SEC as APP_SEC
import com.aritxonly.bdfun.infra.bili.BILI_TV_APP_KEY as TV_APP_KEY
import com.aritxonly.bdfun.infra.bili.BILI_TV_APP_SEC as TV_APP_SEC
import com.aritxonly.bdfun.infra.bili.BILI_APP_HEADERS as APP_HEADERS
import com.aritxonly.bdfun.infra.bili.BILI_PASSPORT as PASSPORT
import com.aritxonly.bdfun.infra.bili.BILI_VC as VC
import com.aritxonly.bdfun.infra.bili.BILI_UA as UA
import com.aritxonly.bdfun.infra.bili.encodeBiliParameter as encode
import com.aritxonly.bdfun.infra.bili.biliMd5 as md5
import com.aritxonly.bdfun.infra.bili.biliImage as image
import com.aritxonly.bdfun.infra.bili.biliAvatar as avatar
import com.aritxonly.bdfun.infra.bili.cleanBiliHtml as cleanHtml
import com.aritxonly.bdfun.infra.bili.biliAidToBvid as aidToBvid
import com.aritxonly.bdfun.infra.bili.parseBiliCountText as parseCountText
import com.aritxonly.bdfun.infra.bili.parseBiliDurationText as parseDurationText
import com.aritxonly.bdfun.infra.bili.biliQualityLabel as qualityLabel

internal suspend fun BiliApiContext.comments(
    aid: Long,
    page: Int,
    upMid: Long = 0,
    sort: Int = 2,
): CommentPage = commentsByOid(aid.toString(), 1, page, upMid, sort)

/**
 * 通用评论区：视频用 type=1 + aid，动态用 type=17 + id_str。
 * 两条路只是 oid/type 不同，解析和分页完全一致。
 */

internal suspend fun BiliApiContext.commentsByOid(
    oid: String,
    type: Int,
    page: Int,
    upMid: Long = 0,
    sort: Int = 2,
): CommentPage = withContext(Dispatchers.IO) {
    val data = dataRequest(
        "/x/v2/reply",
        mapOf(
            "type" to type.toString(),
            "oid" to oid,
            "pn" to page.toString(),
            "ps" to "20",
            "sort" to sort.toString(),
        ),
    )
    mapCommentPage(data, page, upMid)
}

internal suspend fun BiliApiContext.replies(aid: Long, root: Long, page: Int = 1, upMid: Long = 0): CommentPage =
    repliesByOid(aid.toString(), 1, root, page, upMid)

internal suspend fun BiliApiContext.repliesByOid(
    oid: String,
    type: Int,
    root: Long,
    page: Int = 1,
    upMid: Long = 0,
    sortTime: Boolean = false,
): CommentPage = withContext(Dispatchers.IO) {
    val data = dataRequest(
        "/x/v2/reply/reply",
        mapOf(
            "oid" to oid,
            "type" to type.toString(),
            "root" to root.toString(),
            "pn" to page.toString(),
            "ps" to "20",
            "nohot" to if (sortTime) "1" else "0",
        ),
    )
    mapCommentPage(data, page, upMid, includeTop = false)
}

internal suspend fun BiliApiContext.addComment(
    aid: Long,
    message: String,
    root: Long = 0,
    parent: Long = 0,
    pictures: String? = null,
    atNameToMid: Map<String, Long> = emptyMap(),
): Long? = addCommentByOid(aid.toString(), 1, message, root, parent, pictures, atNameToMid)

internal suspend fun BiliApiContext.addCommentByOid(
    oid: String,
    type: Int,
    message: String,
    root: Long = 0,
    parent: Long = 0,
    pictures: String? = null,
    atNameToMid: Map<String, Long> = emptyMap(),
): Long? = withContext(Dispatchers.IO) {
    val params = mutableMapOf(
        "oid" to oid,
        "type" to type.toString(),
        "message" to message,
        "plat" to "1",
        "csrf" to csrf,
    )
    if (root > 0) params["root"] = root.toString()
    if (parent > 0) params["parent"] = parent.toString()
    if (!pictures.isNullOrBlank()) params["pictures"] = pictures
    // 回复楼里的某个人时，让正文里的「@某人」变成真正的提及：对方会收到 @ 通知，
    // 客户端里这段文字也能点进对方主页。官方网页和 PiliPlus 都带这个字段。
    if (atNameToMid.isNotEmpty()) {
        params["at_name_to_mid"] = JSONObject(atNameToMid.filterValues { it > 0L }).toString()
    }
    val data = postRequest("/x/v2/reply/add", params, tolerateEmptyBody = false)
    data.l("rpid").takeIf { it > 0 }
        ?: data.optJSONObject("reply")?.l("rpid")?.takeIf { it > 0 }
        ?: 0L
}

/** 上传评论图片，返回可直接塞进 `pictures` 的 JSON；失败返回 null。 */

internal suspend fun BiliApiContext.uploadCommentImage(uri: android.net.Uri): String? = withContext(Dispatchers.IO) {
    cancellableCatching {
        val resolver = appContext.contentResolver
        val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return@cancellableCatching null
        val mime = resolver.getType(uri) ?: "image/jpeg"
        val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        val width = bitmap?.width ?: 0
        val height = bitmap?.height ?: 0
        val body = okhttp3.MultipartBody.Builder()
            .setType(okhttp3.MultipartBody.FORM)
            .addFormDataPart(
                "file",
                "image",
                bytes.toRequestBody(mime.toMediaTypeOrNull()),
            )
            .addFormDataPart("csrf", csrf)
            .build()
        val request = Request.Builder()
            .url("$API/x/dynamic/feed/dyn_img/upload")
            .header("User-Agent", UA)
            .header("Referer", "https://www.bilibili.com/")
            .header("Origin", "https://www.bilibili.com")
            .post(body)
            .build()
        val json = http.await(request).use { response ->
            val text = response.body?.string().orEmpty()
            if (text.isBlank()) return@use null
            JSONObject(text)
        } ?: return@cancellableCatching null
        if (json.optInt("code", 0) != 0) return@cancellableCatching null
        val data = json.optJSONObject("data") ?: return@cancellableCatching null
        val url = data.s("image_url").ifBlank { data.s("url") }
        if (url.isBlank()) return@cancellableCatching null
        JSONObject().apply {
            put("img_src", url)
            put("img_width", width)
            put("img_height", height)
        }.toString()
    }.getOrNull()
}

/** 发送弹幕（videoup 的普通滚动弹幕）。 */

internal suspend fun BiliApiContext.likeComment(aid: Long, rpid: Long, like: Boolean): Boolean =
    likeCommentByOid(aid.toString(), 1, rpid, like)

internal suspend fun BiliApiContext.likeCommentByOid(
    oid: String,
    type: Int,
    rpid: Long,
    like: Boolean,
): Boolean = withContext(Dispatchers.IO) {
    postRequest(
        "/x/v2/reply/action",
        mapOf(
            "oid" to oid,
            "type" to type.toString(),
            "rpid" to rpid.toString(),
            "action" to if (like) "1" else "0",
            "csrf" to csrf,
        ),
    )
    true
}

internal suspend fun BiliApiContext.reportComment(aid: Long, rpid: Long, reason: Int = 1): Boolean =
    reportCommentByOid(aid.toString(), 1, rpid, reason)

internal suspend fun BiliApiContext.reportCommentByOid(
    oid: String,
    type: Int,
    rpid: Long,
    reason: Int = 1,
): Boolean = withContext(Dispatchers.IO) {
    postRequest(
        "/x/v2/reply/report",
        mapOf(
            "oid" to oid,
            "type" to type.toString(),
            "rpid" to rpid.toString(),
            "reason" to reason.toString(),
            "csrf" to csrf,
        ),
    )
    true
}

/** 删除自己发出的评论。 */

internal suspend fun BiliApiContext.deleteComment(aid: Long, rpid: Long): Boolean =
    deleteCommentByOid(aid.toString(), 1, rpid)

internal suspend fun BiliApiContext.deleteCommentByOid(oid: String, type: Int, rpid: Long): Boolean =
    withContext(Dispatchers.IO) {
    postRequest(
        "/x/v2/reply/del",
        mapOf(
            "oid" to oid,
            "type" to type.toString(),
            "rpid" to rpid.toString(),
            "csrf" to csrf,
        ),
    )
    true
}

/** 拉黑（加入黑名单）。 */

internal suspend fun BiliApiContext.blockUser(mid: Long): Boolean = withContext(Dispatchers.IO) {
    postRequest(
        "/x/relation/modify",
        mapOf(
            "fid" to mid.toString(),
            "act" to "5",
            "re_src" to "11",
            "csrf" to csrf,
        ),
    )
    true
}

/** 评论表情面板（需要登录；失败时返回空列表，界面回退到系统 emoji）。 */

internal suspend fun BiliApiContext.replyEmotes(): List<EmoteItem> = withContext(Dispatchers.IO) {
    cancellableCatching {
        val data = dataRequest(
            "/x/emote/user/panel/web",
            mapOf("business" to "reply"),
        )
        mapReplyEmotes(data)
    }.getOrDefault(emptyList())
}
