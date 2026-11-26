package com.aritxonly.bdfun


/**
 * 需要「非小米退出动画」的 Activity 基类。
 *
 * 视频页、动态详情页的退出流程完全一致：先抓一帧交给源页面，再真正 finish。
 * 抽到这里，避免每个页面各抄一份 finish 守卫（漏一处就会出现有的页面能收、有的不能收）。
 * 小米机型或没有起飞点时 [OneTakeExit.begin] 返回 false，保持系统原行为。
 */
abstract class OneTakeExitActivity : BdFunActivity() {

    private var handoffInFlight = false

    override fun finish() {
        if (isFinishing) {
            finishNow()
            return
        }
        if (handoffInFlight) return
        handoffInFlight = true
        val started = OneTakeExit.begin(this) {
            handoffInFlight = false
            finishNow()
        }
        if (!started) {
            handoffInFlight = false
            finishNow()
        }
    }

    private fun finishNow() {
        super.finish()
    }
}
