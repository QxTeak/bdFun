package com.aritxonly.bdfun.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.teak.common.settings.TeakSettingsCard
import com.aritxonly.teak.common.settings.TeakSettingsPage
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 帮助中心：问答形式，入口在设置 → 关于 → 帮助中心。 */
@Composable
fun HelpScreen() {
    TeakSettingsPage(title = "帮助中心") {
        item(key = "help-qa") {
            TeakSettingsCard(title = "常见问题") {
                HelpQaRow(
                    question = "为什么我无法播放视频？",
                    answer = "大概率由于账号被哔哩哔哩风控，请切换小号后重试。",
                )
                HelpQaRow(
                    question = "唱片如何使用？",
                    answer = "左/右划动唱片进入下/上一集；上划唱片关闭；下划唱片暂停；长按唱片暂停/继续；单击唱片可回到原页面。",
                )
                HelpFeedbackRow()
                HelpQaRow(
                    question = "为什么我非常卡顿？",
                    answer = "请注意是否安装了 Release 包，Debug 专用于测试与日志抓取，不是日常用包。",
                )
            }
        }
    }
}

@Composable
private fun HelpQaRow(question: String, answer: String) {
    val colors = MiuixTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 11.dp),
    ) {
        Text(
            text = question,
            color = colors.onBackground,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = answer,
            color = colors.onBackgroundVariant,
            fontSize = 13.sp,
            lineHeight = 19.sp,
        )
    }
}

@Composable
private fun HelpFeedbackRow() {
    val colors = MiuixTheme.colorScheme
    val context = LocalContext.current
    fun open(url: String) {
        runCatching {
            context.startActivity(
                android.content.Intent(
                    android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse(url),
                ),
            )
        }.onFailure { context.showToast("无法打开链接") }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 11.dp),
    ) {
        Text(
            text = "我该去哪里提出反馈？",
            color = colors.onBackground,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "请加入 Telegram 群组，或在 GitHub 上提交 Issue。",
            color = colors.onBackgroundVariant,
            fontSize = 13.sp,
            lineHeight = 19.sp,
        )
        Spacer(Modifier.height(8.dp))
        Row {
            Text(
                text = "Telegram 群组",
                color = colors.primary,
                fontSize = 13.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(interactionSource = null, indication = null) {
                        open("https://t.me/+tQrDYf-hb702ZjM1")
                    }
                    .padding(horizontal = 6.dp, vertical = 3.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "GitHub Issue",
                color = colors.primary,
                fontSize = 13.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(interactionSource = null, indication = null) {
                        open("https://github.com/QxTeak/bdFun")
                    }
                    .padding(horizontal = 6.dp, vertical = 3.dp),
            )
        }
    }
}
