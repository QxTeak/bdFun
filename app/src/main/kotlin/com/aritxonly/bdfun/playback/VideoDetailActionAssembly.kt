package com.aritxonly.bdfun.ui

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.aritxonly.bdfun.AppGraph
import com.aritxonly.bdfun.MainActivity

private class AndroidVideoDetailActionHost(
    private val context: Context, private val page: VideoPagePresentationState,
) : VideoDetailActionHost {
    override val settings get() = page.settings
    override val downloads get() = page.dependencies.downloads
    override var progressMs: Long
        get() = page.danmakuProgressMs
        set(value) { page.danmakuProgressMs = value }
    override fun showToast(text: String) = context.showToast(text)
    override fun share(text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "分享视频"))
    }
    override fun requestHandOff() = page.dependencies.background.requestHandOff()
    override fun openHome() {
        // 主界面可能被多层视频页盖住（相关推荐连开），用 CLEAR_TOP 一次清到主壳。
        AppGraph.mainTabRequests.requestHome()
        context.startActivity(
            Intent(context, MainActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP,
            ),
        )
        // 从外部直接拉起的视频页下面可能没有主界面；补一次 finish，避免返回又回到视频。
        (context as? android.app.Activity)?.finish()
    }
}

@Composable
internal fun videoDetailActionHost(): VideoDetailActionHost =
    AndroidVideoDetailActionHost(LocalContext.current, LocalVideoPageState.current)
