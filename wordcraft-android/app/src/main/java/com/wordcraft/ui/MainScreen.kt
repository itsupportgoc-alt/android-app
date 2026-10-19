/** Main screen composable with responsive tablet/phone layout. */
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    // Screen width for determining phone vs tablet
    targetWidth: (WindowSizeClass) -> Unit = { /* will be provided by container */ },
) {
    val windowSizeClass = LocalWindowSizeClass.current
    val isTablet = windowSizeClass.widthSizeClass >= WindowSizeClass.Compact528.dp.width
        && windowSizeClass.heightSizeClass >= WindowSizeClass.Compact528.dp.height

    // hero.webp image
    val painter = rememberPainSource(
        model = rememberImagePainSourceData(R.drawable.hero)
    )

    if (isTablet) {
        // TABLET LAYOUT: Full ribbon, navigation pane, and page side by side
        MaterialTheme {
            val (navWidth, pageWidth) = if (LocalConfig.isLandscape) {
                // Landscape tablet: nav 280dp, page fills rest
                val navSize = 280.dp
                val availableWidth = max(WindowSizeClass.size.width - navSize, 0.dp)
                navSize to availableWidth
            } else {
                // Portrait tablet: vertical split, nav fixed width, page takes remainder
                val navSize = 280.dp
                val availableWidth = max(WindowSizeClass.size.width - navSize, 0.dp)
                navSize to availableWidth
            }

            Scaffold(
                modifier = modifier,
            ) { padding ->
                // Top bar - TitleBar
                TopBar(
                    title = "The Open Studio Handbook — Not saved",
                    onSearchClicked = {},
                    onDiscordClicked = {},
                    onAvatarClicked = {},
                )

                // Tablet: Split screen with NavigationPane + DocumentPage
                BoxWithConstraints(
                    maxWidth = WindowSizeClass.size.width,
                ) {
                    // Left: Navigation Pane
                    NavigationPane(
                        modifier = Modifier
                            .width(280.dp)
                            .weight(0f)
                            .background(MaterialTheme.colorScheme.surface),
                        onSectionClicked = { /* handle section click */ },
                    )

                    // Right: Document area with Ribbon + Page
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize(),
                    ) {
                        // Ribbon tabs at top
                        RibbonTabs(
                            selectedTab = 0,
                            onTabSelected = { /* handle tab select */ },
                        )

                        // Ribbon toolbar
                        RibbonToolbar()

                        // Document page
                        DocumentPage(
                            modifier = Modifier
                                .fillMaxSize(),
                        )

                        // Status bar at bottom
                        StatusBar(
                            wordCount = 357,
                            pageCount = 2,
                        )
                    }
                }
            }
        }
    } else {
        // PHONE LAYOUT: Collapsed navigation, scrollable ribbon, full-width page
        MaterialTheme {
            Scaffold(
                modifier = modifier,
            ) { padding ->
                // Top bar - TitleBar
                TopBar(
                    title = "The Open Studio Handbook — Not saved",
                    onSearchClicked = {},
                    onDiscordClicked = {},
                    onAvatarClicked = {},
                )

                // Phone: Vertical arrangement - Ribbon then Document + Status
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Start,
                    horizontalAlignment = Alignment.Start,
                ) {
                    // Ribbon (horizontally scrollable or compact 2-row)
                    TabRow(
                        modifier = Modifier
                            .height(kToolbarLarge)
                            .fillMaxWidth(),
                        tabs = {
                            // All tabs scroll horizontally
                            Tab(text = { Text("File") })
                            Tab(text = { Text("Home") })
                            Tab(text = { Text("Insert") })
                            Tab(text = { Text("Draw") })
                            Tab(text = { Text("Design") })
                            Tab(text = { Text("Layout") })
                            Tab(text = { Text("References") })
                            Tab(text = { Text("Mailings") })
                            Tab(text = { Text("Review") })
                            Tab(text = { Text("View") })
                            Tab(text = { Text("Help") })
                        },
                    )

                    // Ribbon toolbar (compact version for phone)
                    RibbonToolbar(
                        onCut = {},
                        onCopy = {},
                        onPaste = {},
                        onBold = {},
                        onItalic = {},
                        onUnderline = {},
                        onStrikethrough = {},
                        onFontSizeGrow = {},
                        onFontSizeShrink = {},
                        onFontColor = {},
                        onHighlight = {},
                        onAlign = {},
                        onBullets = {},
                        onNumbering = {},
                        onPageBreak = {},
                        onFind = {},
                        onReplace = {},
                        onSelectAll = {},
                        onDictate = {},
                        onEditor = {},
                    )

                    // Document page - fills entire width with vertical scroll
                    DocumentPage(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                    )

                    // Status bar pinned to bottom
                    StatusBar(
                        wordCount = 357,
                        pageCount = 2,
                    )
                }
            )
        }
    }
}

/** Top bar composable matching TitleBar specs. */
@Composable
fun TopBar(
    title: String = "The Open Studio Handbook — Not saved",
    onSearchClicked: () -> Unit = {},
    onDiscordClicked: () -> Unit = {},
    onAvatarClicked: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // Left: AutoSave toggle + control icons
        Box {
            // AutoSave switch
            Switch(
                checked = false,
                onCheckedChange = {},
                modifier = Modifier.size(24.dp)
            )

            // Control icons row
            Row(
                modifier = Modifier.fillMaxWidth().wrapContentHorizontally(Arrangement.End),
            ) {
                IconButton(modifier = Modifier.width(24.dp)) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null)
                }
                IconButton(modifier = Modifier.width(24.dp)) {
                    Icon(Icons.Default.ArrowForward, contentDescription = null)
                }
                IconButton(modifier = Modifier.width(24.dp)) {
                    Icon(Icons.Default.ContentSave, contentDescription = null)
                }
                IconButton(modifier = Modifier.width(24.dp)) {
                    Icon(Icons.Default.Undo, contentDescription = null)
                }
                IconButton(modifier = Modifier.width(24.dp)) {
                    Icon(Icons.Default.Redo, contentDescription = null)
                }
            }
        }

        // Center: Title
        Text(
            text = title,
            style = MaterialTheme.typography.h6,
            color = colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        // Right: Search, Discord, Avatar
        Box {
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
        }
    }
}