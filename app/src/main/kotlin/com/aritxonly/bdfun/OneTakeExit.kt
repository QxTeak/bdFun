package com.aritxonly.bdfun

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.view.PixelCopy
import android.view.View
import android.view.ViewGroup
import android.view.animation.Interpolator
import android.view.animation.PathInterpolator

/**
 * 非小米机型「一镜到底」的取景交接。
 *
 * 打开视频时走的是系统公开的缩略图放大动画（[TeakOneTake.startPublic]），系统不提供反向退出；
 * 这里按系统同一组参数把它倒着做一遍：
 * - 时长 336ms（受系统「过渡动画时长」缩放）；
 * - 位移/缩放曲线 PathInterpolator(0.3, 0, 0.1, 1)；
 * - 后半程淡出，露出下面的真实卡片；
 * - 圆角随截图等比缩放，落点正好是起飞卡片。
 *
 * 视频页只负责在 finish 前把整屏截图交给这里；真正的动画画在**上一个 Activity** 上，
 * 因为只有它下面才是视频卡片，视频页自己的窗口是不透明的。
 */
object OneTakeExit {

    private const val EXTRA_ANCHOR = "com.aritxonly.bdfun.oneTake.exitAnchor"
    private const val EXTRA_SOURCE = "com.aritxonly.bdfun.oneTake.exitSource"
    private const val EXTRA_ROTATION = "com.aritxonly.bdfun.oneTake.exitRotation"
    private const val EXTRA_SCREEN = "com.aritxonly.bdfun.oneTake.exitScreen"

    /** 系统 TransitionAnimation 的缩略图转场固定时长。 */
    private const val BASE_DURATION_MS = 336L

    private val MOTION = PathInterpolator(0.3f, 0f, 0.1f, 1f)
    private val LINEAR_OUT_SLOW_IN = PathInterpolator(0f, 0f, 0.2f, 1f)

    /** 与系统 mThumbnailFadeOutInterpolator 等价：前半程不透明，后半程按 LinearOutSlowIn 淡出。 */
    private val FADE_OUT = Interpolator { t ->
        if (t < 0.5f) 0f else LINEAR_OUT_SLOW_IN.getInterpolation((t - 0.5f) / 0.5f)
    }

    private data class Pending(
        val bitmap: Bitmap,
        val anchor: Rect,
        val radiusPx: Float,
        val sourceClass: String,
        val atUptime: Long,
    )

    @Volatile
    private var pending: Pending? = null

    @Volatile
    private var installed = false

