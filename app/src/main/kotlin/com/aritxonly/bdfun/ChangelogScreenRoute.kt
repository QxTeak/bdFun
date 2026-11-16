package com.aritxonly.bdfun

import androidx.compose.runtime.*
import com.aritxonly.bdfun.core.domain.update.AppVersion
import com.aritxonly.bdfun.feature.update.application.UpdateStateHolder
import com.aritxonly.bdfun.infra.bili.update.GithubUpdateRepository
import com.aritxonly.bdfun.ui.ChangelogContent
import kotlinx.coroutines.launch

@Composable
internal fun ChangelogScreenRoute() {
    val controller = remember { UpdateStateHolder(GithubUpdateRepository(),
        AppVersion(BuildConfig.VERSION_NAME.removeSuffix("-debug"), BuildConfig.VERSION_CODE)) }
    val state by controller.state.collectAsState()
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { controller.check() }
    ChangelogContent(state.releases, state.checking, state.error) { scope.launch { controller.check() } }
}
