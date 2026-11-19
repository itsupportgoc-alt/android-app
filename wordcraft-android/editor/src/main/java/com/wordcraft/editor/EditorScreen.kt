package com.wordcraft.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wordcraft.documentcore.EditorState

/**
 * The document surface for the View-tab copy. Everything visible here is driven by [ViewState]:
 *
 *  - **Ruler** (Show ▸ Ruler): horizontal ruler above the page + vertical strip down its edge.
 *  - **Gridlines** (Show ▸ Gridlines): a real grid overlay on the page sheet.
 *  - **Navigation Pane** (Show ▸ Navigation Pane): the left-docked pane, toggleable.
 *  - **Views / Page Movement**: Print/Web/Read/Outline/Draft change the surface itself;
 *    Side to Side pages horizontally instead of scrolling vertically.
 *  - **Zoom**: pinch + the status bar/ribbon controls set [ViewState.zoom]; the page reflows
 *    (the text scales, the sheet keeps the width of the page) so a zoomed document never runs
 *    off the screen, on a phone or on a tablet.
 *  - **Zoom presets** (One Page / Multiple / Page Width): resolved here, where pixel sizes are known.
 *  - **Split / Side by Side / Arrange All / Sync scroll**: two real panes, optionally linked.
 *
 * Sizing: the page sheet is never wider than the viewport (minus its margins) and always takes
 * the height it is given, so the last paragraph of a long document stays reachable on a phone;
 * the ruler is drawn at the sheet's own width instead of the page's 595pt width.
 */
@Composable
fun EditorScreen(modifier: Modifier = Modifier) {
    val state = EditorController.state
    val items = remember(state) { EditorController.items() }

    BoxWithConstraints(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val viewportW = this.maxWidth
        val viewportH = this.maxHeight

        // Resolve pending zoom presets now that pixel sizes are known.
        val density = LocalDensity.current
        LaunchedEffect(ViewState.zoom, ZoomTargets.pending, viewportW, viewportH) {
            val preset = ZoomTargets.pending ?: return@LaunchedEffect
            val pageW = PAGE_WIDTH_PT.dp
            val pageH = PAGE_HEIGHT_PT.dp
            when (preset) {
                ViewState.ZoomPreset.PAGE_WIDTH ->
                    ViewState.zoomFitPageWidth(with(density) { viewportW.toPx() }, with(density) { pageW.toPx() })
                ViewState.ZoomPreset.ONE_PAGE ->
                    ViewState.zoomFitOnePage(with(density) { viewportH.toPx() }, with(density) { pageH.toPx() })
                ViewState.ZoomPreset.MULTIPLE ->
                    ViewState.zoomMultiplePages(with(density) { viewportW.toPx() }, with(density) { pageW.toPx() })
            }
            ZoomTargets.consume()
        }

        // Focus mode: hide chrome around the page (the page remains fully editable).
        val chromeVisible = !ViewState.focusMode
        // The navigation pane needs room beside the page; below this it would leave a sliver.
        val navPaneFits = viewportW >= 480.dp

        Box(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxSize()) {
                if (chromeVisible && ViewState.showNavPane && navPaneFits) {
                    NavigationPane(
                        items = items,
                        onNavigate = { idx -> scopeScrollTarget(idx) },
                        onClose = { ViewState.closeNavPane() },
                        width = if (viewportW >= 840.dp) 260.dp else 200.dp,
                    )
                }

                Column(Modifier.weight(1f).fillMaxHeight()) {
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        when {
                            ViewState.splitView -> SplitSurface(items, state, viewportW)
                            ViewState.view == ViewState.DocView.READ_MODE -> ReadModeSurface(items, state, viewportW)
                            ViewState.view == ViewState.DocView.WEB_LAYOUT -> WebLayoutSurface(items, state)
                            ViewState.view == ViewState.DocView.OUTLINE ||
                                ViewState.view == ViewState.DocView.DRAFT -> PlainSurface(items, state)
                            ViewState.pageMovement == ViewState.PageMovement.SIDE_TO_SIDE ->
                                SideToSideSurface(items, state, viewportW)
                            else -> SingleSurface(items, state, viewportW)
                        }
                    }
                }
            }

            // A phone has no room to dock the pane beside the page, so it slides over the page
            // and a tap on the dimmed area closes it. The control works at every width.
            if (chromeVisible && ViewState.showNavPane && !navPaneFits) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.25f))
                        .clickable { ViewState.closeNavPane() },
                )
                NavigationPane(
                    items = items,
                    onNavigate = { idx -> scopeScrollTarget(idx) },
                    onClose = { ViewState.closeNavPane() },
                    modifier = Modifier.align(Alignment.CenterStart),
                    width = 280.dp,
                )
            }
        }
    }
}

