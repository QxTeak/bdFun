package com.aritxonly.bdfun.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Interpolator

/**
 * Telegram 式的一次性位图形变层。
 *
 * 只做一件事：用一张已经在内存里的缩略图 bitmap，按 [photoMorphFrame] 的
 * 「裁剪框 + 图片倍率」从卡片矩形展开到全屏，或从全屏缩回卡片。
 * 所有帧只改 Canvas 的 clip/matrix，不触发 Compose 组合、不加载图片。
 */
internal class PhotoMorphView(context: Context) : View(context) {

    private val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    private val clipPath = Path()
    private val imageRect = RectF()
    private val srcRect = Rect()

    private var bitmap: Bitmap? = null
    private var origin = RectF()
    private var originRadius = 0f
    private var imageAspect = 1f
    private var progress = 0f
    private var startOffsetY = 0f
    private var startDim = 1f
    private var animator: ValueAnimator? = null
    private var pendingStart: (() -> Unit)? = null

    var onProgress: ((Float) -> Unit)? = null
    var onFinished: (() -> Unit)? = null
    private var fadeEndAction: (() -> Unit)? = null

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        isClickable = false
    }

    fun start(
        bitmap: Bitmap,
        origin: Rect,
        originRadiusPx: Float,
        open: Boolean,
        startFrom: Float = if (open) 0f else 1f,
        durationMs: Long = 200L,
        /** 跟手拖动后再关闭：起始帧要带上这段位移，否则会从手指位置"跳"回屏幕中心。 */
        dragOffsetY: Float = 0f,
        /** 关闭时黑底从拖动后的透明度接着淡出，避免先闪回全黑。 */
        startDim: Float = 1f,
    ) {
        animator?.cancel()
        animate().cancel()
        alpha = 1f
        fadeEndAction = null
        this.bitmap = bitmap
        this.origin.set(origin)
        this.originRadius = originRadiusPx
        this.startOffsetY = dragOffsetY
        this.startDim = startDim.coerceIn(0f, 1f)
        this.imageAspect = if (bitmap.height > 0) bitmap.width.toFloat() / bitmap.height else 1f
        progress = startFrom
        invalidate()
        val begin = {
            val target = if (open) 1f else 0f
            animator = ValueAnimator.ofFloat(startFrom, target).apply {
                duration = durationMs
                interpolator = INTERPOLATOR
                addUpdateListener {
                    progress = it.animatedValue as Float
                    onProgress?.invoke(progress)
                    invalidate()
                }
                addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        onFinished?.invoke()
                    }
                })
                start()
            }
            pendingStart = null
        }
        // AndroidView 的 factory 里 View 还没有测量，直接 start 可能整段动画在首次绘制前就跑完，
        // 看起来就是"没有动画"。等第一次有尺寸再开始。
        if (width > 0 && height > 0) {
            begin()
        } else {
            pendingStart = begin
            post { pendingStart?.invoke() }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        pendingStart?.invoke()
    }

    /** 原图已经画出来后，淡出盖层再交给 Compose，避免中间闪一帧空。 */
    fun fadeOut(durationMs: Long = 120L, onEnd: () -> Unit) {
        if (fadeEndAction != null) return
        fadeEndAction = onEnd
        animate()
            .alpha(0f)
            .setDuration(durationMs)
            .withEndAction {
                val end = fadeEndAction
                fadeEndAction = null
                end?.invoke()
            }
            .start()
    }

    fun cancel() {
        animator?.cancel()
        animator = null
    }

    override fun onDetachedFromWindow() {
        cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        val source = bitmap ?: return
        val width = width.toFloat()
        val height = height.toFloat()
        if (width <= 0f || height <= 0f) return

        // 黑底随进度淡入/淡出；关闭时从拖动后的透明度接着走，避免瞬间闪回全黑。
        val dim = (progress.coerceIn(0f, 1f) * startDim * 255f).toInt().coerceIn(0, 255)
        if (dim > 0) canvas.drawColor(Color.argb(dim, 0, 0, 0))

        val morph = photoMorphFrame(
            progress = progress.coerceIn(0f, 1f),
            originX = origin.left,
            originY = origin.top,
            originWidth = origin.width(),
            originHeight = origin.height(),
            screenWidth = width,
            screenHeight = height,
            imageAspect = imageAspect,
            originRadius = originRadius,
        )
        clipPath.reset()
        // 关闭动画从手指拖到的位置开始：位移随进度线性收回，末帧正好落回缩略图。
        val dragOffset = startOffsetY * progress.coerceIn(0f, 1f)
        clipPath.addRoundRect(
            RectF(
                morph.clipLeft,
                morph.clipTop + dragOffset,
                morph.clipLeft + morph.clipWidth,
                morph.clipTop + morph.clipHeight + dragOffset,
            ),
            morph.cornerRadius,
            morph.cornerRadius,
            Path.Direction.CW,
        )
        val save = canvas.save()
        canvas.clipPath(clipPath)
        val drawWidth = morph.imageWidth * morph.imageScale
        val drawHeight = morph.imageHeight * morph.imageScale
        imageRect.set(
            morph.imageCenterX - drawWidth / 2f,
            morph.imageCenterY - drawHeight / 2f + dragOffset,
            morph.imageCenterX + drawWidth / 2f,
            morph.imageCenterY + drawHeight / 2f + dragOffset,
        )
        srcRect.set(0, 0, source.width, source.height)
        canvas.drawBitmap(source, srcRect, imageRect, paint)
        canvas.restoreToCount(save)
    }

    private companion object {
        val INTERPOLATOR: Interpolator = AccelerateDecelerateInterpolator()
    }
}
