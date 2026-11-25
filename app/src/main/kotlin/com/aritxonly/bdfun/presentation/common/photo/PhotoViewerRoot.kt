package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.viewinterop.AndroidView
import android.graphics.Bitmap
import android.graphics.Rect
import com.aritxonly.teak.common.material.LocalTeakBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop

/**
 * 褰撳墠绐楀彛鍐呯殑鍥剧墖鏌ョ湅鍣ㄥ涓汇€?
 *
 * 鍜?Telegram 鐨?PhotoViewer 涓€鏍风洿鎺ョ敾鍦ㄧ獥鍙ｆ渶涓婂眰锛屼笉鍒涘缓 Dialog锛涘叆鍦?閫€鍦虹敤
 * 鍒楄〃閲屽凡缁忕紦瀛樼殑缂╃暐鍥?bitmap 璺戜竴娆?[PhotoMorphView]锛屾湡闂翠笉缁勫悎 pager銆佷笉瑙ｇ爜鍘熷浘銆?
 * 鍔ㄧ敾缁撴潫鍚庢墠鏄剧ず鍙氦浜掔殑 Compose 鏌ョ湅鍣ㄥ苟鍔犺浇鍘熷浘銆?
 *
 * 妤间腑妤艰繖绫荤嫭绔?Dialog 浼氬湪鑷繁鐨勫唴瀹归噷鍐嶅寘涓€灞傛湰瀹夸富锛屾煡鐪嬪櫒鐢诲湪寮圭獥鑷繁鐨勭獥鍙ｉ噷銆?
 */
@Composable
internal fun PhotoViewerRoot(
    animationsEnabled: Boolean,
    content: @Composable () -> Unit,
) {
    val state = remember { PhotoViewerStateHolder() }
    val view = remember { PhotoViewerTransitionState() }
    val incoming = state.request
    val durationMs = if (animationsEnabled) PhotoViewerDurationMs else 0L
    val viewerBackdrop = rememberLayerBackdrop {
        drawRect(androidx.compose.ui.graphics.Color.Black)
        drawContent()
    }
    var morphViewRef by remember { mutableStateOf<PhotoMorphView?>(null) }

    LaunchedEffect(incoming) {
        val request = incoming
        val current = view.displayed
        when {
            request != null && current == null -> {
                // 涓婁竴娆℃嫋鍔ㄥ叧闂彲鑳界暀涓嬩綅绉?缂╂斁锛涗笉閲嶇疆浼氳鍐嶆鎵撳紑鏃朵綅缃拰鑳屾櫙閮戒笉瀵广€?
                view.resetTransient()
                view.displayed = request
                view.pageIndex = request.index
                view.settled = false
                view.progress.floatValue = 0f
                view.startMorph(request, request.index, open = true, durationMs)
            }
            request == null && current != null -> {
                view.settled = false
                view.startMorph(current, view.pageIndex, open = false, durationMs)
            }
        }
    }
    // 鍘熷浘/缂╃暐鍥捐繜杩熸病鐢诲嚭鏉ャ€佹垨 Compose 灞傛姤鍛婅兘鐢讳簡锛氱粺涓€鐢辫繖涓?effect 澶勭悊鐩栧眰娣″嚭銆?
    PhotoMorphCoverFallback(view) { morphViewRef }

    // 鍏ュ満鍔ㄧ敾/缂╃暐鍥剧洊灞傛湡闂寸偣鍑讳篃瑕佽兘绔嬪埢鍏抽棴锛屼笉鑳界瓑鍘熷浘鍔犺浇瀹屻€?
    val dismissOnTap = view.displayed != null && (!view.settled || view.morphCovering)
    Box(
        Modifier.fillMaxSize()
            .onSizeChanged { view.size = it }
            .then(if (dismissOnTap) Modifier.pointerInput(Unit) {
                detectTapGestures { state.close() }
            } else Modifier),
    ) {
        // 搴曞眰椤甸潰锛氭墦寮€鏃堕殢杩涘害鏀惧ぇ涓€鐐圭偣锛屽叧闂椂鎸夊悓鏍峰€嶆暟缂╁洖锛?
        // 璺熸墜鎷栧姩鏃跺啀缂╁埌 0.9銆?
        Box(
            modifier = Modifier.fillMaxSize().graphicsLayer {
                val scale = photoBackdropScaleAt(view.progress.value, view.dismissProgress)
                scaleX = scale
                scaleY = scale
            },
        ) {
            CompositionLocalProvider(LocalPhotoViewer provides state) { content() }
        }
        view.displayed?.let { request ->
            if (view.settled) {
                PhotoViewerSettledLayer(request, view, state, viewerBackdrop)
            }
            // 鍔ㄧ敾缁撴潫鍚庣缉鐣ュ浘浠嶇洊鍦ㄦ渶涓婇潰锛岀洿鍒?Compose 灞傜湡姝ｆ妸鍘熷浘/缂╃暐鍥剧敾鍑烘潵銆?
            if ((!view.settled || view.morphCovering) && view.morphParams != null) {
                PhotoMorphLayer(view, state) { morphViewRef = it }
            }
        }
    }
}

