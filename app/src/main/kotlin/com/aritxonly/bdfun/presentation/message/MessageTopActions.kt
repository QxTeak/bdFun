package com.aritxonly.bdfun.ui

import top.yukonga.miuix.kmp.glass.GlassPopupItem

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.core.navigation.Route
import com.aritxonly.teak.common.chrome.teakGlassGlyphColor
import com.aritxonly.teak.common.icons.AppGlyph
import com.aritxonly.teak.common.navigation.TeakMorphMenuAnchor
import com.aritxonly.teak.common.navigation.TeakMorphMenuHost
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ContactsBook
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import com.aritxonly.teak.common.chrome.TeakTopGlassButton

/** 右上角操作放在 Activity 的 overlay 里：真玻璃必须在 backdrop 采集层之外，否则会 SIGSEGV。 */
@Composable
fun MessageTopActions(showContacts: Boolean = false) {
    val MessagePageBus = LocalPageChromeState.current

    if (showContacts) {
        val navigate = messageRouteAction()
        TeakTopGlassButton(
            onClick = { navigate(Route.MessageTools("contacts")) },
            modifier = Modifier.semantics { contentDescription = "联系人" },
        ) { AppGlyph(MiuixIcons.ContactsBook, teakGlassGlyphColor(), size = 21.dp) }
    }
    TeakTopGlassButton(onClick = { MessagePageBus.requestReadAll() },
        modifier = Modifier.semantics { contentDescription = "全部已读" }) { MarkReadGlyph() }
    // 省略号改成收藏页同款的玻璃形变菜单：按钮矩形被吸进玻璃，菜单从按钮位置长出来。
    TeakMorphMenuAnchor(MessagePageBus.moreMenu) { onClick ->
        TeakTopGlassButton(
            onClick = onClick,
            modifier = Modifier.semantics { contentDescription = "更多消息操作" },
        ) { ThreeDotsGlyph() }
    }
}

/**
 * 消息页“更多”的形变菜单宿主，必须放在 Activity 的 menuOverlay 槽里。
 *
 * 页面内容在 backdrop 采集树内，宿主必须在采集树外才能安全采样玻璃；菜单内容由页面
 * 通过 [PageChromePresentationState.moreMenuContent] 登记，撤场动画期间也一直留在组合里。
 */
@Composable
internal fun MessageInboxMorphMenuHost() {
    val chrome = LocalPageChromeState.current
    TeakMorphMenuHost(chrome.moreMenu) { dismiss ->
        val content = chrome.moreMenuContent
        if (content != null) content(dismiss)
    }
}


@Composable
internal fun MessageSettingsMenu(onSettings: () -> Unit) {
    MessageOverflowMenu { _, dismiss ->
        GlassPopupItem(text = "设置", onClick = { dismiss(); onSettings() })
    }
}

@Composable
internal fun MessageOverflowMenu(content: @Composable ColumnScope.(Boolean, () -> Unit) -> Unit) {
    var anchor by remember { mutableStateOf(Rect.Zero) }
    var expanded by remember { mutableStateOf(false) }
    val dismiss = { expanded = false }
    TeakTopGlassButton(
        onClick = { expanded = anchor != Rect.Zero },
        modifier = Modifier
            .onGloballyPositioned { anchor = it.boundsInWindow() }
            .semantics { contentDescription = "更多设置" },
    ) { ThreeDotsGlyph() }
    TeakGlassMenu(visible = expanded, onDismiss = dismiss, anchorInWindow = anchor) {
        content(expanded, dismiss)
    }
}
