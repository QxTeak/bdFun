package com.aritxonly.bdfun

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.aritxonly.bdfun.data.LogCollector
import com.aritxonly.bdfun.data.SettingsStore
import com.aritxonly.bdfun.ui.TeakActivityScaffold
import com.aritxonly.bdfun.ui.showToast
import com.aritxonly.teak.common.settings.TeakSettingsActionRow
import com.aritxonly.teak.common.settings.TeakSettingsCard
import com.aritxonly.teak.common.settings.TeakSettingsPage
import com.kyant.shapes.Capsule
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.theme.MiuixTheme

class LogActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val settings = AppGraph.settings
        setContent {
            TeakActivityScaffold(
                settings = settings,
                onBack = { finish() },
                options = com.aritxonly.bdfun.ui.ActivityPageOptions(title = "日志抓取"),
            ) {
                LogScreen()
            }
        }
    }
}

@Composable
private fun LogScreen() {
    val context = LocalContext.current
    val colors = MiuixTheme.colorScheme
    val presentation = remember { LogPresentationStateHolder() }
    var tick by presentation::tick
    var preview by presentation::preview

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            tick++
            preview = LogCollector.tail(14)
        }
    }
    LaunchedEffect(tick) {
        if (preview.isBlank()) preview = LogCollector.tail(14)
    }

    TeakSettingsPage(title = "日志抓取") {
        item(key = "intro") { LogIntro() }

        item(key = "actions") { LogActions({ preview = "" }) }

        item(key = "freeze") { LogFreezeActions() }

        item(key = "export") { LogExport() }

        item(key = "preview") { LogPreview(preview, LogCollector.size()) }

        item(key = "bottom") { Spacer(Modifier.height(24.dp)) }
    }
}

private fun exportFreezeLog(context: Context): java.io.File {
    val dir = java.io.File(context.cacheDir, "bdFun-logs").apply { mkdirs() }
    val out = java.io.File(dir, "freeze-log.txt")
    out.writeText(com.aritxonly.bdfun.data.FreezeWatchdog.readTail(context, 20000))
    return out
}

private fun shareLog(context: Context, file: java.io.File) {
    val uri: Uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file,
    )
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, "bdFun 日志")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(send, "导出日志"))
}

private class LogPresentationStateHolder {
    var tick by mutableIntStateOf(0)
    var preview by mutableStateOf("")
}

@Composable
private fun LogIntro() {
    val context = LocalContext.current
    val colors = MiuixTheme.colorScheme

    Text(
        text = "复现问题前先点「开始新一轮抓取」，然后按平时操作复现；" +
            "完成后回来点「导出日志并分享」。日志只保存在本机，不会自动上传。",
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
        color = colors.onBackgroundVariant,
        fontSize = 13.sp,
        lineHeight = 20.sp,
    )

}

@Composable
private fun LogActions(onClear: () -> Unit) {
    val context = LocalContext.current
    val colors = MiuixTheme.colorScheme

    TeakSettingsCard {
        TeakSettingsActionRow(title = "开始新一轮抓取", trailing = "清空旧日志") {
            LogCollector.beginSession()
            context.showToast("已开始记录，请复现问题")
        }
        TeakSettingsActionRow(title = "复制最近日志") {
            val text = LogCollector.snapshot()
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE)
                as android.content.ClipboardManager
            clipboard.setPrimaryClip(
                android.content.ClipData.newPlainText("bdFun log", text),
            )
            context.showToast("已复制 ${LogCollector.size()} 行")
        }
        TeakSettingsActionRow(title = "清空日志") {
            LogCollector.clear()
            onClear()
            context.showToast("日志已清空")
        }
    }

}

@Composable
private fun LogFreezeActions() {
    val context = LocalContext.current
    val colors = MiuixTheme.colorScheme

    TeakSettingsCard {
        TeakSettingsActionRow(
            title = "复制卡住日志",
            trailing = "${com.aritxonly.bdfun.data.FreezeWatchdog.size(context) / 1024} KB",
        ) {
            val text = com.aritxonly.bdfun.data.FreezeWatchdog.readTail(context, 20000)
            if (text.isBlank()) {
                context.showToast("还没有卡住日志")
            } else {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE)
                    as android.content.ClipboardManager
                clipboard.setPrimaryClip(
                    android.content.ClipData.newPlainText("bdFun freeze log", text),
                )
                context.showToast("已复制卡住日志")
            }
        }
        TeakSettingsActionRow(title = "导出卡住日志并分享") {
            runCatching {
                shareLog(context, exportFreezeLog(context))
            }.onFailure {
                context.showToast("导出失败：${it.message}")
            }
        }
        TeakSettingsActionRow(title = "清空卡住日志") {
            com.aritxonly.bdfun.data.FreezeWatchdog.clear(context)
            context.showToast("卡住日志已清空")
        }
    }

}

@Composable
private fun LogExport() {
    val context = LocalContext.current
    val colors = MiuixTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(Capsule())
            .background(colors.primary)
            .clickable {
                runCatching {
                    val file = LogCollector.export(context)
                    shareLog(context, file)
                }.onFailure {
                    context.showToast("导出失败：${it.message}")
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "导出日志并分享",
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Normal,
        )
    }

}

@Composable
private fun LogPreview(preview: String, count: Int) {
    val context = LocalContext.current
    val colors = MiuixTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface)
            .padding(12.dp),
    ) {
        Text(
            text = "最近日志（${count} 行）",
            color = colors.onBackground,
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = preview.ifBlank { "暂无日志，先开始抓取并操作一下" },
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .verticalScroll(rememberScrollState()),
            color = colors.onBackgroundVariant,
            fontSize = 10.sp,
            lineHeight = 14.sp,
            fontFamily = FontFamily.Monospace,
        )
    }

}
