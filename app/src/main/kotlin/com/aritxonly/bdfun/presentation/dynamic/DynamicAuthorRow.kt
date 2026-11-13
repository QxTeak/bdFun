package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun DynamicAuthorRow(name: String, face: String, selected: Boolean, updated: Boolean,
    live: Boolean = false, onClick: () -> Unit) {
    val colors = MiuixTheme.colorScheme
    Row(Modifier.fillMaxWidth().clip(ContinuousCornerShape(14.dp))
        .background(if (selected) colors.surfaceContainer else Color.Transparent)
        .clickable(onClick = onClick).padding(8.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(38.dp).semantics { stateDescription = if (updated) "有更新" else if (live) "直播中" else "" }) {
            if (face.isBlank()) Box(Modifier.fillMaxSize().background(colors.primary, CircleShape), contentAlignment = Alignment.Center) {
                Text("全", color = colors.onPrimary, fontSize = 16.sp)
            } else AsyncImage(model = rememberImageRequest(face), contentDescription = null,
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(CircleShape))
            if (updated || live) Box(Modifier.align(Alignment.TopEnd).size(8.dp).background(colors.primary, CircleShape))
        }
        Text(name, color = if (selected) colors.primary else colors.onBackground, fontSize = 12.sp,
            maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
    }
}
