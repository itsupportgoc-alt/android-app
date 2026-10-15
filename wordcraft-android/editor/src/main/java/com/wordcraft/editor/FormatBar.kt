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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignJustify
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.FormatAlignRight
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertLink
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wordcraft.ui.parseHexColor

/** Word's highlighter colours (engine `format.highlight` accepts these names). */
val HighlightPalette: List<Pair<String, String>> = listOf(
    "yellow" to "FFFF00",
    "brightGreen" to "00FF00",
    "turquoise" to "00FFFF",
    "pink" to "FF00FF",
    "blue" to "0000FF",
    "red" to "FF0000",
    "darkBlue" to "000080",
    "teal" to "008080",
    "green" to "008000",
    "violet" to "800080",
    "darkRed" to "800000",
    "darkYellow" to "808000",
    "gray50" to "808080",
    "gray25" to "C0C0C0",
    "black" to "000000",
)

/** Text colours for `format.color` (RRGGBB). */
val TextColorPalette: List<String?> = listOf(
    null, // auto
    "000000", "434343", "666666", "999999", "BFBFBF",
    "C00000", "FF0000", "FF6600", "FFC000", "FFFF00",
    "92D050", "00B050", "00B0F0", "0070C0", "002060", "7030A0", "FFFFFF",
)

val FontSizes: List<Float> = listOf(8f, 9f, 10f, 10.5f, 11f, 12f, 14f, 16f, 18f, 20f, 22f, 24f, 26f, 28f, 36f, 48f, 72f)

val FontNames: List<String> = listOf(
    "Aptos", "Calibri", "Cambria", "Arial", "Times New Roman", "Courier New", "Verdana", "Georgia", "Comic Sans MS",
)

val Styles: List<Pair<String, String>> = listOf(
    "Normal" to "Normal",
    "Heading1" to "Heading 1",
    "Heading2" to "Heading 2",
    "Heading3" to "Heading 3",
    "Title" to "Title",
    "Subtitle" to "Subtitle",
)

/**
 * The compact mobile toolbar: `B I U S · Align · + · ⋮`.
 * The "+" menu inserts; "⋮" opens the full formatting sheet.
 */
@Composable
fun FormatBar(
    onPickImage: () -> Unit,
    onInsertLink: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = EditorController.state
    val f = state.format
    val p = state.para
    var insertMenu by remember { mutableStateOf(false) }
    var alignMenu by remember { mutableStateOf(false) }
    var sheet by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Toggle(Icons.Filled.FormatBold, "Bold", f.bold) { EditorController.toggleBold() }
            Toggle(Icons.Filled.FormatItalic, "Italic", f.italic) { EditorController.toggleItalic() }
            Toggle(Icons.Filled.FormatUnderlined, "Underline", f.underline) { EditorController.toggleUnderline() }
            Toggle(Icons.Filled.FormatStrikethrough, "Strikethrough", f.strike) { EditorController.toggleStrike() }

            Box {
                IconButton(onClick = { alignMenu = true }) {
                    Icon(
                        imageVector = when (p.align) {
                            "center" -> Icons.Filled.FormatAlignCenter
                            "right" -> Icons.Filled.FormatAlignRight
                            "justify", "distribute" -> Icons.Filled.FormatAlignJustify
                            else -> Icons.Filled.FormatAlignLeft
                        },
                        contentDescription = "Alignment",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                DropdownMenu(expanded = alignMenu, onDismissRequest = { alignMenu = false }) {
                    DropdownMenuItem(text = { Text("Left") }, onClick = { EditorController.setAlign("left"); alignMenu = false })
                    DropdownMenuItem(text = { Text("Center") }, onClick = { EditorController.setAlign("center"); alignMenu = false })
                    DropdownMenuItem(text = { Text("Right") }, onClick = { EditorController.setAlign("right"); alignMenu = false })
                    DropdownMenuItem(text = { Text("Justify") }, onClick = { EditorController.setAlign("justify"); alignMenu = false })
                }
            }

            Box {
                IconButton(onClick = { insertMenu = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "Insert", tint = MaterialTheme.colorScheme.onSurface)
                }
                DropdownMenu(expanded = insertMenu, onDismissRequest = { insertMenu = false }) {
                    DropdownMenuItem(text = { Text("Table") }, onClick = { EditorController.insertTable(3, 3); insertMenu = false })
                    DropdownMenuItem(text = { Text("Link") }, onClick = { insertMenu = false; onInsertLink() })
                    DropdownMenuItem(text = { Text("Image") }, onClick = { insertMenu = false; onPickImage() })
                    DropdownMenuItem(text = { Text("Page break") }, onClick = { EditorController.insertPageBreak(); insertMenu = false })
                    DropdownMenuItem(text = { Text("Bullets") }, onClick = { EditorController.toggleBullets(); insertMenu = false })
                    DropdownMenuItem(text = { Text("Numbering") }, onClick = { EditorController.toggleNumbering(); insertMenu = false })
                }
            }

            Spacer(Modifier.weight(1f))

            Toggle(Icons.Filled.FormatListBulleted, "Bullets", p.inList && p.style == "ListParagraph") {
                EditorController.toggleBullets()
            }
            IconButton(onClick = { EditorController.showFind(true) }) {
                Icon(Icons.Filled.Search, contentDescription = "Find", tint = MaterialTheme.colorScheme.onSurface)
            }
            IconButton(onClick = { sheet = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "More", tint = MaterialTheme.colorScheme.onSurface)
            }
        }
    }

    if (sheet) {
        FormatSheet(onDismiss = { sheet = false }, onPickImage = onPickImage, onInsertLink = onInsertLink)
    }
}

