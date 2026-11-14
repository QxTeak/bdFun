package com.aritxonly.bdfun

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.aritxonly.bdfun.core.model.message.ChatMessageCard
import com.aritxonly.bdfun.ui.rememberImageRequest
import com.aritxonly.bdfun.ui.showToast
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun ChatMessageCardContent(card: ChatMessageCard) {
    val context = LocalContext.current
    val colors = MiuixTheme.colorScheme
    Column(Modifier.widthIn(max = 272.dp).clip(RoundedCornerShape(14.dp))
        .background(colors.surfaceContainerHigh).clickable(enabled = card.url.isNotBlank()) { openChatCard(context, card.url) }.padding(12.dp)) {
        if (card.image.isNotBlank()) AsyncImage(rememberImageRequest(card.image), "消息图片", modifier = Modifier.fillMaxWidth())
        if (card.title.isNotBlank()) Text(card.title, color = colors.onBackground)
        if (card.text.isNotBlank()) Text(card.text, color = colors.onBackgroundVariant)
        card.children.forEach { child -> ChatMessageCardContent(child) }
    }
}

private fun openChatCard(context: android.content.Context, url: String) {
    val uri = Uri.parse(url)
    val bvid = Regex("BV[0-9A-Za-z]{10}").find(url)?.value
    val aid = Regex("/video/av(\\d+)").find(url)?.groupValues?.get(1)?.toLongOrNull()
    when {
        bvid != null -> navigateToVideoByBvid(context, bvid)
        aid != null -> navigateToVideoAtComment(context, aid = aid, bvid = "", title = "", rootRpid = 0, targetRpid = 0)
        uri.scheme in listOf("http", "https", "bilibili") -> {
            try { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
            catch (_: android.content.ActivityNotFoundException) { context.showToast("无法打开该链接") }
        }
    }
}
