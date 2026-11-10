package com.aritxonly.bdfun.ui

import com.aritxonly.teak.common.chrome.centeredListWidth
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.BrandPink
import com.aritxonly.teak.common.chrome.TeakLargeTitle
import com.kyant.shapes.Capsule
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val TERMS_SECTIONS = listOf(
    "本项目为第三方非官方客户端，与哔哩哔哩（上海宽娱数码科技有限公司）及其关联方无任何隶属、授权或合作关系。",
    "一、项目性质与目的\n" +
        "1. 本项目系开发者个人出于学习、研究与技术交流目的而开发的开源项目，不以任何形式盈利。\n" +
        "2. 本项目不提供、不包含、不传播任何破解内容、付费内容绕过机制或会员权益破解功能。所有可访问的数据均来源于哔哩哔哩官方公开渠道或社区已知的公开接口信息。\n" +
        "3. 本项目的代码仅作为技术实现的参考，不构成对哔哩哔哩官方客户端的替代，亦不意图替代官方客户端。",
    "二、使用者义务与责任\n" +
        "1. 使用者在使用本项目前，应自行确认其行为符合哔哩哔哩用户服务协议及相关法律法规。\n" +
        "2. 使用者不得将本项目用于任何商业用途、盈利行为、批量操作、数据抓取、绕过风控或任何侵犯哔哩哔哩及第三方合法权益的场景。\n" +
        "3. 因使用者违反上述约定或相关协议而产生的一切后果（包括但不限于账号封禁、法律责任），由使用者自行承担，开发者不承担任何连带责任。",
    "三、知识产权声明\n" +
        "1. 哔哩哔哩平台内的所有内容（包括但不限于视频、音频、文字、图片、弹幕、用户数据）的知识产权及相关权益，均归上海宽娱数码科技有限公司及/或相应权利人所有。\n" +
        "2. 本项目仅提供技术层面的展示与播放能力，不对任何内容主张权利。",
    "四、风险提示与免责\n" +
        "1. 本项目按“现状”提供，开发者不对软件的稳定性、可用性、安全性作任何明示或默示的保证。\n" +
        "2. 因哔哩哔哩官方接口变更、策略调整或不可抗力导致本项目功能失效或产生任何损失，开发者不承担任何责任。\n" +
        "3. 开发者保留在收到权利方通知后，立即停止维护、下架或删除本项目全部代码的权利，且无需事先通知使用者。",
    "五、权利方联系\n" +
        "如哔哩哔哩官方或相关权利人认为本项目存在侵权或不当之处，请通过项目仓库的 Issues 或其他公开渠道联系开发者，开发者将在核实后积极配合处理（包括但不限于修改、删除相关代码或下架项目）。",
    "使用本项目即表示你已阅读、理解并同意上述全部声明。如不同意，请立即停止使用并删除本项目及相关文件。",
)

@Composable
fun TermsScreen(
    onAccept: () -> Unit,
    onExit: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .centeredListWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, top = 0.dp, bottom = 150.dp),
        ) {
            Text(text = "使用须知", style = androidx.compose.material3.MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(12.dp))
            TERMS_SECTIONS.forEach { section ->
                Text(
                    text = section,
                    color = colors.onBackgroundVariant,
                    fontSize = 13.sp,
                    lineHeight = 21.sp,
                )
                Spacer(Modifier.height(16.dp))
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(colors.surface.copy(alpha = 0.96f))
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TermsButton(
                text = "退出",
                primary = false,
                modifier = Modifier.weight(1f),
                onClick = onExit,
            )
            TermsButton(
                text = "我已知晓",
                primary = true,
                modifier = Modifier.weight(1f),
                onClick = onAccept,
            )
        }
    }
}

@Composable
private fun TermsButton(
    text: String,
    primary: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    Box(
        modifier = modifier
            .height(46.dp)
            .clip(Capsule())
            .background(if (primary) BrandPink else colors.surfaceContainerHigh)
            .border(
                width = 1.dp,
                color = if (primary) Color.White.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.28f),
                shape = Capsule(),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (primary) Color.White else colors.onBackgroundVariant,
            fontSize = 14.sp,
            fontWeight = if (primary) FontWeight.Normal else FontWeight.Normal,
        )
    }
}
