package com.wordcraft.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.CropLandscape
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Drafts
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.RecentActors
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The Word ribbon: the tab strip (`File · Home · … · Help`) plus the selected tab's content.
 *
 * This file renders the **View** tab exactly as the reference image shows it: eight captioned
 * groups in order — Views, Immersive, Page Movement, Show, Zoom, Window, Macros, Properties —
 * every control present with its icon, label and state. Other tabs fall back to their own
 * content composables; when a tab has no content yet the strip still switches highlight and the
 * body shows a neutral placeholder (see [TabContent]).
 */
@Composable
fun Ribbon(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        RibbonTabStrip(
            active = ViewState.activeTab,
            onSelect = { ViewState.selectTab(it) },
        )
        TabContent(ViewState.activeTab)
    }
}

// ------------------------------------------------------------------ tab strip

@Composable
private fun RibbonTabStrip(active: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // File is the blue Word button, not a plain tab.
        Box(
            modifier = Modifier
                .padding(end = 4.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.primary)
                .clickable { onSelect("File") }
                .padding(horizontal = 14.dp, vertical = 8.dp),
        ) {
            Text(
                text = "File",
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        ViewTabs.forEach { tab ->
            val isActive = tab == active
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onSelect(tab) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = tab,
                    fontSize = 13.sp,
                    color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                )
                // Active tab gets Word's underline bar.
                Spacer(Modifier.height(3.dp))
                Box(
                    modifier = Modifier
                        .width(if (isActive) 28.dp else 0.dp)
                        .height(2.dp)
                        .background(
                            if (isActive) MaterialTheme.colorScheme.primary
                            else androidx.compose.ui.graphics.Color.Transparent,
                        ),
                )
            }
        }
    }
    androidx.compose.material3.HorizontalDivider(
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
    )
}

/** Tabs after File, in reference order. */
private val ViewTabs = listOf(
    "Home", "Insert", "Draw", "Design", "Layout",
    "References", "Mailings", "Review", "View", "Help",
)

// ------------------------------------------------------------------ tab bodies

@Composable
private fun TabContent(tab: String) {
    when (tab) {
        "View" -> ViewTabGroups()
        else -> PlaceholderTab(tab)
    }
}

/** Neutral, clearly-labelled body for tabs other than View (View is the reference copy). */
@Composable
private fun PlaceholderTab(tab: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(92.dp)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = "$tab tab — not part of the View reference; switch back to View for the full ribbon.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ------------------------------------------------------------------ View tab groups

@Composable
private fun ViewTabGroups() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        ViewsGroup()
        GroupDivider()
        ImmersiveGroup()
        GroupDivider()
        PageMovementGroup()
        GroupDivider()
        ShowGroup()
        GroupDivider()
        ZoomGroup()
        GroupDivider()
        WindowGroup()
        GroupDivider()
        MacrosGroup()
        GroupDivider()
        PropertiesGroup()
    }
    androidx.compose.material3.HorizontalDivider(
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
    )
}

/** Thin vertical separator between ribbon groups (as in Word). */
@Composable
private fun GroupDivider() {
    Box(
        modifier = Modifier
            .padding(horizontal = 6.dp)
            .width(1.dp)
            .height(78.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
    )
}

/** A captioned group: content on top, small-caps caption beneath. */
@Composable
private fun Group(caption: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) { content() }
        Spacer(Modifier.height(4.dp))
        Text(
            text = caption,
            fontSize = 10.sp,
            letterSpacing = 0.4.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
        )
    }
}

