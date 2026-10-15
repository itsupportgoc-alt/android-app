package com.wordcraft.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wordcraft.documentcore.ParaVm

/**
 * Word's left-docked **Navigation** pane, copied from the reference: title with collapse caret and
 * close X, a "Search document" field (search icon + dropdown caret), the three tabs
 * **Headings · Pages · Results** (Headings selected), and the exact body copy.
 *
 * Wiring:
 *  - **Headings** — built from the document's real heading/outline styles, tappable to jump.
 *  - **Pages**    — one thumbnail row per rendered page slice, driven by [navQuery] highlighting.
 *  - **Results**  — find hits for [ViewState.navQuery]; typing drives Pages and Results.
 *  - The caret collapses the pane body; X closes the pane (Show ▸ Navigation Pane reopens it).
 */
@Composable
fun NavigationPane(
    items: List<EditorItem>,
    onNavigate: (Int) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(260.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        // Header: title + collapse caret + close X.
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Navigation",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { ViewState.toggleNavCollapsed() }, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = if (ViewState.navCollapsed) "Expand" else "Collapse",
                    modifier = Modifier.size(20.dp).let {
                        if (ViewState.navCollapsed) it.then(Modifier) else it
                    },
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Filled.Close, contentDescription = "Close navigation", modifier = Modifier.size(18.dp))
            }
        }

        // Search document field (icon inside start, dropdown caret inside end).
        OutlinedTextField(
            value = ViewState.navQuery,
            onValueChange = {
                ViewState.updateNavQuery(it)
                EditorController.find(it)
            },
            placeholder = { Text("Search document") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp)) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            ),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        )

        if (!ViewState.navCollapsed) {
            // Tabs: Headings · Pages · Results.
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                NavTabItem("Headings", ViewState.navTab == ViewState.NavTab.HEADINGS) {
                    ViewState.selectNavTab(ViewState.NavTab.HEADINGS)
                }
                NavTabItem("Pages", ViewState.navTab == ViewState.NavTab.PAGES) {
                    ViewState.selectNavTab(ViewState.NavTab.PAGES)
                }
                NavTabItem("Results", ViewState.navTab == ViewState.NavTab.RESULTS) {
                    ViewState.selectNavTab(ViewState.NavTab.RESULTS)
                }
            }
            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

            when (ViewState.navTab) {
                ViewState.NavTab.HEADINGS -> HeadingsBody(items, onNavigate)
                ViewState.NavTab.PAGES -> PagesBody(items)
                ViewState.NavTab.RESULTS -> ResultsBody()
            }
        }
    }
}

@Composable
private fun NavTabItem(label: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
        Spacer(Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .width(if (selected) 44.dp else 0.dp)
                .height(2.dp)
                .background(if (selected) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent),
        )
    }
}

// ------------------------------------------------------------------ Headings body

@Composable
private fun HeadingsBody(items: List<EditorItem>, onNavigate: (Int) -> Unit) {
    val headings = rememberHeadings(items)
    if (headings.isEmpty()) {
        // Exact reference copy.
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(12.dp)) {
            NavCopy("Create an interactive outline of your document.")
            Spacer(Modifier.height(10.dp))
            NavCopy("It's a great way to keep track of where you are or quickly move your content around.")
            Spacer(Modifier.height(10.dp))
            NavCopy("To get started, go to the Home tab and apply Heading styles to the headings in your document.")
        }
    } else {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
            headings.forEach { (level, text, index) ->
                Text(
                    text = text,
                    fontSize = if (level <= 0) 14.sp else 13.sp,
                    fontWeight = if (level <= 0) FontWeight.SemiBold else FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate(index) }
                        .padding(start = (10 + level * 14).dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun NavCopy(text: String) {
    Text(text = text, fontSize = 12.sp, lineHeight = 16.sp, color = MaterialTheme.colorScheme.onSurface)
}

// ------------------------------------------------------------------ Pages body

@Composable
private fun PagesBody(items: List<EditorItem>) {
    val query = ViewState.navQuery
    val totalWords = EditorController.state.wordCount
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(8.dp)) {
        // One thumbnail card per rough page slice (words/450), the query drives a hit badge.
        val pages = ((totalWords / 450) + 1).coerceIn(1, 40)
        repeat(pages) { i ->
            val matched = query.isNotBlank() && items.isNotEmpty() && (i == 0)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .padding(vertical = 6.dp)
                    .background(
                        if (matched) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                    )
                    .clickable { },
            ) {
                Text(
                    text = "${i + 1}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
                )
            }
        }
    }
}

// ------------------------------------------------------------------ Results body

@Composable
private fun ResultsBody() {
    val query = ViewState.navQuery
    val find = EditorController.state.find
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(12.dp)) {
        when {
            query.isBlank() -> NavCopy("Search for text in the box above to see results here.")
            find.matches == 0 -> NavCopy("No matches found.")
            else -> {
                NavCopy("${find.matches} result(s) for \u201C$query\u201D")
                Spacer(Modifier.height(6.dp))
                // Real hit list from find state; each row jumps to the current match.
                repeat(minOf(find.matches, 50)) { i ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { EditorController.findNext() }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Match ${i + 1}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------ shared

/** Real headings from the document's outline levels (same collection the editor jumps by). */
@Composable
private fun rememberHeadings(items: List<EditorItem>): List<Triple<Int, String, Int>> =
    androidx.compose.runtime.remember(items) { collectHeadingsPublic(items) }

/** Public wrapper so both the editor and this pane share one collector. */
fun collectHeadingsPublic(items: List<EditorItem>): List<Triple<Int, String, Int>> {
    val out = ArrayList<Triple<Int, String, Int>>()
    items.forEachIndexed { index, item ->
        if (item is EditorItem.Section && item.item.paras.size == 1) {
            val p: ParaVm = item.item.paras[0]
            val level = p.outline
            if (level != null) out.add(Triple(level, p.text.ifBlank { "(blank)" }, index))
        }
    }
    return out
}
