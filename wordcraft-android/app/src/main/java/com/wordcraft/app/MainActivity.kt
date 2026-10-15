package com.wordcraft.app

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.wordcraft.documentcore.PdfImport
import com.wordcraft.documentcore.PdfImportException
import com.wordcraft.documentcore.SENTINEL
import com.wordcraft.editor.EditorController
import com.wordcraft.editor.EditorScreen
import com.wordcraft.editor.FindBar
import com.wordcraft.editor.FormatBar
import com.wordcraft.editor.Ribbon
import com.wordcraft.editor.RibbonDialogs
import com.wordcraft.editor.StatusBar
import com.wordcraft.editor.pageCountFor
import com.wordcraft.storage.Recents
import com.wordcraft.ui.WordCraftTheme
import kotlinx.coroutines.launch
import java.io.File
import java.util.Base64

class MainActivity : ComponentActivity() {

    private lateinit var recents: Recents

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        recents = Recents(this)
        EditorController.requestSave = { saveFlow.value?.invoke() }

        setContent {
            WordCraftTheme(darkTheme = false) {
                AppScreen(recents = recents, activity = this)
            }
        }
    }

    companion object {
        /** Survives rotation (the engine session is a process singleton too). */
        var currentUri: Uri? = null
    }
}

/** Save flow installed by [MainActivity] so Ctrl+S / Save can trigger the SAF write. */
val saveFlow = androidx.compose.runtime.mutableStateOf<(() -> Unit)?>(null)

private const val DOCX_MIME = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"

