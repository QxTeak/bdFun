package com.aritxonly.bdfun

import com.aritxonly.bdfun.navigation.createRouteIntent
import android.app.Activity
import android.app.ActivityOptions
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.PixelCopy
import android.view.View
import com.aritxonly.bdfun.data.VideoItem
import java.lang.reflect.Method

/**
 * 让"卡片 → 视频页"走系统自带的窗口放大动画（就是桌面点图标打开 App 的那套）。
 *
 * 我们只提供三样东西：起点矩形、卡片截图、圆角；放大过程由系统合成器完成，
 * 所以文字不会糊、不会拉伸，背景压暗也是系统做的。
 */
object TeakOneTake {

    private const val TAG = "TeakOneTake"

    /** 带缩略图的启动动画 */
    private const val ANIM_SCALED_THUMB = 103

    @Volatile
    private var method: Method? = null

    @Volatile
    private var resolved = false

    /** release 仅保留转场路径诊断，避免把窗口策略问题误判为路由问题。 */
    private fun trace(message: String) {
        Log.i(TAG, message)
        com.aritxonly.bdfun.data.LogCollector.record(TAG, message)
    }

    /**
     * 用系统动画打开视频页。
     * @param anchor 卡片在**屏幕坐标系**里的矩形（px）
     * @param radiusPx 卡片圆角（px）
     * @param fallback 拿不到条件时退回普通跳转
     */
    @JvmStatic
    fun openVideo(
        context: Context,
        video: VideoItem,
        anchor: Rect?,
        radiusPx: Float,
        foregroundColor: Int = 0,
        fallback: () -> Unit,
    ) {
        openWith(context, anchor, radiusPx, foregroundColor, fallback) { activity, opts ->
            val intent = createRouteIntent(activity,
                com.aritxonly.bdfun.core.navigation.Route.Video(video.bvid, video.aid, video,
                    fromVideoPage = activity is VideoActivity))
            val options = opts.toBundle()
            trace(
                "launch mode=route-intent source=${activity.javaClass.simpleName} target=VideoActivity " +
                    "animType=${options.getInt("android:activity.animType", -1)} " +
                    "flags=${intent.flags} anchor=$anchor " +
                    "orientation=${activity.resources.configuration.orientation} " +
                    "smallestWidth=${activity.resources.configuration.smallestScreenWidthDp} " +
                    "requestedOrientation=${activity.requestedOrientation} " +
                    "window=${activity.windowManager.currentWindowMetrics.bounds}")
            activity.startActivity(intent, options)
        }
    }

    /** 动态详情也用同一套系统放大动画；起点是整张动态卡片。 */
    @JvmStatic
    fun openFeed(
        context: Context,
        dynamic: com.aritxonly.bdfun.data.FeedItem,
        anchor: Rect?,
        radiusPx: Float,
        foregroundColor: Int = 0,
        fallback: () -> Unit,
    ) {
        openWith(context, anchor, radiusPx, foregroundColor, fallback) { activity, opts ->
            activity.startActivity(
                createRouteIntent(activity,
                    com.aritxonly.bdfun.core.navigation.Route.Feed(dynamic.id, dynamic)),
                opts.toBundle(),
            )
        }
    }

    private fun openWith(
        context: Context,
        anchor: Rect?,
        radiusPx: Float,
        foregroundColor: Int,
        fallback: () -> Unit,
        launch: (Activity, ActivityOptions) -> Unit,
    ) {
        Log.d(TAG, "openWith 进入 anchor=$anchor radius=$radiusPx")
        val activity = findActivity(context)
        if (activity == null) {
            fail("拿不到 Activity（${context.javaClass.simpleName}）", fallback)
            return
        }
        val decor = activity.window?.decorView
        if (decor == null || decor.width <= 0 || decor.height <= 0) {
            fail("拿不到窗口", fallback)
            return
        }
        if (anchor == null || anchor.width() <= 0 || anchor.height() <= 0) {
            fail("卡片位置还没算出来", fallback)
            return
        }
        val m = resolveMethod()

        // 截图必须走 PixelCopy：Compose 的界面用 view.draw() 画出来是空的
        captureByPixelCopy(activity, decor, anchor) { thumb ->
            if (thumb == null) {
                fail("截图失败", fallback)
                return@captureByPixelCopy
            }
            if (m == null) {
                startPublic(activity, decor, anchor, thumb, launch, fallback)
            } else {
                start(activity, m, decor, anchor, radiusPx, foregroundColor, thumb, launch) {
                    startPublic(activity, decor, anchor, thumb, launch, fallback)
                }
            }
        }
    }

    /** 非厂商系统也保留连续缩略图过渡，坐标转换到启动 View。 */
    private fun startPublic(
        activity: Activity, decor: View, anchor: Rect, thumb: Bitmap,
        launch: (Activity, ActivityOptions) -> Unit, fallback: () -> Unit,
    ) {
        try {
            val location = IntArray(2)
            decor.getLocationOnScreen(location)
            val options = ActivityOptions.makeThumbnailScaleUpAnimation(
                decor, thumb, anchor.left - location[0], anchor.top - location[1],
            )
            trace( "path=public-thumbnail")
            launch(activity, options)
        } catch (error: Exception) {
            Log.w(TAG, "公开缩略图过渡启动失败", error)
            fallback()
        }
    }

