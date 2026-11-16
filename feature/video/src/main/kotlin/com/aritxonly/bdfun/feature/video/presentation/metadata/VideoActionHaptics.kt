package com.aritxonly.bdfun.ui


internal class VideoActionHaptics(
    private val context: android.content.Context,
    private val enabled: Boolean = true,
) {
    fun defaultVibrator(): android.os.Vibrator? {
        if (!enabled) return null
        val vibrator = if (android.os.Build.VERSION.SDK_INT >= 31) {
            (context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE)
                as? android.os.VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? android.os.Vibrator
        }
        return vibrator?.takeIf { it.hasVibrator() }
    }
    /** 按住期间脉冲式的轻震动，和小水波同一个节奏（170ms 一下）。 */
    fun pulseVibrate() {
        val vibrator = defaultVibrator() ?: return
        runCatching {
            vibrator.vibrate(
                android.os.VibrationEffect.createOneShot(
                    26L,
                    72,
                ),
            )
        }
    }
    fun strongVibrate() {
        val vibrator = defaultVibrator() ?: return
        vibrator.cancel()
        // 一下大的长震，而不是连续两下
        vibrator.vibrate(
            android.os.VibrationEffect.createOneShot(
                420,
                255,
            ),
        )
    }
}
