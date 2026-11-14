package com.aritxonly.bdfun

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import com.aritxonly.bdfun.core.navigation.Route
import com.aritxonly.bdfun.navigation.createRouteIntent

/** 使用游客数据进入真实页面，覆盖 JVM 状态测试看不到的 CompositionLocal 装配顺序。 */
class PrivateChatLaunchRegression : Instrumentation() {
    private var messageNavigation = false
    override fun onCreate(arguments: Bundle?) {
        super.onCreate(arguments)
        messageNavigation = arguments?.getString("suite") == "message-navigation"
        start()
    }

    override fun onStart() {
        if (messageNavigation) { MessageNavigationRegression(this).run(); return }
        val status = Bundle().apply {
            putString("id", "InstrumentationTestRunner")
            putInt("numtests", 1)
            putInt("current", 1)
            putString("class", this@PrivateChatLaunchRegression.javaClass.name)
            putString("test", "launchPrivateChatWithInjectedDependencies")
        }
        sendStatus(1, status)
        var activity: Activity? = null
        var error: Throwable? = null
        try {
            runOnMainSync { AppGraph.ensure(targetContext.applicationContext) }
            check(!AppGraph.api.isLoggedIn) { "请在游客测试实例运行，避免真实账号的已读回执" }
            val intent = createRouteIntent(targetContext, Route.PrivateChat(1, name = "私信启动回归"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            activity = startActivitySync(intent)
            waitForIdleSync()
            SystemClock.sleep(1500)
            waitForIdleSync()
            val target = checkNotNull(activity)
            runOnMainSync {
                check(target is PrivateChatActivity && !target.isFinishing && !target.isDestroyed) {
                    "私信页面没有保持打开"
                }
            }
            sendStatus(0, status)
        } catch (failure: Throwable) {
            error = failure
            status.putString("stack", failure.stackTraceToString())
            sendStatus(-2, status)
        } finally {
            activity?.let { target -> runOnMainSync { target.finish() } }
        }
        finish(if (error == null) Activity.RESULT_OK else Activity.RESULT_CANCELED,
            Bundle().apply { putString("stream", if (error == null) "\nOK (1 test)\n" else "\n${error.message}\n") })
    }
}
