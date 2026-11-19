package com.wordcraft.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Preview matrix for the Word-for-PC chrome: the title bar, the ribbon tab strip, the ribbon
 * body and the status bar at four device classes.
 *
 * Breakpoints (dp): phone 360 × 640, phone landscape 640 × 360, tablet 800 × 1280 and tablet
 * landscape 1280 × 800. The tab strip scrolls horizontally, so a narrow preview never clips a
 * tab — it only moves it off the first screenful, as on the device.
 */

@Preview(name = "Chrome – phone", widthDp = 360, heightDp = 640, showBackground = true)
@Composable
fun WordChromePhonePreview() = WordChromePreviewScaffold(Modifier.width(360.dp).height(640.dp))

@Preview(name = "Chrome – phone landscape", widthDp = 640, heightDp = 360, showBackground = true)
@Composable
fun WordChromePhoneLandscapePreview() = WordChromePreviewScaffold(Modifier.width(640.dp).height(360.dp))

@Preview(name = "Chrome – tablet", widthDp = 800, heightDp = 1280, showBackground = true)
@Composable
fun WordChromeTabletPreview() = WordChromePreviewScaffold(Modifier.width(800.dp).height(800.dp))

@Preview(name = "Chrome – tablet landscape", widthDp = 1280, heightDp = 800, showBackground = true)
@Composable
fun WordChromeTabletLandscapePreview() = WordChromePreviewScaffold(Modifier.width(1280.dp).height(800.dp))

@Composable
private fun WordChromePreviewScaffold(modifier: Modifier) {
    MaterialTheme {
        Column(modifier.background(MaterialTheme.colorScheme.background)) {
            WordTopBar(
                title = "Quarterly report.docx",
                dirty = true,
                wordCount = 128,
                canUndo = true,
                canRedo = false,
                onUndo = {},
                onRedo = {},
                onSave = {},
                onSearch = {},
            )
            WordRibbonTabs()
            WordStatusBar(wordCount = 128, dirty = true)
            Box(Modifier.weight(1f).fillMaxWidth().background(MaterialTheme.colorScheme.background))
        }
    }
}

/** The ribbon body on its own, as it appears under the tab strip (Home is the default tab). */
@Preview(name = "Ribbon body – Home", widthDp = 800, heightDp = 200, showBackground = true)
@Composable
fun WordRibbonPaneContentPreview() {
    MaterialTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            RibbonTabContent("Home")
        }
    }
}

/** Standalone previews of the individual regions. */
@Preview(name = "Navigation pane", widthDp = 260, heightDp = 480, showBackground = true)
@Composable
fun NavigationPanePreview() {
    MaterialTheme {
        Box(Modifier.width(260.dp).height(480.dp)) {
            NavigationPane(items = emptyList(), onNavigate = {}, onClose = {})
        }
    }
}

@Preview(name = "Horizontal ruler", widthDp = 640, heightDp = 40, showBackground = true)
@Composable
fun HorizontalRulerPreview() {
    MaterialTheme {
        Box(Modifier.width(640.dp).height(40.dp)) {
            HorizontalRuler(pageWidth = 595.dp)
        }
    }
}
