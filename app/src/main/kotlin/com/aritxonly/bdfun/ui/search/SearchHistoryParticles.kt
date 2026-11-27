package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.aritxonly.teak.common.motion.TeakParticleFlow
import top.yukonga.miuix.kmp.theme.Colors

/**
 * 搜索历史胶囊组：删除时由 [TeakParticleFlow] 在原位置炸出粒子，其余胶囊滑到新位置。
 *
 * 取位、爆炸、重排这些通用逻辑已经下沉到 TeakUI（`TeakParticleFlow`）；
 * 这里只留业务接线——每一项长什么样（[HistoryPill]），以及按被删胶囊取色。
 */
@Composable
internal fun HistoryPillsFlow(
    history: List<String>,
    colors: Colors,
    editing: Boolean,
    selected: Set<String>,
    onPickKeyword: (String) -> Unit,
    onToggle: ((String) -> Unit)?,
    onBeginEdit: (() -> Unit)?,
    roomy: Boolean,
) {
    TeakParticleFlow(
        items = history,
        itemKey = { it },
        // 只取胶囊自己的底色（HistoryPill 的 surfaceContainerHigh）。不要混 primary
        // 或 onBackground：灰色胶囊炸出红/黑碎屑会和"从胶囊上掉下来"对不上。
        burstColor = { colors.surfaceContainerHigh },
        horizontalSpacing = if (roomy) 10.dp else 8.dp,
        verticalSpacing = if (roomy) 12.dp else 8.dp,
    ) { word ->
        HistoryPill(
            word = word,
            onClick = {
                if (editing) onToggle?.invoke(word) else onPickKeyword(word)
            },
            roomy = roomy,
            onLongClick = if (editing) null else onBeginEdit,
            selected = editing && word in selected,
            showCheck = editing,
        )
    }
}