/** Large ribbon button: icon above a (possibly wrapped, two-line) label. */
@Composable
private fun LargeButton(
    icon: ImageVector,
    label: String,
    selected: Boolean = false,
    enabled: Boolean = true,
    labelWidth: Int = 56,
    onClick: () -> Unit,
) {
    val tint = when {
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        selected -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurface
    }
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .width(labelWidth.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            lineHeight = 13.sp,
            color = tint,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

/** Small horizontal ribbon button: icon left, label right (Show/Zoom/Window groups). */
@Composable
private fun SmallButton(
    icon: ImageVector,
    label: String,
    selected: Boolean = false,
    enabled: Boolean = true,
    showCaret: Boolean = false,
    onClick: () -> Unit,
) {
    val tint = when {
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        selected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        Text(label, fontSize = 12.sp, color = tint, maxLines = 1)
        if (showCaret) {
            Icon(
                Icons.Filled.ArrowDropDown,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

/** Checkbox row (Show group): box + label; checked state from [checked]. */
@Composable
private fun CheckRow(label: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable { onToggle() }
            .padding(horizontal = 6.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = if (checked) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
            contentDescription = label,
            tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
    }
}

// ------------------------------------------------------------------ 1. Views

@Composable
private fun ViewsGroup() {
    Group("Views") {
        LargeButton(Icons.Filled.MenuBook, "Read Mode",
            selected = ViewState.view == ViewState.DocView.READ_MODE) {
            ViewState.selectView(ViewState.DocView.READ_MODE)
        }
        LargeButton(Icons.Filled.Description, "Print Layout",
            selected = ViewState.view == ViewState.DocView.PRINT_LAYOUT) {
            ViewState.selectView(ViewState.DocView.PRINT_LAYOUT)
        }
        LargeButton(Icons.Filled.Language, "Web Layout",
            selected = ViewState.view == ViewState.DocView.WEB_LAYOUT) {
            ViewState.selectView(ViewState.DocView.WEB_LAYOUT)
        }
        LargeButton(Icons.Filled.Drafts, "Outline",
            selected = ViewState.view == ViewState.DocView.OUTLINE) {
            ViewState.selectView(ViewState.DocView.OUTLINE)
        }
        LargeButton(Icons.Filled.Drafts, "Draft",
            selected = ViewState.view == ViewState.DocView.DRAFT) {
            ViewState.selectView(ViewState.DocView.DRAFT)
        }
    }
}

// ------------------------------------------------------------------ 2. Immersive

@Composable
private fun ImmersiveGroup() {
    Group("Immersive") {
        LargeButton(Icons.Filled.RecentActors, "Focus",
            selected = ViewState.focusMode) { ViewState.toggleFocus() }
        LargeButton(Icons.Filled.MenuBook, "Immersive Reader",
            selected = ViewState.immersiveReader, labelWidth = 70) { ViewState.toggleImmersive() }
    }
}

// ------------------------------------------------------------------ 3. Page Movement

@Composable
private fun PageMovementGroup() {
    Group("Page Movement") {
        LargeButton(Icons.Filled.SwapVert, "Vertical",
            selected = ViewState.pageMovement == ViewState.PageMovement.VERTICAL) {
            ViewState.selectPageMovement(ViewState.PageMovement.VERTICAL)
        }
        LargeButton(Icons.Filled.SwapHoriz, "Side to Side",
            selected = ViewState.pageMovement == ViewState.PageMovement.SIDE_TO_SIDE) {
            ViewState.selectPageMovement(ViewState.PageMovement.SIDE_TO_SIDE)
        }
    }
}

// ------------------------------------------------------------------ 4. Show

@Composable
private fun ShowGroup() {
    Group("Show") {
        Column {
            CheckRow("Ruler", ViewState.showRuler) { ViewState.toggleRuler() }
            CheckRow("Gridlines", ViewState.showGridlines) { ViewState.toggleGridlines() }
            CheckRow("Navigation Pane", ViewState.showNavPane) { ViewState.toggleNavPane() }
        }
    }
}

// ------------------------------------------------------------------ 5. Zoom

@Composable
private fun ZoomGroup() {
    Group("Zoom") {
        Column {
            // Row 1: Zoom + 100%
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                SmallButton(Icons.Filled.ZoomIn, "Zoom") { ZoomDialog.show() }
                SmallButton(Icons.Filled.Straighten, "100%") { ViewState.setZoomPercent(100) }
            }
            // Row 2: One Page · Multiple Pages · Page Width
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                SmallButton(Icons.Filled.CropSquare, "One Page") { ZoomTargets.request(ViewState.ZoomPreset.ONE_PAGE) }
                SmallButton(Icons.Filled.Dashboard, "Multiple") { ZoomTargets.request(ViewState.ZoomPreset.MULTIPLE) }
                SmallButton(Icons.Filled.CropLandscape, "Page Width") { ZoomTargets.request(ViewState.ZoomPreset.PAGE_WIDTH) }
            }
        }
    }
}

// ------------------------------------------------------------------ 6. Window

@Composable
private fun WindowGroup() {
    Group("Window") {
        Column {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                SmallButton(Icons.Filled.OpenInNew, "New Window") { WindowDialog.newWindow() }
                SmallButton(Icons.Filled.Dashboard, "Arrange All") { WindowDialog.arrangeAll() }
                SmallButton(Icons.Filled.CropSquare, "Split") { WindowDialog.split() }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                // Side to Side / Synchronous / Reset: wired (see WindowDialog); enabled when split exists.
                SmallButton(Icons.Filled.SwapHoriz, "Side by Side", showCaret = true) { WindowDialog.sideBySide() }
                SmallButton(Icons.Filled.CropLandscape, "Sync Scroll") { WindowDialog.syncScroll() }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                SmallButton(Icons.Filled.Straighten, "Reset Window") { WindowDialog.resetWindow() }
                // Switch Windows has a caret (expander) per the reference.
                SmallButton(Icons.Filled.RecentActors, "Switch Windows", showCaret = true) { WindowDialog.switchWindows() }
            }
        }
    }
}

// ------------------------------------------------------------------ 7. Macros

@Composable
private fun MacrosGroup() {
    Group("Macros") {
        LargeButton(Icons.Filled.Add, "Macros", labelWidth = 56) { MacrosDialog.show() }
    }
}

// ------------------------------------------------------------------ 8. Properties

@Composable
private fun PropertiesGroup() {
    Group("Properties") {
        LargeButton(Icons.Filled.GridOn, "Properties", labelWidth = 64) { PropertiesDialog.show() }
    }
}
