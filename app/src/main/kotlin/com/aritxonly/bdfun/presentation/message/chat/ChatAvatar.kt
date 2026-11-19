package com.aritxonly.bdfun

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aritxonly.bdfun.ui.rememberImageRequest
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun ChatAvatar(url: String, placeholder: String = "", profileMid: Long = 0) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val colors = MiuixTheme.colorScheme
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .then(if (profileMid > 0) Modifier.clickable(onClickLabel = "查看个人空间") {
                navigateToSpace(context, profileMid)
            } else Modifier)
            .background(colors.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        if (url.isNotBlank()) {
            AsyncImage(
                model = rememberImageRequest(url),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else if (placeholder.isNotBlank()) {
            Text(placeholder, color = colors.onBackgroundVariant, fontSize = 11.sp)
        }
    }
}
