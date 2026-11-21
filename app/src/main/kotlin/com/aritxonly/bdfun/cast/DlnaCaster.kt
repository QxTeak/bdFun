package com.aritxonly.bdfun.cast

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Xml
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.URI
import java.util.concurrent.TimeUnit

private const val SSDP_HOST = "239.255.255.250"
private const val SSDP_PORT = 1900
private const val SSDP_SEARCH_TARGET = "urn:schemas-upnp-org:device:MediaRenderer:1"
private const val DEFAULT_AVT_SERVICE = "urn:schemas-upnp-org:service:AVTransport:1"
private const val SEARCH_DURATION_MS = 8_000L
private const val SEARCH_RESEND_MS = 2_500L
private const val RECEIVE_TIMEOUT_MS = 500

/** 局域网里发现的一台 MediaRenderer。address 只用于展示，controlUrl 才是 SOAP 目标。 */
data class CastDevice(
    val friendlyName: String,
    val address: String,
    val controlUrl: String,
    val serviceType: String,
)

/** 网络或设备侧的错误；message 已经是可直接展示的中文文案。 */
class DlnaException(message: String, cause: Throwable? = null) : Exception(message, cause)

private val UPNP_ERROR_DESCRIPTION = Regex("<errorDescription>([^<]+)</errorDescription>")
private val UPNP_ERROR_CODE = Regex("<errorCode>(\\d+)</errorCode>")

/**
 * 最小 SSDP + UPnP AVTransport 实现，避免为一次投屏引入整套 DLNA 依赖。
 *
 * 发现流程：组播 M-SEARCH 到 239.255.255.250:1900，设备用单播回 LOCATION，
 * 再抓取 description XML 找 AVTransport 的 controlURL。组播锁必须持有，
 * 否则部分机型会在息屏或省电策略下丢弃 SSDP 报文。
 */
class DlnaCaster(context: Context) {
    private val appContext = context.applicationContext
    private val http = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .callTimeout(8, TimeUnit.SECONDS)
        .build()

    @Volatile
    private var socket: DatagramSocket? = null
    private var multicastLock: WifiManager.MulticastLock? = null
    private var lockHolders = 0

    /**
     * 搜索设备并把结果逐个交给 [onDevice]。回调发生在 IO 线程，UI 侧自行切主线程。
     * 协程取消、[stop] 或 [close] 都会尽快结束阻塞接收。
     */
    suspend fun discover(onDevice: suspend (CastDevice) -> Unit) = withContext(Dispatchers.IO) {
        acquireMulticastLock()
        val searchSocket = DatagramSocket()
        searchSocket.soTimeout = RECEIVE_TIMEOUT_MS
        socket = searchSocket
        try {
            val target = java.net.InetSocketAddress(InetAddress.getByName(SSDP_HOST), SSDP_PORT)
            val payload = buildSearchMessage().toByteArray(Charsets.UTF_8)
            val buffer = ByteArray(8192)
            val seen = HashSet<String>()
            val deadline = System.currentTimeMillis() + SEARCH_DURATION_MS
            var nextSendAt = 0L
            while (currentCoroutineContext().isActive && System.currentTimeMillis() < deadline) {
                val now = System.currentTimeMillis()
                if (now >= nextSendAt) {
                    searchSocket.send(DatagramPacket(payload, payload.size, target))
                    nextSendAt = now + SEARCH_RESEND_MS
                }
                val packet = DatagramPacket(buffer, buffer.size)
                try {
                    searchSocket.receive(packet)
                } catch (_: SocketTimeoutException) {
                    continue
                } catch (_: SocketException) {
                    // stop() 关闭 socket 后 receive 会抛 SocketException，按取消处理。
                    break
                }
                val location = parseLocation(String(packet.data, 0, packet.length, Charsets.UTF_8))
                    ?: continue
                if (!seen.add(location)) continue
                val device = runCatching { resolveDevice(location) }.getOrNull() ?: continue
                onDevice(device)
            }
        } finally {
            runCatching { searchSocket.close() }
            if (socket === searchSocket) socket = null
            releaseMulticastLock()
        }
    }

