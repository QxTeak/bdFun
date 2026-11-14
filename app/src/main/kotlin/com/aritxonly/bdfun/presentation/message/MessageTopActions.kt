package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import com.aritxonly.teak.common.chrome.TeakTopGlassButton

/** 右上角操作放在 Activity 的 overlay 里：真玻璃必须在 backdrop 采集层之外，否则会 SIGSEGV。 */
@Composable
fun MessageTopActions() {
    val MessagePageBus = LocalPageChromeState.current

    TeakTopGlassButton(onClick = { MessagePageBus.requestReadAll() },
        modifier = Modifier.semantics { contentDescription = "全部已读" }) { MarkReadGlyph() }
    TeakTopGlassButton(onClick = { MessagePageBus.requestMore() },
        modifier = Modifier.semantics { contentDescription = "更多消息操作" }) { ThreeDotsGlyph() }
}
