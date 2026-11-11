package com.aritxonly.bdfun.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.aritxonly.bdfun.BuildConfig
import com.aritxonly.bdfun.R
import com.aritxonly.bdfun.data.CdnAutoSelector
import com.aritxonly.bdfun.data.CdnServices
import com.aritxonly.bdfun.data.ScrollTopPages
import com.aritxonly.bdfun.data.SettingsStore
import com.aritxonly.teak.common.settings.TeakSettingsActionRow
import com.aritxonly.teak.common.settings.TeakSettingsCard
import com.aritxonly.teak.common.settings.TeakSettingsDropdownRow
import com.aritxonly.teak.common.settings.TeakSettingsPage
import com.aritxonly.teak.common.settings.TeakSettingsSwitchRow
import com.aritxonly.teak.common.settings.TeakSettingsValueRow
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun SettingsScreen(
    settings: SettingsStore,
    onBack: () -> Unit,
    onLogout: () -> Unit = {},
    loggedIn: Boolean = true,
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var cdnSpeeds by remember { mutableStateOf(CdnAutoSelector.speedsSnapshot()) }
    LaunchedEffect(Unit) {
        if (!CdnAutoSelector.isFresh() || cdnSpeeds.isEmpty()) {
            CdnAutoSelector.refresh(
                com.aritxonly.bdfun.AppGraph.api,
                force = cdnSpeeds.isEmpty(),
            )
        }
        cdnSpeeds = CdnAutoSelector.speedsSnapshot()
        while (CdnAutoSelector.isProbing()) {
            cdnSpeeds = CdnAutoSelector.speedsSnapshot()
            delay(400)
        }
        cdnSpeeds = CdnAutoSelector.speedsSnapshot()
    }
    val cdnOptions = remember(cdnSpeeds) { cdnMenuLabels(cdnSpeeds) }
    val wallpaperPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            settings.splashWallpaper = uri.toString()
        }
    }

    TeakSettingsPage(
        title = "设置",
    ) {
        item(key = "appearance") {
            var pendingDark by remember { mutableStateOf<Int?>(null) }
            var pendingColor by remember { mutableStateOf<Int?>(null) }
            LaunchedEffect(settings.darkMode) { pendingDark = null }
            LaunchedEffect(settings.colorStyle) { pendingColor = null }
            TeakSettingsCard(title = "外观与显示") {
                TeakSettingsDropdownRow(
                    title = "主题",
                    options = listOf("跟随系统", "浅色", "深色"),
                    selected = pendingDark ?: settings.darkMode,
                    onSelected = { index ->
                        pendingDark = index
                        scope.launch {
                            delay(260)
                            settings.darkMode = index
                        }
                    },
                )
                TeakSettingsDropdownRow(
                    title = "色彩风格",
                    options = listOf("AC 粉", "Hyper 蓝"),
                    selected = pendingColor ?: settings.colorStyle,
                    onSelected = { index ->
                        pendingColor = index
                        scope.launch {
                            delay(260)
                            settings.colorStyle = index
                            com.aritxonly.bdfun.ThemePalette.style = index
                        }
                    },
                )
                TeakSettingsDropdownRow(
                    title = "界面风格",
                    options = listOf("官方", "经典"),
                    selected = settings.interfaceStyle.coerceIn(0, 1),
                    onSelected = {
                        settings.interfaceStyle = it
                        com.aritxonly.teak.common.theme.TeakUiStyle.style = it
                    },
                )
                TeakSettingsSwitchRow(
                    title = "模糊效果",
                    checked = settings.blurEnabled,
                    onCheckedChange = { settings.blurEnabled = it },
                )
                TeakSettingsSwitchRow(
                    title = "回到顶部控件",
                    checked = settings.scrollTopEnabled,
                    onCheckedChange = { settings.scrollTopEnabled = it },
                )
                if (settings.scrollTopEnabled) {
                    TeakSettingsDropdownRow(
                        title = "回到顶部控件位置",
                        options = listOf("左侧", "右侧"),
                        selected = settings.scrollTopSide,
                        onSelected = { settings.scrollTopSide = it },
                    )
                    // 每个页面单独开关；默认全部打开。
                    ScrollTopPages.entries.forEach { (key, label) ->
                        TeakSettingsSwitchRow(
                            title = label,
                            checked = settings.isScrollTopPageEnabled(key),
                            onCheckedChange = { settings.setScrollTopPageEnabled(key, it) },
                        )
                    }
                }
                TeakSettingsSwitchRow(
                    title = "减弱动态效果",
                    checked = !settings.animationsEnabled,
                    onCheckedChange = { settings.animationsEnabled = !it },
                )
                TeakSettingsSwitchRow(
                    title = "智能刷新率",
                    description = "根据画面状态自动调整屏幕刷新率",
                    checked = settings.smartRefresh,
                    onCheckedChange = { settings.smartRefresh = it },
                )
                TeakSettingsSwitchRow(
                    title = "抬高底栏",
                    description = "解决未开启安卓导航条时底栏位置过低的问题",
                    checked = settings.bottomBarRaise,
                    onCheckedChange = { settings.bottomBarRaise = it },
                )
                TeakSettingsActionRow(
                    title = "设置开屏壁纸",
                    trailing = if (settings.splashWallpaper.isBlank()) "未设置" else "已设置",
                ) {
                    wallpaperPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                }
                TeakSettingsActionRow(title = "清除开屏壁纸") {
                    settings.splashWallpaper = ""
                    context.showToast("已清除开屏壁纸")
                }
                GlobalFontScaleRow(
                    value = settings.fontScale,
                    onValueChange = { settings.fontScale = it },
                )
            }
        }

        item(key = "player") {
            TeakSettingsCard(title = "播放器") {
                TeakSettingsSwitchRow(
                    title = "显示正在观看人数",
                    checked = settings.showViewerCount,
                    onCheckedChange = { settings.showViewerCount = it },
                )
                TeakSettingsSwitchRow(
                    title = "播放进度记忆",
                    checked = settings.resumePlayback,
                    onCheckedChange = { settings.resumePlayback = it },
                )
                TeakSettingsSwitchRow(
                    title = "亮度/音量划动手势",
                    checked = settings.gestureBrightnessVolume,
                    onCheckedChange = { settings.gestureBrightnessVolume = it },
                )
                TeakSettingsSwitchRow(
                    title = "视频中部上下划屏切换/退出全屏模式",
                    checked = settings.gestureCenterFullscreen,
                    onCheckedChange = { settings.gestureCenterFullscreen = it },
                )
                TeakSettingsSwitchRow(
                    title = "长按倍速划动调速",
                    checked = settings.gestureBoostSwipe,
                    onCheckedChange = { settings.gestureBoostSwipe = it },
                )
                TeakSettingsDropdownRow(
                    title = "长按默认倍率",
                    options = BOOST_SPEED_LABELS,
                    selected = BOOST_SPEED_VALUES
                        .indexOfFirst { kotlin.math.abs(it - settings.boostDefaultSpeed) < 0.01f }
                        .coerceAtLeast(0),
                    onSelected = { settings.boostDefaultSpeed = BOOST_SPEED_VALUES[it] },
                )
                TeakSettingsDropdownRow(
                    title = "默认播放倍速",
                    options = PLAYBACK_SPEED_LABELS,
                    selected = PLAYBACK_SPEED_VALUES
                        .indexOfFirst { kotlin.math.abs(it - settings.defaultPlaybackSpeed) < 0.01f }
                        .coerceAtLeast(0),
                    onSelected = { settings.defaultPlaybackSpeed = PLAYBACK_SPEED_VALUES[it] },
                )
                TeakSettingsSwitchRow(
                    title = "全屏显示时间",
                    checked = settings.showFullscreenTime,
                    onCheckedChange = { settings.showFullscreenTime = it },
                )
                TeakSettingsSwitchRow(
                    title = "全屏显示互动按钮",
                    checked = settings.showFullscreenActions,
                    onCheckedChange = { settings.showFullscreenActions = it },
                )
                TeakSettingsDropdownRow(
                    title = "进度条常显",
                    options = listOf("关闭", "仅半屏时常显", "仅全屏时常显", "永久显示"),
                    selected = settings.progressBarMode,
                    onSelected = { settings.progressBarMode = it },
                )
                TeakSettingsSwitchRow(
                    title = "响度均衡",
                    checked = settings.loudnessEq,
                    onCheckedChange = { settings.loudnessEq = it },
                )
                TeakSettingsDropdownRow(
                    title = "默认清晰度",
                    options = QUALITY_LABELS,
                    selected = QUALITY_VALUES.indexOf(settings.qualityPreference).coerceAtLeast(0),
                    onSelected = { settings.qualityPreference = QUALITY_VALUES[it] },
                )
                TeakSettingsDropdownRow(
                    title = "硬解模式",
                    description = when (settings.decoderMode) {
                        1 -> "优先硬件解码，失败时自动回退"
                        2 -> "只使用软件解码，兼容性优先"
                        3 -> "只使用硬件解码，失败时不回退"
                        else -> "自动选择，优先硬件解码"
                    },
                    options = DECODER_LABELS,
                    selected = settings.decoderMode.coerceIn(0, DECODER_LABELS.lastIndex),
                    onSelected = { settings.decoderMode = it },
                )
                TeakSettingsDropdownRow(
                    title = "首选解码格式",
                    description = "不可用时自动回退到其他编码",
                    options = CODEC_LABELS,
                    selected = CODEC_VALUES.indexOf(settings.codecPreference).coerceAtLeast(0),
                    onSelected = { settings.codecPreference = CODEC_VALUES[it] },
                )
                TeakSettingsDropdownRow(
                    title = "CDN 设置",
                    description = "如发现播放问题，请选择原地址",
                    options = cdnOptions,
                    closedValue = CdnServices[cdnSelectedIndex(settings.cdnService)].label,
                    selected = cdnSelectedIndex(settings.cdnService),
                    panelWidth = 300.dp,
                    onSelected = { index ->
                        settings.cdnService = CdnServices[index].id
                    },
                )
                TeakSettingsSwitchRow(
                    title = "自动旋转",
                    checked = settings.autoRotate,
                    onCheckedChange = { settings.autoRotate = it },
                )
                TeakSettingsSwitchRow(
                    title = "进入视频自动播放",
                    checked = settings.autoPlay,
                    onCheckedChange = { settings.autoPlay = it },
                )
                TeakSettingsSwitchRow(
                    title = "蓝牙耳机延迟补偿",
                    description = "检测到蓝牙耳机设备时自动校准播放延迟",
                    checked = settings.bluetoothLatencyEnabled,
                    onCheckedChange = { settings.bluetoothLatencyEnabled = it },
                )
                if (settings.bluetoothLatencyEnabled) {
                    TeakSettingsDropdownRow(
                        title = "补偿延迟",
                        options = LATENCY_VALUES.map { "$it ms" },
                        selected = LATENCY_VALUES.indexOf(settings.bluetoothLatencyMs)
                            .coerceAtLeast(0),
                        onSelected = { settings.bluetoothLatencyMs = LATENCY_VALUES[it] },
                    )
                }
            }
        }

        item(key = "home") {
            TeakSettingsCard(title = "首页") {
                TeakSettingsSwitchRow(
                    title = "视频页退出不会继续播放",
                    description = "返回离开视频页或关闭画中画时停止播放；从视频页切到桌面仍进入画中画",
                    checked = settings.stopOnVideoPageExit,
                    onCheckedChange = { settings.stopOnVideoPageExit = it },
                )
                TeakSettingsSwitchRow(
                    title = "退出视频后继续播放",
                    checked = settings.showDiscAfterVideo,
                    onCheckedChange = { settings.showDiscAfterVideo = it },
                )
                if (settings.showDiscAfterVideo && !settings.stopOnVideoPageExit) {
                    TeakSettingsDropdownRow(
                        title = "播放控件",
                        options = listOf("画中画", "唱片"),
                        selected = settings.exitVideoControl.coerceIn(0, 1),
                        onSelected = { settings.exitVideoControl = it },
                    )
                }
                TeakSettingsSwitchRow(
                    title = "唱片呼吸灯",
                    description = "唱片周围出现一圈缓慢呼吸的光效",
                    checked = settings.discBreathEnabled,
                    onCheckedChange = { settings.discBreathEnabled = it },
                )
                if (settings.discBreathEnabled) {
                    TeakSettingsSwitchRow(
                        title = "呼吸灯跟随封面取色",
                        description = "关闭时为白色",
                        checked = settings.discBreathCoverColor,
                        onCheckedChange = { settings.discBreathCoverColor = it },
                    )
                }
                TeakSettingsDropdownRow(
                    title = "卡片样式",
                    options = listOf("双排", "大卡"),
                    selected = settings.homeCardStyle.coerceIn(0, 1),
                    onSelected = { settings.homeCardStyle = it },
                )
                TeakSettingsSwitchRow(
                    title = "首页使用 Web 端推荐",
                    checked = settings.recommendWeb,
                    onCheckedChange = { settings.recommendWeb = it },
                )
            }
        }

        item(key = "video_detail") {
            TeakSettingsCard(title = "视频详情页") {
                TeakSettingsSwitchRow(
                    title = "沉浸状态栏",
                    checked = settings.videoImmersiveStatusBar,
                    onCheckedChange = { settings.videoImmersiveStatusBar = it },
                )
                TeakSettingsSwitchRow(
                    title = "显示评论 IP 属地",
                    checked = settings.showIpLocation,
                    onCheckedChange = { settings.showIpLocation = it },
                )
            }
        }

        item(key = "danmaku") {
            TeakSettingsCard(title = "弹幕") {
                TeakSettingsSwitchRow(
                    title = "空降助手",
                    checked = settings.sponsorSkipEnabled,
                    onCheckedChange = { settings.sponsorSkipEnabled = it },
                )
                TeakSettingsSwitchRow(
                    title = "弹幕计数",
                    checked = settings.hotDanmakuEnabled,
                    onCheckedChange = { settings.hotDanmakuEnabled = it },
                )
                TeakSettingsSwitchRow(
                    title = "降低弹幕刷新率",
                    checked = settings.danmakuLowRefresh,
                    onCheckedChange = { settings.danmakuLowRefresh = it },
                )
            }
        }

        item(key = "dynamics") {
            TeakSettingsCard(title = "动态页") {
                TeakSettingsSwitchRow(
                    title = "动态视频自动播放",
                    checked = settings.dynamicAutoPlay,
                    onCheckedChange = { settings.dynamicAutoPlay = it },
                )
                TeakSettingsDropdownRow(
                    title = "动态视频清晰度",
                    options = listOf("1080P", "720P", "480P"),
                    selected = listOf(80, 64, 32)
                        .indexOf(settings.dynamicAutoPlayQuality)
                        .coerceAtLeast(0),
                    onSelected = {
                        settings.dynamicAutoPlayQuality = listOf(80, 64, 32)[it]
                    },
                )
            }
        }

        item(key = "search") {
            TeakSettingsCard(title = "搜索页") {
                TeakSettingsSwitchRow(
                    title = "显示热搜",
                    checked = settings.showHotSearch,
                    onCheckedChange = { settings.showHotSearch = it },
                )
            }
        }

        item(key = "mine") {
            TeakSettingsCard(title = "我的页") {
                TeakSettingsSwitchRow(
                    title = "显示「我的评论与弹幕」入口",
                    checked = settings.sentHistoryEntry,
                    onCheckedChange = { settings.sentHistoryEntry = it },
                )
            }
        }

        item(key = "developers") {
            TeakSettingsCard(title = "开发者") {
                DeveloperRow(
                    name = "Artix",
                    avatar = R.drawable.dev_artix,
                    url = "https://github.com/AritxOnly",
                )
                DeveloperRow(
                    name = "Teak",
                    avatar = R.drawable.dev_teak,
                    url = "https://github.com/QxTeak",
                )
            }
        }

        item(key = "about") {
            val aboutContext = LocalContext.current
            TeakSettingsCard(title = "关于") {
                TeakSettingsValueRow(
                    title = "版本",
                    value = BuildConfig.VERSION_NAME,
                )
                TeakSettingsActionRow(title = "加入讨论组", trailing = "Telegram 群") {
                    openTelegramGroup(aboutContext)
                }
                TeakSettingsActionRow(title = "更新日志") {
                    com.aritxonly.bdfun.TeakNavigator.openChangelog(aboutContext)
                }
                TeakSettingsActionRow(title = "帮助中心") {
                    com.aritxonly.bdfun.TeakNavigator.openHelp(aboutContext)
                }
                TeakSettingsActionRow(title = "查看开始页") {
                    com.aritxonly.bdfun.TeakNavigator.openOnboarding(aboutContext)
                }
                TeakSettingsActionRow(title = "捐赠") {
                    com.aritxonly.bdfun.TeakNavigator.openDonation(aboutContext)
                }
                TeakSettingsActionRow(title = "引用") {
                    com.aritxonly.bdfun.TeakNavigator.openCredits(aboutContext)
                }
                TeakSettingsActionRow(title = "项目地址") {
                    runCatching {
                        aboutContext.startActivity(
                            android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse("https://github.com/QxTeak/bdFun"),
                            ),
                        )
                    }.onFailure { aboutContext.showToast("无法打开链接") }
                }
                if (BuildConfig.DEBUG) {
                    TeakSettingsActionRow(title = "直播间（预览）", trailing = "调试") {
                        com.aritxonly.bdfun.TeakNavigator.openLiveRoom(aboutContext)
                    }
                    TeakSettingsActionRow(title = "日志抓取", trailing = "调试") {
                        com.aritxonly.bdfun.openLogs(aboutContext)
                    }
                }
            }
        }

        if (loggedIn) {
            item(key = "account") {
                TeakSettingsCard(title = "账号") {
                    TeakSettingsActionRow(
                        title = "退出登录",
                        color = Color(0xFFE5484D),
                        onClick = onLogout,
                    )
                }
            }
        }
    }
}

