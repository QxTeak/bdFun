package com.aritxonly.bdfun

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.aritxonly.bdfun.core.model.pgc.BangumiRankTab
import com.aritxonly.bdfun.presentation.pgc.BangumiRankBoardScreen
import com.aritxonly.bdfun.presentation.pgc.rememberBangumiOpener
import com.aritxonly.bdfun.ui.ActivityPageOptions
import com.aritxonly.bdfun.ui.TeakActivityScaffold

/**
 * 单榜全量页（「查看更多」的目标）。对齐官方：排行榜是独立页面，参数是 rank_id + oid。
 * 入参走 Intent 显式传，不写 AppGraph.pendingXxx。
 */
class BangumiRankBoardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AppGraph.ensure(applicationContext)
        val rankId = intent.getLongExtra(EXTRA_RANK_ID, 0L)
        val oid = intent.getStringExtra(EXTRA_OID).orEmpty()
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty().ifBlank { "排行榜" }
        if (rankId <= 0L || oid.isBlank()) {
            finish()
            return
        }
        val settings = AppGraph.settings
        val holder = AppGraph.bangumiRankBoardState(rankId, oid, title)
        setContent {
            TeakActivityScaffold(
                settings = settings,
                onBack = { finish() },
                options = ActivityPageOptions(title = title),
            ) {
                BangumiRankBoardScreen(holder, onOpenSeason = rememberBangumiOpener(this))
            }
        }
    }

    companion object {
        private const val EXTRA_RANK_ID = "rank_id"
        private const val EXTRA_OID = "oid"
        private const val EXTRA_TITLE = "title"

        fun open(context: Context, tab: BangumiRankTab, title: String = tab.title) {
            context.startActivity(
                Intent(context, BangumiRankBoardActivity::class.java)
                    .putExtra(EXTRA_RANK_ID, tab.rankId)
                    .putExtra(EXTRA_OID, tab.oid)
                    .putExtra(EXTRA_TITLE, title),
            )
        }
    }
}
