package com.aritxonly.bdfun

import com.aritxonly.bdfun.ui.rememberImageRequest

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aritxonly.bdfun.core.model.user.SpaceArticle
import com.aritxonly.bdfun.core.model.user.SpaceShopItem
import com.aritxonly.bdfun.ui.ContinuousCornerShape
import com.aritxonly.bdfun.ui.formatCount
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun SpaceArticleRow(
    article: SpaceArticle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MiuixTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(interactionSource = null, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(120.dp)
                .height(68.dp)
                .clip(ContinuousCornerShape(10.dp))
                .background(colors.surfaceContainerHigh),
        ) {
            if (article.cover.isNotBlank()) {
                AsyncImage(
                    model = rememberImageRequest(article.cover, crossfade = false),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = article.title,
                color = colors.onBackground,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${formatCount(article.view)} 阅读",
                color = colors.onBackgroundVariant,
                fontSize = 10.sp,
            )
        }
    }
}

@Composable
internal fun SpaceShopCard(
    item: SpaceShopItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surfaceContainer)
            .clickable(interactionSource = null, indication = null, onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(colors.surfaceContainerHigh),
        ) {
            if (item.cover.isNotBlank()) {
                AsyncImage(
                    model = rememberImageRequest(item.cover),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = item.title,
                color = colors.onBackground,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (item.price.isNotBlank()) {
                Spacer(Modifier.height(5.dp))
                Text(
                    text = item.price,
                    color = colors.primary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                )
            }
            if (item.source.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = item.source,
                    color = colors.onBackgroundVariant,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
