package com.aritxonly.bdfun.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.aritxonly.teak.common.chrome.TeakMotion
import kotlinx.coroutines.delay

/**
 * 通用的「数值补间」：target 变了就从**当前值**重新起跑。
 *
 * 这一点很关键：输入框上弹时如果中途点空白（要缩回），或者中途又换了目标，
 * 动画不会跳回起点再重来，而是接着当前位置滑向新目标，也就是"打断动画"。
 */
@Composable
fun rememberSmoothFloat(
    target: Float,
    durationMs: Float = 240f,
    easing: Easing = EaseOutQuint,
): Float {
    var value by remember { mutableFloatStateOf(target) }
    LaunchedEffect(target, durationMs) {
        val from = value
        if (from == target) return@LaunchedEffect
        if (!TeakMotion.enabled) {
            value = target
            return@LaunchedEffect
        }
        if (durationMs <= 0f) {
            value = target
            return@LaunchedEffect
        }
        val startNanos = System.nanoTime()
        while (true) {
            val p = ((System.nanoTime() - startNanos) / 1_000_000f / durationMs).coerceIn(0f, 1f)
            value = from + (target - from) * easing.transform(p)
            if (p >= 1f) break
            delay(8)
        }
        value = target
    }
    return value
}

/**
 * 真弹簧：用简单的阻尼谐振子逐帧积分，有真实的过冲与回弹。
 * target 变化时从当前值和当前速度继续，所以中途打断也连贯。
 */
@Composable
fun rememberSpringFloat(
    target: Float,
    stiffness: Float = 320f,
    damping: Float = 22f,
    mass: Float = 1f,
): Float {
    var value by remember { mutableFloatStateOf(target) }
    var velocity by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(target) {
        if (!TeakMotion.enabled) {
            value = target
            velocity = 0f
            return@LaunchedEffect
        }
        var last = System.nanoTime()
        while (true) {
            val now = System.nanoTime()
            val dt = ((now - last) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
            last = now
            val accel = (-stiffness * (value - target) - damping * velocity) / mass
            velocity += accel * dt
            value += velocity * dt
            if (kotlin.math.abs(value - target) < 0.0006f &&
                kotlin.math.abs(velocity) < 0.006f
            ) {
                value = target
                velocity = 0f
                break
            }
            delay(8)
        }
    }
    return value
}

/** 标准减速（ease-out-quint），无回弹，适合位移。 */
val EaseOutQuint: Easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

/**
 * 柔和减速：起手比 quint 缓一点，收尾更绵长，适合大面积面板的掀开/落回。
 * 仍然是先快后慢的非线性曲线，只是少了 quint 那种"猛地一冲"的观感。
 */
val EaseOutSoft: Easing = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)

/** 果冻：终点冲过头约 12%，再回收。 */
val JellyEasing: Easing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)

fun Context.showToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

fun Context.copyToClipboard(label: String, text: String) {
    val manager = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    manager.setPrimaryClip(ClipData.newPlainText(label, text))
}

