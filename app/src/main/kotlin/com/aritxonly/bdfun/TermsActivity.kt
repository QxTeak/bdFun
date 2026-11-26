package com.aritxonly.bdfun

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.aritxonly.bdfun.ui.OnboardingScreen
import com.aritxonly.bdfun.ui.TeakBareTheme

/** 首次进入 / 安装新版本后展示一次的开始页。 */
class TermsActivity : BdFunActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.aritxonly.bdfun.data.LogCollector.init(applicationContext)
        AppGraph.ensure(applicationContext)
        val settings = AppGraph.settings
        val replay = intent.getBooleanExtra(EXTRA_REPLAY, false)
        // 新版开始页只在这个版本第一次进入时显示；看过一次后不再显示。
        // 从设置里点「查看开始页」时（replay）无条件再展示一次。
        if (!replay && settings.onboardingAcked) {
            startActivity(
                Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                },
            )
            finish()
            return
        }
        enableEdgeToEdge()
        setContent {
            TeakBareTheme(settings = settings) { _, _ ->
                OnboardingScreen(
                    onAccept = {
                        settings.onboardingAcked = true
                        settings.termsAckedVersion = BuildConfig.VERSION_NAME
                        if (replay) {
                            finish()
                        } else {
                            startActivity(
                                Intent(this, MainActivity::class.java).apply {
                                    addFlags(
                                        Intent.FLAG_ACTIVITY_NEW_TASK or
                                            Intent.FLAG_ACTIVITY_CLEAR_TASK,
                                    )
                                },
                            )
                            finish()
                        }
                    },
                    onExit = { if (replay) finish() else finishAffinity() },
                )
            }
        }
    }

    companion object {
        const val EXTRA_REPLAY = "replay_onboarding"
    }
}
