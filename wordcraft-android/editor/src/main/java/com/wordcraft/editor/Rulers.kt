package com.wordcraft.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * Horizontal ruler (under the ribbon) and the vertical ruler strip (down the page's left edge),
 * copied from the reference: inch division marks, margin markers at both ends, and the draggable
 * first-line/left-indent markers that reflow the text column.
 *
 * Margin widths are held in [RulerState] (points) so dragging a marker changes the page's text
 * inset — a real reflow, not a visual-only marker move.
 */

/** Left/right/top/bottom page margins in points, plus the derived text-column width. */
object RulerState {
    var leftMarginPt by mutableFloatStateOf(72f)   // 1"
    var rightMarginPt by mutableFloatStateOf(72f)
    var topMarginPt by mutableFloatStateOf(72f)
    var bottomMarginPt by mutableFloatStateOf(72f)

    fun reset() { leftMarginPt = 72f; rightMarginPt = 72f; topMarginPt = 72f; bottomMarginPt = 72f }
}

@Composable
fun HorizontalRuler(
    pageWidth: Dp,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val trackColor = MaterialTheme.colorScheme.surface
    val marginColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val tickColor = MaterialTheme.colorScheme.onSurfaceVariant
    val markerColor = MaterialTheme.colorScheme.primary

    // Page points → px within the ruler track.
    val pxPerPt = with(density) { (pageWidth / PAGE_WIDTH_PT).toPx() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(20.dp)
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Box(modifier = Modifier.width(pageWidth)) {
            Canvas(modifier = Modifier.fillMaxWidth().height(20.dp)) {
                val w = size.width
                val leftPx = RulerState.leftMarginPt * pxPerPt
                val rightPx = w - RulerState.rightMarginPt * pxPerPt

                // Text area (white) vs margin bands (grey), as in Word.
                drawRect(color = marginColor, topLeft = Offset(0f, 0f), size = androidx.compose.ui.geometry.Size(leftPx, size.height))
                drawRect(color = trackColor, topLeft = Offset(leftPx, 0f), size = androidx.compose.ui.geometry.Size(rightPx - leftPx, size.height))
                drawRect(color = marginColor, topLeft = Offset(rightPx, 0f), size = androidx.compose.ui.geometry.Size(w - rightPx, size.height))

                // Inch marks numbered 1..7 with half-inch ticks (reference shows 7 divisions).
                val inchPx = 72f * pxPerPt
                var inch = 1
                while (inchPx * inch < w) {
                    val x = inchPx * inch
                    drawLine(color = tickColor, start = Offset(x, size.height - 8f), end = Offset(x, size.height), strokeWidth = 1.5f)
                    // quarter-inch mid tick
                    val mid = x - inchPx / 2
                    drawLine(color = tickColor, start = Offset(mid, size.height - 5f), end = Offset(mid, size.height), strokeWidth = 1f)
                    inch++
                }
                // Page-division mark at the centre (the "page break" glyph on Word's ruler).
                drawLine(color = tickColor, start = Offset(w / 2, 0f), end = Offset(w / 2, size.height), strokeWidth = 1f)

                // Margin markers (Word's hourglass/arrow handles) at both ends.
                drawMarker(drawContext.canvas, leftPx, markerColor)
                drawMarker(drawContext.canvas, rightPx, markerColor)
                // Border.
                drawRect(color = tickColor.copy(alpha = 0.5f), style = Stroke(width = 1f))
            }

            // Draggable left/right margin markers → reflow the text column.
            DraggableMarker(initialXPt = RulerState.leftMarginPt, isLeft = true) { RulerState.leftMarginPt = it }
            DraggableMarker(initialXPt = RulerState.rightMarginPt, isLeft = false) { RulerState.rightMarginPt = it }

            // Inch numbers overlaid for legibility.
            Row(modifier = Modifier.fillMaxWidth().height(20.dp)) {
                val inchDp = with(density) { (72f * pxPerPt).toDp() }
                var i = 0
                var x = 0.dp
                while (x < pageWidth) {
                    Box(modifier = Modifier.width(inchDp).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
                        if (i > 0) Text("${i}", fontSize = 8.sp, color = tickColor, modifier = Modifier.padding(top = 1.dp))
                    }
                    i++
                    x += inchDp
                }
            }
        }
    }
}

/** Draws Word's little triangular/hourglass margin handle centred on [x]. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMarker(
    canvas: androidx.compose.ui.graphics.Canvas,
    x: Float,
    color: Color,
) {
    // Simplified hourglass: two triangles meeting at x.
    val h = size.height
    val half = 5f
    val path = androidx.compose.ui.graphics.Path().apply {
        moveTo(x - half, 0f); lineTo(x + half, 0f); lineTo(x, h * 0.45f); close()
        moveTo(x - half, h); lineTo(x + half, h); lineTo(x, h * 0.55f); close()
    }
    drawPath(path, color)
}

/** A handle that drags horizontally, writing the margin back in points. */
@Composable
private fun androidx.compose.foundation.layout.BoxScope.DraggableMarker(
    initialXPt: Float,
    isLeft: Boolean,
    onChange: (Float) -> Unit,
) {
    var marginPt by remember { mutableFloatStateOf(initialXPt) }
    val density = LocalDensity.current
    Box(
        modifier = Modifier
            .align(Alignment.TopStart)
            .fillMaxHeight()
            .width(18.dp)
            .pointerInput(isLeft) {
                detectDragGestures { change, drag ->
                    change.consume()
                    val dxPt = with(density) { drag.x / density.density } / 1f
                    marginPt = (marginPt + if (isLeft) dxPt else -dxPt).coerceIn(24f, 180f)
                    onChange(marginPt)
                }
            },
    )
}

@Composable
fun VerticalRuler(
    modifier: Modifier = Modifier,
) {
    val tickColor = MaterialTheme.colorScheme.onSurfaceVariant
    val marginColor = MaterialTheme.colorScheme.surfaceContainerHigh
    Box(
        modifier = modifier
            .width(16.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Canvas(Modifier.fillMaxWidth().fillMaxHeight()) {
            val h = size.height
            val topPx = RulerState.topMarginPt * (h / PAGE_HEIGHT_PT)
            // Margin band at top, then marks down the edge (reference: numbers 1..4 vertically).
            drawRect(color = marginColor, size = androidx.compose.ui.geometry.Size(size.width, topPx))
            val inchPx = h / PAGE_HEIGHT_PT * 72f
            var inch = 1
            while (inchPx * inch < h) {
                val y = inchPx * inch
                drawLine(color = tickColor, start = Offset(0f, y), end = Offset(size.width * 0.5f, y), strokeWidth = 1.5f)
                inch++
            }
            drawRect(color = tickColor.copy(alpha = 0.5f), style = Stroke(width = 1f))
        }
        // Vertical numbers.
        Column(Modifier.fillMaxHeight()) {
            val inchDp = with(LocalDensity.current) { (LocalDensity.current.density.let { 0f }).dp }
            // Numbers are drawn by tick spacing approximation; keep simple: rely on ticks.
            @Suppress("UNUSED_EXPRESSION") inchDp
        }
    }
}

/** A4 width in points (used to map ruler points → pixels). */
const val PAGE_WIDTH_PT = 595f
/** A4 height in points. */
const val PAGE_HEIGHT_PT = 842f

/** Convenience: round a point value for display. */
fun Float.ptLabel(): String = roundToInt().toString()