@Composable
private fun Toggle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    // Word ribbon style: light-blue tile with the Word-blue glyph when active.
    val bg = if (active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    IconButton(onClick = onClick) {
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
            )
        }
    }
}

/** The expandable formatting tools: font, size, colours, alignment, lists, indent, styles. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormatSheet(
    onDismiss: () -> Unit,
    onPickImage: () -> Unit,
    onInsertLink: () -> Unit,
) {
    val state = EditorController.state
    val f = state.format
    val p = state.para
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            // Styles
            SectionLabel("Styles")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Styles.forEach { (id, name) ->
                    Chip(name, selected = f.style == id) { EditorController.applyStyle(id) }
                }
            }

            Spacer(Modifier.height(12.dp))
            SectionLabel("Font")
            Row(verticalAlignment = Alignment.CenterVertically) {
                FontDropdown(f.font ?: "Aptos")
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = { EditorController.shrinkFont() }) { Text("A−", fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                Text(
                    text = f.sizePt?.let { trimSize(it) } ?: "—",
                    modifier = Modifier.width(48.dp),
                    style = MaterialTheme.typography.titleMedium,
                )
                IconButton(onClick = { EditorController.growFont() }) { Text("A+", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FontSizes.forEach { s ->
                    Chip(trimSize(s), selected = f.sizePt == s) { EditorController.setFontSize(s) }
                }
            }

            Spacer(Modifier.height(12.dp))
            SectionLabel("Text colour")
            ColorRow(colors = TextColorPalette, selected = f.color, onPick = { hex ->
                EditorController.setTextColor(hex ?: "auto")
            })
            Spacer(Modifier.height(8.dp))
            SectionLabel("Highlight")
            ColorRow(
                colors = HighlightPalette.map { it.second },
                selected = f.highlight,
                onPick = { hex ->
                    val name = HighlightPalette.firstOrNull { it.second == hex }?.first ?: "yellow"
                    EditorController.setHighlight(name)
                },
            )

            Spacer(Modifier.height(12.dp))
            SectionLabel("Paragraph")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconChip(Icons.Filled.FormatAlignLeft, p.align == "left") { EditorController.setAlign("left") }
                IconChip(Icons.Filled.FormatAlignCenter, p.align == "center") { EditorController.setAlign("center") }
                IconChip(Icons.Filled.FormatAlignRight, p.align == "right") { EditorController.setAlign("right") }
                IconChip(Icons.Filled.FormatAlignJustify, p.align == "justify" || p.align == "distribute") { EditorController.setAlign("justify") }
                Spacer(Modifier.width(8.dp))
                IconChip(Icons.Filled.FormatListBulleted, p.inList) { EditorController.toggleBullets() }
                IconChip(Icons.Filled.FormatListNumbered, p.inList) { EditorController.toggleNumbering() }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("Indent +", selected = false) { EditorController.indent() }
                Chip("Indent −", selected = false) { EditorController.outdent() }
                Chip("Clear formatting", selected = false) { EditorController.clearFormatting() }
            }

            Spacer(Modifier.height(12.dp))
            SectionLabel("Insert")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("Table", selected = false) { EditorController.insertTable(3, 3) }
                Chip("Link", selected = false) { onInsertLink() }
                Chip("Image", selected = false) { onPickImage() }
                Chip("Page break", selected = false) { EditorController.insertPageBreak() }
            }

            Spacer(Modifier.height(12.dp))
            SectionLabel("Document")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${state.wordCount} words",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(8.dp))
                Chip("Undo", selected = false) { EditorController.undo() }
                Chip("Redo", selected = false) { EditorController.redo() }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 6.dp),
    )
}

@Composable
private fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
    val fg = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Text(text, color = fg, fontSize = 13.sp)
    }
}

@Composable
private fun IconChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val bg = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(8.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun ColorRow(colors: List<String?>, selected: String?, onPick: (String?) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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

@Composable
private fun FontDropdown(current: String) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(current, fontSize = 14.sp)
            Icon(Icons.Filled.ArrowDropDown, contentDescription = "Font", tint = MaterialTheme.colorScheme.onSurface)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            FontNames.forEach { name ->
                DropdownMenuItem(text = { Text(name) }, onClick = { EditorController.setFont(name); expanded = false })
            }
        }
    }
}

private fun trimSize(v: Float): String = if (v == v.toInt().toFloat()) v.toInt().toString() else v.toString()
