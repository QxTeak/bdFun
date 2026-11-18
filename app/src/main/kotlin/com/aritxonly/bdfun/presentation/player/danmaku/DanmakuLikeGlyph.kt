package com.aritxonly.bdfun.ui

import android.content.res.Resources
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.PathParser
import com.aritxonly.teak.common.icons.AppGlyph
import org.xmlpull.v1.XmlPullParser

private const val AndroidAttributes = "http://schemas.android.com/apk/res/android"
private const val LikeViewport = 256f

/** 空心版沿原 SVG 的两侧收边，实心版仍使用同一套点赞资源的选中形状。 */
@Composable
internal fun DanmakuLikeGlyph(liked: Boolean, tint: Color, outlineInset: Dp, size: Dp = 15.dp) {
    val resource = TeakIcon.like(liked)
    if (liked || outlineInset <= 0.dp) {
        AppGlyph(resource, tint, size = size)
        return
    }
    val resources = LocalContext.current.resources
    val path = remember(resources, resource, size, outlineInset) {
        val inset = outlineInset.value / size.value * LikeViewport
        runCatching { thinnerLikePath(resources, resource, inset) }.getOrNull()?.asComposePath()
    }
    if (path == null) AppGlyph(resource, tint, size = size)
    else Canvas(Modifier.size(size)) {
        scale(this.size.width / LikeViewport, this.size.height / LikeViewport, pivot = androidx.compose.ui.geometry.Offset.Zero) {
            drawPath(path, tint)
        }
    }
}

private fun thinnerLikePath(resources: Resources, resource: Int, inset: Float): Path {
    val icon = Path()
    resources.getXml(resource).use { xml ->
        var offsetX = 0f
        var offsetY = 0f
        while (xml.next() != XmlPullParser.END_DOCUMENT) {
            when {
                xml.eventType == XmlPullParser.START_TAG && xml.name == "group" -> {
                    offsetX = xml.getAttributeFloatValue(AndroidAttributes, "translateX", 0f)
                    offsetY = xml.getAttributeFloatValue(AndroidAttributes, "translateY", 0f)
                }
                xml.eventType == XmlPullParser.END_TAG && xml.name == "group" -> {
                    offsetX = 0f
                    offsetY = 0f
                }
                xml.eventType == XmlPullParser.START_TAG && xml.name == "path" -> {
                    val data = xml.getAttributeValue(AndroidAttributes, "pathData")
                    val contour = PathParser.createPathFromPathData(data) ?: continue
                    icon.addPath(contour, offsetX, offsetY)
                }
            }
        }
    }
    val edge = Path()
    val paint = Paint().apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeWidth = inset * 2f
    }
    paint.getFillPath(icon, edge)
    val adjusted = Path(icon)
    return if (adjusted.op(edge, Path.Op.DIFFERENCE) && !adjusted.isEmpty) adjusted else icon
}