// scroll target plumbing (set by nav pane jumps)
private var pendingScrollIndex: Int? = null
private fun scopeScrollTarget(idx: Int) { pendingScrollIndex = idx }

/** The page width the sheet can use: the page's own width, clamped to the viewport. */
private fun sheetWidthFor(viewportW: Dp): Dp = minOf(viewportW - 24.dp, PAGE_WIDTH_PT.dp)

// ------------------------------------------------------------------ surfaces

@Composable
private fun SingleSurface(items: List<EditorItem>, state: EditorState, viewportW: Dp) {
    val listState = rememberLazyListState()
    ConsumeScrollTarget(listState, items.size)
    val sheetWidth = sheetWidthFor(viewportW)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, _, gestureZoom, _ ->
                    if (gestureZoom != 1f) ViewState.applyZoom(ViewState.zoom * gestureZoom)
                }
            }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (ViewState.showRuler && !ViewState.focusMode) {
            HorizontalRuler(pageWidth = sheetWidth)
        }
        PageSheet(items, state, listState, sheetWidth, Modifier.weight(1f))
    }
}

/** Read Mode: no page sheet, text on the canvas directly, wider measure. */
@Composable
private fun ReadModeSurface(items: List<EditorItem>, state: EditorState, viewportW: Dp) {
    val listState = rememberLazyListState()
    ConsumeScrollTarget(listState, items.size)
    val side = if (viewportW < 600.dp) 20.dp else 32.dp
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        ZoomedContent {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = side, end = side, top = 24.dp, bottom = 64.dp),
            ) { EditorItems(items, state) }
        }
    }
}

/** Web Layout: continuous sheet, no page edges (full-width reflow). */
@Composable
private fun WebLayoutSurface(items: List<EditorItem>, state: EditorState) {
    val listState = rememberLazyListState()
    ConsumeScrollTarget(listState, items.size)
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Surface(
            Modifier.fillMaxSize().padding(horizontal = 4.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            ZoomedContent {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 64.dp),
                ) { EditorItems(items, state) }
            }
        }
    }
}

/** Outline / Draft: plain continuous list, no sheet chrome, tighter spacing. */
@Composable
private fun PlainSurface(items: List<EditorItem>, state: EditorState) {
    val listState = rememberLazyListState()
    ConsumeScrollTarget(listState, items.size)
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        ZoomedContent {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 64.dp),
            ) { EditorItems(items, state) }
        }
    }
}

/** Two real panes of the same document (Split); [horizontal] = side-by-side. Sync links scroll. */
@Composable
private fun SplitSurface(
    items: List<EditorItem>,
    state: EditorState,
    viewportW: Dp,
) {
    val a = rememberLazyListState()
    val b = rememberLazyListState()
    // Side by side needs two readable columns; on a phone the split stays top/bottom.
    val horizontal = ViewState.sideBySide && viewportW >= 600.dp
    // Synchronous scrolling: mirror first-visible index across panes.
    LaunchedEffect(ViewState.syncScroll) {
        if (!ViewState.syncScroll) return@LaunchedEffect
        while (true) {
            val firstA = a.firstVisibleItemIndex
            val firstB = b.firstVisibleItemIndex
            if (firstA != firstB) {
                if (a.isScrollInProgress) b.scrollToItem(firstA) else if (b.isScrollInProgress) a.scrollToItem(firstB)
            }
            kotlinx.coroutines.delay(16)
        }
    }
    if (horizontal) {
        Row(Modifier.fillMaxSize()) {
            Pane(items, state, a, Modifier.weight(1f).fillMaxHeight())
            Pane(items, state, b, Modifier.weight(1f).fillMaxHeight())
        }
    } else {
        Column(Modifier.fillMaxSize()) {
            Pane(items, state, a, Modifier.weight(1f).fillMaxWidth())
            HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.outline)
            Pane(items, state, b, Modifier.weight(1f).fillMaxWidth())
        }
    }
}

