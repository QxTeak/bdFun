package com.aritxonly.bdfun.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aritxonly.bdfun.BrandPink
import com.aritxonly.bdfun.data.LiveChatKind
import com.aritxonly.bdfun.data.LiveChatMessage
import com.aritxonly.bdfun.data.LiveRoomInfo
import com.aritxonly.teak.common.navigation.TeakMorphMenuAnchor
import com.aritxonly.teak.common.navigation.TeakMorphMenuHost
import com.aritxonly.teak.common.navigation.TeakMorphMenuState
import com.kyant.shapes.Capsule
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ChevronBackward
import top.yukonga.miuix.kmp.icon.extended.More

/**
 * 直播详情页（移植自 BiliPai 的直播间布局，第一阶段全部使用 mock 数据）。
 *
 * 布局来源：BiliPai `feature/live/LivePlayerScreen.kt` 的竖屏形态：
 * 播放器在顶部，下面是互动区（弹幕 + 发送条），顶栏浮在播放器上。
 *
 * 和 BiliPai 不同的地方：
 * - 返回键 / 省略号：纯图标，没有底板（和 BiliPai 的紧凑模式一样）
 * - 省略号菜单动效沿用 bdFun 的 `TeakMorphMenuAnchor`
 * - 本页完全不使用玻璃材质，输入条用纯色胶囊
 */
@Composable
fun LiveRoomScreen(
    room: LiveRoomInfo,
    messages: List<LiveChatMessage>,
    onSend: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    player: androidx.media3.exoplayer.ExoPlayer? = null,
    statusText: String? = null,
) {
    val context = LocalContext.current
    val landscape = LocalConfiguration.current.screenWidthDp >
        LocalConfiguration.current.screenHeightDp
    val menuState = remember { TeakMorphMenuState() }
    var following by remember(room.roomId) { mutableStateOf(room.following) }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        if (landscape) {
            LandscapeLiveLayout(
                room = room,
                messages = messages,
                following = following,
                menuState = menuState,
                player = player,
                statusText = statusText,
                onSend = onSend,
                onBack = onBack,
                onFollow = { following = !following },
            )
        } else {
            PortraitLiveLayout(
                room = room,
                messages = messages,
                following = following,
                menuState = menuState,
                player = player,
                statusText = statusText,
                onSend = onSend,
                onBack = onBack,
                onFollow = { following = !following },
            )
        }

        TeakMorphMenuHost(menuState) { dismiss ->
            LiveMenuRow("复制直播间链接") {
                val url = "https://live.bilibili.com/${room.roomId}"
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                clipboard?.setPrimaryClip(ClipData.newPlainText("直播间链接", url))
                dismiss { context.showToast("已复制直播间链接") }
            }
            LiveMenuRow("在浏览器中打开") {
                dismiss {
                    runCatching {
                        context.startActivity(
                            android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse("https://live.bilibili.com/${room.roomId}"),
                            ),
                        )
                    }.onFailure { context.showToast("无法打开链接") }
                }
            }
            LiveMenuRow("举报直播间") {
                dismiss { context.showToast("举报功能稍后接入") }
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/* 竖屏：上播放器，下互动区                                             */
/* ------------------------------------------------------------------ */

@Composable
private fun PortraitLiveLayout(
    room: LiveRoomInfo,
    messages: List<LiveChatMessage>,
    following: Boolean,
    menuState: TeakMorphMenuState,
    player: androidx.media3.exoplayer.ExoPlayer?,
    statusText: String? = null,
    onSend: (String) -> Unit,
    onBack: () -> Unit,
    onFollow: () -> Unit,
) {
    val density = LocalDensity.current
    val imeBottom = with(density) { WindowInsets.ime.getBottom(density).toDp() }
    val navBottom = with(density) { WindowInsets.navigationBars.getBottom(density).toDp() }
    val bottomInset = if (imeBottom > navBottom) imeBottom else navBottom
    // 互动区要给悬浮输入条让出高度：44dp 输入框 + 上下 8dp 内边距 + 底部系统留白
    val inputAreaHeight = 44.dp + 16.dp + bottomInset

    Box(Modifier.fillMaxSize()) {
        // 背景：直播间封面放大模糊，避免播放器下方纯黑一块
        BackdropGlow(room, Modifier.fillMaxSize())

        Column(Modifier.fillMaxSize()) {
            // ---- 播放器区（16:9）----
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color.Black),
            ) {
                if (player != null) {
                    LivePlayerSurface(player, Modifier.fillMaxSize())
                } else {
                    LiveVideoPlaceholder(room, statusText, Modifier.fillMaxSize())
                }
                LiveTopBar(
                    room = room,
                    menuState = menuState,
                    onBack = onBack,
                )
            }

            // ---- 互动区（底部留白，输入条单独悬浮，不压在黑底上）----
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(bottom = inputAreaHeight)
                    .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                    .background(Color(0x66000000)),
            ) {
                Column(Modifier.fillMaxSize()) {
                    LiveRoomTitleRow(
                        room = room,
                        following = following,
                        onFollow = onFollow,
                    )
                    LiveChatList(
                        messages = messages,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                    )
                }
            }
        }

        // 输入条：单独悬浮在页面底部，背后是页面背景，没有黑底
        LiveInputBar(
            onSend = onSend,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
        )
    }
}

