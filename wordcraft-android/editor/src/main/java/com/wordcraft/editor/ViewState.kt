package com.wordcraft.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Zoom bounds (Word: 10%…500%). Top-level so rulers/status/ribbon can share them. */
val ZOOM_MIN = 0.1f
val ZOOM_MAX = 5.0f

/**
 * Observable model for everything the Word **View** tab copy controls.
 *
 * Kept separate from [EditorController] (which owns document truth) because these are pure
 * view settings: none of them mutate the engine's document. The only exception is zoom, which
 * is exposed both as a view factor and, when the engine FFI hook is available, as a layout
 * scale that reflows text through the engine's layout path (see [engineZoomAvailable]).
 *
 * A process-wide singleton (like [EditorController]) so the setting survives recomposition and
 * rotation.
 */
object ViewState {

    // ---------------------------------------------------------------- ribbon

    /** The eleven ribbon tabs, in reference order. `File` renders as the blue button. */
    val tabs: List<String> = listOf(
        "File", "Home", "Insert", "Draw", "Design", "Layout",
        "References", "Mailings", "Review", "View", "Help",
    )

    /** Currently selected ribbon tab. Word opens on `Home`. */
    var activeTab: String by mutableStateOf("Home")
        private set

    fun selectTab(tab: String) {
        if (tab in tabs) activeTab = tab
    }

    // ---------------------------------------------------------------- bottom ribbon pane

    /**
     * The ribbon body under the tab strip. Word for the PC shows it expanded; on a short
     * viewport there is no room for it, so [applyDefaultForViewport] collapses it to the strip.
     */
    var ribbonOpen: Boolean by mutableStateOf(true)
        private set

    /** The last viewport class [applyDefaultForViewport] was applied for (null = never). */
    private var ribbonDefaultForShortViewport: Boolean? = null

    /**
     * Pick Word's default ribbon state for the current viewport: expanded where there is room
     * for both the ribbon and the page, collapsed to the tab strip on a short screen (a phone
     * in landscape). The user can still open or collapse it from the strip's chevron.
     */
    fun applyDefaultForViewport(shortViewport: Boolean) {
        if (ribbonDefaultForShortViewport == shortViewport) return
        ribbonDefaultForShortViewport = shortViewport
        ribbonOpen = !shortViewport
    }

    /** Open [tab] in the bottom pane (tab strip, or one of the tabs behind ⋯). */
    fun openTab(tab: String) {
        if (tab !in tabs) return
        activeTab = tab
        ribbonOpen = true
    }

    /** Tap on a tab strip item: open it, or close the pane when that tab is already open. */
    fun toggleTab(tab: String) {
        if (tab !in tabs) return
        if (ribbonOpen && activeTab == tab) {
            ribbonOpen = false
        } else {
            activeTab = tab
            ribbonOpen = true
        }
    }

    /** Collapse the pane (its chevron). */
    fun closeRibbon() { ribbonOpen = false }

    // ---------------------------------------------------------------- Views group

    /** Word's five document views (Views group). The reference shows Print Layout selected. */
    enum class DocView(val label: String) {
        READ_MODE("Read Mode"),
        PRINT_LAYOUT("Print Layout"),
        WEB_LAYOUT("Web Layout"),
        OUTLINE("Outline"),
        DRAFT("Draft"),
    }

    var view: DocView by mutableStateOf(DocView.PRINT_LAYOUT)
        private set

    fun selectView(v: DocView) { view = v }

    // ---------------------------------------------------------------- Immersive group

    var focusMode: Boolean by mutableStateOf(false)
        private set

    var immersiveReader: Boolean by mutableStateOf(false)
        private set

    fun toggleFocus() { focusMode = !focusMode; if (focusMode) immersiveReader = false }
    fun toggleImmersive() { immersiveReader = !immersiveReader; if (immersiveReader) focusMode = false }

    // ---------------------------------------------------------------- Page Movement group

    enum class PageMovement { VERTICAL, SIDE_TO_SIDE }

    var pageMovement: PageMovement by mutableStateOf(PageMovement.VERTICAL)
        private set

    fun selectPageMovement(m: PageMovement) { pageMovement = m }

    // ---------------------------------------------------------------- Show group

    var showRuler: Boolean by mutableStateOf(true)
        private set

    var showGridlines: Boolean by mutableStateOf(false)
        private set

    var showNavPane: Boolean by mutableStateOf(false)
        private set

    fun toggleRuler() { showRuler = !showRuler }
    fun toggleGridlines() { showGridlines = !showGridlines }
    fun toggleNavPane() { showNavPane = !showNavPane }

