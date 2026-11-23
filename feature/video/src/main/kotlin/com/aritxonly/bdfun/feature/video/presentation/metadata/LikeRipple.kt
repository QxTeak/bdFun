package com.aritxonly.bdfun.ui

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.View
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * 一键三连的流体水波纹。
 *
 * 按下时抓一次**整页**画面，之后由 RuntimeShader 采样这张位图。
 * 按住期间一圈接一圈往外荡，三连完成时再放更大的几圈；所有波峰都带尾部淡出。
 * 采样范围比按钮本身大很多（168dp），边缘有较长渐隐，避免波纹传到边上突然断掉。
 */
@Composable
internal fun LikeRipple(
    holding: Boolean,
    burst: Int,
    modifier: Modifier = Modifier,
    activity: android.app.Activity?,
) {
    val context = LocalContext.current
    var view by remember { mutableStateOf<LikeRippleView?>(null) }
    var rect by remember { mutableStateOf<Rect?>(null) }
    var lastBurst by remember { mutableStateOf(0) }

    AndroidView(
        modifier = modifier
            .requiredSize(168.dp)
            .onGloballyPositioned { coords ->
                val b = coords.boundsInWindow()
                rect = Rect(
                    b.left.roundToInt(),
                    b.top.roundToInt(),
                    b.right.roundToInt(),
                    b.bottom.roundToInt(),
                )
            },
        factory = { ctx -> LikeRippleView(ctx).also { view = it } },
        update = {
            it.setHolding(holding)
            if (burst != lastBurst) {
                lastBurst = burst
                if (burst > 0) it.burst()
            }
        },
    )

    LaunchedEffect(holding, rect) {
        if (!holding) return@LaunchedEffect
        val r = rect ?: return@LaunchedEffect
        val act = activity ?: return@LaunchedEffect
        val v = view ?: return@LaunchedEffect
        val window = act.window ?: return@LaunchedEffect
        val decor = window.decorView
        val width = decor.width
        val height = decor.height
        if (width <= 0 || height <= 0 || r.width() <= 0 || r.height() <= 0) {
            return@LaunchedEffect
        }
        // 整页截图：波纹折射的是整个页面。
        val bitmap = v.obtainContentBitmap(width, height)
        // 视图在窗口里的位置，着色器用它把“视图坐标”换算成“整页截图坐标”。
        v.setContentOrigin(r.left, r.top)
        // 抓帧时先隐藏自己，避免拍到上一轮波纹。
        v.visibility = View.INVISIBLE
        PixelCopy.request(
            window,
            Rect(0, 0, width, height),
            bitmap,
            { result ->
                if (result == PixelCopy.SUCCESS && !bitmap.isRecycled) {
                    v.setBitmap(bitmap)
                }
                v.visibility = View.VISIBLE
            },
            Handler(Looper.getMainLooper()),
        )
    }
}

internal class LikeRippleView(context: android.content.Context) : View(context) {

    private data class Crest(
        val bornNs: Long,
        val durationMs: Float,
        val maxRadiusPx: Float,
        val strength: Float,
    )

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var bitmap: Bitmap? = null
    private var contentBitmap: Bitmap? = null
    private var contentOriginX = 0f
    private var contentOriginY = 0f
    private var holding = false
    private val crests = ArrayList<Crest>()

    private val spawnRunnable = object : Runnable {
        override fun run() {
            if (!holding) return
            crests.add(
                Crest(
                    bornNs = System.nanoTime(),
                    durationMs = 900f,
                    // 每一圈真正荡出去：半径到视图的 46%。
                    maxRadiusPx = width.coerceAtLeast(1) * 0.46f,
                    strength = 0.55f,
                ),
            )
            while (crests.size > 8) crests.removeAt(0)
            invalidate()
            postDelayed(this, 320L)
        }
    }

    fun obtainContentBitmap(width: Int, height: Int): Bitmap {
        val cached = contentBitmap
        if (cached != null && !cached.isRecycled &&
            cached.width == width && cached.height == height
        ) {
            return cached
        }
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            .also { contentBitmap = it }
    }

    fun setContentOrigin(x: Int, y: Int) {
        contentOriginX = x.toFloat()
        contentOriginY = y.toFloat()
    }

    fun setBitmap(value: Bitmap?) {
        bitmap = value
        invalidate()
        postInvalidateOnAnimation()
    }

