/** Ribbon tabs row composable. */
@Composable
fun RibbonTabs(
    selectedTab: Int = 0,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val tabs = listOf(
        "File",
        "Home",
        "Insert",
        "Draw",
        "Design",
        "Layout",
        "References",
        "Mailings",
        "Review",
        "View",
        "Help"
    )

    TabRow(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        backgroundColor = colors.surface,
        contentColor = colors.onSurface,
        selectedTabIndex = selectedTab,
        onTabIndexChange = { onTabSelected(it) },
        selectedContentColor = colors.primary,
        unselectedContentColor = colors.onSurface,
    ) {
        tabs.forEachIndexed { index, text ->
            Tab(
                text = { Text(text) },
                icon = { /* TODO: add icon */ },
                selected = selectedTab == index,
            ) {
                onTabSelected(index)
            }
        }
    }
}