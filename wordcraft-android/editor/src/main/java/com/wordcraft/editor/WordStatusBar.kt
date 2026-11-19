package com.wordcraft.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * Word for the PC's **status bar**, along the bottom of the window: the word count and the
 * document's language on the left, then the three view buttons (Read Mode · Print Layout · Web
 * Layout) and Word's zoom controls (− · percentage · +) on the right.
 *
 * Tapping the percentage resets the zoom to 100%, as Word does. On a phone the bar keeps only
 * what fits (words + zoom); the view buttons appear once there is room for them.
 */
@Composable
fun WordStatusBar(
    wordCount: Int,
    dirty: Boolean,
    modifier: Modifier = Modifier,
) {
    val wide = LocalConfiguration.current.screenWidthDp >= 600
    Column(modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)) {
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .padding(start = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "$wordCount words",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            if (wide) {
                StatusDivider()
                Text(
                    text = "English (US)",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                StatusDivider()
                Text(
                    text = if (dirty) "Unsaved changes" else "Saved",
                    fontSize = 11.sp,
                    color = if (dirty) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }

            Spacer(Modifier.weight(1f))

            if (wide) {
                ViewButton(Icons.Filled.MenuBook, "Read Mode", ViewState.DocView.READ_MODE)
                ViewButton(Icons.Filled.Description, "Print Layout", ViewState.DocView.PRINT_LAYOUT)
                ViewButton(Icons.Filled.Language, "Web Layout", ViewState.DocView.WEB_LAYOUT)
                StatusDivider()
            }

            IconButton(onClick = { ViewState.zoomOut() }, modifier = Modifier.size(30.dp)) {
                Text("−", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
            }
            Text(
                text = "${(ViewState.zoom * 100).roundToInt()}%",
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .width(44.dp)
                    .clickable { ViewState.setZoomPercent(100) },
            )
            IconButton(onClick = { ViewState.zoomIn() }, modifier = Modifier.size(30.dp)) {
                Text("+", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

/** One of the status bar's view buttons; the active view is tinted Word blue. */
@Composable
private fun ViewButton(icon: ImageVector, label: String, view: ViewState.DocView) {
    val selected = ViewState.view == view
    IconButton(
        onClick = { ViewState.selectView(view) },
        modifier = Modifier.size(28.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun StatusDivider() {
    Box(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .width(1.dp)
            .height(14.dp)
            .background(MaterialTheme.colorScheme.outline),
    )
}
