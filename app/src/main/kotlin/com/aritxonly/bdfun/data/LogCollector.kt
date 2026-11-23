package com.aritxonly.bdfun.data

import android.content.Context
import android.os.Build
import com.aritxonly.bdfun.BuildConfig
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.ArrayDeque
import java.util.Date
import java.util.Locale

/**
 * 轻量应用日志收集：
 * - 关键事件写入内存环形缓冲 + filesDir，进程被杀也能留下最后一段；
 * - 未捕获异常自动记录；
 * - 导出时把应用日志、设备信息、logcat 快照合并成一个 txt。
 *
 * 只记录调试所需信息，不主动读取 cookie/token。
 */
object LogCollector {
    private const val MAX_MEMORY_LINES = 3000
    private const val MAX_FILE_BYTES = 2 * 1024 * 1024

    private val lines = ArrayDeque<String>()
    private val timeFormat = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US)
    private val fileFormat = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)

    @Volatile
    private var initialized = false

    private var logFile: File? = null
    private var appContext: Context? = null

    /** 待落盘的行。主线程只入队，文件写入交给专用线程批量做。 */
    private val pending = java.util.concurrent.ConcurrentLinkedQueue<String>()
    private val writerLock = Any()

    @Volatile
    private var writerStarted = false

    @Synchronized
    fun init(context: Context) {
        // 生产包不初始化日志文件和崩溃处理器：这些只用于开发排查。
        if (!BuildConfig.DEBUG) return
        if (initialized) return
        initialized = true
        appContext = context.applicationContext
        val dir = File(context.filesDir, "bdFun-logs").apply { mkdirs() }
        val file = File(dir, "current.log")
        logFile = file
        // 崩溃后重新进入时，内存缓冲是空的；先把上次落盘的内容读回来，
        // 否则「日志抓取」页面会显示“暂无日志”。
        // 只读文件尾部一小段（不是整份 2MB），启动首帧不做大 IO。
        runCatching {
            if (file.isFile) {
                readTailLines(file, 128 * 1024)
                    .takeLast(MAX_MEMORY_LINES)
                    .forEach { lines.addLast(it) }
            }
        }
        record(
            "APP",
            "start version=${BuildConfig.VERSION_NAME} code=${BuildConfig.VERSION_CODE} " +
                "sdk=${Build.VERSION.SDK_INT} device=${Build.MANUFACTURER} ${Build.MODEL}",
        )
        installCrashHandler()
    }

    inline fun record(tag: String, message: String) {
        if (!BuildConfig.DEBUG) return
        recordInternal(tag, message)
    }

    @Synchronized
    @PublishedApi
    internal fun recordInternal(tag: String, message: String) {
        val line = "${timeFormat.format(Date())} [${Thread.currentThread().name}] [$tag] $message"
        lines.addLast(line)
        while (lines.size > MAX_MEMORY_LINES) lines.removeFirst()
        ensureWriter()
        pending.add(line)
    }

    /** 只读文件末尾 [maxBytes] 内的行；第一行可能被截断，丢掉。 */
    private fun readTailLines(file: File, maxBytes: Int): List<String> {
        val length = file.length()
        if (length <= 0L) return emptyList()
        val start = (length - maxBytes).coerceAtLeast(0L)
        val text = runCatching {
            java.io.RandomAccessFile(file, "r").use { raf ->
                raf.seek(start)
                val buffer = ByteArray((length - start).toInt().coerceAtMost(maxBytes))
                raf.readFully(buffer)
                String(buffer, Charsets.UTF_8)
            }
        }.getOrDefault("")
        val all = text.split('\n')
        return if (start > 0L) all.drop(1) else all
    }

    /** 单线程批量落盘：避免每个播放器回调都在主线程打开/关闭文件。 */
    private fun ensureWriter() {
        if (writerStarted) return
        synchronized(writerLock) {
            if (writerStarted) return
            writerStarted = true
            Thread({
                while (true) {
                    val first = pending.poll()
                    if (first == null) {
                        runCatching { Thread.sleep(60L) }
                        continue
                    }
                    val batch = StringBuilder(first).append('\n')
                    var count = 1
                    while (count < 200) {
                        val next = pending.poll() ?: break
                        batch.append(next).append('\n')
                        count++
                    }
                    runCatching {
                        val file = logFile ?: return@runCatching
                        if (file.length() + batch.length > MAX_FILE_BYTES) {
                            file.writeText("")
                        }
                        java.io.FileOutputStream(file, true).use { out ->
                            out.write(batch.toString().toByteArray(Charsets.UTF_8))
                        }
                    }
                }
            }, "bdFun-log-writer").apply {
                isDaemon = true
                start()
            }
        }
    }

    @Synchronized
    fun clear() {
        lines.clear()
        runCatching { logFile?.writeText("") }
        record("APP", "log cleared")
    }

    @Synchronized
    fun beginSession() {
        clear()
        record("APP", "capture session started")
    }

    @Synchronized
    fun snapshot(): String = lines.joinToString("\n")

    @Synchronized
    fun tail(count: Int): String =
        lines.toList().takeLast(count).joinToString("\n")

    @Synchronized
    fun size(): Int = lines.size

    fun export(context: Context): File {
        val sb = StringBuilder()
        sb.appendLine("bdFun debug log")
        sb.appendLine("version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        sb.appendLine("package: ${context.packageName}")
        sb.appendLine("device: ${Build.MANUFACTURER} ${Build.MODEL}")
        sb.appendLine("android: ${Build.VERSION.RELEASE} / API ${Build.VERSION.SDK_INT}")
        sb.appendLine("exported: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}")
        sb.appendLine()
        sb.appendLine("===== app log =====")
        sb.appendLine(snapshot())
        sb.appendLine()
        sb.appendLine("===== logcat snapshot =====")
        sb.appendLine(captureLogcat())
        val dir = File(context.cacheDir, "bdFun-logs").apply { mkdirs() }
        val out = File(dir, "bdFun-log-${fileFormat.format(Date())}.txt")
        out.writeText(sb.toString())
        return out
    }

    private fun captureLogcat(): String {
        val pid = android.os.Process.myPid()
        val commands = listOf(
            arrayOf("logcat", "-d", "-t", "4000", "--pid=$pid"),
            arrayOf("logcat", "-d", "-t", "2000"),
        )
        for (cmd in commands) {
            val result = runCatching {
                val process = ProcessBuilder(*cmd)
                    .redirectErrorStream(true)
                    .start()
                val text = process.inputStream.bufferedReader().use { it.readText() }
                process.waitFor()
                text
            }.getOrNull()
            if (!result.isNullOrBlank() && !result.contains("Permission denied")) {
                return result.take(300_000)
            }
        }
        return "(logcat unavailable; app log only)"
    }

    /** 崩溃前同步落盘：异步写入线程可能还没跑，进程就已经结束了。 */
    @Synchronized
    fun flushSync() {
        if (!BuildConfig.DEBUG) return
        val file = logFile ?: return
        runCatching {
            val batch = StringBuilder()
            while (true) {
                val next = pending.poll() ?: break
                batch.append(next).append('\n')
            }
            if (batch.isNotEmpty()) {
                FileOutputStream(file, true).use { out ->
                    out.write(batch.toString().toByteArray(Charsets.UTF_8))
                }
            }
        }
    }

    private fun installCrashHandler() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                record(
                    "CRASH",
                    "thread=${thread.name} ${throwable.javaClass.name}: ${throwable.message}\n" +
                        throwable.stackTraceToString(),
                )
                flushSync()
            }
            previous?.uncaughtException(thread, throwable)
        }
    }
}