private val OPEN_MIMES = arrayOf(
    DOCX_MIME,
    "application/msword",
    "application/rtf",
    "application/vnd.oasis.opendocument.text",
    "text/markdown",
    "text/html",
    "text/plain",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppScreen(recents: Recents, activity: MainActivity) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var menuOpen by remember { mutableStateOf(false) }
    var showRecents by remember { mutableStateOf(false) }
    var showNewConfirm by remember { mutableStateOf(false) }
    var linkDialog by remember { mutableStateOf(false) }

    // ---- file pickers ----
    val openLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) openUri(uri, recents, activity) {
            scope.launch { snackbar.showSnackbar(it) }
        }
    }
    val saveAsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(DOCX_MIME)) { uri ->
        if (uri != null) writeBytesTo(uri, activity, nameFor(uri, activity)) {
            scope.launch { snackbar.showSnackbar(it) }
        }
    }
    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) insertImage(uri, activity) {
            scope.launch { snackbar.showSnackbar(it) }
        }
    }
    val shareLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(DOCX_MIME)) { uri ->
        if (uri != null) writeBytesTo(uri, activity, nameFor(uri, activity)) {
            scope.launch { snackbar.showSnackbar(it) }
        }
    }
    // Save as PDF / open a PDF for editing.
    val pdfSaveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) writePdfTo(uri, activity) {
            scope.launch { snackbar.showSnackbar(it) }
        }
    }
    val pdfOpenLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) openPdf(uri, activity) {
            scope.launch { snackbar.showSnackbar(it) }
        }
    }

    val doSave: () -> Unit = {
        val target = MainActivity.currentUri
        if (target == null) {
            saveAsLauncher.launch(defaultSaveName())
        } else {
            writeBytesTo(target, activity, nameFor(target, activity)) {
                scope.launch { snackbar.showSnackbar(it) }
            }
        }
    }
    androidx.compose.runtime.SideEffect { saveFlow.value = doSave }

    val doShare: () -> Unit = {
        try {
            val bytes = EditorController.saveBytes()
            shareDocument(bytes, EditorController.fileName, activity) { err ->
                if (err != null) scope.launch { snackbar.showSnackbar(err) }
            }
        } catch (e: Exception) {
            scope.launch { snackbar.showSnackbar(e.message ?: "Save failed") }
        }
    }

    // ---- messages ----
    LaunchedEffect(EditorController.lastError) {
        EditorController.lastError?.let {
            snackbar.showSnackbar(it)
            EditorController.dismissError()
        }
    }
    LaunchedEffect(EditorController.statusMessage) {
        EditorController.statusMessage?.let {
            snackbar.showSnackbar(it)
            EditorController.dismissStatus()
        }
    }

    // ---- links ----
    if (linkDialog) {
        LinkDialog(onDismiss = { linkDialog = false })
    }
    if (showNewConfirm) {
        AlertDialog(
            onDismissRequest = { showNewConfirm = false },
            title = { Text("New document?") },
            text = { Text("Unsaved changes in \"${EditorController.fileName}\" will be lost.") },
            confirmButton = {
                TextButton(onClick = {
                    showNewConfirm = false
                    MainActivity.currentUri = null
                    EditorController.newDocument()
                }) { Text("New") }
            },
            dismissButton = {
                TextButton(onClick = { showNewConfirm = false }) { Text("Cancel") }
            },
        )
    }
    if (showRecents) {
        RecentsDialog(
            recents = recents,
            onDismiss = { showRecents = false },
            onPick = { doc ->
                showRecents = false
                val uri = Uri.parse(doc.uri)
                openUri(uri, recents, activity) { scope.launch { snackbar.showSnackbar(it) } }
            },
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Word identity mark: white tile with a blue "W".
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(MaterialTheme.colorScheme.onPrimary),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "W",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = EditorController.fileName + if (EditorController.state.dirty) " •" else "",
                            maxLines = 1,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                actions = {
                    IconButton(onClick = doSave) {
                        Icon(Icons.Filled.Save, contentDescription = "Save")
                    }
                    IconButton(onClick = doShare) {
                        Icon(Icons.Filled.Share, contentDescription = "Share")
                    }
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("New") },
                            leadingIcon = { Icon(Icons.Filled.FileOpen, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                if (EditorController.state.dirty) showNewConfirm = true
                                else {
                                    MainActivity.currentUri = null
                                    EditorController.newDocument()
                                }
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Open…") },
                            onClick = {
                                menuOpen = false
                                openLauncher.launch(OPEN_MIMES)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Save") },
                            onClick = {
                                menuOpen = false
                                doSave()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Save as…") },
                            onClick = {
                                menuOpen = false
                                saveAsLauncher.launch(defaultSaveName())
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Save as PDF") },
                            onClick = {
                                menuOpen = false
                                val base = EditorController.fileName.substringBeforeLast('.')
                                    .ifBlank { "document" }
                                pdfSaveLauncher.launch("$base.pdf")
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Open PDF…") },
                            onClick = {
                                menuOpen = false
                                pdfOpenLauncher.launch(arrayOf("application/pdf"))
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Share") },
                            onClick = {
                                menuOpen = false
                                doShare()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Recent documents") },
                            onClick = {
                                menuOpen = false
                                showRecents = true
                            },
                        )
                    }
                },
            )
        },
    ) { padding ->
        // Ribbon dialogs (Zoom / Window / Macros / Properties).
        RibbonDialogs()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Word ribbon: tab strip + the selected tab's groups (View is the reference copy).
            Ribbon(modifier = Modifier.fillMaxWidth())
            if (EditorController.findVisible) {
                FindBar()
            }
            EditorScreen(modifier = Modifier.weight(1f).fillMaxWidth())
            FormatBar(
                onPickImage = { imageLauncher.launch(arrayOf("image/*")) },
                onInsertLink = { linkDialog = true },
            )
            // Word status bar: page/words left, view toggles + zoom slider right.
            StatusBar(
                wordCount = EditorController.state.wordCount,
                pageCount = pageCountFor(EditorController.state.wordCount),
            )
        }
    }
}

// ---------------------------------------------------------------- file helpers

private fun openUri(uri: Uri, recents: Recents, activity: MainActivity, onError: (String) -> Unit) {
    try {
        val name = nameFor(uri, activity)
        val bytes = activity.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalStateException("Cannot read $name")
        if (EditorController.open(name, bytes)) {
            MainActivity.currentUri = uri
            recents.record(name, uri.toString())
        }
    } catch (e: Exception) {
        onError(e.message ?: "Could not open document")
    }
}

private fun writeBytesTo(uri: Uri, activity: MainActivity, name: String, onError: (String) -> Unit) {
    try {
        val bytes = EditorController.saveBytes(name)
        activity.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
            ?: throw IllegalStateException("Cannot write $name")
        EditorController.rename(name)
    } catch (e: Exception) {
        onError(e.message ?: "Could not save document")
    }
}

/** Save the current document as a PDF at [uri] (SAF `CreateDocument("application/pdf")`). */
private fun writePdfTo(uri: Uri, activity: MainActivity, onError: (String) -> Unit) {
    try {
        val bytes = PdfExport.export(EditorController.state)
        activity.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
            ?: throw IllegalStateException("Cannot write PDF")
        EditorController.showStatus("Saved as PDF")
    } catch (e: Exception) {
        onError(e.message ?: "Could not save PDF")
    }
}

/**
 * Open a PDF: extract its text ([PdfImport]) and load it as a fresh editable document.
 * The original file is untouched; edit and use *Save as PDF* / *Save as…* to write changes.
 */
private fun openPdf(uri: Uri, activity: MainActivity, onError: (String) -> Unit) {
    try {
        val bytes = activity.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalStateException("Cannot read PDF")
        val text = PdfImport.extractText(bytes)
        EditorController.importPlainText(text)
        val name = nameFor(uri, activity).removeSuffix(".pdf").removeSuffix(".PDF")
        EditorController.rename(name)
        MainActivity.currentUri = null // writes go through Save as… (target is a PDF)
        EditorController.showStatus("Opened $name.pdf — editable")
    } catch (e: PdfImportException) {
        onError(e.message ?: "Cannot open this PDF")
    } catch (e: Exception) {
        onError(e.message ?: "Could not open PDF")
    }
}

private fun nameFor(uri: Uri, activity: MainActivity): String {
    activity.contentResolver.query(uri, null, null, null, null)?.use { c ->
        val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (i >= 0 && c.moveToFirst()) c.getString(i)?.let { return it }
    }
    return uri.lastPathSegment ?: "document.docx"
}

private fun defaultSaveName(): String {
    val base = EditorController.fileName.substringBeforeLast('.')
    return if (base.isBlank()) "document.docx" else "$base.docx"
}

private fun insertImage(uri: Uri, activity: MainActivity, onError: (String) -> Unit) {
    try {
        val bytes = activity.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalStateException("Cannot read image")
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val densityDpi = activity.resources.displayMetrics.densityDpi.coerceAtLeast(160)
        // Fit the picture to the page width (~420pt) preserving aspect.
        val widthPt = if (bounds.outWidth > 0) {
            val natural = bounds.outWidth * 72f / densityDpi
            natural.coerceAtMost(420f)
        } else 240f
        val b64 = Base64.getEncoder().encodeToString(bytes)
        EditorController.insertPictureBase64(b64, widthPt)
    } catch (e: Exception) {
        onError(e.message ?: "Could not insert image")
    }
}

private fun shareDocument(bytes: ByteArray, name: String, activity: MainActivity, done: (String?) -> Unit) {
    try {
        val dir = File(activity.cacheDir, "share").apply { mkdirs() }
        val outName = if (name.contains('.')) name else "$name.docx"
        val f = File(dir, outName)
        f.writeBytes(bytes)
        val uri = FileProvider.getUriForFile(activity, "${activity.packageName}.fileprovider", f)
        done(null)
        // Hand the URI to the caller through a chooser.
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = DOCX_MIME
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        activity.startActivity(Intent.createChooser(intent, "Share document"))
    } catch (e: Exception) {
        done(e.message ?: "Share failed")
    }
}

// ---------------------------------------------------------------- dialogs

@Composable
private fun RecentsDialog(recents: Recents, onDismiss: () -> Unit, onPick: (com.wordcraft.storage.RecentDoc) -> Unit) {
    val docs = remember { recents.list() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Recent documents") },
        text = {
            if (docs.isEmpty()) {
                Text("No recent documents yet.")
            } else {
                Column {
                    docs.forEach { d ->
                        TextButton(onClick = { onPick(d) }) {
                            Text(d.name, maxLines = 1, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

@Composable
private fun LinkDialog(onDismiss: () -> Unit) {
    var url by remember { mutableStateOf(TextFieldValue("")) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Insert link") },
        text = {
            androidx.compose.material3.OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                singleLine = true,
                placeholder = { Text("https://example.com") },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = {
                val u = url.text.trim()
                if (u.isNotEmpty()) EditorController.insertLink(u, null)
                onDismiss()
            }) { Text("Insert") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