    fun setHolding(value: Boolean) {
        if (holding == value) return
        holding = value
        removeCallbacks(spawnRunnable)
        if (value) {
            crests.clear()
            spawnRunnable.run()
        } else {
            invalidate()
        }
    }

    fun burst() {
        val now = System.nanoTime()
        val maxR = width.coerceAtLeast(1) * 0.50f
        repeat(4) { index ->
            crests.add(
                Crest(
                    bornNs = now + index * 140_000_000L,
                    durationMs = 1050f,
                    maxRadiusPx = maxR,
                    strength = 1f,
                ),
            )
        }
        while (crests.size > 8) crests.removeAt(0)
        invalidate()
        postInvalidateOnAnimation()
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(spawnRunnable)
        // 页面关掉就把整屏截图放掉；下次进入会重新抓，不能让它跟着进程常驻。
        contentBitmap?.takeIf { !it.isRecycled }?.recycle()
        contentBitmap = null
        bitmap?.takeIf { !it.isRecycled }?.recycle()
        bitmap = null
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val now = System.nanoTime()
        crests.removeAll { (now - it.bornNs) / 1_000_000f > it.durationMs }
        val bmp = bitmap
        if (bmp != null && !bmp.isRecycled && crests.isNotEmpty() &&
            Build.VERSION.SDK_INT >= 33
        ) {
            val active = crests.takeLast(8)
            val shader = try {
                RuntimeShader(SHADER)
            } catch (_: Throwable) {
                null
            }
            if (shader != null) {
                val density = resources.displayMetrics.density
                shader.setInputShader(
                    "content",
                    BitmapShader(bmp, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP),
                )
                shader.setFloatUniform("uSize", width.toFloat(), height.toFloat())
                shader.setFloatUniform("uCenter", width / 2f, height / 2f)
                shader.setFloatUniform("uRing", 18f * density)
                shader.setFloatUniform("uRefract", 16f * density)
                shader.setFloatUniform("uDispersion", 2.0f * density)
                shader.setFloatUniform("uHighlight", 0.5f)
                shader.setFloatUniform("uSwirl", 0.45f)
                shader.setFloatUniform("uGain", 1f)
                shader.setFloatUniform("uHasContent", 1f)
                // 边缘渐隐拉长：波纹接近采样区边缘时慢慢消失，不会有生硬的直线断口。
                shader.setFloatUniform("uEdge", 40f * density)
                shader.setFloatUniform("uGhostRim", 0.6f * density)
                shader.setFloatUniform("uContentOrigin", contentOriginX, contentOriginY)
                shader.setFloatUniform("uContentScale", 1f, 1f)
                for (i in 0..7) {
                    val name = "uC$i"
                    val c = active.getOrNull(i)
                    if (c == null) {
                        shader.setFloatUniform(name, 0f, 0f, 1f, 0f)
                    } else {
                        val age = ((now - c.bornNs) / 1_000_000f).coerceAtLeast(0f)
                        val t = (age / c.durationMs).coerceIn(0f, 1f)
                        val ease = 1f - (1f - t) * (1f - t)
                        val radius = c.maxRadiusPx * ease
                        // 尾部额外衰减，保证所有波纹都是淡出。
                        val envelope = sin(PI.toFloat() * t) * c.strength *
                            (1f - t * t * t * t)
                        val band = 22f * density * (0.4f + 0.6f * t)
                        shader.setFloatUniform(name, radius, envelope, band, 0f)
                    }
                }
                paint.shader = shader
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            }
        }
        if (holding || crests.isNotEmpty()) postInvalidateOnAnimation()
    }