@Composable
private fun Pane(
    items: List<EditorItem>,
    state: EditorState,
    listState: LazyListState,
    modifier: Modifier,
) {
    Box(modifier.background(MaterialTheme.colorScheme.surface)) {
        ZoomedContent {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 48.dp),
            ) { EditorItems(items, state) }
        }
    }
}

/** Side to Side: horizontal row of page-width sheets, each scrolling on its own. */
@Composable
private fun SideToSideSurface(items: List<EditorItem>, state: EditorState, viewportW: Dp) {
    val scroll = rememberScrollState()
    val sheetWidth = sheetWidthFor(viewportW)
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            Modifier
                .fillMaxSize()
                .horizontalScroll(scroll)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            repeat(3) {
                Surface(
                    Modifier.width(sheetWidth).fillMaxHeight(),
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 6.dp,
                ) {
                    val listState = rememberLazyListState()
                    ZoomedContent {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 48.dp),
                        ) { EditorItems(items, state) }
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------ shared pieces

@Composable
private fun PageSheet(
    items: List<EditorItem>,
    state: EditorState,
    listState: LazyListState,
    sheetWidth: Dp,
    modifier: Modifier,
) {
    // Page margins are in points; drawing them at the sheet's own scale keeps the text column
    // proportional to the page instead of eating a phone's whole width.
    val scale = sheetWidth.value / PAGE_WIDTH_PT
    Box(modifier.width(sheetWidth)) {
        Surface(
            Modifier.fillMaxSize(),
            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp,
        ) {
            Box(Modifier.fillMaxSize().clipToBounds()) {
                ZoomedContent {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = (RulerState.leftMarginPt * scale).dp,
                            end = (RulerState.rightMarginPt * scale).dp,
                            top = (RulerState.topMarginPt * scale).dp,
                            bottom = 48.dp,
                        ),
                    ) { EditorItems(items, state) }
                }
                if (ViewState.showGridlines) GridlinesOverlay()
            }
        }
        // Vertical ruler down the left edge of the sheet.
        if (ViewState.showRuler && !ViewState.focusMode) {
            VerticalRuler(Modifier.align(Alignment.CenterStart).fillMaxHeight())
        }
    }
}

/** Shared LazyColumn item builder so every surface renders the same engine content. */
private fun LazyListScope.EditorItems(
    items: List<EditorItem>,
    state: EditorState,
) {
    items(
        count = items.size,
        key = { i ->
            val it = items[i]
            when (it) {
                is EditorItem.Section -> "s:" + it.item.key
                is EditorItem.Table -> "t:" + it.vm.path.joinToString("/")
            }
        },
    ) { i ->
        when (val it = items[i]) {
            is EditorItem.Section -> SectionField(it.item, state)
            is EditorItem.Table -> TableBlock(it.vm, state, Modifier.padding(vertical = 6.dp))
        }
    }
}

/** Real gridlines overlay (Show ▸ Gridlines). */
@Composable
private fun GridlinesOverlay() {
    val line = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    Canvas(Modifier.fillMaxSize()) {
        val step = 36f
        var x = 0f
        while (x < size.width) { drawLine(line, Offset(x, 0f), Offset(x, size.height)); x += step }
        var y = 0f
        while (y < size.height) { drawLine(line, Offset(0f, y), Offset(size.width, y)); y += step }
    }
}

/**
 * Applies the view zoom by scaling the text (fontScale), so the page reflows inside the sheet
 * instead of being drawn larger than the screen. When the engine exposes its own layout zoom
 * (`view.zoom`), the engine has already changed the text sizes and nothing extra is applied.
 */
@Composable
private fun ZoomedContent(content: @Composable () -> Unit) {
    if (ViewState.engineZoomAvailable || ViewState.zoom == 1f) {
        content()
        return
    }
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(
            density = density.density,
            fontScale = density.fontScale * ViewState.zoom,
        ),
    ) { content() }
}

/** Consume a pending navigation jump by scrolling the list. */
@Composable
private fun ConsumeScrollTarget(listState: LazyListState, count: Int) {
    val target = pendingScrollIndex
    LaunchedEffect(target, count) {
        if (target != null && target in 0 until count) {
            listState.animateScrollToItem(target)
        }
        pendingScrollIndex = null
    }
}
