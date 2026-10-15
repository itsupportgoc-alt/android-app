package com.wordcraft.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.Surface

/**
 * Wiring for the View-tab controls that need real, non-dead behaviour.
 *
 * - [ZoomDialog]     — the "% field" Word's Zoom button opens (and the 100% reset).
 * - [ZoomTargets]    — one-shot requests (One Page / Multiple / Page Width) resolved by the
 *                      editor surface, which knows the viewport/page pixel sizes.
 * - [WindowDialog]   — Split / Side by Side / Arrange All / Sync / Reset / New Window /
 *                      Switch Windows, mapped onto [ViewState] (mobile equivalents noted).
 * - [MacrosDialog]   — Macros has no Android runtime; shows the honest Word-style status.
 * - [PropertiesDialog] — real document properties from [EditorController.state].
 */

// ------------------------------------------------------------------ Zoom

object ZoomDialog {
    var visible by mutableStateOf(false)
    fun show() { visible = true }
    fun dismiss() { visible = false }
}

/**
 * Deferred zoom-fit requests. The ribbon buttons don't know pixel sizes; the editor surface
 * (which does) observes [pending] and resolves it, then clears.
 */
object ZoomTargets {
    var pending by mutableStateOf<ViewState.ZoomPreset?>(null)
    fun request(preset: ViewState.ZoomPreset) { pending = preset }
    fun consume() { pending = null }
}

@Composable
fun ZoomDialogSurface() {
    if (!ZoomDialog.visible) return
    var text by remember { mutableStateOf("${(ViewState.zoom * 100).toInt()}") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = { ZoomDialog.dismiss() },
        title = { Text("Zoom") },
        text = {
            Column {
                Text("Percent", fontWeight = FontWeight.Medium)
                OutlinedTextField(
                    value = text,
                    onValueChange = { v -> text = v.filter { it.isDigit() }; error = null },
                    singleLine = true,
                    isError = error != null,
                    supportingText = { if (error != null) Text(error!!) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                    TextButton(onClick = { text = "200" }) { Text("200%") }
                    TextButton(onClick = { text = "100" }) { Text("100%") }
                    TextButton(onClick = { text = "75" }) { Text("75%") }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val pct = text.toIntOrNull()
                when {
                    pct == null || pct < 10 || pct > 500 ->
                        error = "Enter a value between 10 and 500"
                    else -> { ViewState.setZoomPercent(pct); ZoomDialog.dismiss() }
                }
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = { ZoomDialog.dismiss() }) { Text("Cancel") } },
    )
}

// ------------------------------------------------------------------ Window

object WindowDialog {
    var visible by mutableStateOf(false)
    var message by mutableStateOf("")
    fun show() { visible = true }
    fun dismiss() { visible = false }

    fun split() { ViewState.toggleSplit(); message = if (ViewState.splitView) "Split view on" else "Split view off"; show() }
    fun sideBySide() { ViewState.toggleSideBySide(); message = if (ViewState.sideBySide) "Viewing side by side" else "Side by side off"; show() }
    fun arrangeAll() { ViewState.toggleArrangeAll(); message = if (ViewState.arrangeAll) "Panes arranged" else "Arrange off"; show() }
    fun syncScroll() {
        ViewState.toggleSyncScroll()
        message = if (ViewState.syncScroll) "Synchronous scrolling on" else "Synchronous scrolling off"
        show()
    }
    fun resetWindow() { ViewState.resetWindowLayout(); message = "Window layout reset"; show() }
    fun newWindow() { message = "A second window would duplicate this document session; WordCraft runs one session per app, so this opens a new split of the same document instead."; ViewState.toggleSplit(); show() }
    fun switchWindows() { message = "Only one document window exists in WordCraft on Android; use Split to show it twice."; show() }
}

@Composable
fun WindowDialogSurface() {
    if (!WindowDialog.visible) return
    AlertDialog(
        onDismissRequest = { WindowDialog.dismiss() },
        title = { Text("Window") },
        text = { Text(WindowDialog.message) },
        confirmButton = { TextButton(onClick = { WindowDialog.dismiss() }) { Text("OK") } },
    )
}

// ------------------------------------------------------------------ Macros

object MacrosDialog {
    var visible by mutableStateOf(false)
    fun show() { visible = true }
    fun dismiss() { visible = false }
}

@Composable
fun MacrosDialogSurface() {
    if (!MacrosDialog.visible) return
    AlertDialog(
        onDismissRequest = { MacrosDialog.dismiss() },
        title = { Text("Macros") },
        text = {
            Text("WordCraft for Android has no macro runtime. The desktop engine's VBA layer is not compiled into this build, so there are no macros to run or record.")
        },
        confirmButton = { TextButton(onClick = { MacrosDialog.dismiss() }) { Text("OK") } },
    )
}

// ------------------------------------------------------------------ Properties

object PropertiesDialog {
    var visible by mutableStateOf(false)
    fun show() { visible = true }
    fun dismiss() { visible = false }
}

@Composable
fun PropertiesDialogSurface() {
    if (!PropertiesDialog.visible) return
    val state = EditorController.state
    AlertDialog(
        onDismissRequest = { PropertiesDialog.dismiss() },
        title = { Text("Properties") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                PropertyRow("File name", EditorController.fileName)
                PropertyRow("Words", state.wordCount.toString())
                PropertyRow("Paragraphs", state.blocks.size.toString())
                PropertyRow("Style", state.para.styleName)
                PropertyRow("Modified", if (state.dirty) "Yes (unsaved)" else "No")
            }
        },
        confirmButton = { TextButton(onClick = { PropertiesDialog.dismiss() }) { Text("Close") } },
    )
}

@Composable
private fun PropertyRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text("$label: ", fontWeight = FontWeight.Medium, modifier = Modifier.padding(end = 4.dp))
        Text(value)
    }
}

/** All non-modal dialog surfaces; call once near the top of the app scaffold. */
@Composable
fun RibbonDialogs() {
    ZoomDialogSurface()
    WindowDialogSurface()
    MacrosDialogSurface()
    PropertiesDialogSurface()
}
