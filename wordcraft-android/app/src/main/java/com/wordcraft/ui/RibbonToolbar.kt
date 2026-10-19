/** Ribbon toolbar composable with all tool groups. */
@Composable
fun RibbonToolbar(
    modifier: Modifier = Modifier,
    onCut: () -> Unit = {},
    onCopy: () -> Unit = {},
    onPaste: () -> Unit = {},
    onBold: () -> Unit = {},
    onItalic: () -> Unit = {},
    onUnderline: () -> Unit = {},
    onStrikethrough: () -> Unit = {},
    onFontSizeGrow: () -> Unit = {},
    onFontSizeShrink: () -> Unit = {},
    onFontColor: (String) -> Unit = {},
    onHighlight: (String) -> Unit = {},
    onAlign: (String) -> Unit = {},
    onBullets: () -> Unit = {},
    onNumbering: () -> Unit = {},
    onPageBreak: () -> Unit = {},
    onFind: () -> Unit = {},
    onReplace: () -> Unit = {},
    onSelectAll: () -> Unit = {},
    onDictate: () -> Unit = {},
    onEditor: () -> Unit = {},
) {
    val colors = MaterialTheme.colorScheme
    val fontNames = listOf("Aptos", "Times New Roman", "Arial", "Calibri")
    val fontSizes = listOf(8f, 9f, 10f, 11f, 12f, 14f, 16f, 18f, 20f, 22f, 24f, 26f, 28f, 36f, 44f, 72f)

    // Clipboard group
    Box(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Paste
            OutlinedTextButton(
                modifier = Modifier.width(48.dp).height(36.dp),
                onClick = onPaste,
            ) {
                Icon(Icons.Default.ContentPaste, contentDescription = null)
            }
            Text("Paste")

            Separator(modifier = Modifier.width(8.dp))

            // Cut
            OutlinedTextButton(
                modifier = Modifier.width(48.dp).height(36.dp),
                onClick = onCut,
            ) {
                Icon(Icons.Default.ContentCut, contentDescription = null)
            }
            Text("Cut")

            Separator(modifier = Modifier.width(8.dp))

            // Copy
            OutlinedTextButton(
                modifier = Modifier.width(48.dp).height(36.dp),
                onClick = onCopy,
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null)
            }
            Text("Copy")

            Separator(modifier = Modifier.width(8.dp))

            // Format Painter
            OutlinedTextButton(
                modifier = Modifier.width(48.dp).height(36.dp),
                onClick = {},
            ) {
                Icon(Icons.Default.FormatPainter, contentDescription = null)
            }
            Text("Format Painter")
        }
    }

    // Font group
    Box(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            // Font family
            OutlinedTextField(
                value = "Aptos",
                onValueChange = {},
                readOnly = true,
                placeholder = { Text("Font") },
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp)
            )

            // Font size
            OutlinedTextField(
                value = "12",
                onValueChange = {},
                readOnly = true,
                placeholder = { Text("12") },
                modifier = Modifier
                    .width(48.dp)
                    .padding(start = 4.dp)
            )

            // A^ (grow)
            OutlinedTextButton(modifier = Modifier.width(24.dp), onClick = onFontSizeGrow) {
                Text("A^")
            }

            // A˅ (shrink)
            OutlinedTextButton(modifier = Modifier.width(24.dp), onClick = onFontSizeShrink) {
                Text("A˅")
            }

            // Bold
            ToggleIconButton(onToggle = onBold) {
                Icon(Icons.Default.ContentBold, contentDescription = "Bold")
            }

            // Italic
            ToggleIconButton(onToggle = onItalic) {
                Icon(Icons.Default.ContentItalic, contentDescription = "Italic")
            }

            // Underline
            ToggleIconButton(onToggle = onUnderline) {
                Icon(Icons.Default.ContentUnderline, contentDescription = "Underline")
            }

            // Strikethrough
            OutlinedTextButton(onClick = onStrikethrough) {
                Icon(Icons.Default.ContentStrikethrough, contentDescription = "Strikethrough")
            }

            // Text highlight
            OutlinedTextButton(onClick = { onHighlight("yellow") }) {
                Icon(Icons.Default.Highlight, contentDescription = "Highlight")
            }

            // Font color
            OutlinedTextButton(onClick = { onHighlight("#FF0000") }) {
                Icon(Icons.Default.TextFormatColor, contentDescription = "Color")
            }
        }
    }

    // Paragraph group
    Box(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            // Bullet list
            OutlinedTextButton(onClick = onBullets) {
                Icon(Icons.Default.ListBulleted, contentDescription = "Bullet list")
            }
            Text("Bullet")

            // Numbered list
            OutlinedTextButton(onClick = onNumbering) {
                Icon(Icons.Default.ListNumbers, contentDescription = "Numbered list")
            }
            Text("Numbering")

            // Multilevel list
            OutlinedTextButton(onClick = {}) {
                Icon(Icons.Default.FormatListNumbered, contentDescription = "Multilevel")
            }
            Text("Multilevel")

            // Align left
            OutlinedTextButton(onClick = { onAlign("left") }) {
                Icon(Icons.Default.AlignLeft, contentDescription = "Align left")
            }
            Text("Align left")

            // Center
            OutlinedTextButton(onClick = { onAlign("center") }) {
                Icon(Icons.Default.AlignCenter, contentDescription = "Center")
            }
            Text("Center")

            // Align right
            OutlinedTextButton(onClick = { onAlign("right") }) {
                Icon(Icons.Default.AlignRight, contentDescription = "Align right")
            }
            Text("Align right")

            // Justify
            OutlinedTextButton(onClick = {}) {
                Icon(Icons.Default.Justify, contentDescription = "Justify")
            }
            Text("Justify")
        }
    }

    // Additional paragraph tools (line spacing, shading, borders)
    Box(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Line spacing
            OutlinedTextButton(onClick = {}) {
                Icon(Icons.Default.LineSpacing, contentDescription = "Line spacing")
            }
            Text("Line spacing")

            // Shading
            OutlinedTextButton(onClick = {}) {
                Icon(Icons.Default.Background, contentDescription = "Shading")
            }
            Text("Shading")

            // Borders
            OutlinedTextButton(onClick = {}) {
                Icon(Icons.Default.BorderColor, contentDescription = "Borders")
            }
            Text("Borders")
        }
    }

    // Styles gallery
    Box(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            wrapContentWrap = Wrap.Hop,
        ) {
            styleChip("Normal", true)
            styleChip("No Spacing", false)
            styleChip("Heading 1", false)
            styleChip("Title", false)
            styleChip("Heading 2", false)
            styleChip("Subtitle", false)
        }
    }

    // Editing group
    Box(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            // Find
            OutlinedTextButton(onClick = onFind) {
                Icon(Icons.Default.Find, contentDescription = "Find")
            }
            Text("Find")

            // Replace
            OutlinedTextButton(onClick = onReplace) {
                Icon(Icons.Default.Replace, contentDescription = "Replace")
            }
            Text("Replace")

            // Select
            OutlinedTextButton(onClick = onSelectAll) {
                Icon(Icons.Default.SelectAll, contentDescription = "Select")
            }
            Text("Select")

            // Dictate
            OutlinedTextButton(onClick = onDictate) {
                Icon(Icons.Default.Mic, contentDescription = "Dictate")
            }
            Text("Dictate")

            // Editor
            OutlinedTextButton(onClick = onEditor) {
                Icon(Icons.Default.Editor, contentDescription = "Editor")
            }
            Text("Editor")
        }
    }
}

/** Helper to create a style chip. */
private fun styleChip(text: String, selected: Boolean, onClick: () -> Unit = {}): Chip {
    return Chip(
        label = { Text(text) },
        checkable = true,
        selected = selected,
        onClick = onClick,
        modifier = Modifier
            .padding(2.dp),
    )
}