    /** 在进程启动时注册一次；源页面 resume 的瞬间接住视频页交出来的截图。 */
    @Synchronized
    fun install(context: Context) {
        if (installed) return
        installed = true
        val app = context.applicationContext as? Application ?: return
        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) {
                val frame = pending ?: return
                // 只让启动视频/动态的那个页面接帧：相关推荐会回到上一个 VideoActivity，
                // 动态详情会回到动态所在的 MainActivity，不能只认某一种 Activity 类型。
                if (activity.isFinishing) return
                if (activity.javaClass.name != frame.sourceClass) return
                if (SystemClock.uptimeMillis() - frame.atUptime > 1500L) {
                    pending = null
                    return
                }
                pending = null
                val decor = activity.window?.decorView as? ViewGroup ?: return
                play(activity, decor, frame)
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityStarted(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivityStopped(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
    }

    /** 打开视频时由 [TeakOneTake] 写入起飞点；只在公开缩略图路径下调用。 */
    fun putExtras(
        intent: Intent,
        activity: Activity,
        anchor: Rect,
    ) {
        val screen = activity.windowManager.currentWindowMetrics.bounds
        intent.putExtra(EXTRA_ANCHOR, intArrayOf(anchor.left, anchor.top, anchor.right, anchor.bottom))
        intent.putExtra(EXTRA_SOURCE, activity.javaClass.name)
        // 转屏后源页面的卡片位置已经变了，宁可不做也不要飞错地方。
        intent.putExtra(EXTRA_ROTATION, activity.display?.rotation ?: -1)
        intent.putExtra(EXTRA_SCREEN, intArrayOf(screen.width(), screen.height()))
    }

    /** 从启动 Intent 里取回退出所需的起飞信息；任何一项对不上都放弃。 */
    private data class ExitRequest(
        val anchor: Rect,
        val radiusPx: Float,
        val sourceClass: String,
    )

    private fun readRequest(activity: Activity): ExitRequest? {
        val intent = activity.intent ?: return null
        val values = intent.getIntArrayExtra(EXTRA_ANCHOR) ?: return null
        if (values.size != 4) return null
        if (intent.getIntExtra(EXTRA_ROTATION, -1) != (activity.display?.rotation ?: -1)) return null
        val screen = intent.getIntArrayExtra(EXTRA_SCREEN) ?: return null
        val bounds = activity.windowManager.currentWindowMetrics.bounds
        if (bounds.width() != screen[0] || bounds.height() != screen[1]) return null
        if (activity.isInPictureInPictureMode || activity.isChangingConfigurations) return null
        val sourceClass = intent.getStringExtra(EXTRA_SOURCE) ?: return null
        val decor = activity.window?.decorView ?: return null
        return ExitRequest(
            anchor = Rect(values[0], values[1], values[2], values[3]),
            // 与打开时 withDisplayCorners 的基准一致：设备圆角，随截图宽度等比缩到卡片。
            radiusPx = com.aritxonly.bdfun.ui.viewCornerRadiusPx(decor).toFloat(),
            sourceClass = sourceClass,
        )
    }

    /**
     * 在视频页 finish 前抓整屏并发布交接帧。
     * @return true 表示已经开始抓图，调用方必须等 [onReady] 再真正 finish。
     */
    fun begin(activity: Activity, onReady: () -> Unit): Boolean {
        val request = readRequest(activity) ?: return false
        val animatorScale = ValueAnimatorScale.current()
        val transitionScale = transitionAnimationScale(activity)
        // 系统关掉过渡动画时保持原来的瞬间关闭，别硬塞一个动画。
        if (animatorScale <= 0f || transitionScale <= 0f) return false

        val decor = activity.window?.decorView ?: return false
        if (decor.width <= 0 || decor.height <= 0) return false
        val bitmap = runCatching {
            Bitmap.createBitmap(decor.width, decor.height, Bitmap.Config.ARGB_8888)
        }.getOrNull() ?: return false

        val handler = Handler(Looper.getMainLooper())
        var finished = false

        fun deliver(frame: Pending?) {
            if (finished) return
            finished = true
            if (frame != null) {
                pending = frame
                suppressWindowTransition(activity)
            } else {
                bitmap.recycle()
            }
            onReady()
        }

        val frame = Pending(
            bitmap = bitmap,
            anchor = request.anchor,
            radiusPx = request.radiusPx,
            sourceClass = request.sourceClass,
            atUptime = SystemClock.uptimeMillis(),
        )

        try {
            PixelCopy.request(
                activity.window,
                bitmap,
                { result ->
                    if (result == PixelCopy.SUCCESS) deliver(frame) else deliver(null)
                },
                handler,
            )
        } catch (t: Throwable) {
            deliver(null)
        }
        // 截图回调异常时不把页面卡在这里。
        handler.postDelayed({ deliver(null) }, 200L)
        // 源页面没有回来（比如直接回到桌面）时别把整屏位图一直挂在进程里。
        handler.postDelayed({
            val current = pending
            if (current != null && current.bitmap === bitmap) {
                pending = null
                bitmap.recycle()
            }
        }, 2000L)
        return true
    }

    private fun suppressWindowTransition(activity: Activity) {
        // Android 14 起 overridePendingTransition 对 targetSdk 34+ 不再生效，
        // 必须走 overrideActivityTransition，否则视频页会先播一遍系统默认淡出，
        // 我们的截图动画被盖在下面，看起来就像只动了一半。
        if (android.os.Build.VERSION.SDK_INT >= 34) {
            runCatching {
                activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE, 0, 0)
            }
        } else {
            @Suppress("DEPRECATION")
            runCatching { activity.overridePendingTransition(0, 0) }
        }
    }

    private fun transitionAnimationScale(context: Context): Float = runCatching {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.TRANSITION_ANIMATION_SCALE,
            1f,
        )
    }.getOrDefault(1f)

