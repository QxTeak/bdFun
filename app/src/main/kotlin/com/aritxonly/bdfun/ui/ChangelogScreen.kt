package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.BrandPink
import com.aritxonly.bdfun.BuildConfig
import com.aritxonly.teak.common.chrome.TeakLargeTitle
import com.aritxonly.teak.common.chrome.TrackTeakPageScroll
import top.yukonga.miuix.kmp.theme.MiuixTheme

private data class ChangelogVersion(
    val version: String,
    val changes: List<String>,
)

private val CHANGELOG = listOf(
    ChangelogVersion(
        version = "preview-5.0",
        changes = listOf(
            "新增 唱片控件",
            "新增 视频暂停后收起",
            "新增 合集列表控件",
            "新增 动态接入一镜到底",
            "新增 链接跳转",
            "重构 消息页功能",
            "焕新 个人空间页",
            "优化 弹幕计数样式",
            "优化 底层代码",
            "优化 部分界面显示",
            "修复 选择个别动态时，动态列表仅有一条动态的问题",
            "修复 大量Bug（忘记修了哪些了）",
        ),
    ),
    ChangelogVersion(
        version = "preview-4.3",
        changes = listOf(
            "新增 HyperOS 4 一镜到底转场动画",
            "新增 视频详情页查看合集与分P",
            "新增 默认播放倍速",
            "优化 部分动画效果",
            "优化 部分页面显示效果",
            "修复 评论不显示图片",
            "修复 自动连播未记忆的问题",
            "修复 历史、收藏、稍后再看页划动至底部时不刷新视频的问题",
        ),
    ),
    ChangelogVersion(
        version = "preview-4.0",
        changes = listOf(
            "新增 播放器锁定",
            "新增 进度条常显设置",
            "新增 弹幕空降助手",
            "新增 弹幕计数",
            "新增 首页卡片样式选项，加入大卡样式（尚未完成）",
            "新增 界面风格选项，加入经典样式（尚未完成）",
            "新增 消息页功能完善",
            "重构 弹幕（移植 BiliPai 实现方案）",
            "重构 搜索详情页",
            "优化 全局文字显示",
            "优化 设置页排版",
            "优化 收藏页等页面的界面设计",
            "优化 整体横划手势灵敏度",
            "优化 大量图标与界面显示",
            "修复 播放器控件呼出卡顿问题",
            "修复 首页滑动时视频卡片二次跳动问题",
        ),
    ),
    ChangelogVersion(
        version = "preview-3.9.4",
        changes = listOf(
            "新增 检查更新与自动提示",
            "新增 显示热搜开关",
            "修复 搜索页深色模式仍显示白色背景的问题",
        ),
    ),
    ChangelogVersion(
        version = "preview-3.9",
        changes = listOf(
            "新增 视频中部上下划屏切换/退出全屏模式",
            "重构 弹幕设置与弹幕绘制",
            "优化 播放器操作手感",
            "修复 有时手势失效的问题",
            "修复 打开已观看视频时无法自动跳转观看进度的问题",
        ),
    ),
    ChangelogVersion(
        version = "preview-3.8",
        changes = listOf(
            "新增 引用页",
            "新增 底栏抬高选项",
            "新增 CDN 设置",
            "优化 精简部分代码",
            "优化 统一已完成页面的视觉效果",
        ),
    ),
    ChangelogVersion(
        version = "preview-3.7",
        changes = listOf(
            "新增 半屏播放器听视频与更多设置面板",
            "新增 动态页与评论区支持点击头像或昵称进入个人主页",
            "新增 设置关于页捐赠入口",
            "新增 设置减弱动态效果选项",
            "焕新 消息页",
            "优化 部分界面的视觉效果",
        ),
    ),
    ChangelogVersion(
        version = "preview-3.6",
        changes = listOf(
            "新增 视频缓存功能，支持缓存当前视频、后台下载、离线播放与缓存管理",
            "修复 进入视频时状态栏图标反色异常",
            "修复 部分机型动态详情页状态栏显示异常",
            "修复 从相关推荐进入视频时出现的声音问题",
        ),
    ),
    ChangelogVersion(
        version = "preview-3.5",
        changes = listOf(
            "焕新 半屏播放器 UI，统一全屏与半屏的进度条与播放控件",
            "优化 切换下一集时直接在播放器内加载，不再跳转新的播放页",
            "优化 大量 UI 显示效果",
            "修复 弹幕、手势、面板与加载相关问题",
        ),
    ),
    ChangelogVersion(
        version = "preview-3.4",
        changes = listOf(
            "新增 听视频、小窗播放、自动连播、循环播放与镜像翻转等功能",
            "优化 播放器功能 UI 与易用性",
        ),
    ),
    ChangelogVersion(
        version = "preview-3.3",
        changes = listOf(
            "焕新 播放器 UI，采用与原版基本一致的布局",
            "优化 播放器操作手感",
            "修复 部分 UI 显示问题",
        ),
    ),
    ChangelogVersion(
        version = "preview-3.2",
        changes = listOf(
            "新增 响度均衡选项",
            "改进 视频详情页的部分 UI 与动效",
            "优化 播放器更好地支持 4K 与 HDR 视频",
        ),
    ),
    ChangelogVersion(
        version = "preview-3.1",
        changes = listOf(
            "新增 动态视频自动播放功能",
            "优化 动态页排版",
            "优化 部分图标的显示清晰度",
        ),
    ),
    ChangelogVersion(
        version = "preview-3.0",
        changes = listOf(
            "新增 动态页相关功能",
            "新增 评论 IP 属地显示与播放器观看人数显示选项",
            "优化 部分 UI 显示效果",
        ),
    ),
    ChangelogVersion(
        version = "preview-2.9",
        changes = listOf(
            "新增 色彩风格调整",
            "新增 重新打开视频时是否恢复原播放进度的选项",
            "新增 首页推荐使用 Web 端或 App 端接口的选项",
            "优化 「我的」页面的显示效果",
            "优化 部分 UI 调整",
        ),
    ),
    ChangelogVersion(
        version = "preview-2.8",
        changes = listOf(
            "新增 全局字号调整",
            "新增 我的评论与弹幕记录",
            "优化 设置、收藏、历史记录等页面的 UI",
        ),
    ),
    ChangelogVersion(
        version = "preview-2.7",
        changes = listOf(
            "新增 更多弹幕设置项",
            "新增 打开视频自动播放开关",
            "焕新 部分 UI 与排版调整",
            "修复 无法点赞、点踩的问题",
            "修复 状态栏沉浸导致的 UI 问题",
        ),
    ),
)