/** 缂╃暐鍥剧洊灞傜殑涓ょ娣″嚭鏃舵満锛氱瓑澶箙鍏滃簳锛屾垨 Compose 灞傛姤鍛婂凡缁忚兘鐢汇€?*/
@Composable
private fun PhotoMorphCoverFallback(
    view: PhotoViewerTransitionState,
    overlay: () -> PhotoMorphView?,
) {
    // 鍘熷浘/缂╃暐鍥捐繜杩熸病鐢诲嚭鏉ユ椂锛岀缉鐣ュ浘涓嶈兘姘歌繙鐩栫潃锛涜秴鏃跺悗闇插嚭 Compose 灞傘€?
    LaunchedEffect(view.settled, view.morphCovering) {
        if (view.settled && view.morphCovering) {
            kotlinx.coroutines.delay(600)
            if (view.morphCovering) {
                val target = overlay()
                if (target != null) target.fadeOut { view.morphCovering = false } else view.morphCovering = false
            }
        }
    }
    // Compose 灞傝鍥惧凡缁忚兘鐢讳簡锛氱瓑瀹冪湡姝ｇ敾鍑轰竴涓ゅ抚鍚庡啀娣″嚭鐩栧眰锛屾潨缁濅腑闂撮棯涓€甯с€?
    PhotoCoverReadyEffect(view, overlay)
}

@Composable
private fun PhotoCoverReadyEffect(
    view: PhotoViewerTransitionState,
    overlay: () -> PhotoMorphView?,
) {    LaunchedEffect(view.settled, view.coverReadyToken) {
        if (view.settled && view.morphCovering && view.coverReadyToken > 0) {
            kotlinx.coroutines.delay(120)
            if (!view.morphCovering) return@LaunchedEffect
            val target = overlay()
            if (target != null) {
                target.fadeOut { view.morphCovering = false }
            } else {
                view.morphCovering = false
            }
        }
    }
}

@Composable
private fun PhotoViewerSettledLayer(
    request: PhotoViewerRequest,
    view: PhotoViewerTransitionState,
    state: PhotoViewerStateHolder,
    viewerBackdrop: top.yukonga.miuix.kmp.blur.LayerBackdrop,
) {
    // 鍜?Telegram 涓€鏍风敾鍦ㄥ悓涓€涓獥鍙ｉ噷锛涙ゼ涓ゼ鏈夎嚜宸辩殑涓€灞傚涓汇€?
    CompositionLocalProvider(LocalTeakBackdrop provides viewerBackdrop) {
        Box(
            Modifier.fillMaxSize().then(
                if (view.menuVisible) Modifier.layerBackdrop(viewerBackdrop) else Modifier,
            ),
        ) {
            PhotoViewerOverlay(
                request = request,
                view = view,
                onDismiss = state::close,
                onPageChanged = {
                    view.pageIndex = it
                    // 鎹竴寮犲浘浠庛€屾湭鏃嬭浆銆嶅紑濮嬶紝閬垮厤涓婁竴寮犵殑鏃嬭浆甯﹀埌涓嬩竴寮犮€?
                    view.rotationQuarter = 0
                },
            )
        }
    }
}

