/** Status bar composable replicating the Microsoft Word status bar. */
@Composable
fun StatusBar(
    wordCount: Int = 357,
    pageCount: Int = 2,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme

    Row(
        modifier = modifier
            .height(32.dp)
            .background(colors.surface),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // Left group: Page/word info
        Box(modifier = Modifier.padding(start = 16.dp)) {
            Text(
                text = "Page $pageCount of 2",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )

            Text(
                text = "$wordCount of 357 words",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp),
            )

            Text(
                text = "English (United States)",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp),
            )
        }

        // Right group: Focus, Reading view, Page layout, Zoom controls
        Box(modifier = Modifier.padding(end = 16.dp)) {
            // Focus mode
            IconButton(modifier = Modifier.width(24.dp), onClick = {}) {
                Icon(Icons.Default.Focus, contentDescription = "Focus mode")
            }

            // Reading view
            IconButton(modifier = Modifier.width(24.dp), onClick = {}) {
                Icon(Icons.Default.ReadMore, contentDescription = "Reading view")
            }

            // Page layout
            IconButton(modifier = Modifier.width(24.dp), onClick = {}) {
                Icon(Icons.Default.FormatListBulleted, contentDescription = "Page layout")
            }

            // Zoom controls in a row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.wrapContentWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                // Zoom out
                IconButton(modifier = Modifier.width(24.dp), onClick = {}) {
                    Icon(Icons.ContentRemove, contentDescription = "Zoom out")
                }

                // Zoom slider
                OutlinedTextField(
                    value = "100%",
                    onValueChange = {},
                    readOnly = true,
                    placeholder = { Text("100%", modifier = Modifier.width(48.dp)) },
                    modifier = Modifier
                        .width(60.dp)
                        .height(24.dp)
                )

                // Zoom in
                IconButton(modifier = Modifier.width(24.dp), onClick = {}) {
                    Icon(Icons.ContentAdd, contentDescription = "Zoom in")
                }
            }
        }
    }
}