/*
 * Copyright (C) 2022 ByteDance Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bytedance.danmaku.render.engine.render.layer.scroll

import android.graphics.Canvas
import android.view.MotionEvent
import com.bytedance.danmaku.render.engine.control.ConfigChangeListener
import com.bytedance.danmaku.render.engine.control.DanmakuConfig
import com.bytedance.danmaku.render.engine.control.DanmakuController
import com.bytedance.danmaku.render.engine.control.Events
import com.bytedance.danmaku.render.engine.data.DanmakuData
import com.bytedance.danmaku.render.engine.render.IRenderLayer
import com.bytedance.danmaku.render.engine.render.cache.IDrawCachePool
import com.bytedance.danmaku.render.engine.render.cache.LayerBuffer
import com.bytedance.danmaku.render.engine.render.draw.DrawItem
import com.bytedance.danmaku.render.engine.touch.ITouchDelegate
import com.bytedance.danmaku.render.engine.touch.ITouchTarget
import com.bytedance.danmaku.render.engine.utils.EVENT_DANMAKU_DISMISS
import com.bytedance.danmaku.render.engine.utils.EVENT_DANMAKU_SHOW
import com.bytedance.danmaku.render.engine.utils.LAYER_TYPE_SCROLL
import com.bytedance.danmaku.render.engine.utils.LAYER_Z_INDEX_SCROLL
import java.util.*

/**
 * Created by dss886 on 2018/11/8.
 */
class ScrollLayer : IRenderLayer, ITouchDelegate, ConfigChangeListener {

    private lateinit var mController: DanmakuController
    private lateinit var mCachePool: IDrawCachePool
    private lateinit var mBuffer: LayerBuffer
    private lateinit var mConfig: DanmakuConfig
    private val mLines = LinkedList<ScrollLine>()
    private val mPreDrawItems = ArrayList<DrawItem<DanmakuData>>()
    private var mTotalDanmakuCountInLayer = 0
    private var mWidth = 0
    private var mHeight = 0

    override fun init(controller: DanmakuController, cachePool: IDrawCachePool) {
        mController = controller
        mCachePool = cachePool
        mConfig = mController.config
        mBuffer = LayerBuffer(mConfig, mCachePool, mConfig.scroll.bufferSize, mConfig.scroll.bufferMaxTime)
        mConfig.addListener(this)
    }

    override fun getLayerType(): Int {
        return LAYER_TYPE_SCROLL
    }

    override fun getLayerZIndex(): Int {
        return LAYER_Z_INDEX_SCROLL
    }

    override fun onLayoutSizeChanged(width: Int, height: Int) {
        mWidth = width
        mHeight = height
        configLines()
    }

    override fun addItems(playTime: Long, list: List<DrawItem<DanmakuData>>) {
        mBuffer.addItems(list)
        mBuffer.trimBuffer(playTime)
    }

    override fun addData(
        playTime: Long,
        list: List<DanmakuData>,
        createItem: (DanmakuData) -> DrawItem<DanmakuData>,
    ) = mBuffer.addData(playTime, list, createItem)

    override fun releaseItem(item: DrawItem<DanmakuData>) {
        mController.notifyEvent(Events.obtainEvent(EVENT_DANMAKU_DISMISS, item.data))
        mCachePool.release(item)
    }

    override fun typesetting(playTime: Long, isPlaying: Boolean, configChanged: Boolean): Int {
        mBuffer.forEach {
            distributeItemToLines(playTime, it)
        }
        mTotalDanmakuCountInLayer = 0
        mLines.forEach { line ->
            mTotalDanmakuCountInLayer += line.typesetting(playTime, isPlaying, configChanged)
        }
        // 顶部让位变化后，行一旦空出来就移到新位置；有弹幕在屏上的行保持不动。
        repositionIdleLines()
        if (configChanged) {
            mBuffer.measureItems()
        }
        return mTotalDanmakuCountInLayer
    }

