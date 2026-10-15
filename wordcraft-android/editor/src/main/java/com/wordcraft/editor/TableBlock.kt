package com.wordcraft.editor

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wordcraft.documentcore.BlockVm
import com.wordcraft.documentcore.EditorState
import com.wordcraft.documentcore.TableVm

/**
 * A table: rows of cells sized by the engine's grid (weights scaled to the available width),
 * each cell holding its own block list (paragraphs as mini sections, nested tables recursively).
 */
@Composable
fun TableBlock(
    vm: TableVm,
    state: EditorState,
    modifier: Modifier = Modifier,
) {
    val border = Modifier.border(0.75.dp, MaterialTheme.colorScheme.outline)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        vm.rows.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                var col = 0
                row.forEach { cell ->
                    val span = cell.span.coerceIn(1, vm.grid.size.coerceAtLeast(1))
                    var weight = 0f
                    for (s in 0 until span) weight += vm.grid.getOrNull(col + s) ?: 1f
                    if (weight <= 0f) weight = 1f
                    Box(
                        modifier = Modifier
                            .weight(weight)
                            .then(border)
                            .padding(6.dp),
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            for (b in cell.blocks) RenderBlock(b, state)
                        }
                    }
                    col += span
                }
                // Trailing cells (rowspan artefacts): pad the row with empty weight.
                if (col < vm.grid.size) {
                    val rest = vm.grid.subList(col, vm.grid.size).sum()
                    if (rest > 0f) Box(Modifier.weight(rest).then(border).padding(6.dp)) {
                        Text("", style = TextStyle(fontSize = 14.sp))
                    }
                }
            }
        }
    }
}

/** One block inside a body run, table cell, or nested container. */
@Composable
fun RenderBlock(b: BlockVm, state: EditorState) {
    val para = b.asPara
    val table = b.asTable
    when {
        table != null -> TableBlock(table, state, Modifier.padding(vertical = 4.dp))
        para != null -> {
            val section = splitSections(listOf(para)).firstOrNull() ?: return
            SectionField(section, state, Modifier.padding(vertical = 2.dp))
        }
    }
}
