package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aritxonly.teak.common.skeleton.TeakSkeletonBlock
import com.aritxonly.teak.common.skeleton.rememberTeakSkeletonSweep

/** 消息会话首屏骨架：头像 + 名字 + 预览两行，尺寸与真实会话行一致。 */
@Composable
internal fun MessageSessionSkeletonRow() {
    val sweep = rememberTeakSkeletonSweep()
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TeakSkeletonBlock(sweep, Modifier.size(48.dp), CircleShape)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            TeakSkeletonBlock(sweep, Modifier.fillMaxWidth(0.32f).height(15.dp))
            Spacer(Modifier.height(8.dp))
            TeakSkeletonBlock(sweep, Modifier.fillMaxWidth(0.62f).height(12.dp))
        }
        Spacer(Modifier.width(12.dp))
        TeakSkeletonBlock(sweep, Modifier.width(52.dp).height(11.dp))
    }
}

/** 消息详情（回复与@ / 收到喜欢 / 新增粉丝）首屏骨架：卡片 + 两行内容。 */
@Composable
internal fun MessageFeedSkeletonCard() {
    val sweep = rememberTeakSkeletonSweep()
    Column(Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
        TeakSkeletonBlock(sweep, Modifier.fillMaxWidth().height(96.dp), RoundedCornerShape(18.dp))
        Spacer(Modifier.height(8.dp))
        TeakSkeletonBlock(sweep, Modifier.fillMaxWidth(0.7f).height(12.dp))
    }
}
