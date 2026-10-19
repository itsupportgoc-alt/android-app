/** Navigation pane composable. */
@Composable
fun NavigationPane(
    modifier: Modifier = Modifier,
    onSectionClicked: (String) -> Unit = {},
    collapsed: Boolean = false,
    onCollapseClicked: () -> Unit = {},
) {
    val colors = MaterialTheme.colorScheme
    val navItems = listOf(
        "Why share a studio?" to false,
        "What we agree on" to false,
        "Shared equipment" to false,
        "Opening the studio" to false,
        "Membership" to true,
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        // Navigation header
        Box(modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text(
                text = "Navigation",
                style = MaterialTheme.typography.h6,
                color = colors.onSurface,
            )

            Button(onClick = onCollapseClicked) {
                Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Collapse")
            }
        }

        // Search field
        OutlinedTextField(
            value = "Search document",
            onValueChange = {},
            readOnly = true,
            placeholder = { Text("Search document", modifier = Modifier.padding(vertical = 8.dp)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
        )

        // Tab navigation (Headings, Pages, Results)
        TabRow(
            modifier = Modifier.fillMaxHeight().height(36.dp),
            backgroundColor = colors.surface,
            contentColor = colors.onSurface,
        ) {
            PillTab("Headings", selected = true)
            PillTab("Pages", selected = false)
            PillTab("Results", selected = false)
        }

        // Heading tree
        Spacer(modifier = Modifier.height(8.dp))
        navItems.forEach { (text, isBold) ->
            TextButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onSectionClicked(text) },
            ) {
                Text(
                    text = text,
                    style = if (isBold) MaterialTheme.typography.h6 else MaterialTheme.typography.body2,
                    color = colors.onSurface,
                )
            }
        }
    }
}

/** Pill-shaped tab. */
private fun PillTab(text: String, selected: Boolean, modifier: Modifier = Modifier): Chip {
    return Chip(
        label = { Text(text, modifier = Modifier.padding(horizontal = 8.dp)) },
        backgroundColor = if (selected) MaterialTheme.colorScheme.primaryContainer else colors.surface,
        selected = selected,
        modifier = modifier
            .height(28.dp)
            .padding(2.dp),
    )
}