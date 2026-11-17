package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider

/**
 * 鎶曞竵鏁伴噺娴眰锛氫袱鏋氭鑹茬‖甯佸崱骞舵帓锛岀偣鍦ㄥ崱鐗囦笂鐩存帴鎶曞竵銆? *
 * 鍒绘剰涓嶇敤搴曢儴 sheet锛氭姇甯佹槸鎾斁鍣ㄤ笂鐨勮交閲忓姩浣滐紝寮瑰眰鍙鐩栦腑闂翠竴灏忓潡銆? * 鐐圭┖鐧藉鍗冲叧闂紝鍜屾挱鏀捐繘搴︿繚鎸佸悓涓€灞忋€? */
@Composable
internal fun CoinChoicePopup(
    visible: Boolean,
    onDismiss: () -> Unit,
    onPick: (Int) -> Unit,
) {
    if (!visible) return
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnClickOutside = true,
        ),
    ) {
        val view = LocalView.current
        SideEffect {
            val window = (view.parent as? DialogWindowProvider)?.window
            window?.setDimAmount(0f)
            window?.setBackgroundDrawable(
                android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { onDismiss() },
            contentAlignment = Alignment.Center,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                CoinChoiceTile(1, onPick)
                CoinChoiceTile(2, onPick)
            }
        }
    }
}

@Composable
private fun CoinChoiceTile(count: Int, onPick: (Int) -> Unit) {
    Column(
        modifier = Modifier
            .width(96.dp)
            .height(94.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(listOf(Color(0xFFFFC46B), Color(0xFFFF8A00))),
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onPick(count) },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.94f)),
            contentAlignment = Alignment.Center,
        ) {
            MenuGlyph(MenuGlyphKind.Coin, Color(0xFFFF8A00), size = 24.dp)
        }
        Spacer(Modifier.height(9.dp))
        Text(
            text = "$count 纭竵",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

