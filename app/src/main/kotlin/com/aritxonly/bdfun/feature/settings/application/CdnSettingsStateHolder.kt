package com.aritxonly.bdfun.feature.settings.application

import com.aritxonly.bdfun.core.domain.CdnSettingsSource
import com.aritxonly.bdfun.core.model.playback.CdnService
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal data class CdnSettingsUiState(val speeds: Map<String, Double>, val labels: List<String>)

/**
 * CDN 选项与测速状态。
 *
 * 测速只在用户主动打开 CDN 选择框时触发（[probe]），设置页打开本身不再探测：
 * 一次探测要下载十几个节点、每个节点两轮 1MB 样本，不能作为页面加载的副作用。
 */
internal class CdnSettingsStateHolder(private val source: CdnSettingsSource) {
    val services = source.services
    private val initial = source.speeds()
    private val mutable = MutableStateFlow(CdnSettingsUiState(initial, cdnMenuLabels(services, initial, source.currentServiceId())))
    val state = mutable.asStateFlow()
    private var closed = false
    private var loading = false
    private var probing = false
    fun close() { closed = true }
    fun selectedIndex(id: String) = services.indexOfFirst { it.id == id }.let { if (it < 0) 0 else it }

    /** 只读取当前已有的缓存分数，不发网络请求。 */
    suspend fun load() {
        if (closed || loading) return
        loading = true
        try { sample() } finally { loading = false }
    }

    /** 用户打开选择框时才测速；一小时内已有结果直接复用。 */
    suspend fun probe() {
        if (closed || probing) return
        probing = true
        try {
            if (!source.isFresh()) source.refresh(force = false)
            sample()
            while (source.isProbing()) { sample(); delay(400); checkActive() }
            sample()
        } finally { probing = false }
    }

    private suspend fun sample() {
        checkActive()
        val speeds = source.speeds()
        if (speeds != mutable.value.speeds) mutable.value = CdnSettingsUiState(speeds,
            cdnMenuLabels(services, speeds, source.currentServiceId()))
    }
    private suspend fun checkActive() {
        currentCoroutineContext().ensureActive()
        if (closed) throw kotlinx.coroutines.CancellationException("设置页已关闭")
    }
}

internal fun cdnMenuLabels(services: List<CdnService>, speeds: Map<String, Double>, currentId: String?): List<String> =
    services.map { service ->
        val speed = if (service.auto) currentId?.let(speeds::get) else speeds[service.id]
        if (speed != null && speed > 0.01) {
            "${service.label}  ${String.format(java.util.Locale.US, "%.1f", speed)} MB/s"
        } else service.label
    }
