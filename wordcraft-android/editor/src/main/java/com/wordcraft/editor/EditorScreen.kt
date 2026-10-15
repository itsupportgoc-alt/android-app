package com.wordcraft.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp

/**
 * The document surface: a reflow *page* (white sheet with shadow and margins on the gray
 * canvas, Word-style) rendered from the engine state as a lazy list of sections and
 * tables. Hardware-keyboard shortcuts (Ctrl+B/I/U/Z/Y/…) dispatch the same commands as
 * the toolbar; the soft keyboard and IME go through each section's text field.
 */
@Composable
fun EditorScreen(
    modifier: Modifier = Modifier,
) {
    val state = EditorController.state
    val items = remember(state) { EditorController.items() }

    Box(
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
                    modifier = Modifier.fillMaxSize(),
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
}