/* ------------------------------------------------------------------ */
/* 横屏：播放器铺满，弹幕浮在左下                                       */
/* ------------------------------------------------------------------ */

@Composable
private fun LandscapeLiveLayout(
    room: LiveRoomInfo,
    messages: List<LiveChatMessage>,
    following: Boolean,
    menuState: TeakMorphMenuState,
    player: androidx.media3.exoplayer.ExoPlayer?,
    statusText: String? = null,
    onSend: (String) -> Unit,
    onBack: () -> Unit,
    onFollow: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        if (player != null) {
            LivePlayerSurface(player, Modifier.fillMaxSize())
        } else {
            LiveVideoPlaceholder(room, statusText, Modifier.fillMaxSize())
        }
        LiveTopBar(room = room, menuState = menuState, onBack = onBack)

        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(start = 12.dp, bottom = 10.dp)
                .width(360.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xB30B0C0F)),
        ) {
            LiveRoomTitleRow(
                room = room,
                following = following,
                onFollow = onFollow,
                compact = true,
            )
            LiveChatList(messages = messages, modifier = Modifier.height(200.dp).fillMaxWidth())
            LiveInputBar(
                onSend = onSend,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/* ------------------------------------------------------------------ */
/* 顶栏：返回键 + 主播信息 + 省略号                                     */
/* ------------------------------------------------------------------ */

@Composable
private fun LiveTopBar(
    room: LiveRoomInfo,
    menuState: TeakMorphMenuState,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 返回键：纯图标，不加玻璃底（压在画面上，白箭头 + 点击热区）
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onBack,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = MiuixIcons.ChevronBackward,
                contentDescription = "返回",
                tint = Color.White,
                // 和收藏页等页面一致：40dp 热区 + 22dp 图标
                modifier = Modifier.size(22.dp),
            )
        }

        Spacer(Modifier.width(10.dp))

        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LiveAnchorChip(room)
        }

        Spacer(Modifier.width(8.dp))

        // 省略号：同样不加玻璃底，只保留 bdFun 的形变菜单动效
        TeakMorphMenuAnchor(menuState) { onClick ->
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = MiuixIcons.More,
                    contentDescription = "更多",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

/** 主播头像 + 昵称 + 副标题，完全照 BiliPai 的裸排版：没有底板、没有玻璃。 */
@Composable
private fun LiveAnchorChip(room: LiveRoomInfo) {
    Row(
        modifier = Modifier.padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LiveAvatar(room, size = 34)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = room.uname.ifBlank { room.title },
                    color = Color.White,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (room.onlineText.isNotBlank()) {
                    Text(
                        text = room.onlineText,
                        color = Color.White,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 10.dp),
                    )
                }
            }
            Text(
                text = buildString {
                    if (room.live) append("直播中")
                    if (room.areaText.isNotBlank()) {
                        if (isNotEmpty()) append(" · ")
                        append(room.areaText)
                    }
                    if (isNotEmpty() && room.onlineText.isNotBlank()) append(" · ")
                    if (room.onlineText.isNotBlank()) append("${room.onlineText}人气")
                },
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/* ------------------------------------------------------------------ */
/* 真实直播画面：Media3 PlayerView（控制器关闭，界面全部由我们自己画）    */
/* ------------------------------------------------------------------ */

@Composable
private fun LivePlayerSurface(
    player: androidx.media3.exoplayer.ExoPlayer,
    modifier: Modifier = Modifier,
) {
    androidx.compose.ui.viewinterop.AndroidView(
        modifier = modifier.background(Color.Black),
        factory = { ctx ->
            androidx.media3.ui.PlayerView(ctx).apply {
                useController = false
                setShutterBackgroundColor(android.graphics.Color.BLACK)
                this.player = player
            }
        },
        update = { view -> view.player = player },
        onRelease = { view -> view.player = null },
    )
}

/* ------------------------------------------------------------------ */
/* 播放器占位：接真实流之前，先用封面 + 渐变撑住画面                      */
/* ------------------------------------------------------------------ */

@Composable
private fun LiveVideoPlaceholder(
    room: LiveRoomInfo,
    statusText: String? = null,
    modifier: Modifier = Modifier,
) {
    Box(modifier.background(Color.Black)) {
        BackdropGlow(room, Modifier.fillMaxSize())
        // 中间给一个「直播中」提示，说明这里是画面区域
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 14.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LiveBadge()
            if (room.areaText.isNotBlank()) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = room.areaText,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.2.sp,
                )
            }
        }
        Text(
            text = statusText ?: "正在连接直播…",
            color = Color.White.copy(alpha = 0.55f),
            fontSize = 13.sp,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 24.dp),
        )
    }
}

