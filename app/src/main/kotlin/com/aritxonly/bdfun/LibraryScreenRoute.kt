package com.aritxonly.bdfun

import com.aritxonly.bdfun.navigateToVideo
import com.aritxonly.bdfun.navigateToLogin
import android.content.Context
import androidx.compose.runtime.Composable
import com.aritxonly.bdfun.ui.LibraryScreen
import com.aritxonly.bdfun.ui.LibraryScreenRequest
import com.aritxonly.bdfun.ui.LibraryNavigation

@Composable
internal fun LibraryScreenRoute(
    context: Context, fixedMode: Int = 0, title: String = "收藏",
    searchOverlayHost: com.aritxonly.bdfun.ui.LibrarySearchOverlayHost? = null,
) {
    val owner = AppGraph.session.value?.mid
    LibraryScreen(
        LibraryScreenRequest(owner != null, owner, fixedMode, title, AppGraph.session.identity.revision),
        LibraryNavigation({ navigateToLogin(context) }, { (context as? android.app.Activity)?.finish() },
            { navigateToVideo(context, it) }),
        AppGraph.libraryDependencies,
        searchOverlayHost = searchOverlayHost,
    )
}
