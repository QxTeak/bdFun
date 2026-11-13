package com.aritxonly.bdfun.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.teak.common.settings.TeakSettingsCard
import com.aritxonly.teak.common.settings.TeakSettingsPage
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.icon.basic.ArrowRight

private data class CreditProject(
    val name: String,
    val url: String,
)

private val CreditProjects = listOf(
    CreditProject("Hyper-PiliPlus", "https://github.com/AritxOnly/Hyper-PiliPlus"),
    CreditProject("Hyper-PiliPlus（fork）", "https://github.com/Mutx163/Hyper-PiliPlus"),
    CreditProject("HyperIsland", "https://github.com/1812z/HyperIsland"),
    CreditProject("Miuix", "https://github.com/compose-miuix-ui/miuix"),
    CreditProject("AndroidLiquidGlass（Kyant0）", "https://github.com/Kyant0/AndroidLiquidGlass"),
    CreditProject("Shapes / Capsule（Kyant0）", "https://github.com/Kyant0/Shapes"),
    CreditProject("PiliPlus", "https://github.com/bggRGjQaUbCoE/PiliPlus"),
    CreditProject("Guiyuan", "https://github.com/CHS-Haple/Guiyuan"),
    CreditProject("BiliPai", "https://github.com/jay3-yy/BiliPai"),
    CreditProject("bilibili-API-collect", "https://github.com/SocialSisterYi/bilibili-API-collect"),
    CreditProject("AndroidX / Jetpack Compose", "https://github.com/androidx/androidx"),
    CreditProject("Media3 / ExoPlayer", "https://github.com/androidx/media"),
    CreditProject("OkHttp", "https://github.com/square/okhttp"),
    CreditProject("Coil", "https://github.com/coil-kt/coil"),
    CreditProject("Kotlin / kotlinx.coroutines", "https://github.com/JetBrains/kotlin"),
    CreditProject("ZXing", "https://github.com/zxing/zxing"),
)

@Composable
fun CreditsScreen() {
    val colors = MiuixTheme.colorScheme
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    fun open(url: String) {
        runCatching { uriHandler.openUri(url) }
            .onFailure { context.showToast("无法打开链接") }
    }

    TeakSettingsPage(title = "引用") {
        item(key = "thanks") {
            Text(
                text = "bdFun 十分感谢以下项目的支持，感谢项目作者为开源社区所做的贡献。",
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp),
                color = colors.onBackground,
                fontSize = 14.sp,
                lineHeight = 21.sp,
            )
        }

        item(key = "credit-list") {
            TeakSettingsCard {
                CreditProjects.forEach { project ->
                    CreditRow(project) { open(project.url) }
                }
            }
        }
    }
}

/** 与设置页箭头行同一套 Miuix 行：标题 + 灰色链接 + 右侧箭头。 */
@Composable
private fun CreditRow(project: CreditProject, onClick: () -> Unit) {
    val colors = MiuixTheme.colorScheme
    top.yukonga.miuix.kmp.basic.BasicComponent(
        onClick = onClick,
        endActions = {
            top.yukonga.miuix.kmp.basic.Icon(
                imageVector = top.yukonga.miuix.kmp.icon.MiuixIcons.Basic.ArrowRight,
                contentDescription = null,
                tint = colors.onSurfaceVariantActions,
                modifier = Modifier.size(width = 10.dp, height = 16.dp),
            )
        },
    ) {
        top.yukonga.miuix.kmp.basic.Text(
            text = project.name,
            fontSize = MiuixTheme.textStyles.headline1.fontSize,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
            color = colors.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        top.yukonga.miuix.kmp.basic.Text(
            text = project.url,
            fontSize = MiuixTheme.textStyles.body2.fontSize,
            color = colors.onSurfaceVariantSummary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