@Composable
fun ChangelogScreen(onBack: () -> Unit) {
    val colors = MiuixTheme.colorScheme
    val cardShape = remember { RoundedCornerShape(22.dp) }
    val listState = rememberLazyListState()
    TrackTeakPageScroll(listState)

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "header") {
            TeakLargeTitle(
                title = "更新日志",
                subtitle = "当前版本 ${BuildConfig.VERSION_NAME}",
            )
        }

        CHANGELOG.forEach { entry ->
            item(key = entry.version) {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    Text(
                        text = entry.version,
                        modifier = Modifier.padding(start = 14.dp, bottom = 5.dp),
                        color = colors.onBackgroundVariant,
                        fontSize = 13.sp,
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(cardShape)
                            .background(colors.surfaceContainer)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                    ) {
                        entry.changes.forEachIndexed { index, line ->
                            Row(
                                modifier = Modifier.padding(bottom = 9.dp),
                                verticalAlignment = Alignment.Top,
                            ) {
                                Text(
                                    text = "•",
                                    color = BrandPink,
                                    fontSize = 14.sp,
                                    lineHeight = 21.sp,
                                    modifier = Modifier.width(18.dp),
                                )
                                Text(
                                    text = line,
                                    color = colors.onBackgroundVariant,
                                    fontSize = 14.sp,
                                    lineHeight = 21.sp,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

