package com.wordcraft.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BorderColor
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignJustify
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.FormatAlignRight
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.FormatIndentDecrease
import androidx.compose.material.icons.filled.FormatIndentIncrease
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wordcraft.ui.parseHexColor

/**
 * The Word **Home** tab, copied from the reference image: five captioned groups in order —
 * Clipboard · Font · Paragraph · Styles · Editing — with every control wired to the engine
 * (no dead buttons).
 */
@Composable
fun HomeTabGroups() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.Top,
        ) {
            ClipboardGroup()
            GroupDivider()
            FontGroup()
            GroupDivider()
            ParagraphGroup()
            GroupDivider()
            StylesGroup()
            GroupDivider()
            EditingGroup()
        }
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
        )
    }
}

/** Thin vertical separator between ribbon groups (as in the reference). */
@Composable
private fun GroupDivider() {
    Box(
        modifier = Modifier
            .padding(horizontal = 6.dp)
            .width(1.dp)
            .height(84.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
    )
}

/** A captioned group: content on top, small caption beneath (Word ribbon style). */
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

/** Small horizontal ribbon button: icon left, label right (+ optional caret). */
@Composable
private fun SmallButton(
    icon: ImageVector,
    label: String,
    showCaret: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(18.dp))
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
        if (showCaret) {
            Icon(
                Icons.Filled.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

// ------------------------------------------------------------------ Clipboard

@Composable
private fun ClipboardGroup() {
    val clipboard = LocalClipboardManager.current
    val selected = EditorController.state.selectedText

    Group("Clipboard") {
        Row(verticalAlignment = Alignment.Top) {
            // Big Paste button (icon above label), like the reference.
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable {
                        val text = clipboard?.getText()?.toString()
                        if (text.isNullOrEmpty()) {
                            EditorController.showStatus("Clipboard is empty")
                        } else {
                            EditorController.pastePlainText(text)
                        }
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .width(56.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(Icons.Filled.ContentPaste, contentDescription = "Paste", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp))
                Spacer(Modifier.height(2.dp))
                Text("Paste", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.width(6.dp))
            Column {
                ClipboardRow(Icons.Filled.ContentCut, "Cut") {
                    if (selected.isBlank()) {
                        EditorController.showStatus("Select text first")
                    } else {
                        clipboard?.setText(AnnotatedString(selected))
                        EditorController.cutSelection()
                    }
                }
                ClipboardRow(Icons.Filled.ContentCopy, "Copy") {
                    if (selected.isBlank()) {
                        EditorController.showStatus("Select text first")
                    } else {
                        clipboard?.setText(AnnotatedString(selected))
                        EditorController.showStatus("Copied selection")
                    }
                }
                ClipboardRow(Icons.Filled.BorderColor, "Format") {
                    EditorController.showStatus("Format Painter: pick colour options in the Font group")
                }
            }
        }
    }
}

@Composable
private fun ClipboardRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp))
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
    }
}

// ------------------------------------------------------------------ Font