    private fun start(
        activity: Activity,
        m: Method,
        decor: View,
        anchor: Rect,
        radiusPx: Float,
        foregroundColor: Int,
        thumb: Bitmap,
        launch: (Activity, ActivityOptions) -> Unit,
        fallback: () -> Unit,
    ) {
        try {
            val handler = Handler(Looper.getMainLooper())
            val opts = m.invoke(
                null,
                decor,                          // 只用来取包名
                thumb,                          // 起飞用的截图
                anchor.left, anchor.top,        // 屏幕坐标
                radiusPx.toInt(),               // 圆角
                foregroundColor,                // 对应官方配置的 bgColor
                1.0f,                           // 按下缩放：1.0 = 不做缩放
                handler,
                null, null, null, null,         // 进出动画回调
                ANIM_SCALED_THUMB,
            ) as? ActivityOptions
            if (opts == null) {
                fail("系统没给出动画选项", fallback)
                return
            }
            trace(
                "path=private requestedType=$ANIM_SCALED_THUMB " +
                    "returnedType=${opts.toBundle().getInt("android:activity.animType", -1)}")
            Log.d(TAG, "系统动画启动 rect=$anchor r=$radiusPx thumb=${thumb.width}x${thumb.height}")
            launch(activity, opts)
        } catch (t: Throwable) {
            Log.w(TAG, "启动失败", t)
            fail("启动失败：${t.javaClass.simpleName}", fallback)
        }
    }

    /** 用 PixelCopy 抓窗口里卡片那一块的像素（API 26+）。 */
    private fun captureByPixelCopy(
        activity: Activity,
        decor: View,
        anchor: Rect,
        onDone: (Bitmap?) -> Unit,
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            onDone(captureByDraw(decor, anchor))
            return
        }
        val loc = IntArray(2)
        decor.getLocationOnScreen(loc)
        val src = Rect(
            anchor.left - loc[0],
            anchor.top - loc[1],
            anchor.right - loc[0],
            anchor.bottom - loc[1],
        )
        // 夹到窗口范围内
        val win = Rect(0, 0, decor.width, decor.height)
        if (!src.intersect(win) || src.width() <= 0 || src.height() <= 0) {
            onDone(null)
            return
        }
        val bitmap = try {
            Bitmap.createBitmap(src.width(), src.height(), Bitmap.Config.ARGB_8888)
        } catch (t: Throwable) {
            onDone(null)
            return
        }
        try {
            PixelCopy.request(
                activity.window,
                src,
                bitmap,
                { result ->
                    if (result == PixelCopy.SUCCESS) {
                        onDone(bitmap)
                    } else {
                        Log.w(TAG, "PixelCopy 失败 result=$result")
                        onDone(null)
                    }
                },
                Handler(Looper.getMainLooper()),
            )
        } catch (t: Throwable) {
            Log.w(TAG, "PixelCopy 抛错", t)
            onDone(null)
        }
    }

    /** 兜底方案：直接画（普通 View 能用，Compose 可能画不出来）。 */
    private fun captureByDraw(root: View, anchor: Rect): Bitmap? {
        val w = root.width
        val h = root.height
        if (w <= 0 || h <= 0) return null
        return try {
            val full = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            root.draw(Canvas(full))
            val loc = IntArray(2)
            root.getLocationOnScreen(loc)
            val l = (anchor.left - loc[0]).coerceIn(0, w - 1)
            val t = (anchor.top - loc[1]).coerceIn(0, h - 1)
            val r = (anchor.right - loc[0]).coerceIn(l + 1, w)
            val b = (anchor.bottom - loc[1]).coerceIn(t + 1, h)
            Bitmap.createBitmap(full, l, t, r - l, b - t)
        } catch (t: Throwable) {
            Log.w(TAG, "截图失败", t)
            null
        }
    }

    /** 任何一个环节不满足就退回普通跳转（只在日志里留一行原因）。 */
    private fun fail(reason: String, fallback: () -> Unit) {
        trace( "path=plain reason=$reason")
        Log.w(TAG, "退回普通跳转：$reason")
        fallback()
    }

    /** Compose 里拿到的 Context 常常被包装过，要一层层剥到底。 */
    private fun findActivity(context: Context): Activity? {
        var c: Context? = context
        while (c != null) {
            if (c is Activity) return c
            c = (c as? ContextWrapper)?.baseContext
        }
        return null
    }

    /** 反射拿隐藏接口：先直取，失败再用元反射绕一层。 */
    private fun resolveMethod(): Method? {
        method?.let { return it }
        if (resolved) return method
        synchronized(this) {
            if (resolved) return method
            val params = arrayOf<Class<*>>(
                View::class.java, Bitmap::class.java,
                Int::class.javaPrimitiveType!!, Int::class.javaPrimitiveType!!,
                Int::class.javaPrimitiveType!!, Int::class.javaPrimitiveType!!,
                Float::class.javaPrimitiveType!!, Handler::class.java,
                Runnable::class.java, Runnable::class.java,
                Runnable::class.java, Runnable::class.java,
                Int::class.javaPrimitiveType!!,
            )
            var found: Method? = try {
                ActivityOptions::class.java
                    .getDeclaredMethod("makeScaleUpDown", *params)
                    .apply { isAccessible = true }
            } catch (t: Throwable) {
                null
            }
            if (found == null) {
                found = try {
                    val forName = Class::class.java.getDeclaredMethod("forName", String::class.java)
                    val classArray: Class<*> = emptyArray<Class<*>>().javaClass
                    val getDeclared = Class::class.java.getDeclaredMethod(
                        "getDeclaredMethod", String::class.java, classArray,
                    )
                    @Suppress("UNCHECKED_CAST")
                    val cls = forName.invoke(null, "android.app.ActivityOptions") as Class<*>
                    (getDeclared.invoke(cls, "makeScaleUpDown", *params) as Method)
                        .apply { isAccessible = true }
                } catch (t: Throwable) {
                    Log.w(TAG, "找不到 makeScaleUpDown", t)
                    null
                }
            }
            method = found
            resolved = true
            return found
        }
    }
}
