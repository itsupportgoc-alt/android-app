package com.wordcraft.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The chrome of Word for the PC, rebuilt natively and sized for phones and tablets:
 *
 *  - [WordTopBar] — the title bar: Word's blue **W** tile, the document name with its
 *    "Saved to this device / Not saved" caption, then Save, undo, redo and search.
 *  - [WordRibbonTabs] — the ribbon tab strip across the top
 *    (**File · Home · Insert · Draw · Design · Layout · References · Mailings · Review · View ·
 *    Help**) with Word's ribbon-collapse chevron at its right end.
 *  - [WordRibbonPane] — the open tab's group set, directly under the strip.
 *
 * The strip scrolls sideways on a phone and fits without scrolling on a tablet; the pane is
 * expanded on tall viewports and collapsed on short ones (Word's collapsed-ribbon state), so the
 * page always keeps its room. Every control runs a real engine command — nothing is decorative.
 */

/** Word's brand blue, used for the app tile and the File tab. */
private val WordBlue = Color(0xFF2B579A)

// ------------------------------------------------------------------ title bar

/**
 * Word's title bar. [dirty] drives the caption ("Not saved" turns Word blue); undo/redo are
 * disabled through [canUndo] / [canRedo] exactly as the engine reports them.
 */
@Composable
fun WordTopBar(
    title: String,
    dirty: Boolean,
    wordCount: Int,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onSave: () -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(start = 12.dp, end = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Word's blue app tile with the white W monogram.
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(WordBlue),
                contentAlignment = Alignment.Center,
            ) {
                Text("W", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (dirty) {
                        "Not saved · $wordCount words"
                    } else {
                        "Saved to this device · $wordCount words"
                    },
                    fontSize = 11.sp,
                    color = if (dirty) WordBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            // Word's Quick Access Toolbar: save, undo, redo.
            IconButton(onClick = onSave) {
                Icon(Icons.Filled.Save, "Save", Modifier.size(19.dp))
            }
            IconButton(onClick = onUndo, enabled = canUndo) {
                Icon(Icons.AutoMirrored.Filled.Undo, "Undo", Modifier.size(19.dp))
            }
            IconButton(onClick = onRedo, enabled = canRedo) {
                Icon(Icons.AutoMirrored.Filled.Redo, "Redo", Modifier.size(19.dp))
            }
            IconButton(onClick = onSearch) {
                Icon(Icons.Filled.Search, "Search", Modifier.size(19.dp))
            }
        }
    }
}

// ------------------------------------------------------------------ ribbon tab strip

/**
 * Word's ribbon tab strip. Tapping a tab opens its groups under the strip; tapping the open tab
 * again collapses the ribbon, exactly as double-clicking a tab does on the PC. The chevron at
 * the right end collapses/restores the pane without changing the selected tab. On a narrow
 * phone the strip scrolls sideways, so no tab is ever clipped.
 */
@Composable
fun WordRibbonTabs(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(38.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ViewState.tabs.forEach { tab -> RibbonTabItem(tab) }
            }
            IconButton(
                onClick = {
                    if (ViewState.ribbonOpen) ViewState.closeRibbon() else ViewState.openTab(ViewState.activeTab)
                },
                modifier = Modifier.size(38.dp),
            ) {
                Icon(
                    imageVector = if (ViewState.ribbonOpen) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (ViewState.ribbonOpen) "Collapse the ribbon" else "Pin the ribbon",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

/** One tab of the strip: the open tab is white and merges into the pane below, as on the PC. */
@Composable
private fun RibbonTabItem(tab: String) {
    val selected = ViewState.ribbonOpen && ViewState.activeTab == tab
    val color = when {
        selected -> MaterialTheme.colorScheme.onSurface
        tab == "File" -> WordBlue
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .background(if (selected) MaterialTheme.colorScheme.surface else Color.Transparent)
            .clickable { ViewState.toggleTab(tab) }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = tab,
            fontSize = 12.sp,
            maxLines = 1,
            color = color,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

// ------------------------------------------------------------------ ribbon body

/**
 * The open tab's groups, in the white band under the tab strip (Word's ribbon body). It slides
 * open downwards from the strip and is dismissed by the strip's chevron or by tapping the open
 * tab again.
 */
@Composable
fun WordRibbonPane(modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = ViewState.ribbonOpen,
        enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
        exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
    ) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 3.dp,
        ) {
            Column {
                RibbonTabContent(ViewState.activeTab)
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                )
            }
        }
    }
}
