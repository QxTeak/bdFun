package com.aritxonly.bdfun

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.domain.update.AppVersion
import com.aritxonly.bdfun.infra.bili.update.GithubUpdateRepository
import kotlinx.coroutines.launch

/** 冷启动最多每日检查一次；失败不写检查时间，下次启动可重试。 */
internal fun ComponentActivity.checkUpdateOnStartup() {
    if (BuildConfig.DEBUG) return
    val prefs = getSharedPreferences("app_updates", android.content.Context.MODE_PRIVATE)
    if (System.currentTimeMillis() - prefs.getLong("last_check", 0) < 86_400_000) return
    lifecycleScope.launch {
        val result = GithubUpdateRepository().latest(BuildConfig.VERSION_NAME.startsWith("preview-"))
        if (result is Outcome.Success) {
            prefs.edit().putLong("last_check", System.currentTimeMillis()).apply()
            val release = result.value ?: return@launch
            if (release.version.code > BuildConfig.VERSION_CODE && !isFinishing && !isDestroyed) {
                android.app.AlertDialog.Builder(this@checkUpdateOnStartup)
                    .setTitle("发现新版本 ${release.version.name}")
                    .setMessage("已有新版本可下载，是否查看更新日志？")
                    .setNegativeButton("稍后", null)
                    .setPositiveButton("查看更新") { _, _ ->
                        startActivity(Intent(this@checkUpdateOnStartup, UpdateActivity::class.java))
                    }.show()
            }
        }
    }
}