    private fun targetY(index: Int): Float =
        mConfig.scroll.marginTop + index * (mConfig.scroll.lineMargin + mConfig.scroll.lineHeight)

    private fun repositionIdleLines() {
        mLines.forEachIndexed { index, line ->
            if (line.itemCount > 0) return@forEachIndexed
            val target = targetY(index)
            if (kotlin.math.abs(line.y - target) > 0.5f) {
                line.onLayoutChanged(mWidth.toFloat(), mConfig.scroll.lineHeight, 0F, target)
            }
        }
    }

    override fun drawBounds(canvas: Canvas) {
        mLines.forEach { line ->
            line.drawBounds(canvas)
        }
    }

    override fun getPreDrawItems(): List<DrawItem<DanmakuData>> {
        mPreDrawItems.clear()
        mLines.forEach { line ->
            mPreDrawItems.addAll(line.getPreDrawItems())
        }
        return mPreDrawItems
    }

    override fun clear() {
        mLines.forEach { line ->
            line.clearRender()
        }
        mBuffer.clear()
    }

    override fun findTouchTarget(event: MotionEvent): ITouchTarget? {
        mLines.forEach { line ->
            if (event.y > line.y + line.height) {
                return@forEach
            }
            if (event.y >= line.y && line.onTouchEvent(event)) {
                return line
            }
            return null
        }
        return null
    }

    override fun onConfigChanged(type: Int) {
        when (type) {
            DanmakuConfig.TYPE_SCROLL_LINE_HEIGHT,
            DanmakuConfig.TYPE_SCROLL_LINE_COUNT,
            DanmakuConfig.TYPE_SCROLL_LINE_MARGIN -> configLines()
            // 顶部让位（弹幕计数出现/消失）只挪空行：已经在屏上的弹幕保持原位，
            // 之后新刷出来的弹幕才从新位置出发。
            DanmakuConfig.TYPE_SCROLL_MARGIN_TOP -> configLines(idleOnly = true)
            DanmakuConfig.TYPE_SCROLL_BUFFER_MAX_TIME,
            DanmakuConfig.TYPE_SCROLL_BUFFER_SIZE -> {
                mBuffer.onBufferChanged(mConfig.scroll.bufferSize, mConfig.scroll.bufferMaxTime)
            }
        }
    }

    /**
     * Try add item to lines.
     * Return true if find a line has enough space to add, return false otherwise.
     */
    private fun distributeItemToLines(playTime: Long, item: DrawItem<DanmakuData>): Boolean {
        val guard = mConfig.scroll.marginTop
        mLines.forEach { line ->
            // 还停在顶部让位区域里的旧行不接收新弹幕，否则新弹幕会盖到计数上。
            if (line.y + 0.5f < guard) return@forEach
            if (line.addItem(playTime, item)) {
                mController.notifyEvent(Events.obtainEvent(EVENT_DANMAKU_SHOW, item.data))
                return true
            }
        }
        return false
    }

    private fun configLines(idleOnly: Boolean = false) {
        val lineCount = mConfig.scroll.lineCount
        val lineHeight = mConfig.scroll.lineHeight
        val lineSpace = mConfig.scroll.lineMargin
        val marginTop = mConfig.scroll.marginTop
        if (!idleOnly) {
            if (lineCount > mLines.size) {
                for (i in 1..(lineCount - mLines.size)) {
                    mLines.add(ScrollLine(mController, this).apply {
                        mController.registerCmdMonitor(this)
                    })
                }
            } else if (lineCount < mLines.size) {
                for (i in 1..(mLines.size - lineCount)) {
                    mLines.removeAt(mLines.size - 1).let { line ->
                        line.clearRender()
                        mController.unRegisterCmdMonitor(line)
                    }
                }
            }
        }
        mLines.forEachIndexed { index, line ->
            if (idleOnly && line.itemCount > 0) return@forEachIndexed
            line.onLayoutChanged(mWidth.toFloat(), lineHeight, 0F, marginTop + index * (lineSpace + lineHeight))
        }
    }

}
