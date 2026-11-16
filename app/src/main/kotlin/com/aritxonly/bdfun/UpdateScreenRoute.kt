package com.aritxonly.bdfun

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.aritxonly.bdfun.core.domain.update.AppVersion
import com.aritxonly.bdfun.feature.update.application.UpdateStateHolder
import com.aritxonly.bdfun.infra.bili.update.GithubUpdateRepository
import com.aritxonly.bdfun.infra.storage.update.UpdateDownloader
import com.aritxonly.bdfun.presentation.update.UpdatePage
import kotlinx.coroutines.launch

/** Activity 和设置右栏共用同一页面装配，权限请求仍由所在 Activity 持有。 */
@Composable
internal fun UpdateScreenRoute() {
    val context = LocalContext.current
    val controller = remember { UpdateStateHolder(GithubUpdateRepository(),
        AppVersion(BuildConfig.VERSION_NAME.removeSuffix("-debug"), BuildConfig.VERSION_CODE)) }
    val downloader = remember { UpdateDownloader(context.applicationContext) }
    val state by controller.state.collectAsState()
    val download by downloader.state.collectAsState()
    val scope = rememberCoroutineScope()
    val downloadLatest = { controller.state.value.release?.let { downloader.start(it.version.name, it.apk) }; Unit }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) downloadLatest()
        else Toast.makeText(context, "请允许通知权限，以显示下载进度后再重试", Toast.LENGTH_SHORT).show()
    }
    BackHandler(state.history) { controller.showHistory(false) }
    LaunchedEffect(Unit) { controller.check() }
    UpdatePage(BuildConfig.VERSION_NAME, state, download,
        onCheck = { scope.launch { controller.check() } },
        onDownload = {
            if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) downloadLatest()
            else permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        },
        onHistory = {
            controller.showHistory(!state.history)
            if (!state.history && state.releases.isEmpty()) scope.launch { controller.check() }
        },
        animationsEnabled = AppGraph.settings.animationsEnabled,
        onBrowser = { url ->
            try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
            catch (_: Exception) { Toast.makeText(context, "无法打开浏览器", Toast.LENGTH_SHORT).show() }
        })
}