@Composable
private fun LiveBadge() {
    Row(
        modifier = Modifier
            .clip(Capsule())
            .background(BrandPink)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(Color.White),
        )
        Spacer(Modifier.width(5.dp))
        Text(text = "直播中", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}

/** 用封面做一层模糊光晕；没有封面时退化成深色渐变，不会出现纯白块。 */
@Composable
private fun BackdropGlow(room: LiveRoomInfo, modifier: Modifier = Modifier) {
    Box(modifier) {
        if (room.cover.isNotBlank()) {
            AsyncImage(
                model = room.cover,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(48.dp)
                    .graphicsLayer { alpha = 0.55f },
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF12161C),
                            Color(0xFF0A0B0E),
                            Color(0xFF060709),
                        ),
                    ),
                ),
        )
    }
}

/* ------------------------------------------------------------------ */
/* 房间标题 + 关注                                                      */
/* ------------------------------------------------------------------ */

@Composable
private fun LiveRoomTitleRow(
    room: LiveRoomInfo,
    following: Boolean,
    onFollow: () -> Unit,
    compact: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 14.dp,
                end = 14.dp,
                top = if (compact) 10.dp else 14.dp,
                bottom = 8.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = room.title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = room.uname,
                    color = Color.White.copy(alpha = 0.62f),
                    fontSize = 13.2.sp,
                    maxLines = 1,
                )
                if (room.onlineText.isNotBlank()) {
                    Text(
                        text = "  ·  ${room.onlineText}人气",
                        color = Color.White.copy(alpha = 0.62f),
                        fontSize = 13.2.sp,
                        maxLines = 1,
                    )
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        FollowButton(following = following, onClick = onFollow)
    }
}