    private companion object {
        val SHADER = """
            uniform shader content;
            uniform float2 uSize;
            uniform float2 uCenter;
            uniform float  uRing;
            uniform float  uRefract;
            uniform float  uDispersion;
            uniform float  uHighlight;
            uniform float  uSwirl;
            uniform float  uGain;
            uniform float  uHasContent;
            uniform float  uEdge;
            uniform float  uGhostRim;
            uniform float2 uContentOrigin;
            uniform float2 uContentScale;
            uniform float4 uC0;
            uniform float4 uC1;
            uniform float4 uC2;
            uniform float4 uC3;
            uniform float4 uC4;
            uniform float4 uC5;
            uniform float4 uC6;
            uniform float4 uC7;

            half4 main(float2 p) {
                float2 rel = p - uCenter;
                float d = length(rel);
                float2 dir = d > 0.0001 ? rel / d : float2(0.0, 0.0);
                float band = max(uRing, 0.001);
                float env = 0.0;
                float slope = 0.0;
                if (uC0.y > 0.0) {
                    float x = (d - uC0.x) / max(uC0.z, 1.0);
                    float g = exp(-x * x);
                    env += g * uC0.y;
                    slope += min(abs(-2.0 * x * g) * uC0.y, 1.0);
                }
                if (uC1.y > 0.0) {
                    float x = (d - uC1.x) / max(uC1.z, 1.0);
                    float g = exp(-x * x);
                    env += g * uC1.y;
                    slope += min(abs(-2.0 * x * g) * uC1.y, 1.0);
                }
                if (uC2.y > 0.0) {
                    float x = (d - uC2.x) / max(uC2.z, 1.0);
                    float g = exp(-x * x);
                    env += g * uC2.y;
                    slope += min(abs(-2.0 * x * g) * uC2.y, 1.0);
                }
                if (uC3.y > 0.0) {
                    float x = (d - uC3.x) / max(uC3.z, 1.0);
                    float g = exp(-x * x);
                    env += g * uC3.y;
                    slope += min(abs(-2.0 * x * g) * uC3.y, 1.0);
                }
                if (uC4.y > 0.0) {
                    float x = (d - uC4.x) / max(uC4.z, 1.0);
                    float g = exp(-x * x);
                    env += g * uC4.y;
                    slope += min(abs(-2.0 * x * g) * uC4.y, 1.0);
                }
                if (uC5.y > 0.0) {
                    float x = (d - uC5.x) / max(uC5.z, 1.0);
                    float g = exp(-x * x);
                    env += g * uC5.y;
                    slope += min(abs(-2.0 * x * g) * uC5.y, 1.0);
                }
                if (uC6.y > 0.0) {
                    float x = (d - uC6.x) / max(uC6.z, 1.0);
                    float g = exp(-x * x);
                    env += g * uC6.y;
                    slope += min(abs(-2.0 * x * g) * uC6.y, 1.0);
                }
                if (uC7.y > 0.0) {
                    float x = (d - uC7.x) / max(uC7.z, 1.0);
                    float g = exp(-x * x);
                    env += g * uC7.y;
                    slope += min(abs(-2.0 * x * g) * uC7.y, 1.0);
                }
                env = max(env, 0.0);
                slope = min(slope, 1.0);
                float centerFade = clamp(d / band, 0.0, 1.0);
                float wobble1 = sin(dir.x * 7.0 + d * 0.03) * cos(dir.y * 7.0 - d * 0.03);
                float wobble2 = sin((dir.x + dir.y) * 4.0 - d * 0.045);
                env = max(env * (1.0 + uSwirl * centerFade
                    * (0.7 * wobble1 + 0.3 * wobble2)), 0.0);
                float2 tangent = float2(-dir.y, dir.x);
                float2 disp = dir * (uRefract * env)
                    + tangent * (uRefract * uSwirl * 0.55 * env);
                float2 uv = p + disp;
                float2 texCoord = (uv + uContentOrigin) * uContentScale;
                float k = uDispersion * env;
                half4 cr = content.eval(texCoord + dir * k);
                half4 cg = content.eval(texCoord);
                half4 cb = content.eval(texCoord - dir * k);
                half3 rgb = half3(cr.r, cg.g, cb.b) * half(uGain);
                rgb *= half(1.0 - 0.32 * min(env, 1.0));
                float spec = (0.45 + 0.55 * slope) * mix(0.45, 1.0, centerFade);
                half3 tint = half3(0.58, 0.76, 1.0);
                rgb += tint * half(uHighlight * min(env, 1.0) * spec);
                float edgeDistance = min(min(p.x, uSize.x - p.x), min(p.y, uSize.y - p.y));
                float edgeMask = clamp(edgeDistance / max(uEdge, 1.0), 0.0, 1.0);
                // 只覆盖波带本身，并让波带内部完全盖住原画面，只有很窄的一圈羽化。
                // 这样不会出现“整块截图”带来的冻结感和多重影。
                float coverage = clamp(env * 1.6, 0.0, 1.0);
                float mask = smoothstep(0.06, 0.16, coverage) * uHasContent * edgeMask;
                return half4(rgb * mask, mask);
            }
        """.trimIndent()
    }
}
