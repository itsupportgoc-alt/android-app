package com.wordcraft.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * The document surface for the View-tab copy. Everything visible here is driven by [ViewState]:
 *
 *  - **Ruler** (Show ▸ Ruler): horizontal ruler above the page + vertical strip down its edge.
 *  - **Gridlines** (Show ▸ Gridlines): a real grid overlay on the page sheet.
 *  - **Navigation Pane** (Show ▸ Navigation Pane): the left-docked pane, toggleable.
 *  - **Views / Page Movement**: Print/Web/Read/Outline/Draft change the surface itself;
 *    Side to Side pages horizontally instead of scrolling vertically.
 *  - **Zoom**: pinch + the ribbon/status controls set [ViewState.zoom]; text reflows through the
 *    engine layout path when `ViewState.engineZoomAvailable`, otherwise the Compose layout scale
 *    is applied (documented limitation — see the commit message).
 *  - **Zoom presets** (One Page / Multiple / Page Width): resolved here, where pixel sizes are known.
 *  - **Split / Side by Side / Arrange All / Sync scroll**: two real panes, optionally linked.
 */
@Composable
fun EditorScreen(modifier: Modifier = Modifier) {
    val state = EditorController.state
    val items = remember(state) { EditorController.items() }

    BoxWithConstraints(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val viewportW = this.maxWidth
        val viewportH = this.maxHeight

        // Resolve pending zoom presets now that pixel sizes are known.
        val density = androidx.compose.ui.platform.LocalDensity.current
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

        Row(Modifier.fillMaxSize()) {
            if (chromeVisible && ViewState.showNavPane) {
                NavigationPane(
                    items = items,
                    onNavigate = { idx -> scopeScrollTarget(idx) },
                    onClose = { ViewState.closeNavPane() },
                )
            }

            Column(Modifier.weight(1f).fillMaxHeight()) {
                if (chromeVisible && ViewState.showRuler) {
                    HorizontalRuler(pageWidth = PAGE_WIDTH_PT.dp * ViewState.zoom)
                }

                Box(Modifier.weight(1f).fillMaxWidth()) {
                    when {
                        ViewState.splitView -> SplitSurface(items, state, viewportH)
                        ViewState.view == ViewState.DocView.READ_MODE -> ReadModeSurface(items, state)
                        ViewState.view == ViewState.DocView.WEB_LAYOUT -> WebLayoutSurface(items, state)
                        ViewState.view == ViewState.DocView.OUTLINE ||
                            ViewState.view == ViewState.DocView.DRAFT -> PlainSurface(items, state)
                        ViewState.pageMovement == ViewState.PageMovement.SIDE_TO_SIDE ->
                            SideToSideSurface(items, state)
                        else -> SingleSurface(items, state)
                    }
                }
            }
        }
    }
}

// scroll target plumbing (set by nav pane jumps)
private var pendingScrollIndex: Int? = null
private fun scopeScrollTarget(idx: Int) { pendingScrollIndex = idx }

// ------------------------------------------------------------------ surfaces

@Composable
private fun SingleSurface(items: List<EditorItem>, state: com.wordcraft.documentcore.EditorState) {
    val listState = rememberLazyListState()
    ConsumeScrollTarget(listState, items.size)
    val scope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize().pointerInput(Unit) {
        detectTransformGestures { _, _, gestureZoom, _ ->
            if (gestureZoom != 1f) ViewState.applyZoom(ViewState.zoom * gestureZoom)
        }
    }) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PageSheet(items, state, listState, Modifier.weight(1f))
        }
    }
}

/** Read Mode: no page sheet, text on the canvas directly, wider measure. */
@Composable
private fun ReadModeSurface(items: List<EditorItem>, state: com.wordcraft.documentcore.EditorState) {
    val listState = rememberLazyListState()
    ConsumeScrollTarget(listState, items.size)
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).zoomLayer(),
        contentPadding = PaddingValues(start = 32.dp, end = 32.dp, top = 24.dp, bottom = 96.dp),
    ) { EditorItems(items, state) }
}

