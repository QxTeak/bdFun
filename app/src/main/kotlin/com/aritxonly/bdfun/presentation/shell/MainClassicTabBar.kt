package com.aritxonly.bdfun


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.aritxonly.teak.common.chrome.teakClassicBarGlass
import com.aritxonly.teak.common.navigation.TeakFloatingTabItem

/**
 * 经典界面风格的底栏：没有悬浮胶囊、没有玻璃，整条贴着屏幕底边，
 * 顶部一条分割线把它和内容分开。默认的官方风格完全不会走到这里。
 */
@Composable
internal fun ClassicTabBar(
    items: List<TeakFloatingTabItem>,
    selectedKey: String,
    onItemSelected: (TeakFloatingTabItem) -> Unit,
    selectedContentColor: Color,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
) {
    val colors = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
    Box(modifier = modifier.height(58.dp)) {
        if (com.aritxonly.teak.common.theme.TeakUiStyle.isClassic) {
            // 经典底栏整条走 Miuix 玻璃模糊；不再画顶部分割线。
            Box(Modifier.matchParentSize().teakClassicBarGlass())
            Box(
                Modifier.matchParentSize()
                    .background(colors.surfaceContainer.copy(alpha = 0.62f)),
            )
        } else {
            Box(Modifier.matchParentSize().background(colors.surfaceContainer))
        }
        Row(
            modifier = Modifier.fillMaxWidth().height(58.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { item ->
                val selected = item.key == selectedKey
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(58.dp)
                        .clickable(interactionSource = null, indication = null) {
                            onItemSelected(item)
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                ) {
                    Icon(
                        painter = if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label,
                        tint = if (selected) selectedContentColor else colors.onBackgroundVariant,
                        modifier = Modifier.size(24.dp),
                    )
                    androidx.compose.foundation.layout.Spacer(Modifier.height(2.dp))
                    Text(
                        text = item.label,
                        color = if (selected) selectedContentColor else colors.onBackgroundVariant,
                        fontSize = 10.sp,
                        fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                    )
                }
            }
        }
    }
}