    /** Close button (X) on the Navigation pane header. */
    fun closeNavPane() { showNavPane = false }

    // ---------------------------------------------------------------- Zoom group

    /** Zoom presets the Zoom group's buttons request; the editor resolves them to a factor. */
    enum class ZoomPreset { ONE_PAGE, MULTIPLE, PAGE_WIDTH }

    /**
     * Current zoom as a fraction (1.0 = 100%). Bounds match Word's 10%…500%.
     * Set only through [setZoom] / [setZoomPercent] so clamping and reflow stay in one place.
     */
    var zoom: Float by mutableStateOf(1.0f)
        private set

    /** True when the native engine exposes the layout-scale FFI hook (`view.zoom`). */
    var engineZoomAvailable: Boolean = false

    fun setZoomPercent(percent: Int) = applyZoom(percent / 100f)

    fun applyZoom(factor: Float) {
        zoom = factor.coerceIn(ZOOM_MIN, ZOOM_MAX)
        if (engineZoomAvailable) EditorController.setEngineZoom(zoom)
    }

    fun zoomIn() = applyZoom(zoom + 0.1f)
    fun zoomOut() = applyZoom(zoom - 0.1f)

    /** Fit the whole page height into the viewport (One Page). */
    fun zoomFitOnePage(viewportHeightPx: Float, pageHeightPx: Float) {
        if (viewportHeightPx > 0f && pageHeightPx > 0f) applyZoom(viewportHeightPx / pageHeightPx)
    }

    /** Fit exactly one page width into the viewport (Page Width). */
    fun zoomFitPageWidth(viewportWidthPx: Float, pageWidthPx: Float) {
        if (viewportWidthPx > 0f && pageWidthPx > 0f) applyZoom(viewportWidthPx / pageWidthPx)
    }

    /** Multiple Pages: scale so ~two pages sit side by side (Side to Side) or stacked (Vertical). */
    fun zoomMultiplePages(viewportWidthPx: Float, pageWidthPx: Float) {
        if (viewportWidthPx > 0f && pageWidthPx > 0f) applyZoom(viewportWidthPx / (pageWidthPx * 2f))
    }

    // ---------------------------------------------------------------- Window group

    /** Split: two stacked panes (vertical) or side-by-side (horizontal) of the same document. */
    var splitView: Boolean by mutableStateOf(false)
        private set

    var sideBySide: Boolean by mutableStateOf(false)
        private set

    /** Synchronous scrolling: mirror scroll offsets across the split panes. */
    var syncScroll: Boolean by mutableStateOf(false)
        private set

    /** Arrange All: fit both panes evenly (ignored unless split/side-by-side is on). */
    var arrangeAll: Boolean by mutableStateOf(false)
        private set

    fun toggleSplit() { splitView = !splitView; if (!splitView) { sideBySide = false; syncScroll = false; arrangeAll = false } }
    fun toggleSideBySide() { sideBySide = !sideBySide; if (sideBySide) splitView = true }
    fun toggleSyncScroll() { syncScroll = !syncScroll }
    fun toggleArrangeAll() { arrangeAll = !arrangeAll }

    /** Reset Window Position: clear every window/layout flag at once. */
    fun resetWindowLayout() {
        splitView = false
        sideBySide = false
        syncScroll = false
        arrangeAll = false
    }

    // ---------------------------------------------------------------- Navigation pane tabs

    enum class NavTab { HEADINGS, PAGES, RESULTS }

    var navTab: NavTab by mutableStateOf(NavTab.HEADINGS)
        private set

    fun selectNavTab(t: NavTab) { navTab = t }

    /** Navigation-pane search query; drives the Pages and Results tabs. */
    var navQuery: String by mutableStateOf("")
        private set

    fun updateNavQuery(q: String) { navQuery = q }

    /** Navigation pane collapse state (header caret). */
    var navCollapsed: Boolean by mutableStateOf(false)
        private set

    fun toggleNavCollapsed() { navCollapsed = !navCollapsed }

    fun reset() {
        activeTab = "Home"
        ribbonOpen = true
        ribbonDefaultForShortViewport = null
        view = DocView.PRINT_LAYOUT
        focusMode = false
        immersiveReader = false
        pageMovement = PageMovement.VERTICAL
        showRuler = true
        showGridlines = false
        showNavPane = false
        zoom = 1.0f
        splitView = false
        sideBySide = false
        syncScroll = false
        arrangeAll = false
        navTab = NavTab.HEADINGS
        navQuery = ""
        navCollapsed = false
    }
}
