package com.aritxonly.bdfun.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.SpaceBanner
import com.aritxonly.bdfun.core.model.user.UserProfile

@Composable
internal fun MineHero(profile: UserProfile?, navigation: MineNavigation, wide: Boolean = false) {
    val context = LocalContext.current
    val banner = remember(profile?.mid) { MineBannerStateHolder() }
    LaunchedEffect(banner, profile?.banner, profile?.bannerFallbacks) { banner.load(context, profile) }
    val height = if (wide) 208.dp else 250.dp
    val shape = ContinuousCornerShape(24.dp)
    Box(Modifier.fillMaxWidth()) {
        BoxWithConstraints(Modifier.fillMaxWidth().then(if (wide) Modifier.clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)) else Modifier)) {
            SpaceBanner(banner.bitmap, aspect = maxWidth / height, alignmentY = profile?.bannerAlignmentY ?: 0f)
            Box(Modifier.fillMaxWidth().height(height).background(Brush.verticalGradient(
                listOf(Color.Transparent, Color.Black.copy(alpha = 0.18f)))))

        }
        Box(Modifier.fillMaxWidth().padding(top = height - 30.dp)
            .padding(horizontal = if (wide) 0.dp else 14.dp)) {
            if (profile != null) MineProfileCard(profile, shape, navigation)
            else MineGuest(shape, navigation.onLogin)
        }
    }
}
