package com.aritxonly.bdfun

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.io.File

/** 游客真实宿主回归：隐藏的首页不能抢走子页的系统返回。 */
internal class MessageNavigationRegression(private val test: Instrumentation) {
    fun run() {
        var activity: Activity? = null
        var failure: Throwable? = null
        try {
            test.runOnMainSync { AppGraph.ensure(test.targetContext.applicationContext) }
            check(!AppGraph.api.isLoggedIn) { "仅允许游客测试环境" }
            activity = test.startActivitySync(Intent(test.targetContext, MessageActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            awaitText("联系人")
            click("回复与@")
            awaitText("刷新")
            snapshot("message-reply")
            click("@我")
            SystemClock.sleep(400)
            snapshot("message-mention")
            test.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
            awaitText("联系人")
            val target = checkNotNull(activity)
            test.runOnMainSync { check(!target.isFinishing && !target.isDestroyed) { "子页返回退出了消息宿主" } }
            click("收到喜欢")
            awaitText("刷新")
            clickTopBack()
            awaitText("联系人")
            test.runOnMainSync { check(!target.isFinishing && !target.isDestroyed) }
        } catch (error: Throwable) {
            failure = error
            if (activity != null) {
                try { snapshot("message-navigation-failure") } catch (_: Exception) { }
                try {
                    File(test.targetContext.cacheDir, "message-navigation-tree.txt").writeText(tree(test.uiAutomation.rootInActiveWindow))
                } catch (_: Exception) { }
            }
        } finally {
            activity?.let { target -> test.runOnMainSync { target.finish() } }
        }
        test.finish(if (failure == null) Activity.RESULT_OK else Activity.RESULT_CANCELED,
            Bundle().apply { putString("stream", failure?.stackTraceToString() ?: "OK: message system/top back and tab screenshots") })
    }

    private fun awaitText(text: String): AccessibilityNodeInfo {
        val end = SystemClock.uptimeMillis() + 10_000
        while (SystemClock.uptimeMillis() < end) {
            test.waitForIdleSync()
            findText(test.uiAutomation.rootInActiveWindow, text)?.let { return it }
            SystemClock.sleep(100)
        }
        error("未找到界面元素：$text")
    }

    private fun click(text: String) {
        var node: AccessibilityNodeInfo? = awaitText(text)
        while (node != null && !node.isClickable) node = node.parent
        check(node?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true) { "无法点击：$text" }
        test.waitForIdleSync()
    }

    private fun clickTopBack() {
        val density = test.targetContext.resources.displayMetrics.density
        fun find(node: AccessibilityNodeInfo?, depth: Int = 0): AccessibilityNodeInfo? {
            if (node == null || depth > 12) return null
            val bounds = Rect().also { node.getBoundsInScreen(it) }
            // 既有 Canvas 返回键没有文字语义，按实际可点击节点的左上角按钮边界定位。
            if (node.isVisibleToUser && node.isClickable && bounds.width() in 1..(64 * density).toInt() &&
                bounds.centerX() < 80 * density && bounds.centerY() < 180 * density) return node
            for (index in 0 until node.childCount) find(node.getChild(index), depth + 1)?.let { return it }
            return null
        }
        val button = checkNotNull(find(test.uiAutomation.rootInActiveWindow)) { "未找到顶栏返回键" }
        check(button.performAction(AccessibilityNodeInfo.ACTION_CLICK))
        test.waitForIdleSync()
    }

    private fun findText(node: AccessibilityNodeInfo?, text: String, depth: Int = 0): AccessibilityNodeInfo? {
        if (node == null || depth > 12) return null
        if (node.isVisibleToUser && (node.text?.contains(text) == true || node.contentDescription?.contains(text) == true)) return node
        for (index in 0 until node.childCount) findText(node.getChild(index), text, depth + 1)?.let { return it }
        return null
    }

    private fun tree(node: AccessibilityNodeInfo?, depth: Int = 0): String {
        if (node == null || depth > 12) return ""
        return "${node.text} / ${node.contentDescription} / ${node.isVisibleToUser}\n" +
            (0 until node.childCount).joinToString("") { tree(node.getChild(it), depth + 1) }
    }

    private fun snapshot(name: String) {
        val bitmap = checkNotNull(test.uiAutomation.takeScreenshot())
        File(test.targetContext.cacheDir, "$name.png").outputStream().use {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
        bitmap.recycle()
    }
}
