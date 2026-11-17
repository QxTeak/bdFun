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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aritxonly.teak.common.skeleton.TeakSkeletonBlock
import com.aritxonly.teak.common.skeleton.rememberTeakSkeletonSweep

/**
 * 视频详情尾部的骨架：分 P 横条、合集卡、相关推荐行。
 *
 * 三块和真实内容共用同一套外边距，数据到了直接原位替换，不再做上浮/淡入，
 * 所以不会出现"占位先滑一下、内容又滑一下"的双重运动。
 */
@Composable
internal fun VideoPagesSkeleton() {
    val sweep = rememberTeakSkeletonSweep()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(3) {
            TeakSkeletonBlock(sweep, Modifier.width(150.dp).height(54.dp), RoundedCornerShape(10.dp))
        }
    }
}

@Composable
internal fun VideoCollectionSkeleton() {
    val sweep = rememberTeakSkeletonSweep()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 10.dp),
    ) {
        TeakSkeletonBlock(sweep, Modifier.fillMaxWidth().height(54.dp), RoundedCornerShape(14.dp))
    }
}

@Composable
internal fun VideoRelatedSkeleton() {
    val sweep = rememberTeakSkeletonSweep()
    Column(modifier = Modifier.fillMaxWidth().padding(top = 14.dp)) {
        repeat(5) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
            ) {
                TeakSkeletonBlock(sweep, Modifier.width(150.dp).height(84.dp), RoundedCornerShape(12.dp))
                Spacer(Modifier.width(10.dp))
                // 右列两条标题 + 底部 UP 行，和真实 RelatedRow 的高度对齐。
                Column(modifier = Modifier.weight(1f)) {
                    TeakSkeletonBlock(sweep, Modifier.fillMaxWidth().height(14.dp))
                    Spacer(Modifier.height(7.dp))
                    TeakSkeletonBlock(sweep, Modifier.fillMaxWidth(0.72f).height(14.dp))
                    Spacer(Modifier.height(34.dp))
                    TeakSkeletonBlock(sweep, Modifier.width(92.dp).height(11.dp))
                }
            }
        }
        Box(Modifier.height(6.dp))
    }
}