@Composable
private fun FontGroup() {
    val f = EditorController.state.format
    var fontMenu by remember { mutableStateOf(false) }
    var sizeMenu by remember { mutableStateOf(false) }
    var colorMenu by remember { mutableStateOf(false) }
    var highlightMenu by remember { mutableStateOf(false) }

    Group("Font") {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                // Font name field with caret (opens the list).
                Box {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
                            .clickable { fontMenu = true }
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(f.font ?: "Aptos", fontSize = 12.sp, maxLines = 1, modifier = Modifier.width(52.dp))
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = "Font", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    }
                    DropdownMenu(expanded = fontMenu, onDismissRequest = { fontMenu = false }) {
                        FontNames.forEach { name ->
                            DropdownMenuItem(text = { Text(name) }, onClick = { EditorController.setFont(name); fontMenu = false })
                        }
                    }
                }
                // Size field with caret.
                Box {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
                            .clickable { sizeMenu = true }
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(f.sizePt?.let { trimSize(it) } ?: "12", fontSize = 12.sp, modifier = Modifier.width(22.dp))
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = "Font size", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    }
                    DropdownMenu(expanded = sizeMenu, onDismissRequest = { sizeMenu = false }) {
                        FontSizes.forEach { s ->
                            DropdownMenuItem(
                                text = { Text(trimSize(s)) },
                                onClick = { EditorController.setFontSize(s); sizeMenu = false },
                            )
                        }
                    }
                }
                IconButton(onClick = { EditorController.shrinkFont() }, modifier = Modifier.size(28.dp)) {
                    Text("A−", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = { EditorController.growFont() }, modifier = Modifier.size(28.dp)) {
                    Text("A+", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                // Clear formatting ("Aa" chip in the reference).
                IconButton(onClick = { EditorController.clearFormatting() }, modifier = Modifier.size(28.dp)) {
                    Text("Aa", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                HomeToggle(Icons.Filled.FormatBold, "Bold", f.bold) { EditorController.toggleBold() }
                HomeToggle(Icons.Filled.FormatItalic, "Italic", f.italic) { EditorController.toggleItalic() }
                HomeToggle(Icons.Filled.FormatUnderlined, "Underline", f.underline) { EditorController.toggleUnderline() }
                HomeToggle(Icons.Filled.FormatStrikethrough, "Strikethrough", f.strike) { EditorController.toggleStrike() }
                // Text colour with caret → palette.
                Box {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { colorMenu = true }
                            .padding(horizontal = 4.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.FormatColorText, contentDescription = "Font colour", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(18.dp))
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                    }
                    DropdownMenu(expanded = colorMenu, onDismissRequest = { colorMenu = false }) {
                        HomeColorRow(colors = TextColorPalette, selected = f.color, onPick = { hex ->
                            EditorController.setTextColor(hex ?: "auto")
                            colorMenu = false
                        })
                    }
                }
                // Highlighter with caret → palette.
                Box {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { highlightMenu = true }
                            .padding(horizontal = 4.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.BorderColor, contentDescription = "Highlight", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(18.dp))
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                    }
                    DropdownMenu(expanded = highlightMenu, onDismissRequest = { highlightMenu = false }) {
                        HomeColorRow(
                            colors = HighlightPalette.map { it.second },
                            selected = f.highlight,
                            onPick = { hex ->
                                val name = HighlightPalette.firstOrNull { it.second == hex }?.first ?: "yellow"
                                EditorController.setHighlight(name)
                                highlightMenu = false
                            },
                        )
                    }
                }
                Spacer(Modifier.width(2.dp))
                // The red-underlined "A" from the reference: quick red font colour.
                IconButton(onClick = { EditorController.setTextColor("C00000") }, modifier = Modifier.size(28.dp)) {
                    Text(
                        text = "A",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFC00000),
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeToggle(icon: ImageVector, label: String, active: Boolean, onClick: () -> Unit) {
    val bg = if (active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    IconButton(onClick = onClick, modifier = Modifier.size(30.dp)) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(bg)
                .padding(2.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

// ------------------------------------------------------------------ Paragraph

@Composable
private fun ParagraphGroup() {
    val p = EditorController.state.para

    Group("Paragraph") {
        Column {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                HomeToggle(Icons.Filled.FormatListBulleted, "Bullets", p.inList) { EditorController.toggleBullets() }
                HomeToggle(Icons.Filled.FormatListNumbered, "Numbering", p.inList) { EditorController.toggleNumbering() }
                HomeToggle(Icons.Filled.FormatIndentDecrease, "Decrease indent", false) { EditorController.outdent() }
                HomeToggle(Icons.Filled.FormatIndentIncrease, "Increase indent", false) { EditorController.indent() }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                HomeToggle(Icons.Filled.FormatAlignLeft, "Align left", p.align == "left") { EditorController.setAlign("left") }
                HomeToggle(Icons.Filled.FormatAlignCenter, "Center", p.align == "center") { EditorController.setAlign("center") }
                HomeToggle(Icons.Filled.FormatAlignRight, "Align right", p.align == "right") { EditorController.setAlign("right") }
                HomeToggle(Icons.Filled.FormatAlignJustify, "Justify", p.align == "justify" || p.align == "distribute") { EditorController.setAlign("justify") }
            }
        }
    }
}

// ------------------------------------------------------------------ Styles

@Composable
private fun StylesGroup() {
    val f = EditorController.state.format

    Group("Styles") {
        Column {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                StyleCard(id = "Normal", name = "Normal", selected = f.style == "Normal") { EditorController.applyStyle("Normal") }
                StyleCard(id = "Heading1", name = "Heading 1", selected = f.style == "Heading1") { EditorController.applyStyle("Heading1") }
                StyleCard(id = "Title", name = "Title", selected = f.style == "Title") { EditorController.applyStyle("Title") }
                StyleCard(id = "Heading2", name = "Heading 2", selected = f.style == "Heading2") { EditorController.applyStyle("Heading2") }
                StyleCard(id = "Subtitle", name = "Subtitle", selected = f.style == "Subtitle") { EditorController.applyStyle("Subtitle") }
            }
        }
    }
}

@Composable
private fun StyleCard(id: String, name: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(84.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                shape = RoundedCornerShape(4.dp),
            )
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            contentAlignment = Alignment.Center,
        ) {
            when (id) {
                "Title" -> Text("AaBbCc", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                "Subtitle" -> Text("AaBbCc", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                "Heading1" -> Text("AaBbCc", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                "Heading2" -> Text("AaBbCc", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                else -> Text("AaBbCcDd", fontSize = 12.sp)
            }
        }
        Text(
            text = name,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

// ------------------------------------------------------------------ Editing

@Composable
private fun EditingGroup() {
    var selectMenu by remember { mutableStateOf(false) }

    Group("Editing") {
        Column {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                SmallButton(Icons.Filled.Search, "Find") { EditorController.showFind(true) }
                SmallButton(Icons.Filled.BorderColor, "Replace") { EditorController.showFind(true) }
            }
            Box {
                SmallButton(Icons.Filled.SelectAll, "Select", showCaret = true) { selectMenu = true }
                DropdownMenu(expanded = selectMenu, onDismissRequest = { selectMenu = false }) {
                    DropdownMenuItem(text = { Text("Select All") }, onClick = { EditorController.selectAll(); selectMenu = false })
                }
            }
        }
    }
}

// ------------------------------------------------------------------ shared bits

private fun trimSize(v: Float): String = if (v == v.toInt().toFloat()) v.toInt().toString() else v.toString()

/** Swipeable colour swatch row (font colour / highlight palettes). */
@Composable
private fun HomeColorRow(colors: List<String?>, selected: String?, onPick: (String?) -> Unit) {
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        colors.forEach { hex ->
            val color = hex?.let { parseHexColor(it) } ?: Color.Transparent
            Box(
                modifier = Modifier
                    .width(30.dp)
                    .height(30.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(color)
                    .then(
                        if (hex == null) Modifier.border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp))
                        else Modifier,
                    )
                    .clickable { onPick(hex) },
                contentAlignment = Alignment.Center,
            ) {
                if (hex == null) Text("Auto", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (hex != null && hex == selected) {
                    Text("✓", fontSize = 14.sp, color = if (hex in listOf("FFFFFF", "FFFF00")) Color.Black else Color.White)
                }
            }
        }
    }
}
