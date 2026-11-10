package com.aritxonly.bdfun

import com.aritxonly.teak.common.chrome.TeakPageChrome

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.data.DynamicItem
import com.aritxonly.bdfun.ui.CommentReplyBar
import com.aritxonly.bdfun.ui.CommentScreen
import com.aritxonly.bdfun.ui.DynamicCard
import com.aritxonly.bdfun.ui.TeakActivityScaffold
import com.aritxonly.bdfun.ui.showToast
import com.aritxonly.bdfun.ui.syncSystemBarIcons
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 动态详情：设置页同款外壳（大标题 / 折叠小标题 / 顶部渐进式模糊 / 玻璃返回键），
 * 正文是动态卡片，下面是和视频评论区完全同源的评论列表；底部复用视频评论页的回复栏。
 */
class DynamicDetailActivity : ComponentActivity() {
    /** 状态栏背景是页面本身（浅色/深色），图标明暗必须跟着实际页面底色走。 */
    private fun syncStatusBarIcons() {
        val systemDark = (resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val appDark = when (AppGraph.settings.darkMode) {
            1 -> false
            2 -> true
            else -> systemDark
        }
        syncSystemBarIcons(appDark)
    }

    override fun onResume() {
        super.onResume()
        // 从图库/其他页面回来时系统会重置图标明暗，重新同步
        syncStatusBarIcons()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // 转场/分屏后系统可能重置图标明暗，重新获得焦点时再同步一次。
        if (hasFocus) {
            syncStatusBarIcons()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppGraph.ensure(applicationContext)
        // 动态详情是沉浸式渐变页：状态栏透明，让页面背景透上去。
        // 必须自己接管，否则 One UI 等系统会按主题默认值画成白条。
        enableEdgeToEdge(
            statusBarStyle = androidx.activity.SystemBarStyle.dark(
                android.graphics.Color.TRANSPARENT,
            ),
        )
        syncStatusBarIcons()
        val dynamic = AppGraph.pendingDynamic.value
        if (dynamic == null) {
            finish()
            return
        }
        setContent {
            val settings = AppGraph.settings
            TeakActivityScaffold(
                settings = settings,
                onBack = { finish() },
                title = "动态详情",
                overlay = {
                    CommentReplyBar(
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                },
            ) {
                DynamicDetailScreen(dynamic = dynamic, onBack = { finish() })
            }
        }
    }
}

@Composable
private fun DynamicDetailScreen(dynamic: DynamicItem, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colors = MiuixTheme.colorScheme
    var detail by remember { mutableStateOf(dynamic) }
    var liked by remember { mutableStateOf(dynamic.liked) }
    var likeCount by remember { mutableIntStateOf(dynamic.like) }

    // 列表里的动态可能只有摘要；打开详情再问一次官方详情接口，补齐正文 / 图片 / 转发。
    LaunchedEffect(dynamic.id) {
        AppGraph.api.dynamicDetail(dynamic.id)?.let { full ->
            detail = full
            liked = full.liked
            likeCount = full.like
        }
    }

    // 动态接口的 pub_location_text 有时为空：用 UP 资料里的 IP 属地兜底，
    // 至少让详情卡片底部能看到 IP 信息。
    LaunchedEffect(detail.mid, detail.ipLocation) {
        if (detail.ipLocation.isBlank() && detail.mid > 0L) {
            val ip = AppGraph.api.userIpLocation(detail.mid)
            if (ip.isNotBlank()) detail = detail.copy(ipLocation = ip)
        }
    }

    BackHandler(onBack = onBack)

    CommentScreen(
        api = AppGraph.api,
        aid = detail.id.toLongOrNull() ?: 0L,
        upMid = 0L,
        title = detail.name,
        onBack = onBack,
        bvid = "",
        bare = true,
        topInset = 4.dp,
        commentOid = detail.commentOid.ifBlank { detail.id },
        commentType = detail.commentType,
        // 动态列表里点进来时已经在动，返回键直接给完整玻璃
        trackBackProgress = true,
        showLoading = false,
        bottomPadding = 132.dp,
        header = {
            item(key = "detail-big-title") {
                BigTitleHeader()
            }
            item(key = "detail-dynamic") {
                Box(modifier = Modifier.padding(bottom = 4.dp)) {
                    DynamicCard(
                        item = detail.copy(liked = liked, like = likeCount),
                        liked = liked,
                        onOpen = {},
                        expanded = true,
                        onOpenVideo = { TeakNavigator.openVideo(context, it) },
                        onOpenForwarded = { TeakNavigator.openDynamic(context, it) },
                        onLike = {
                            val next = !liked
                            liked = next
                            likeCount += if (next) 1 else -1
                            scope.launch {
                                val ok = AppGraph.api.likeDynamic(detail.id, next)
                                if (ok) {
                                    context.showToast(if (next) "点赞成功" else "已取消赞")
                                } else {
                                    liked = !next
                                    likeCount += if (!next) 1 else -1
                                    context.showToast("点赞失败")
                                }
                            }
                        },
                        onComment = {},
                        onRepost = {},
                    )
                }
            }
            item(key = "detail-comment-label") {
                Text(
                    text = "评论",
                    color = colors.onBackground,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 2.dp, top = 16.dp, bottom = 2.dp),
                )
            }
        },
    )
}

/** 和设置页一样的大标题：顶部留 52dp，30sp Medium，滚动时渐隐 + 模糊。 */
@Composable
private fun BigTitleHeader() {
    val colors = MiuixTheme.colorScheme
    val collapse = com.aritxonly.teak.common.chrome.LocalTeakPageScrollState.current.collapse
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = 1f - (collapse / 0.65f).coerceIn(0f, 1f)
            }
            .blur((12f * (collapse / 0.65f).coerceIn(0f, 1f)).dp),
    ) {
        Spacer(Modifier.height(52.dp))
        Text(
            text = "动态详情",
            color = colors.onBackground,
            fontSize = 30.sp,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.padding(start = 14.dp),
        )
        Spacer(Modifier.height(14.dp))
    }
}