@Composable
private fun PhotoMorphLayer(
    view: PhotoViewerTransitionState,
    state: PhotoViewerStateHolder,
    onView: (PhotoMorphView?) -> Unit,
) {
    val params = view.morphParams ?: return
    key(view.morphSession) {
        AndroidView(
            factory = { context ->
                PhotoMorphView(context).apply {
                    onView(this)
                    onProgress = { view.progress.floatValue = it }
                    onFinished = {
                        if (state.request == null) {
                            view.displayed = null
                            view.progress.floatValue = 0f
                            view.morphParams = null
                            view.morphCovering = false
                            // 鍏抽棴鍔ㄧ敾缁撴潫鍚庢竻鎺夋墜鍔挎畫鐣欙紝涓嬩竴娆℃墦寮€浠庡共鍑€鐘舵€佸紑濮嬨€?
                            view.resetTransient()
                        } else {
                            view.settled = true
                            view.progress.floatValue = 1f
                        }
                    }
                    start(params.bitmap, params.origin, params.radius, params.open,
                        durationMs = params.duration, dragOffsetY = params.dragOffsetY, startDim = params.startDim)
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/** Telegram 鐨勫叆鍦?閫€鍦烘槸 200ms 鐨?AccelerateDecelerate锛屾病鏈夊洖寮广€佹病鏈夎繃鍐层€?*/
internal const val PhotoViewerDurationMs = 200L


/**
 * 涓€娆℃墦寮€/鍏抽棴杩囩▼涓殑鍏ㄩ儴鐬椂鐘舵€併€?
 *
 * 鍏ュ満/閫€鍦虹敱 [PhotoMorphView] 鐨勫崟涓?ValueAnimator 椹卞姩锛沺rogress 鍙槸缁欏簳灞傞〉闈?
 * 缂╂斁鍜?Compose 灞傝鍙栫殑闀滃儚锛屼笉鍐嶇敤 Compose 鍔ㄧ敾閫愬抚閲嶇粍鏌ョ湅鍣ㄣ€?
 */
internal class PhotoViewerTransitionState {
    var displayed by mutableStateOf<PhotoViewerRequest?>(null)
    var settled by mutableStateOf(false)
    var pageIndex by mutableIntStateOf(0)
    val progress = mutableFloatStateOf(0f)
    var dragY by mutableFloatStateOf(0f)
    var zoom by mutableFloatStateOf(1f)
    var chromeVisible by mutableStateOf(true)
    var size by mutableStateOf(IntSize.Zero)
    var menuVisible by mutableStateOf(false)
    /** 鐢ㄦ埛鎵嬪姩鏃嬭浆鐨勭鍑犱釜 90掳锛氬浘鐗囨煡鐪嬪櫒涓嶅啀璺熼殢浼犳劅鍣ㄨ浆鍚戯紝鍙杩欎釜鍊笺€?*/
    var rotationQuarter by mutableIntStateOf(0)
    var morphParams by mutableStateOf<PhotoMorphParams?>(null)
    var morphSession by mutableIntStateOf(0)
    /** 缂╃暐鍥剧户缁洊鍦ㄦ煡鐪嬪櫒涓婏紝鐩村埌鍘熷浘/缂╃暐鍥剧湡姝ｇ敾鍑烘潵锛岄伩鍏嶄腑闂撮棯涓€甯х┖銆?*/
    var morphCovering by mutableStateOf(false)
    /** Compose 灞傛姤鍛娾€滃浘宸茬粡鑳界敾浜嗏€濈殑浠ｆ暟锛汻oot 鏀跺埌鍚庡厛娣″嚭鐩栧眰鍐嶇Щ闄ゃ€?*/
    var coverReadyToken by mutableIntStateOf(0)
    /** 鍏ュ満缂╃暐鍥剧殑鐪熷疄瀹介珮姣旓細浜ゆ帴缁?Compose 椤垫椂鍏堢敤瀹冿紝閬垮厤钀戒綅鐬棿鎸?16:9 閲嶆帓銆?*/
    var openAspect by mutableFloatStateOf(0f)

    val dismissProgress: Float
        get() = photoDismissProgress(dragY, size.height.toFloat())

    /** 鎵撳紑/鍏抽棴涓€娆＄粨鏉熷悗澶嶄綅鎵嬪娍鎬侊紝閬垮厤涓嬩竴娆℃墦寮€娌跨敤涓婃鐨勪綅绉诲拰缂╂斁銆?*/
    fun resetTransient() {
        dragY = 0f
        zoom = 1f
        chromeVisible = true
        menuVisible = false
        rotationQuarter = 0
    }

    fun markCoverReady() {
        coverReadyToken++
    }

    fun startMorph(
        request: PhotoViewerRequest,
        index: Int,
        open: Boolean,
        durationMs: Long,
    ) {
        val bitmap = PhotoThumbCache.get(request.images.getOrNull(index).orEmpty())
        val origin = request.originFor(index)
        if (bitmap == null || origin == null || durationMs <= 0L) {
            // 娌℃湁缂╃暐鍥剧紦瀛樻椂閫€鍥炵洿鎺ユ樉绀猴紝鑷冲皯涓嶈绌虹櫧鎴栧崱鍦ㄥ崐绌恒€?
            morphParams = null
            morphCovering = false
            settled = true
            progress.floatValue = if (open) 1f else 0f
            if (!open) displayed = null
            return
        }
        morphSession++
        openAspect = if (bitmap.height > 0) bitmap.width.toFloat() / bitmap.height else 0f
        morphCovering = true
        coverReadyToken = 0
        morphParams = PhotoMorphParams(
            bitmap = bitmap,
            origin = Rect(
                origin.left.toInt(),
                origin.top.toInt(),
                origin.right.toInt(),
                origin.bottom.toInt(),
            ),
            radius = request.originRadiusPx,
            open = open,
            duration = durationMs,
            // 鍏抽棴鏃舵妸褰撳墠鎷栧姩浣嶇Щ浜ょ粰缂╃暐鍥惧姩鐢伙紝閬垮厤浠庢墜鎸囦綅缃烦鍥炲睆骞曚腑蹇冨啀缂╁洖銆?
            dragOffsetY = if (open) 0f else dragY,
            // 鎷栧姩鏃堕粦搴曞凡缁忓彉娣★紝鍏抽棴鍔ㄧ敾鎺ョ潃杩欎釜閫忔槑搴﹁蛋锛屽惁鍒欎細鍏堥棯鍥炲叏榛戙€?
            startDim = if (open) 1f else 1f - dismissProgress,
        )
    }
}

internal data class PhotoMorphParams(
    val bitmap: Bitmap,
    val origin: Rect,
    val radius: Float,
    val open: Boolean,
    val duration: Long,
    val dragOffsetY: Float = 0f,
    val startDim: Float = 1f,
)
