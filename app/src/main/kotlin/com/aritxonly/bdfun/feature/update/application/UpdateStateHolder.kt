package com.aritxonly.bdfun.feature.update.application

import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.domain.update.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UpdateUiState(val checking: Boolean = false, val checked: Boolean = false,
    val release: UpdateRelease? = null, val error: String? = null,
    val releases: List<UpdateRelease> = emptyList(), val currentRelease: UpdateRelease? = null,
    val history: Boolean = false)

class UpdateStateHolder(private val repository: UpdateRepository, private val current: AppVersion) {
    private val mutable = MutableStateFlow(UpdateUiState())
    val state = mutable.asStateFlow()
    fun showHistory(show: Boolean) { mutable.value = mutable.value.copy(history = show) }
    suspend fun check() {
        if (mutable.value.checking) return
        mutable.value = mutable.value.copy(checking = true, error = null)
        try {
            when (val result = repository.releases()) {
                is Outcome.Success -> mutable.value = mutable.value.copy(checking = false, checked = true,
                    release = latestRelease(result.value, current.name.startsWith("preview-"))?.takeIf { it.version.code > current.code },
                    releases = result.value.sortedByDescending { it.version.code },
                    currentRelease = result.value.firstOrNull { it.version.name == current.name })
                is Outcome.Failure -> mutable.value = mutable.value.copy(checking = false, checked = true,
                    error = "无法获取更新信息，请检查网络后重试")
            }
        } finally { mutable.value = mutable.value.copy(checking = false) }
    }
}