private val QUALITY_LABELS = listOf("自动", "HDR", "4K", "1080P+", "1080P", "720P")
private val QUALITY_VALUES = listOf(0, 125, 120, 112, 80, 64)
private val LATENCY_VALUES = listOf(50, 100, 150, 200, 250, 300)
private val PLAYBACK_SPEED_VALUES = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f, 2.5f, 3f)
private val PLAYBACK_SPEED_LABELS = PLAYBACK_SPEED_VALUES.map {
    if (it % 1f == 0f) "${it.toInt()}×" else "${it}×"
}
private val BOOST_SPEED_VALUES = listOf(1.5f, 2f, 2.5f, 3f, 4f, 5f)
private val BOOST_SPEED_LABELS = BOOST_SPEED_VALUES.map {
    if (it % 1f == 0f) "${it.toInt()}×" else "${it}×"
}
private val DECODER_LABELS = listOf(
    "自动",
    "硬件解码",
    "软件解码",
    "硬件解码（禁回退）",
)

/** 选项文本 = 节点名 + 最近一次测速成绩。 */
private fun cdnMenuLabels(speeds: Map<String, Double>): List<String> {
    return buildList {
        CdnServices.forEach { service ->
            val speed = if (service.auto) {
                CdnAutoSelector.currentService()?.id?.let(speeds::get)
            } else {
                speeds[service.id]
            }
            add(withSpeed(service.label, speed))
        }
    }
}

