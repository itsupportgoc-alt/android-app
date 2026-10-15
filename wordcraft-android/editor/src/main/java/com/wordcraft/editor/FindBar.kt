package com.wordcraft.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NavigateBefore
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Find & replace: `edit.find` / `edit.findNext` / `edit.replace` / `edit.replaceAll`. */
@Composable
fun FindBar(modifier: Modifier = Modifier) {
    var query by remember { mutableStateOf(EditorController.state.find.query) }
    var replace by remember { mutableStateOf(EditorController.state.find.replace) }
    var showReplace by remember { mutableStateOf(false) }
    val find = EditorController.state.find

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = query,
                onValueChange = { v ->
                    query = v
                    EditorController.find(v)
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 4.dp),
                singleLine = true,
                placeholder = { Text("Find") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                ),
            )
            Text(
                text = if (find.matches > 0) "${find.current + 1}/${find.matches}" else "",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            IconButton(onClick = { EditorController.findPrevious() }) {
                Icon(Icons.Filled.NavigateBefore, contentDescription = "Previous match")
            }
            IconButton(onClick = { EditorController.findNext() }) {
                Icon(Icons.Filled.NavigateNext, contentDescription = "Next match")
            }
            IconButton(onClick = { showReplace = !showReplace }) {
                Text("Replace", style = MaterialTheme.typography.labelLarge)
            }
            IconButton(onClick = { EditorController.showFind(false) }) {
                Icon(Icons.Filled.Close, contentDescription = "Close find")
            }
        }
        if (showReplace) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                OutlinedTextField(
                    value = replace,
                    onValueChange = { replace = it },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 4.dp),
                    singleLine = true,
                    placeholder = { Text("Replace with") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    ),
                )
                IconButton(onClick = { EditorController.replace(query, replace) }) {
                    Text("One", style = MaterialTheme.typography.labelLarge)
                }
                IconButton(onClick = { EditorController.replaceAll(query, replace) }) {
                    Text("All", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
