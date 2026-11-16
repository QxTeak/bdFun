package com.aritxonly.bdfun.ui

import android.net.Uri
import androidx.compose.ui.focus.FocusRequester
import com.aritxonly.bdfun.feature.comment.application.CommentComposerUiState
import com.aritxonly.teak.common.material.GlassMaterialRecipe
import top.yukonga.miuix.kmp.blur.Backdrop

internal data class CommentComposerContent(
    val snapshot: CommentComposerUiState,
    val pendingPreview: Uri?,
    val replyName: String?,
    val focusRequester: FocusRequester,
)
internal data class CommentComposerMaterial(val recipe: GlassMaterialRecipe, val backdrop: Backdrop?)
