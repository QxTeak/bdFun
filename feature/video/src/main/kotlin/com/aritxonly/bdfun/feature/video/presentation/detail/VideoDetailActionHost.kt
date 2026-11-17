package com.aritxonly.bdfun.ui

import com.aritxonly.bdfun.core.application.DownloadQueueStateHolder
import com.aritxonly.bdfun.core.domain.PlaybackSettings

/** 页面动作不持有Android对象，平台提示/分享/交接仍由app执行。 */
interface VideoDetailActionHost {
    val settings: PlaybackSettings
    val downloads: DownloadQueueStateHolder
    var progressMs: Long
    fun showToast(text: String)
    fun share(text: String)
    fun requestHandOff()
    /** 收起态的回首页键：请求主壳回到推荐页并结束视频页。 */
    fun openHome()
}
