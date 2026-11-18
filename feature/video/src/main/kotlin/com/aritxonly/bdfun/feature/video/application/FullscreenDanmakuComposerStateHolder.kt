package com.aritxonly.bdfun.feature.video.application

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FullscreenDanmakuComposerState(
    val text: String = "", val sending: Boolean = false,
    val mode: Int = 1, val fontSize: Int = 25, val color: Int = 0xFFFFFF,
    val appearance: com.aritxonly.bdfun.core.domain.DanmakuSendAppearance = com.aritxonly.bdfun.core.domain.DanmakuSendAppearance(),
)

/** 全屏输入无论请求成功与否都收起，保留与半屏输入不同的原有规则。 */
class FullscreenDanmakuComposerStateHolder {
    private val mutable = MutableStateFlow(FullscreenDanmakuComposerState())
    val state = mutable.asStateFlow()
    private var closed = false
    private var active: Any? = null

    fun edit(text: String) = change { it.copy(text = text) }
    fun setMode(mode: Int) = change { it.copy(mode = mode) }
    fun setFontSize(size: Int) = change { it.copy(fontSize = size) }
    fun setColor(color: Int) = change { it.copy(color = color, appearance = it.appearance.copy(vipColor = false)) }
    fun toggleVipColor() = change { it.copy(appearance = it.appearance.copy(vipColor = !it.appearance.vipColor)) }
    fun toggleUpIdentity() = change { it.copy(appearance = it.appearance.copy(upIdentity = !it.appearance.upIdentity)) }
    fun close() { closed = true }
    private fun change(transform: (FullscreenDanmakuComposerState) -> FullscreenDanmakuComposerState) {
        if (!closed) mutable.update(transform)
    }

    fun submit(
        sender: (suspend (String, Int, Int, Int) -> Boolean)?,
        scope: CoroutineScope, onMissingSender: () -> Unit, afterSend: () -> Unit,
    ) {
        val styled = sender?.let { original ->
            val adapter: suspend (String, Int, Int, Int, com.aritxonly.bdfun.core.domain.DanmakuSendAppearance) -> Boolean =
                { text, mode, size, color, _ -> original(text, mode, size, color) }
            adapter
        }
        submitStyled(styled, scope, onMissingSender, afterSend)
    }

    fun submitStyled(
        sender: (suspend (String, Int, Int, Int, com.aritxonly.bdfun.core.domain.DanmakuSendAppearance) -> Boolean)?,
        scope: CoroutineScope, onMissingSender: () -> Unit, afterSend: () -> Unit,
    ) {
        val value = state.value.text.trim()
        if (closed || value.isEmpty() || state.value.sending) return
        if (sender == null) { onMissingSender(); return }
        change { it.copy(sending = true) }
        val request = Any().also { active = it }
        scope.launch {
            try {
                if (closed) return@launch
                // 文本在点击时读取；样式仍在协程开始时读取，不能合并成同一快照。
                val style = state.value
                try { sender(value, style.mode, style.fontSize, style.color, style.appearance) }
                catch (cancel: CancellationException) { throw cancel }
                catch (_: Exception) { /* 原输入窗失败后同样收起。 */ }
                currentCoroutineContext().ensureActive()
                change { it.copy(sending = false) }
                if (!closed) afterSend()
            } finally {
                finish(request)
            }
        }.invokeOnCompletion { finish(request) }
    }

    private fun finish(request: Any) {
        if (active === request) {
            active = null
            change { it.copy(sending = false) }
        }
    }
}
