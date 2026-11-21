package com.aritxonly.bdfun.cast

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.ui.showToast
import com.aritxonly.teak.common.modal.MiuixBottomSheet
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Phone
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.SearchDevice
import top.yukonga.miuix.kmp.squircle.squircleSurface
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 播放器动作层写入、播放器页面层观察的投屏请求。
 *
 * onCast 只能拿到非 Composable 的回调，动作绑定对象又会随组合重建，
 * 因此请求放在进程级可观察单例里，由 [CastDialog] 的宿主读取。
 */
internal object CastRequestState {
    var url: String? by mutableStateOf(null)
        private set

    fun request(mediaUrl: String) {
        url = mediaUrl
    }

    fun clear() {
        url = null
    }
}

/**
 * 投屏设备面板：打开即搜索，关闭即停止。
 *
 * 搜索生命周期交给 LaunchedEffect，弹层关闭、刷新或页面销毁都会取消协程，
 * 且 [DlnaCaster.stop] 会关闭 socket，避免后台继续占用组播与端口。
 */
@Composable
internal fun CastDialog(visible: Boolean, url: String?, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val caster = remember { DlnaCaster(context.applicationContext) }
    var devices by remember { mutableStateOf<List<CastDevice>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var castingTo by remember { mutableStateOf<CastDevice?>(null) }
    var searchTick by remember { mutableIntStateOf(0) }

    LaunchedEffect(visible, searchTick) {
        if (!visible) {
            caster.stop()
            searching = false
            return@LaunchedEffect
        }
        devices = emptyList()
        error = null
        castingTo = null
        searching = true
        try {
            caster.discover { device ->
                withContext(Dispatchers.Main.immediate) {
                    if (devices.none { it.controlUrl == device.controlUrl }) {
                        devices = devices + device
                    }
                }
            }
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (_: Exception) {
            // 网络栈的异常信息多为英文，这里统一换成可展示的中文文案。
            error = "搜索设备失败，请检查网络后重试"
        } finally {
            // 刷新会取消旧搜索并立即启动新搜索，旧协程不能把新的“搜索中”状态覆盖掉。
            if (currentCoroutineContext().isActive) searching = false
        }
    }

    CastLifecycleEffect(caster)

    fun castTo(device: CastDevice) {
        val mediaUrl = url
        if (mediaUrl.isNullOrBlank()) {
            context.showToast("当前没有可投屏的视频")
            return
        }
        if (castingTo != null) return
        castingTo = device
        scope.launch {
            val result = caster.cast(device, mediaUrl)
            castingTo = null
            result.fold(
                onSuccess = {
                    context.showToast("已投屏到 ${device.friendlyName}")
                    onDismiss()
                },
                onFailure = { failure ->
                    context.showToast(
                        failure.message?.takeIf { it.isNotBlank() } ?: "投屏失败，请稍后重试",
                    )
                },
            )
        }
    }

    CastSheet(
        visible = visible,
        searching = searching,
        error = error,
        devices = devices,
        castingTo = castingTo,
        onDismiss = onDismiss,
        onResearch = { searchTick += 1 },
        onCast = ::castTo,
    )
}

@Composable
private fun CastLifecycleEffect(caster: DlnaCaster) {
    DisposableEffect(Unit) {
        onDispose {
            caster.close()
            // 页面直接退出时不会走 onDismiss，避免下一次进入播放器误弹出投屏面板。
            CastRequestState.clear()
        }
    }
}

@Composable
private fun CastSheet(
    visible: Boolean,
    searching: Boolean,
    error: String?,
    devices: List<CastDevice>,
    castingTo: CastDevice?,
    onDismiss: () -> Unit,
    onResearch: () -> Unit,
    onCast: (CastDevice) -> Unit,
) {
    MiuixBottomSheet(
        show = visible,
        title = "投屏",
        onDismissRequest = onDismiss,
        startAction = {
            IconButton(onClick = onDismiss) { Icon(MiuixIcons.Close, "关闭") }
        },
        endAction = {
            // 重新搜索改成白字胶囊；只留图标在深色玻璃上没有可读的文字入口。
            Button(
                onClick = onResearch,
                enabled = !searching,
                cornerRadius = 999.dp,
                colors = top.yukonga.miuix.kmp.basic.ButtonDefaults.buttonColors(
                    color = MiuixTheme.colorScheme.primary,
                    contentColor = Color.White,
                ),
                insideMargin = PaddingValues(horizontal = 14.dp, vertical = 7.dp),
                minHeight = 32.dp,
                minWidth = 0.dp,
            ) {
                Text("重新搜索", color = Color.White, fontSize = 12.sp)
            }
        },
    ) { padding ->
        val direction = LocalLayoutDirection.current
        Column(
            Modifier.fillMaxSize().padding(
                start = 20.dp + padding.calculateStartPadding(direction),
                end = 20.dp + padding.calculateEndPadding(direction),
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding(),
            ),
        ) {
            if (searching) {
                LinearProgressIndicator(Modifier.fillMaxWidth(), progress = null)
                Spacer(Modifier.height(12.dp))
            }
            val failure = error
            when {
                failure != null -> CastPlaceholder(
                    icon = MiuixIcons.SearchDevice,
                    message = failure,
                    actionLabel = "重新搜索",
                    onAction = onResearch,
                )
                devices.isEmpty() && searching -> CastPlaceholder(
                    icon = MiuixIcons.SearchDevice,
                    message = "正在搜索附近的投屏设备…",
                )
                devices.isEmpty() -> CastPlaceholder(
                    icon = MiuixIcons.SearchDevice,
                    message = "没有找到投屏设备",
                    actionLabel = "重新搜索",
                    onAction = onResearch,
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 12.dp),
                ) {
                    items(devices, key = { it.controlUrl }) { device ->
                        CastDeviceRow(
                            device = device,
                            casting = castingTo?.controlUrl == device.controlUrl,
                            enabled = castingTo == null,
                            onClick = { onCast(device) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CastDeviceRow(
    device: CastDevice,
    casting: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    Row(
        Modifier.fillMaxWidth()
            .squircleSurface(colors.surface.copy(alpha = 0.65f), 24.dp)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .heightIn(min = 76.dp)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(MiuixIcons.Phone, null, Modifier.size(26.dp), tint = colors.primary)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = device.friendlyName,
                color = colors.onSurfaceContainer,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (casting) "正在连接…" else device.address,
                color = colors.onSurfaceVariantSummary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (casting) CircularProgressIndicator(size = 20.dp, strokeWidth = 2.dp)
    }
}

@Composable
private fun CastPlaceholder(
    icon: ImageVector,
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(icon, null, Modifier.size(44.dp), tint = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        Text(message, color = MiuixTheme.colorScheme.onSurfaceVariantSummary, fontSize = 14.sp)
        if (actionLabel != null && onAction != null) {
            Button(
                onClick = onAction,
                cornerRadius = 999.dp,
                colors = top.yukonga.miuix.kmp.basic.ButtonDefaults.buttonColors(
                    color = MiuixTheme.colorScheme.primary,
                    contentColor = Color.White,
                ),
                insideMargin = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                minHeight = 36.dp,
                minWidth = 0.dp,
            ) {
                Text(actionLabel, color = Color.White, fontSize = 13.sp)
            }
        }
    }
}
