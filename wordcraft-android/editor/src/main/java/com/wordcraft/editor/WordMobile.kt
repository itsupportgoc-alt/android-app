package com.wordcraft.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Spellcheck
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The phone chrome of Microsoft Word for Android, rebuilt natively:
 *
 *  - [WordTopBar] — the white app bar: Word's blue app tile, the document name with its
 *    "Saved to this device / Not saved" caption, then undo, redo, search and the ⋮ menu.
 *  - [WordTabBar] — the bottom tab strip (Home · Insert · Draw · Layout · Review · View · ⋯).
 *  - [WordRibbonPane] — the full ribbon groups that slide up above the tab strip.
 *
 * [ViewState.ribbonOpen] owns whether the pane is showing, so the chrome survives rotation like
 * the rest of the view state. Every control runs a real engine command — nothing is decorative.
 */

/** Word's brand blue, used for the app tile, the selected tab and the dirty-document caption. */
private val WordBlue = Color(0xFF2B579A)

// ------------------------------------------------------------------ top app bar

/**
 * The Word mobile app bar. [dirty] drives the caption colour ("Not saved" turns Word blue);
 * undo/redo are disabled through [canUndo] / [canRedo] exactly as the engine reports them.
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
    onSearch: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 2.dp, top = 4.dp, bottom = 4.dp),
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
                )
            }
            IconButton(onClick = onUndo, enabled = canUndo) {
                Icon(Icons.AutoMirrored.Filled.Undo, "Undo", Modifier.size(20.dp))
            }
            IconButton(onClick = onRedo, enabled = canRedo) {
                Icon(Icons.AutoMirrored.Filled.Redo, "Redo", Modifier.size(20.dp))
            }
            IconButton(onClick = onSearch) {
                Icon(Icons.Filled.Search, "Search", Modifier.size(20.dp))
            }
            IconButton(onClick = onMore) {
                Icon(Icons.Filled.MoreVert, "More", Modifier.size(20.dp))
            }
        }
    }
}

// ------------------------------------------------------------------ bottom tab strip

/** One item of the bottom tab strip. */
private class WordMobileTab(val id: String, val label: String, val icon: ImageVector)

/** Word mobile's tab order: the everyday tabs, with the rest behind ⋯. */
private val WordMobileTabs = listOf(
    WordMobileTab("Home", "Home", Icons.Filled.Home),
    WordMobileTab("Insert", "Insert", Icons.Filled.Add),
    WordMobileTab("Draw", "Draw", Icons.Filled.Brush),
    WordMobileTab("Layout", "Layout", Icons.Filled.CropSquare),
    WordMobileTab("Review", "Review", Icons.Filled.Spellcheck),
    WordMobileTab("View", "View", Icons.Filled.Visibility),
)

/** Tabs that do not fit on a phone strip; the ⋯ menu opens them in the same pane. */
private val MoreTabs = listOf("Design", "References", "Mailings", "File", "Help")

/**
 * The bottom tab strip. Tapping a tab opens its ribbon groups above the strip; tapping the tab
 * that is already open closes the pane again (Word mobile's behaviour). ⋯ lists the remaining
 * tabs, including the File backstage.
 */
@Composable
fun WordTabBar(modifier: Modifier = Modifier) {
    var moreOpen by remember { mutableStateOf(false) }
    Column(modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)) {
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
        )
        Row(
            modifier = Modifier.fillMaxWidth().height(54.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WordMobileTabs.forEach { tab ->
                TabStripItem(
                    tab = tab,
                    selected = ViewState.ribbonOpen && ViewState.activeTab == tab.id,
                    onClick = { ViewState.toggleTab(tab.id) },
                    modifier = Modifier.weight(1f),
                )
            }
            Box(Modifier.weight(1f)) {
                TabStripItem(
                    tab = WordMobileTab("__more", "More", Icons.Filled.MoreHoriz),
                    selected = moreOpen,
                    onClick = { moreOpen = true },
                    modifier = Modifier.fillMaxWidth(),
                )
                DropdownMenu(expanded = moreOpen, onDismissRequest = { moreOpen = false }) {
                    MoreTabs.forEach { id ->
                        DropdownMenuItem(
                            text = { Text(id) },
                            onClick = {
                                moreOpen = false
                                ViewState.openTab(id)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TabStripItem(
    tab: WordMobileTab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colorScheme.primary
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Word's selected-tab marker: a thin accent bar along the top edge.
        Box(
            Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(if (selected) accent else Color.Transparent),
        )
        Spacer(Modifier.height(8.dp))
        Icon(
            imageVector = tab.icon,
            contentDescription = tab.label,
            tint = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(21.dp),
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = tab.label,
            fontSize = 9.5.sp,
            maxLines = 1,
            color = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

// ------------------------------------------------------------------ ribbon pane

/**
 * The expandable ribbon above the tab strip: a small header naming the open tab (with the
 * collapse chevron) and that tab's full group set, generated from the engine's command registry.
 */
@Composable
fun WordRibbonPane(modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = ViewState.ribbonOpen,
        enter = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
        exit = shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut(),
    ) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp,
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 4.dp, top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = ViewState.activeTab,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.weight(1f))
                    IconButton(
                        onClick = { ViewState.closeRibbon() },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            Icons.Filled.KeyboardArrowDown,
                            "Close ribbon",
                            Modifier.size(20.dp),
                        )
                    }
                }
                Column(
                    Modifier
                        .heightIn(max = 190.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    RibbonTabContent(ViewState.activeTab)
                }
            }
        }
    }
}
