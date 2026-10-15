package com.wordcraft.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wordcraft.documentcore.EditorState
import com.wordcraft.documentcore.ParaVm
import kotlinx.coroutines.launch

/**
 * The document surface, adapted from the desktop Word reference: a white page on the gray
 * canvas with page margins, a horizontal ruler above it on wide screens, an optional
 * headings navigation pane beside it, and zoom (pinch + a Word-style − % + strip).
 * Rendered from the engine state as a lazy list of sections and tables. Hardware-keyboard
 * shortcuts (Ctrl+B/I/U/Z/Y/…) dispatch the same commands as the toolbar.
 */
@Composable
fun EditorScreen(
    modifier: Modifier = Modifier,
) {
    val state = EditorController.state
    val items = remember(state) { EditorController.items() }
    val isWide = LocalConfiguration.current.screenWidthDp >= 720

    var zoom by remember { mutableFloatStateOf(1f) }
    var rulerVisible by remember { mutableStateOf(true) }
    var navVisible by remember { mutableStateOf(isWide) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .onPreviewKeyEvent { ev ->
                if (ev.type != KeyEventType.KeyDown || !ev.isCtrlPressed) return@onPreviewKeyEvent false
                when (ev.key) {
                    Key.B -> { EditorController.toggleBold(); true }
                    Key.I -> { EditorController.toggleItalic(); true }
                    Key.U -> { EditorController.toggleUnderline(); true }
                    Key.Z -> { EditorController.undo(); true }
                    Key.Y -> { EditorController.redo(); true }
                    Key.S -> { EditorController.requestSave?.invoke(); true }
                    Key.A -> { EditorController.selectAll(); true }
                    Key.F -> { EditorController.showFind(true); true }
                    else -> false
                }
            },
    ) {
        // Word-style navigation pane (headings outline). Toggleable; open by default on wide screens.
        if (navVisible) {
            NavigationPane(
                items = items,
                onNavigate = { index -> scope.launch { listState.animateScrollToItem(index) } },
                onClose = { navVisible = false },
                modifier = Modifier
                    .width(240.dp)
                    .fillMaxHeight(),
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        ) {
            // Ruler above the page, only on wide screens (like the desktop reference).
            if (rulerVisible && isWide) {
                Ruler(pageWidthDp = 620.dp * zoom)
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, _, gestureZoom, _ ->
                            if (gestureZoom != 1f) {
                                zoom = (zoom * gestureZoom).coerceIn(ZOOM_MIN, ZOOM_MAX)
                            }
                        }
                    },
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp)
                        .padding(top = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 720.dp)
                            .weight(1f),
                        shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 8.dp,
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = zoom
                                    scaleY = zoom
                                    transformOrigin = TransformOrigin(0.5f, 0f)
                                },
                            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 32.dp, bottom = 120.dp),
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
                    }
                }
            }

            // Word-style zoom strip: [Navigation] · − 100% + (ruler toggle on wide screens).
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 12.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isWide) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .clickable { navVisible = !navVisible }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = if (navVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Navigation",
                            fontSize = 11.sp,
                            color = if (navVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.weight(1f))
                } else {
                    Spacer(Modifier.weight(1f))
                }
                IconButton(onClick = { zoom = (zoom - 0.1f).coerceAtLeast(ZOOM_MIN) }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Remove, contentDescription = "Zoom out", modifier = Modifier.size(18.dp))
                }
                Text(
                    text = "${(zoom * 100).toInt()}%",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 6.dp),
                )
                IconButton(onClick = { zoom = (zoom + 0.1f).coerceAtMost(ZOOM_MAX) }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Add, contentDescription = "Zoom in", modifier = Modifier.size(18.dp))
                }
                if (isWide) {
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = if (rulerVisible) "Hide ruler" else "Show ruler",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { rulerVisible = !rulerVisible }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }
            }
        }
    }
}

private const val ZOOM_MIN = 0.6f
private const val ZOOM_MAX = 2.2f

/** Horizontal ruler above the page, mirroring the desktop Word ruler (numbered inch ticks). */
@Composable
private fun Ruler(pageWidthDp: Dp, modifier: Modifier = Modifier) {
    val tickColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .width(pageWidthDp)
                .height(22.dp)
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            repeat(8) { i ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.BottomCenter) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${i + 1}",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 11.sp,
                        )
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(5.dp)
                                .background(tickColor),
                        )
                    }
                }
            }
        }
    }
}

/** Headings navigation pane (the "Navigation" sidebar in the reference image). */
@Composable
private fun NavigationPane(
    items: List<EditorItem>,
    onNavigate: (Int) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Navigation",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                    fontSize = 15.sp,
                )
                IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "Close navigation", modifier = Modifier.size(18.dp))
                }
            }
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            val headings = remember(items) { collectHeadings(items) }
            if (headings.isEmpty()) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = "Create an interactive outline of your document.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "To get started, apply Heading styles from the styles row in the toolbar sheet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                ) {
                    headings.forEach { (level, text, index) ->
                        Text(
                            text = text,
                            fontSize = if (level == 0) 13.sp else 12.sp,
                            fontWeight = if (level == 0) FontWeight.SemiBold else FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigate(index) }
                                .padding(start = (12 + level * 14).dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

/** Collect (outlineLevel, text, renderItemIndex) for heading paragraphs. */
private fun collectHeadings(items: List<EditorItem>): List<Triple<Int, String, Int>> {
    val out = ArrayList<Triple<Int, String, Int>>()
    items.forEachIndexed { index, item ->
        if (item is EditorItem.Section && item.item.paras.size == 1) {
            val p: ParaVm = item.item.paras[0]
            val level = p.outline
            if (level != null) {
                out.add(Triple(level, p.text.ifBlank { "(blank)" }, index))
            }
        }
    }
    return out
}
