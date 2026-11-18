package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
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
    TeakTopGlassButton(
        onClick = { if (MessagePageBus.moreAnchor != Rect.Zero) MessagePageBus.requestMore() },
        modifier = Modifier
            .onGloballyPositioned { MessagePageBus.moreAnchor = it.boundsInWindow() }
            .semantics { contentDescription = "更多消息操作" },
    ) { ThreeDotsGlyph() }
}


@Composable
internal fun MessageSettingsMenu(onSettings: () -> Unit) {
    var anchor by remember { mutableStateOf(Rect.Zero) }
    var expanded by remember { mutableStateOf(false) }
    TeakTopGlassButton(
        onClick = { expanded = anchor != Rect.Zero },
        modifier = Modifier
            .onGloballyPositioned { anchor = it.boundsInWindow() }
            .semantics { contentDescription = "更多设置" },
    ) { ThreeDotsGlyph() }
    TeakGlassMenu(visible = expanded, onDismiss = { expanded = false }, anchorInWindow = anchor) {
        ActionSheetRow("设置") {
            expanded = false
            onSettings()
        }
    }
}
