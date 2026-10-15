package com.wordcraft.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Asserts the View-tab copy is complete and *wired*: every control named in the reference
 * exists, and invoking it actually mutates state (a control that renders but does nothing is a
 * failure, per the spec).
 */
class ViewStateTest {

    @Before
    fun reset() = ViewState.reset()

    // ---------------------------------------------------------------- tab strip

    @Test
    fun `ribbon tabs are in reference order`() {
        assertEquals(
            listOf("File", "Home", "Insert", "Draw", "Design", "Layout",
                "References", "Mailings", "Review", "View", "Help"),
            ViewState.tabs,
        )
    }

    @Test
    fun `selecting a tab changes the active tab`() {
        assertEquals("View", ViewState.activeTab) // reference default
        ViewState.selectTab("Home")
        assertEquals("Home", ViewState.activeTab)
        ViewState.selectTab("NoSuchTab") // ignored
        assertEquals("Home", ViewState.activeTab)
    }

    // ---------------------------------------------------------------- Views group

    @Test
    fun `every views control changes the surface mode`() {
        ViewState.DocView.values().forEach { v ->
            ViewState.selectView(v)
            assertEquals(v, ViewState.view)
        }
        // Reference shows Print Layout selected by default.
        ViewState.reset()
        assertEquals(ViewState.DocView.PRINT_LAYOUT, ViewState.view)
    }

    // ---------------------------------------------------------------- Immersive

    @Test
    fun `focus and immersive reader are wired and mutually exclusive`() {
        assertFalse(ViewState.focusMode)
        ViewState.toggleFocus()
        assertTrue(ViewState.focusMode)
        ViewState.toggleImmersive()
        assertTrue(ViewState.immersiveReader)
        assertFalse(ViewState.focusMode) // turning one on clears the other
    }

    // ---------------------------------------------------------------- Page Movement

    @Test
    fun `page movement switches vertical and side to side`() {
        assertEquals(ViewState.PageMovement.VERTICAL, ViewState.pageMovement) // reference default
        ViewState.selectPageMovement(ViewState.PageMovement.SIDE_TO_SIDE)
        assertEquals(ViewState.PageMovement.SIDE_TO_SIDE, ViewState.pageMovement)
        ViewState.selectPageMovement(ViewState.PageMovement.VERTICAL)
        assertEquals(ViewState.PageMovement.VERTICAL, ViewState.pageMovement)
    }

    // ---------------------------------------------------------------- Show group

    @Test
    fun `show toggles flip real visibility flags`() {
        // Reference: Ruler on, Gridlines off, Navigation Pane on.
        assertTrue(ViewState.showRuler)
        assertFalse(ViewState.showGridlines)
        assertTrue(ViewState.showNavPane)

        ViewState.toggleRuler(); assertFalse(ViewState.showRuler)
        ViewState.toggleGridlines(); assertTrue(ViewState.showGridlines)
        ViewState.toggleNavPane(); assertFalse(ViewState.showNavPane)

        ViewState.closeNavPane(); assertFalse(ViewState.showNavPane)
        ViewState.toggleNavCollapsed(); assertTrue(ViewState.navCollapsed)
    }

    // ---------------------------------------------------------------- Zoom group

    @Test
    fun `zoom clamps to word bounds and percent field reflects it`() {
        ViewState.setZoomPercent(100)
        assertEquals(1.0f, ViewState.zoom, 0.0001f)

        ViewState.setZoomPercent(500)
        assertEquals(5.0f, ViewState.zoom, 0.0001f)

        ViewState.setZoomPercent(9999) // clamped to 500%
        assertEquals(5.0f, ViewState.zoom, 0.0001f)

        ViewState.setZoomPercent(1) // clamped to 10%
        assertEquals(0.1f, ViewState.zoom, 0.0001f)

        ViewState.zoomIn()
        assertTrue(ViewState.zoom > 0.1f)
        ViewState.zoomOut()
        assertTrue(ViewState.zoom >= 0.1f)
    }

