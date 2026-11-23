package com.aritxonly.bdfun.ui

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.core.model.user.UserProfile
import kotlinx.coroutines.launch

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import com.aritxonly.bdfun.feature.login.application.QrLoginStateHolder

@Composable
internal fun LoginScreen(controller: QrLoginStateHolder, onBack: () -> Unit, onSuccess: (UserProfile?) -> Unit) {
    val context = LocalContext.current
    val state by controller.state.collectAsState()
    // 二维码编码 + 480×480 像素遍历放到后台，别卡登录页首帧。
    var bitmap by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<Bitmap?>(null) }
    androidx.compose.runtime.LaunchedEffect(state.displayQr) {
        val url = state.displayQr?.url
        bitmap = if (url == null) null else kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            runCatching { qrBitmap(url, 480) }.getOrNull()
        }
    }
    val scope = rememberCoroutineScope()
    DisposableEffect(controller) { onDispose { controller.close() } }
    BackHandler(onBack = onBack)
    LaunchedEffect(controller) { controller.generate() }
    LaunchedEffect(controller, state.qr?.key) {
        state.qr?.key?.let { key -> controller.poll(key)?.let { onSuccess(it.profile) } }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LoginHeader()
        LoginQrCard(bitmap, state.loading) { saveLoginQr(context, bitmap, scope) }
        OfficialAppLoginButton(enabled = controller.officialAppLoginLink() != null) {
            controller.officialAppLoginLink()?.let { openOfficialAppLogin(context, it) }
                ?: context.showToast("请先刷新并获取有效二维码")
        }
        LoginStatus(state) { scope.launch { controller.generate() } }
    }
}
private fun saveLoginQr(
    context: android.content.Context,
    bitmap: Bitmap?,
    scope: kotlinx.coroutines.CoroutineScope,
) {
    if (bitmap == null) {
        context.showToast("二维码还没准备好")
        return
    }
    // 相册写入 + PNG 压缩放后台，避免点保存时卡住。
    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
        val ok = saveQrToGallery(context, bitmap)
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
            context.showToast(if (ok) "成功保存二维码" else "保存失败")
        }
    }
}
