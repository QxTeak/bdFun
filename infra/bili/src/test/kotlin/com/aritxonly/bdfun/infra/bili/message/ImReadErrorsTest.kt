package com.aritxonly.bdfun.infra.bili.message

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.Base64

/** 错误载荷为依据固定上游描述符构造的 wire 样本，不冒充登录响应。 */
class ImReadErrorsTest {
    @Test fun settingsReadsIncludePinnedUpstreamClientAndCsrfParameters() {
        val params = imChatReadParams("test-csrf", mapOf("talker_uid" to "123"))
        assertEquals(mapOf("talker_uid" to "123", "build" to "0", "mobi_app" to "web",
            "csrf" to "test-csrf", "csrf_token" to "test-csrf"), params)
    }

    @Test fun failureIdentifiesStageButCancellationPropagates() = runBlocking {
        try { imChatReadStage("聊天偏好") { error("1217") }; fail("应保留失败") }
        catch (error: IllegalStateException) { assertEquals("聊天偏好加载失败：1217", error.message) }
        val cancel = CancellationException("离开页面")
        try { imChatReadStage("置顶设置") { throw cancel }; fail("应传播取消") }
        catch (error: CancellationException) { assertSame(cancel, error) }
    }

    @Test fun percentEncodedMessageKeepsLiteralPlusAndFallbackStatus() {
        assertEquals("参数+错误", imGrpcFailureText("3", "%E5%8F%82%E6%95%B0+%E9%94%99%E8%AF%AF", null))
        assertEquals("消息服务失败 (13)", imGrpcFailureText("13", null, "not base64!"))
    }

    @Test fun statusDetailsExposeBusinessReasonInsteadOfOnly1217() {
        fun field(number: Int, value: ByteArray): ByteArray = ByteArrayOutputStream().apply {
            varint((number shl 3 or 2).toLong()); varint(value.size.toLong()); write(value)
        }.toByteArray()
        val nested = ByteArrayOutputStream().apply {
            varint(8); varint(1217); write(field(2, "测试错误详情".toByteArray()))
        }.toByteArray()
        val any = field(2, nested)
        val status = byteArrayOf(8, 3) + field(2, "1217".toByteArray()) + field(3, any)
        assertEquals("测试错误详情 (1217)", imGrpcFailureText("3", "1217", Base64.getEncoder().encodeToString(status)))
    }
}
