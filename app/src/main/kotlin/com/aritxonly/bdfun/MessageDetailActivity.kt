package com.aritxonly.bdfun

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.aritxonly.bdfun.ui.MessageDetailScreen
import com.aritxonly.bdfun.ui.MessageFeedKind

/** 「回复与@」「收到喜欢」「新增粉丝」的二级消息页。 */
class MessageDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AppGraph.ensure(applicationContext)
        val kind = when (intent?.getStringExtra("kind")) {
            "like" -> MessageFeedKind.Like
            "fans" -> MessageFeedKind.Fans
            "system" -> MessageFeedKind.System
            else -> MessageFeedKind.Reply
        }
        setContent {
            MessageDetailScreen(
                kind = kind,
                onBack = ::finish,
                initialTab = if (intent?.getStringExtra("kind") == "at") 2 else 0,
            )
        }
    }
}