    /** SetAVTransportURI + Play；失败时 Result 里是中文原因。 */
    suspend fun cast(device: CastDevice, mediaUrl: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (mediaUrl.isBlank()) throw DlnaException("当前没有可投屏的视频")
            executeAction(
                device, "SetAVTransportURI",
                "<CurrentURI>${escapeXml(mediaUrl)}</CurrentURI><CurrentURIMetaData></CurrentURIMetaData>",
            )
            executeAction(device, "Play", "<Speed>1</Speed>")
        }.recoverCatching { error ->
            if (error is CancellationException) throw error
            if (error is DlnaException) throw error
            throw DlnaException("连接投屏设备失败，请检查电视与手机是否在同一 Wi-Fi", error)
        }
    }

    /** 立即结束正在进行的搜索（关闭 socket 会唤醒阻塞中的 receive）。 */
    fun stop() {
        val current = socket
        socket = null
        runCatching { current?.close() }
    }

    /** 页面退出时调用，释放搜索与 HTTP 资源。 */
    fun close() {
        stop()
        runCatching { http.dispatcher.executorService.shutdown() }
        runCatching { http.connectionPool.evictAll() }
    }

    private fun buildSearchMessage(): String = buildString {
        append("M-SEARCH * HTTP/1.1\r\n")
        append("HOST: $SSDP_HOST:$SSDP_PORT\r\n")
        append("MAN: \"ssdp:discover\"\r\n")
        append("MX: 2\r\n")
        append("ST: $SSDP_SEARCH_TARGET\r\n")
        append("\r\n")
    }

    private fun parseLocation(response: String): String? = response.lineSequence()
        .firstOrNull { it.startsWith("LOCATION:", ignoreCase = true) }
        ?.substringAfter(':')
        ?.trim()
        ?.takeIf { it.isNotEmpty() }

    private fun resolveDevice(location: String): CastDevice? {
        val locationUri = runCatching { URI(location) }.getOrNull() ?: return null
        when (locationUri.scheme?.lowercase()) {
            "http", "https" -> Unit
            else -> return null
        }
        val description = parseDescription(fetchText(location)) ?: return null
        val base = description.urlBase
            ?.takeIf { it.isNotBlank() }
            ?.let { runCatching { URI(it) }.getOrNull() }
            ?: locationUri
        val controlUri = runCatching { base.resolve(description.controlUrl) }.getOrNull() ?: return null
        val address = controlUri.host ?: locationUri.host ?: return null
        return CastDevice(
            friendlyName = description.friendlyName?.takeIf { it.isNotBlank() } ?: address,
            address = address,
            controlUrl = controlUri.toString(),
            serviceType = description.serviceType ?: DEFAULT_AVT_SERVICE,
        )
    }

    private fun fetchText(url: String): String {
        val request = Request.Builder().url(url).build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw DlnaException("读取设备信息失败（HTTP ${response.code}）")
            return response.body?.string() ?: throw DlnaException("设备信息为空")
        }
    }

    private fun parseDescription(xml: String): Description? {
        val parser = Xml.newPullParser()
        runCatching { parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false) }
        parser.setInput(StringReader(xml))
        var friendlyName: String? = null
        var urlBase: String? = null
        var avServiceType: String? = null
        var avControlUrl: String? = null
        var serviceType: String? = null
        var controlUrl: String? = null
        var inService = false
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            val tag = parser.name?.substringAfter(':').orEmpty()
            when (parser.eventType) {
                XmlPullParser.START_TAG -> when (tag) {
                    "friendlyName" -> if (!inService) friendlyName = readText(parser)
                    "URLBase" -> if (!inService) urlBase = readText(parser)
                    "service" -> {
                        inService = true
                        serviceType = null
                        controlUrl = null
                    }
                    "serviceType" -> if (inService) serviceType = readText(parser)
                    "controlURL" -> if (inService) controlUrl = readText(parser)
                }
                XmlPullParser.END_TAG -> if (tag == "service") {
                    val type = serviceType
                    val control = controlUrl
                    if (type != null && control != null && type.contains("AVTransport", ignoreCase = true)) {
                        avServiceType = type
                        avControlUrl = control
                    }
                    inService = false
                }
            }
            parser.next()
        }
        val control = avControlUrl?.takeIf { it.isNotBlank() } ?: return null
        return Description(friendlyName, urlBase, control, avServiceType)
    }

    private fun readText(parser: XmlPullParser): String? =
        runCatching { parser.nextText().trim() }.getOrNull()?.takeIf { it.isNotEmpty() }

    private fun executeAction(device: CastDevice, action: String, arguments: String) {
        val envelope = buildString {
            append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n")
            append("<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" ")
            append("s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">")
            append("<s:Body><u:$action xmlns:u=\"${device.serviceType}\">")
            append("<InstanceID>0</InstanceID>")
            append(arguments)
            append("</u:$action></s:Body></s:Envelope>")
        }
        val request = Request.Builder()
            .url(device.controlUrl)
            .header("Content-Type", "text/xml; charset=\"utf-8\"")
            .header("SOAPACTION", "\"${device.serviceType}#$action\"")
            .post(envelope.toRequestBody(XML_MEDIA_TYPE))
            .build()
        http.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw DlnaException(upnpErrorMessage(text) ?: "投屏设备拒绝请求（HTTP ${response.code}）")
            }
        }
    }

    private fun upnpErrorMessage(body: String): String? {
        val description = UPNP_ERROR_DESCRIPTION.find(body)?.groupValues?.getOrNull(1)?.trim()
        val code = UPNP_ERROR_CODE.find(body)?.groupValues?.getOrNull(1)
        return when {
            !description.isNullOrEmpty() -> "投屏设备返回错误：$description"
            code != null -> "投屏设备返回错误（$code）"
            else -> null
        }
    }

    private fun escapeXml(value: String): String = buildString(value.length) {
        value.forEach { char ->
            when (char) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&apos;")
                else -> append(char)
            }
        }
    }

    @Synchronized
    private fun acquireMulticastLock() {
        lockHolders++
        if (lockHolders > 1) return
        val wifi = appContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return
        multicastLock = runCatching {
            wifi.createMulticastLock("bdFun-dlna").apply {
                setReferenceCounted(false)
                acquire()
            }
        }.getOrNull()
    }

    @Synchronized
    private fun releaseMulticastLock() {
        lockHolders = (lockHolders - 1).coerceAtLeast(0)
        if (lockHolders > 0) return
        multicastLock?.let { lock -> runCatching { if (lock.isHeld) lock.release() } }
        multicastLock = null
    }

    private data class Description(
        val friendlyName: String?,
        val urlBase: String?,
        val controlUrl: String,
        val serviceType: String?,
    )

    private companion object {
        val XML_MEDIA_TYPE = "text/xml; charset=utf-8".toMediaType()
    }
}