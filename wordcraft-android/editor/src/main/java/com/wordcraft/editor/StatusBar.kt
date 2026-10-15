package com.wordcraft.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Word's blue **status bar**, copied from the reference: "Page 1 of 1" + word count on the left;
 * view toggles (Read/Print/Web), the Focus toggle, a zoom slider and the live percentage on the
 * right. Every element is live:
 *  - page/word counters come from [EditorController.state],
 *  - the view toggles switch [ViewState.view],
 *  - the slider + −/+ set [ViewState.zoom] (real engine reflow when the FFI hook is present),
 *  - the percentage reflects [ViewState.zoom] and is tappable to reset to 100%.
 */
@Composable
fun StatusBar(wordCount: Int, pageCount: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // ---- left: page + words ----
        Text(
            text = "Page 1 of $pageCount",
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = "$wordCount words",
            color = Color.White,
            fontSize = 11.sp,
        )

        Spacer(Modifier.weight(1f))

        // ---- right: Focus + view toggles ----
        StatusChip("Focus", ViewState.focusMode) { ViewState.toggleFocus() }
        Spacer(Modifier.width(8.dp))
        ViewToggle("Read", ViewState.view == ViewState.DocView.READ_MODE) {
            ViewState.selectView(ViewState.DocView.READ_MODE)
        }
        ViewToggle("Print", ViewState.view == ViewState.DocView.PRINT_LAYOUT) {
            ViewState.selectView(ViewState.DocView.PRINT_LAYOUT)
        }
        ViewToggle("Web", ViewState.view == ViewState.DocView.WEB_LAYOUT) {
            ViewState.selectView(ViewState.DocView.WEB_LAYOUT)
        }

        Spacer(Modifier.width(8.dp))

        // ---- zoom slider ----
        Icon(Icons.Filled.Remove, contentDescription = "Zoom out", tint = Color.White, modifier = Modifier.size(14.dp))
        Slider(
            value = ViewState.zoom,
            onValueChange = { ViewState.applyZoom(it) },
            valueRange = ZOOM_MIN..ZOOM_MAX,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White.copy(alpha = 0.9f),
                inactiveTrackColor = Color.White.copy(alpha = 0.35f),
            ),
            modifier = Modifier.width(120.dp).padding(horizontal = 4.dp),
        )
        Icon(Icons.Filled.Add, contentDescription = "Zoom in", tint = Color.White, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        // Percentage — tappable to reset to 100%.
        Text(
            text = "${(ViewState.zoom * 100).toInt()}%",
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                .clickable { ViewState.setZoomPercent(100) }
                .padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun StatusChip(label: String, active: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        color = if (active) MaterialTheme.colorScheme.onPrimary else Color.White,
        fontSize = 11.sp,
        modifier = Modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
            .background(if (active) Color.White.copy(alpha = 0.25f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
private fun ViewToggle(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
            .background(if (active) Color.White.copy(alpha = 0.25f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

/** Rough page count for the status bar (words / 450, min 1) — matches the Pages tab. */
fun pageCountFor(wordCount: Int): Int = ((wordCount / 450) + 1).coerceAtLeast(1)
