package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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

/**
 * 视频详情加载占位：一行行灰条，白色高光从左往右扫。
 *
 * 结构按真实元信息对齐：头像+昵称/粉丝行（右侧有关注胶囊）、两行标题、
 * 播放/弹幕/日期统计、AV 号、两行简介、分类，最后是五个操作圆钮。
 * 扫光本身由 TeakUI 的通用骨架屏负责。
 */
@Composable
internal fun VideoMetadataSkeleton(modifier: Modifier = Modifier) {
    val sweep = rememberTeakSkeletonSweep()

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
        // 作者行：头像 + 昵称/副标题 + 右侧关注胶囊。
        Row(verticalAlignment = Alignment.CenterVertically) {
            TeakSkeletonBlock(sweep, Modifier.size(40.dp), CircleShape)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                TeakSkeletonBlock(sweep, Modifier.width(96.dp).height(14.dp))
                TeakSkeletonBlock(sweep, Modifier.width(72.dp).height(11.dp))
            }
            TeakSkeletonBlock(sweep, Modifier.width(56.dp).height(26.dp), RoundedCornerShape(50))
        }
        Spacer(Modifier.height(10.dp))
        // 标题两行（字号 17sp / 行高 24sp）。
        TeakSkeletonBlock(sweep, Modifier.fillMaxWidth().height(24.dp), RoundedCornerShape(5.dp))
        Spacer(Modifier.height(6.dp))
        TeakSkeletonBlock(sweep, Modifier.fillMaxWidth(0.62f).height(24.dp), RoundedCornerShape(5.dp))
        Spacer(Modifier.height(8.dp))
        // 播放 / 弹幕 / 日期统计行。
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TeakSkeletonBlock(sweep, Modifier.width(64.dp).height(12.dp))
            TeakSkeletonBlock(sweep, Modifier.width(56.dp).height(12.dp))
            TeakSkeletonBlock(sweep, Modifier.width(76.dp).height(12.dp))
        }
        Spacer(Modifier.height(10.dp))
        // AV 号 / 分P 标识行。
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TeakSkeletonBlock(sweep, Modifier.width(64.dp).height(18.dp), RoundedCornerShape(9.dp))
            TeakSkeletonBlock(sweep, Modifier.width(44.dp).height(18.dp), RoundedCornerShape(9.dp))
        }
        Spacer(Modifier.height(10.dp))
        // 简介两行。
        TeakSkeletonBlock(sweep, Modifier.fillMaxWidth().height(13.dp))
        Spacer(Modifier.height(7.dp))
        TeakSkeletonBlock(sweep, Modifier.fillMaxWidth(0.78f).height(13.dp))
        Spacer(Modifier.height(8.dp))
        // 分类。
        TeakSkeletonBlock(sweep, Modifier.width(72.dp).height(13.dp))
        Spacer(Modifier.height(16.dp))
        // 点赞 / 不喜欢 / 投币 / 收藏 / 分享，五个等宽列居中。
        Row(modifier = Modifier.fillMaxWidth()) {
            repeat(5) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    TeakSkeletonBlock(sweep, Modifier.size(30.dp), CircleShape)
                }
            }
        }
    }
}
