package com.aritxonly.bdfun.feature.video.application

import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.domain.*
import com.aritxonly.bdfun.core.model.video.DanmakuItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class DanmakuModerationState(
    val rules: List<DanmakuCloudRule> = emptyList(),
    val recalled: Set<Pair<Long, Long>> = emptySet(),
)

class DanmakuModerationStateHolder(private val repository: VideoMetadataRepository) {
    private val mutableState = MutableStateFlow(DanmakuModerationState())
    val state = mutableState.asStateFlow()
    private val ruleWrites = Mutex()
    private val pendingCommandSelections = mutableSetOf<Triple<Long, Long, Long>>()

    suspend fun specialDanmaku(aid: Long, cid: Long): SpecialDanmakuPlayback? =
        (repository.specialDanmaku(aid, cid) as? Outcome.Success)?.value

    suspend fun commands(aid: Long, cid: Long): List<com.aritxonly.bdfun.core.model.video.DanmakuCommand> =
        (repository.danmakuCommands(aid, cid) as? Outcome.Success)?.value.orEmpty()

    suspend fun voteCounts(voteId: Long): Map<Int, Long> =
        (repository.danmakuVoteCounts(voteId) as? Outcome.Success)?.value.orEmpty()

    suspend fun submitCommand(selection: DanmakuCommandSelection): Boolean {
        if (!selection.valid) return false
        val key = Triple(selection.aid, selection.cid, selection.command.id)
        if (!pendingCommandSelections.add(key)) return false
        return try {
            repository.submitDanmakuCommand(selection) is Outcome.Success
        } finally {
            pendingCommandSelections.remove(key)
        }
    }

    private val pendingRecalls = mutableSetOf<Pair<Long, Long>>()

    suspend fun syncConfig(config: DanmakuCloudConfig): Boolean = repository.syncDanmakuConfig(config) is Outcome.Success

    suspend fun loadRules(): Boolean = ruleWrites.withLock {
        val result = repository.danmakuRules() as? Outcome.Success ?: return@withLock false
        mutableState.update { it.copy(rules = result.value) }
        true
    }

    suspend fun addRule(kind: DanmakuRuleKind, value: String): Boolean = ruleWrites.withLock {
        val normalized = value.trim().let { if (kind == DanmakuRuleKind.Sender) it.lowercase() else it }
        if (normalized.isEmpty()) return@withLock false
        if (kind == DanmakuRuleKind.Pattern && runCatching { Regex(normalized) }.isFailure) return@withLock false
        if (state.value.rules.any { it.kind == kind && it.value == normalized }) return@withLock true
        val result = repository.addDanmakuRule(kind, normalized) as? Outcome.Success ?: return@withLock false
        mutableState.update { it.copy(rules = (it.rules + result.value).distinctBy { rule -> rule.id }) }
        true
    }

    suspend fun deleteRule(id: Long): Boolean = ruleWrites.withLock {
        if (repository.deleteDanmakuRule(id) !is Outcome.Success) return@withLock false
        mutableState.update { it.copy(rules = it.rules.filterNot { rule -> rule.id == id }) }
        true
    }

    suspend fun recall(cid: Long, item: DanmakuItem): Boolean {
        val key = cid to item.id
        if (!item.self || cid <= 0 || item.id <= 0 || !pendingRecalls.add(key)) return false
        try {
            if (repository.recallDanmaku(cid, item.id) !is Outcome.Success) return false
            mutableState.update { it.copy(recalled = it.recalled + key) }
            return true
        } finally {
            pendingRecalls.remove(key)
        }
    }
}
