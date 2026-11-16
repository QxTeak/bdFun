package com.aritxonly.bdfun.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.aritxonly.teak.common.material.*
import kotlinx.coroutines.launch

@Composable
fun CommentReplyBar(modifier: Modifier = Modifier) {
    val page = LocalVideoPageState.current
    val runtime = rememberCommentComposerRuntime(page)
    val controller = runtime.controller
    val snapshot by key(controller) { controller.state.collectAsState() }
    val presentation = remember(controller) { CommentComposerPresentationStateHolder() }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val imagePicker = key(controller) {
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            val owner = presentation.pickerOwner
            presentation.pickerOwner = null
            if (uri != null && owner === controller && controller.selectImage(uri.toString())) {
                runtime.scope.launch { controller.upload(uri.toString()) }
            }
        }
    }
    val actions = CommentComposerActions(controller, page, runtime.scope, presentation.focusRequester, focusManager, keyboard) {
        presentation.pickerOwner = controller
        imagePicker.launch("image/*")
    }
    CommentComposerEffects(page, controller, actions)
    val material = CommentComposerMaterial(
        TeakGlassRecipes.floatingNavigation(LocalTeakMaterialSpec.current.fineTuning), LocalTeakBackdrop.current)
    key(controller) {
        val preview = remember(snapshot.pendingPreview) { snapshot.pendingPreview?.let(Uri::parse) }
        val content = CommentComposerContent(snapshot, preview, page.replyTarget.name, presentation.focusRequester)
        CommentComposerBody(content, actions, material, modifier)
    }
}