    /** 把交接帧画在源页面上：整屏截图缩回卡片，后半程淡出露出真实卡片。 */
    private fun play(activity: Activity, decor: ViewGroup, frame: Pending) {
        if (decor.width <= 0 || decor.height <= 0 ||
            frame.bitmap.width != decor.width || frame.bitmap.height != decor.height
        ) {
            // 源页面尺寸已经变了（分屏/转屏），截图铺上去会变形，直接放弃。
            frame.bitmap.recycle()
            return
        }
        val location = IntArray(2)
        decor.getLocationOnScreen(location)
        val start = Rect(0, 0, decor.width, decor.height)
        val target = Rect(frame.anchor)
        target.offset(-location[0], -location[1])
        // 目标卡片可能被滚动出屏幕：给它一个最小尺寸，避免动画到 0 尺寸后突然消失。
        if (target.width() <= 0 || target.height() <= 0) {
            frame.bitmap.recycle()
            return
        }

        val animatorScale = ValueAnimatorScale.current().coerceAtLeast(0.01f)
        val transitionScale = transitionAnimationScale(activity)
        val duration = (BASE_DURATION_MS * transitionScale / animatorScale).toLong().coerceAtLeast(1L)

        val overlay = ExitOverlay(activity)
        overlay.isClickable = true
        overlay.isFocusable = true
        overlay.applyFrame(frame.bitmap, RectF(start), frame.radiusPx, start.width(), 1f)
        decor.addView(
            overlay,
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
        )

        val animator = android.animation.ValueAnimator.ofFloat(0f, 1f).apply {
            this.duration = duration
            interpolator = null
            addUpdateListener { animation ->
                val raw = animation.animatedFraction
                val motion = MOTION.getInterpolation(raw)
                val rect = RectF(
                    start.left + (target.left - start.left) * motion,
                    start.top + (target.top - start.top) * motion,
                    start.right + (target.right - start.right) * motion,
                    start.bottom + (target.bottom - start.bottom) * motion,
                )
                val alpha = 1f - FADE_OUT.getInterpolation(raw)
                overlay.applyFrame(frame.bitmap, rect, frame.radiusPx, start.width(), alpha)
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    (overlay.parent as? ViewGroup)?.removeView(overlay)
                    frame.bitmap.recycle()
                }
            })
            start()
        }
        // 防止页面在这 336ms 内被回收后动画继续跑；view 被移除时自动取消。
        overlay.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) = Unit
            override fun onViewDetachedFromWindow(v: View) {
                animator.cancel()
            }
        })
    }

    /** 直接按「当前矩形 + 圆角 + 透明度」绘制整屏截图，圆角随矩形宽度等比缩放。 */
    private class ExitOverlay(context: Context) : View(context) {

        private val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        private val clipPath = Path()
        private val drawRect = RectF()
        private var bitmap: Bitmap? = null
        private var radiusPx = 0f
        private var referenceWidth = 1

        init {
            setLayerType(LAYER_TYPE_HARDWARE, null)
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        }

        fun applyFrame(
            bitmap: Bitmap?,
            rect: RectF,
            radiusPx: Float,
            referenceWidth: Int,
            alpha: Float,
        ) {
            this.bitmap = bitmap
            this.radiusPx = radiusPx
            this.referenceWidth = referenceWidth.coerceAtLeast(1)
            drawRect.set(rect)
            this.alpha = alpha
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            val source = bitmap ?: return
            val scale = drawRect.width() / referenceWidth
            val radius = (radiusPx * scale).coerceAtLeast(0f)
            clipPath.reset()
            clipPath.addRoundRect(drawRect, radius, radius, Path.Direction.CW)
            val save = canvas.save()
            canvas.clipPath(clipPath)
            canvas.drawBitmap(source, null, drawRect, bitmapPaint)
            canvas.restoreToCount(save)
        }
    }

    /** 系统「Animator 时长缩放」；关掉动画时返回 0。 */
    private object ValueAnimatorScale {
        fun current(): Float = android.animation.ValueAnimator.getDurationScale()
    }
}
