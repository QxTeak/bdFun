package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable

@Composable
internal fun videoDetailScreenPorts(): VideoDetailScreenPorts {
    val page = LocalVideoPageState.current
    return VideoDetailScreenPorts(rememberVideoDetailStatePorts(), rememberVideoCollapseHost(), page.automaticQuality,
        publishBvid = { page.currentBvid = it }, load = rememberVideoDetailLoadPorts(),
        effects = videoDetailEffectPorts(), actionHost = videoDetailActionHost(),
        slots = VideoDetailScreenSlots(
            layout = { frame ->
                VideoScreenLayout(frame.request, frame.presentation, frame.events, frame.pager, frame.inlineLayout,
                    frame.holders, frame.content, frame.actions, frame.scope)
            },
            sheets = { ui, interaction, actions -> VideoDetailSheets(ui, interaction, actions) },
        ),
        localCollectionFor = { bvid ->
            val local = page.dependencies.localCollection.forBvid(bvid)
            local.items to local.title
        },
    )
}
