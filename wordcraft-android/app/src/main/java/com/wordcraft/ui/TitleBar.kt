/** Title bar composable replicating the Microsoft Word title bar. */
@Composable
fun TitleBar(
    title: String = "The Open Studio Handbook",
    autoSave: Boolean = false,
    onSearchClicked: () -> Unit = {},
    onDiscordClicked: () -> Unit = {},
    onAvatarClicked: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val autoSaveSwitch = remember { SwitchDefaults() }

    Row(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // Left group: AutoSave toggle + document control icons
        Box(modifier = Modifier.fillMaxWidth()) {
            // AutoSave toggle
            Box(
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(36.dp)
            ) {
                Switch(
                    checked = autoSave,
                    onCheckedChange = { /* TODO: implement */ },
                    modifier = Modifier.size(24.dp)
                )
            }

            // Document control icons (back, forward, save, undo, redo, ...)
            val controls = [
                { /* back */ },
                { /* forward */ },
                { /* save */ },
                { /* undo */ },
                { /* redo */ },
            ]

            Row(
                modifier = Modifier.fillMaxWidth().wrapContentHorizontally(horizontalArrangement = Arrangement.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                controls.forEach { control ->
                    IconButton(modifier = Modifier.width(24.dp)) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                }
            }
        }

        // Center: Window title
        Text(
            text = "$title — Not saved",
            style = MaterialTheme.typography.h6,
            color = colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        // Right group: Search, Discord button, Avatar & Info
        Box(
            modifier = Modifier
                .padding(end = 4.dp)
                .wrapContentWidth(WrapContentWidth.MaxLinesLimit(1))
        ) {
            // Search field
            OutlinedTextField(
                value = "",
                onValueChange = {},
                readOnly = true,
                placeholder = { Text("🔍 Search commands and help", modifier = Modifier.padding(start = 4.dp)) },
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .padding(start = 4.dp, end = 4.dp),
            )

            // Discord button
            OutlinedTextField(
                value = "",
                onValueChange = {},
                readOnly = true,
                placeholder = { Text("Discord", modifier = Modifier.padding(start = 4.dp)) },
                modifier = Modifier
                    .fillMaxWidth(false)
                    .width(60.dp)
            )

            // Avatar
            Box(
                modifier = Modifier.size(36.dp)
                    .background(colorScheme.primary),
                contentAlignment = Alignment.Center,
                border = BorderStroke(2.dp, colorScheme.onPrimary),
            ) {
                Text(
                    text = "WU",
                    color = colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                )
            }

            // Info (i) circle button
            OutlinedTextButton(
                modifier = Modifier.size(32.dp).padding(start = 8.dp),
                onClick = onAvatarClicked,
            ) {
                Icon(Icons.Outlined.Info, contentDescription = null)
            }
            Text("i")
        }
    }
}