@Composable
private fun FollowButton(following: Boolean, onClick: () -> Unit) {
    val bg = if (following) Color.White.copy(alpha = 0.14f) else BrandPink
    Box(
        modifier = Modifier
            .clip(Capsule())
            .background(bg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 7.dp),
    ) {
        Text(
            text = if (following) "已关注" else "关注",
            color = if (following) Color.White.copy(alpha = 0.8f) else Color.White,
            fontSize = 14.4.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

/* ------------------------------------------------------------------ */
/* 弹幕列表                                                            */
/* ------------------------------------------------------------------ */

@Composable
private fun LiveChatList(messages: List<LiveChatMessage>, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }
    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 14.dp,
            end = 14.dp,
            top = 2.dp,
            bottom = 6.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        items(messages, key = { it.id }) { message ->
            LiveChatRow(message)
        }
    }
}

@Composable
private fun LiveChatRow(message: LiveChatMessage) {
    when (message.kind) {
        LiveChatKind.SYSTEM -> Text(
            text = message.text,
            color = Color.White.copy(alpha = 0.45f),
            fontSize = 14.4.sp,
        )

        LiveChatKind.GIFT -> Text(
            text = buildString {
                append(message.uname)
                append("：")
                append(message.text)
            },
            color = Color(0xFFFFD36B),
            fontSize = 14.4.sp,
        )

        LiveChatKind.SUPER_CHAT -> Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0x33FF6699))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = message.amountText.orEmpty(),
                color = BrandPink,
                fontSize = 13.2.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = message.uname,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.2.sp,
                )
                Text(
                    text = message.text,
                    color = Color.White,
                    fontSize = 14.4.sp,
                )
            }
        }

        LiveChatKind.DANMAKU -> Text(
            text = buildString {
                if (!message.medal.isNullOrBlank()) {
                    append("[")
                    append(message.medal)
                    append("] ")
                }
                append(message.uname)
                append("：")
                append(message.text)
            },
            color = Color.White.copy(alpha = 0.92f),
            fontSize = 14.4.sp,
            // 昵称单独染色：整行保持一致的情况下用行内样式成本更高，先统一白色，
            // 后续接真实弹幕时可以换成 AnnotatedString 上色。
        )
    }
}

/* ------------------------------------------------------------------ */
/* 输入条：和视频页同款的胶囊，但用纯色底（本页不用玻璃）                */
/* ------------------------------------------------------------------ */

@Composable
private fun LiveInputBar(
    onSend: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    // 和视频页输入条一致：底部让出「输入法高度 / 导航栏高度」里较大的那个
    val density = LocalDensity.current
    val imeDp = with(density) { WindowInsets.ime.getBottom(density).toDp().value }
    val navDp = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().value
    val bottomDp = if (imeDp > navDp) imeDp else navDp

    fun send() {
        val value = text.trim()
        if (value.isEmpty()) return
        onSend(value)
        text = ""
        focusManager.clearFocus()
    }

    Row(
        modifier = modifier
            .padding(bottom = bottomDp.dp, start = 12.dp, end = 12.dp)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .clip(Capsule())
                .background(Color.Black.copy(alpha = 0.5f)),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(start = 16.dp, end = 46.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    textStyle = ComposerTextStyle.copy(color = Color.White),
                    cursorBrush = SolidColor(BrandPink),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { send() }),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (text.isEmpty()) {
                                Text(
                                    text = "发一条友善的弹幕",
                                    color = Color.White.copy(alpha = 0.45f),
                                    fontSize = 13.sp,
                                )
                            }
                            inner()
                        }
                    },
                )
            }
            Row(
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ComposerIconButton(
                    kind = ComposerIcon.Send,
                    tint = if (text.isNotBlank()) BrandPink else Color.White.copy(alpha = 0.45f),
                    onClick = { send() },
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "\uD83D\uDE00", fontSize = 18.sp)
        }
    }
}

/* ------------------------------------------------------------------ */
/* 小零件                                                              */
/* ------------------------------------------------------------------ */

@Composable
private fun LiveAvatar(room: LiveRoomInfo, size: Int) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(Color(0xFF2A2D33)),
        contentAlignment = Alignment.Center,
    ) {
        if (room.face.isNotBlank()) {
            AsyncImage(
                model = room.face,
                contentDescription = room.uname,
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            )
        } else {
            Text(
                text = room.uname.take(1),
                color = Color.White,
                fontSize = (size / 2.4f).sp,
            )
        }
    }
}

@Composable
private fun LiveMenuRow(label: String, onClick: () -> Unit) {
    val colors = androidx.compose.material3.MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = null, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(text = label, color = colors.onBackground, fontSize = 14.sp)
    }
}