private fun withSpeed(label: String, speed: Double?): String =
    if (speed != null && speed > 0.01) {
        "$label  ${String.format(java.util.Locale.US, "%.1f", speed)} MB/s"
    } else {
        label
    }

private fun cdnSelectedIndex(id: String): Int {
    val index = CdnServices.indexOfFirst { it.id == id }
    return if (index < 0) 0 else index
}

private val CODEC_LABELS = listOf(
    "自动",
    "AVC (H.264)",
    "HEVC (H.265)",
    "AV1",
    "DVH1",
)

private val CODEC_VALUES = listOf("auto", "avc", "hevc", "av1", "dvh1")

/** 和帮助中心同一个 Telegram 群链接，避免两处入口指向不同地方。 */
private const val TELEGRAM_GROUP = "https://t.me/+tQrDYf-hb702ZjM1"

/** 加入讨论组：直接打开 Telegram 群；没装 Telegram 时退回系统浏览器。 */
private fun openTelegramGroup(context: android.content.Context) {
    val uri = android.net.Uri.parse(TELEGRAM_GROUP)
    val telegram = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
        .setPackage("org.telegram.messenger")
        .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
    if (runCatching { context.startActivity(telegram) }.isSuccess) return
    runCatching {
        context.startActivity(
            android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }.onFailure { context.showToast("无法打开链接") }
}

/** 开发者行：左边圆形头像，右边名字；点击打开 GitHub 主页。 */
@Composable
private fun DeveloperRow(
    name: String,
    avatar: Int,
    url: String,
) {
    val context = LocalContext.current
    val colors = MiuixTheme.colorScheme
    top.yukonga.miuix.kmp.preference.ArrowPreference(
        title = name,
        startAction = {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(avatar),
                contentDescription = name,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(colors.surfaceContainerHigh),
            )
        },
        onClick = {
            runCatching {
                context.startActivity(
                    android.content.Intent(
                        android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse(url),
                    ),
                )
            }.onFailure { context.showToast("无法打开链接") }
        },
    )
}

/** 全局字号：默认 100%，只缩放文字 sp，不改控件尺寸；使用 Miuix 原生滑条行。 */
@Composable
private fun GlobalFontScaleRow(value: Float, onValueChange: (Float) -> Unit) {
    val min = 0.8f
    val max = 1.2f
    var sliderValue by remember(value) { mutableFloatStateOf(value) }
    top.yukonga.miuix.kmp.preference.SliderPreference(
        value = sliderValue,
        onValueChange = {
            sliderValue = if (kotlin.math.abs(it - 1f) < 0.025f) 1f else it
        },
        title = "全局字号",
        valueText = "${(sliderValue * 100).toInt()}%",
        valueRange = min..max,
        keyPoints = listOf(1f),
        showKeyPoints = false,
        magnetThreshold = 0.035f,
        onValueChangeFinished = { onValueChange(sliderValue) },
    )
}