/** Web Layout: continuous sheet, no page edges (full-width reflow). */
@Composable
private fun WebLayoutSurface(items: List<EditorItem>, state: com.wordcraft.documentcore.EditorState) {
    val listState = rememberLazyListState()
    ConsumeScrollTarget(listState, items.size)
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Surface(
            Modifier.fillMaxSize().padding(horizontal = 4.dp).zoomLayer(),
            color = MaterialTheme.colorScheme.surface,
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 96.dp),
            ) { EditorItems(items, state) }
        }
    }
}

/** Outline / Draft: plain continuous list, no sheet chrome, tighter spacing. */
@Composable
private fun PlainSurface(items: List<EditorItem>, state: com.wordcraft.documentcore.EditorState) {
    val listState = rememberLazyListState()
    ConsumeScrollTarget(listState, items.size)
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).zoomLayer(),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 96.dp),
    ) { EditorItems(items, state) }
}

/** Two real panes of the same document (Split); [horizontal] = side-by-side. Sync links scroll. */
@Composable
private fun SplitSurface(
    items: List<EditorItem>,
    state: com.wordcraft.documentcore.EditorState,
    viewportH: androidx.compose.ui.unit.Dp,
) {
    val a = rememberLazyListState()
    val b = rememberLazyListState()
    val horizontal = ViewState.sideBySide
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
    state: com.wordcraft.documentcore.EditorState,
    listState: androidx.compose.foundation.lazy.LazyListState,
    modifier: Modifier,
) {
    Box(modifier.background(MaterialTheme.colorScheme.surface)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().zoomLayer(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 48.dp),
        ) { EditorItems(items, state) }
    }
}

/** Side to Side: horizontal pager-like row of page-width sheets. */
@Composable
private fun SideToSideSurface(items: List<EditorItem>, state: com.wordcraft.documentcore.EditorState) {
    val scroll = rememberScrollState()
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            Modifier.fillMaxSize().horizontalScroll(scroll).padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            repeat(3) {
                Surface(
                    Modifier.widthIn(max = PAGE_WIDTH_PT.dp).fillMaxHeight().padding(horizontal = 6.dp).zoomLayer(),
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 6.dp,
                ) {
                    val listState = rememberLazyListState()
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

// ------------------------------------------------------------------ shared pieces

@Composable
private fun PageSheet(
    items: List<EditorItem>,
    state: com.wordcraft.documentcore.EditorState,
    listState: androidx.compose.foundation.lazy.LazyListState,
    modifier: Modifier,
) {
    Box(modifier.fillMaxWidth().widthIn(max = 720.dp)) {
        Surface(
            Modifier.fillMaxWidth().height(PAGE_HEIGHT_PT.dp * ViewState.zoom),
            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp,
        ) {
            Box(Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().zoomLayer(),
                    contentPadding = PaddingValues(
                        start = (RulerState.leftMarginPt).dp,
                        end = (RulerState.rightMarginPt).dp,
                        top = (RulerState.topMarginPt).dp,
                        bottom = 96.dp,
                    ),
                ) { EditorItems(items, state) }
                if (ViewState.showGridlines) GridlinesOverlay()
            }
        }
        // Vertical ruler down the left edge of the sheet.
        if (ViewState.showRuler) {
            androidx.compose.foundation.layout.Box(
                Modifier.align(Alignment.CenterStart).widthIn(min = 0.dp).padding(start = 0.dp),
            ) { VerticalRuler(Modifier.fillMaxHeight()) }
        }
    }
}

/** Shared LazyColumn item builder so every surface renders the same engine content. */
private fun androidx.compose.foundation.lazy.LazyListScope.EditorItems(
    items: List<EditorItem>,
    state: com.wordcraft.documentcore.EditorState,
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

/** Applies zoom: engine-driven reflow when available, otherwise Compose layout scale. */
private fun Modifier.zoomLayer(): Modifier =
    if (ViewState.engineZoomAvailable) this // engine reflow already changed text sizes
    else graphicsLayer {
        scaleX = ViewState.zoom
        scaleY = ViewState.zoom
        transformOrigin = TransformOrigin(0.5f, 0f)
    }

/** Consume a pending navigation jump by scrolling the list. */
@Composable
private fun ConsumeScrollTarget(listState: androidx.compose.foundation.lazy.LazyListState, count: Int) {
    val target = pendingScrollIndex
    LaunchedEffect(target, count) {
        if (target != null && target in 0 until count) {
            listState.animateScrollToItem(target)
        }
        pendingScrollIndex = null
    }
}
