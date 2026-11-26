package com.aritxonly.bdfun

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.aritxonly.bdfun.core.domain.isTabletDevice

/**
 * 所有应用内 Activity 的公共外壳。
 *
 * 手机禁止通过系统旋转进入横屏：非视频全屏页面一律锁竖屏，避免页面出现
 * 只有平板才适配好的横屏布局。平板（含展开折叠屏）保持系统自由旋转。
 *
 * 视频页在全屏时由播放器自己写横屏朝向，全屏退出后再回到这里定义的 idle 朝向，
 * 所以基类只负责“非全屏”的默认值。
 */
abstract class BdFunActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyIdleOrientation()
    }

    /** 手机锁竖屏；平板/展开折叠屏不干预，保留系统旋转。 */
    protected open fun applyIdleOrientation() {
        val smallestWidthDp = resources.configuration.smallestScreenWidthDp
        if (smallestWidthDp <= 0 || isTabletDevice(smallestWidthDp)) return
        if (requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }
}
