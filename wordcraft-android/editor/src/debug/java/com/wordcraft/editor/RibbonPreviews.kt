package com.wordcraft.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Preview matrix for the Word View-tab copy. Four device classes, both orientations, so layout
 * regressions (clipped icons, overlapping rows) surface in IDE previews / screenshot tooling.
 *
 * Breakpoints (dp):
 *  - phone portrait    360 × 640
 *  - phone landscape   640 × 360
 *  - tablet portrait   800 × 1280
 *  - tablet landscape  1280 × 800
 *
 * The ribbon scrolls horizontally, so on narrow widths every control stays reachable by scrolling
 * rather than being dropped — matching the "never reduce the feature set" rule.
 */

@Preview(name = "Phone portrait", widthDp = 360, heightDp = 640, showBackground = true)
@Composable
fun RibbonPhonePortrait() = RibbonPreviewScaffold(Modifier.width(360.dp).height(640.dp))

@Preview(name = "Phone landscape", widthDp = 640, heightDp = 360, showBackground = true)
@Composable
fun RibbonPhoneLandscape() = RibbonPreviewScaffold(Modifier.width(640.dp).height(360.dp))

@Preview(name = "Tablet portrait", widthDp = 800, heightDp = 1280, showBackground = true)
@Composable
fun RibbonTabletPortrait() = RibbonPreviewScaffold(Modifier.width(800.dp).height(1280.dp))

@Preview(name = "Tablet landscape", widthDp = 1280, heightDp = 800, showBackground = true)
@Composable
fun RibbonTabletLandscape() = RibbonPreviewScaffold(Modifier.width(1280.dp).height(800.dp))

@Composable
private fun RibbonPreviewScaffold(modifier: Modifier) {
    androidx.compose.material3.MaterialTheme {
        Box(modifier.fillMaxSize()) {
            Ribbon()
        }
    }
}

/** Standalone previews of the individual reference regions. */
@Preview(name = "Navigation pane", widthDp = 260, heightDp = 480, showBackground = true)
@Composable
fun NavigationPanePreview() {
    androidx.compose.material3.MaterialTheme {
        Box(Modifier.width(260.dp).height(480.dp)) {
            NavigationPane(items = emptyList(), onNavigate = {}, onClose = {})
        }
    }
}

@Preview(name = "Horizontal ruler", widthDp = 640, heightDp = 40, showBackground = true)
@Composable
fun HorizontalRulerPreview() {
    androidx.compose.material3.MaterialTheme {
        Box(Modifier.width(640.dp).height(40.dp)) {
            HorizontalRuler(pageWidth = 595.dp)
        }
    }
}

@Preview(name = "Status bar", widthDp = 640, heightDp = 44, showBackground = true)
@Composable
fun StatusBarPreview() {
    androidx.compose.material3.MaterialTheme {
        StatusBar(wordCount = 128, pageCount = 1)
    }
}