    @Test
    fun `zoom presets resolve to a real factor`() {
        ViewState.zoomFitPageWidth(viewportWidthPx = 1000f, pageWidthPx = 595f)
        assertEquals(1000f / 595f, ViewState.zoom, 0.01f)

        ViewState.zoomFitOnePage(viewportHeightPx = 842f, pageHeightPx = 842f)
        assertEquals(1.0f, ViewState.zoom, 0.01f)

        ViewState.zoomMultiplePages(viewportWidthPx = 1190f, pageWidthPx = 595f)
        assertEquals(1.0f, ViewState.zoom, 0.01f) // two pages across → 100%

        // Guard against divide-by-zero producing NaN/Infinity.
        ViewState.zoomFitPageWidth(0f, 0f)
        assertFalse(ViewState.zoom.isNaN() || ViewState.zoom.isInfinite())
    }

    // ---------------------------------------------------------------- Window group

    @Test
    fun `split side-by-side arrange and sync are wired`() {
        assertFalse(ViewState.splitView)
        ViewState.toggleSplit()
        assertTrue(ViewState.splitView)

        ViewState.toggleSideBySide()
        assertTrue(ViewState.sideBySide)
        assertTrue(ViewState.splitView) // side-by-side implies split

        ViewState.toggleSyncScroll()
        assertTrue(ViewState.syncScroll)

        ViewState.toggleArrangeAll()
        assertTrue(ViewState.arrangeAll)

        ViewState.resetWindowLayout()
        assertFalse(ViewState.splitView)
        assertFalse(ViewState.sideBySide)
        assertFalse(ViewState.syncScroll)
        assertFalse(ViewState.arrangeAll)

        // Turning split off clears the dependent flags too.
        ViewState.toggleSplit()
        ViewState.toggleSideBySide()
        ViewState.toggleSplit()
        assertFalse(ViewState.sideBySide)
    }

    // ---------------------------------------------------------------- Navigation pane

    @Test
    fun `navigation tabs and search query drive the pane`() {
        // Default (reference) tab is Headings, checked before any switching.
        assertEquals(ViewState.NavTab.HEADINGS, ViewState.navTab)
        ViewState.NavTab.values().forEach { t ->
            ViewState.selectNavTab(t)
            assertEquals(t, ViewState.navTab)
        }
        val beforeQuery = ViewState.navTab
        ViewState.updateNavQuery("hello")
        assertEquals("hello", ViewState.navQuery)
        assertEquals(beforeQuery, ViewState.navTab) // typing must not change the selected tab
    }

    // ---------------------------------------------------------------- macros/properties

    @Test
    fun `macros and properties dialogs open and dismiss`() {
        assertFalse(MacrosDialog.visible)
        MacrosDialog.show(); assertTrue(MacrosDialog.visible)
        MacrosDialog.dismiss(); assertFalse(MacrosDialog.visible)

        assertFalse(PropertiesDialog.visible)
        PropertiesDialog.show(); assertTrue(PropertiesDialog.visible)
        PropertiesDialog.dismiss(); assertFalse(PropertiesDialog.visible)
    }

    @Test
    fun `zoom dialog opens and window dialog reports state`() {
        ZoomDialog.show(); assertTrue(ZoomDialog.visible)
        ZoomDialog.dismiss(); assertFalse(ZoomDialog.visible)

        WindowDialog.split()
        assertTrue(WindowDialog.visible)
        assertTrue(ViewState.splitView) // the Split control really splits
        assertTrue(WindowDialog.message.isNotBlank())
        WindowDialog.dismiss()

        WindowDialog.resetWindow()
        assertFalse(ViewState.splitView)
    }

    // ---------------------------------------------------------------- reset

    @Test
    fun `reset returns every control to the reference defaults`() {
        ViewState.selectTab("Home")
        ViewState.selectView(ViewState.DocView.DRAFT)
        ViewState.toggleRuler()
        ViewState.setZoomPercent(300)
        ViewState.toggleSplit()
        ViewState.updateNavQuery("x")

        ViewState.reset()

        assertEquals("View", ViewState.activeTab)
        assertEquals(ViewState.DocView.PRINT_LAYOUT, ViewState.view)
        assertTrue(ViewState.showRuler)
        assertEquals(1.0f, ViewState.zoom, 0.0001f)
        assertFalse(ViewState.splitView)
        assertEquals("", ViewState.navQuery)
    }
}
