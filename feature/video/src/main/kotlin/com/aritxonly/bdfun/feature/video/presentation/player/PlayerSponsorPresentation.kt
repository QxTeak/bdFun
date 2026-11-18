package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.aritxonly.bdfun.core.domain.SponsorSkipAction
import com.aritxonly.bdfun.core.domain.sponsorSkipAction
import com.aritxonly.bdfun.core.model.video.SponsorSegment
import com.aritxonly.bdfun.feature.video.application.PlaybackTrackingStateHolder

/** [prompt] 用于右下角提示条，[autoSkip] 用于进入片段后直接跳转；两者互斥。 */
data class PlayerSponsorPresentation(
    val prompt: SponsorSegment?,
    val autoSkip: SponsorSegment?,
    val dismiss: (SponsorSegment) -> Unit,
)

private class PlayerSponsorUiStateHolder {
    val dismissed = mutableStateOf<Set<String>>(emptySet())
    fun dismiss(segment: SponsorSegment) { dismissed.value = dismissed.value + segment.uuid }
}

@Composable
fun rememberPlayerSponsor(
    bvid: String, tracking: PlaybackTrackingStateHolder, observed: PlayerObservedStateHolder, sponsorSkipMode: () -> Int,
): PlayerSponsorPresentation {
    val state by tracking.state.collectAsState()
    val ui = remember(bvid) { PlayerSponsorUiStateHolder() }
    val dismissed by ui.dismissed
    val action = sponsorSkipAction(sponsorSkipMode())
    val enabled = action != SponsorSkipAction.Off
    LaunchedEffect(bvid, enabled) { tracking.loadSegments(bvid, enabled) }
    val positionMs by observed.positionMs
    val active = tracking.activeSegment(state, bvid, positionMs, dismissed).takeIf { enabled }
    return PlayerSponsorPresentation(
        prompt = active.takeIf { action == SponsorSkipAction.Prompt },
        autoSkip = active.takeIf { action == SponsorSkipAction.Auto },
        dismiss = ui::dismiss,
    )
}
