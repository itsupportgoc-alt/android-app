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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
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
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.wordcraft.documentcore.PdfImport
import com.wordcraft.documentcore.PdfImportException
import com.wordcraft.documentcore.SENTINEL
import com.wordcraft.editor.AppActionKind
import com.wordcraft.editor.AppHost
import com.wordcraft.editor.CommandSearch
import com.wordcraft.editor.EditorController
import com.wordcraft.editor.EditorScreen
import com.wordcraft.editor.FindBar
import com.wordcraft.editor.Ribbon
import com.wordcraft.editor.RibbonDialogs
import com.wordcraft.editor.ViewState
import com.wordcraft.editor.StatusBar
import com.wordcraft.editor.pageCountFor
import com.wordcraft.storage.Recents
import com.wordcraft.ui.WordCraftTheme
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.util.Base64

class MainActivity : ComponentActivity() {

    private lateinit var recents: Recents

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        recents = Recents(this)
        EditorController.requestSave = { saveFlow.value?.invoke() }
        // The ribbon's platform services (open, save, print, share, pickers).
        AppHost.handler = { kind -> appActionFlow.value?.invoke(kind) }
        AppHost.urlHandler = { url -> openUrl(url, this) }
        AppHost.appVersion = runCatching {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0"
        }.getOrDefault("1.0")

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

/** The ribbon's platform actions, installed by the app screen. */
val appActionFlow = androidx.compose.runtime.mutableStateOf<((AppActionKind) -> Unit)?>(null)

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
    // The ribbon's own pickers: a file to insert, a recipient list, a document to compare with.
    val textFileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val path = copyToCache(uri, activity, "text")
        if (path == null) {
            scope.launch { snackbar.showSnackbar("Could not read that file") }
        } else {
            EditorController.run("insert.textFromFile", JSONObject().put("path", path))
        }
    }
    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val path = copyToCache(uri, activity, "recipients")
        if (path == null) {
            scope.launch { snackbar.showSnackbar("Could not read that file") }
        } else {
            EditorController.run("mailings.recipients", JSONObject().put("path", path))
        }
    }
    val compareLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val path = copyToCache(uri, activity, "compare")
        if (path == null) {
            scope.launch { snackbar.showSnackbar("Could not read that file") }
        } else {
            EditorController.run("review.compare", JSONObject().put("path", path))
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

    // The ribbon's platform actions (File backstage, Insert › Pictures, Mailings, Review, …).
    val doAction: (AppActionKind) -> Unit = { kind ->
        when (kind) {
            AppActionKind.NEW ->
                if (EditorController.state.dirty) showNewConfirm = true
                else {
                    MainActivity.currentUri = null
                    EditorController.newDocument()
                }

            AppActionKind.OPEN -> openLauncher.launch(OPEN_MIMES)
            AppActionKind.SAVE -> doSave()
            AppActionKind.SAVE_AS -> saveAsLauncher.launch(defaultSaveName())
            AppActionKind.SAVE_AS_PDF -> {
                val base = EditorController.fileName.substringBeforeLast('.').ifBlank { "document" }
                pdfSaveLauncher.launch("$base.pdf")
            }

            AppActionKind.OPEN_PDF -> pdfOpenLauncher.launch(arrayOf("application/pdf"))
            AppActionKind.SHARE -> doShare()
            AppActionKind.PRINT -> printDocument(activity) { scope.launch { snackbar.showSnackbar(it) } }
            AppActionKind.RECENTS -> showRecents = true
            AppActionKind.PICK_IMAGE -> imageLauncher.launch(arrayOf("image/*"))
            AppActionKind.PICK_TEXT_FILE -> textFileLauncher.launch(arrayOf("text/*", "application/octet-stream"))
            AppActionKind.PICK_CSV -> csvLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "text/plain"))
            AppActionKind.PICK_DOCUMENT -> compareLauncher.launch(OPEN_MIMES)
            AppActionKind.OPEN_URL -> Unit
        }
    }
    androidx.compose.runtime.SideEffect { appActionFlow.value = doAction }

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
            // Word-style header: a white window bar with AutoSave · save/undo/redo · the centred
            // "file — Not saved" title · search · Share, followed by the ribbon (File/Home/…).
            WindowBar(
                title = EditorController.fileName + if (EditorController.state.dirty) " — Not saved" else " — Saved",
                onSave = doSave,
                onShare = doShare,
                onOpenMenu = { menuOpen = true },
            )
            Box {
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
            }
        },
    ) { padding ->
        // Ribbon dialogs (Zoom / Window / Macros / Properties).
        RibbonDialogs()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Word ribbon: tab strip + the selected tab's groups (Home/View are the full copies).
            Ribbon(modifier = Modifier.fillMaxWidth())
            if (EditorController.findVisible) {
                FindBar()
            }
            EditorScreen(modifier = Modifier.weight(1f).fillMaxWidth())
            // Word status bar: page/words left, view toggles + zoom slider right.
            StatusBar(
                wordCount = EditorController.state.wordCount,
                pageCount = pageCountFor(EditorController.state.wordCount),
            )
        }
    }
}

// ---------------------------------------------------------------- window bar

/**
 * The reference header: a white window bar with an AutoSave pill, save/undo/redo/more cluster,
 * the centred "title — Not saved" caption, a "Search commands and help" field and a blue Share
 * button — all in the top bar, above the ribbon tabs.
 */
@Composable
private fun WindowBar(
    title: String,
    onSave: () -> Unit,
    onShare: () -> Unit,
    onOpenMenu: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // AutoSave pill.
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                    .clickable { EditorController.showStatus("AutoSave: use Save (disc) or the ⋮ menu") }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("AutoSave", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onSave, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.Save, contentDescription = "Save", modifier = Modifier.size(18.dp))
            }
            IconButton(
                onClick = { EditorController.undo() },
                modifier = Modifier.size(32.dp),
                enabled = EditorController.state.canUndo,
            ) {
                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo", modifier = Modifier.size(18.dp))
            }
            IconButton(
                onClick = { EditorController.redo() },
                modifier = Modifier.size(32.dp),
                enabled = EditorController.state.canRedo,
            ) {
                Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo", modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onOpenMenu, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.MoreVert, contentDescription = "More", modifier = Modifier.size(18.dp))
            }

            // Centred title ("file — Not saved"), Word window style.
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )

            // Search "commands and help" field (decorative input, like the reference).
            OutlinedTextField(
                value = TextFieldValue(""),
                onValueChange = {},
                singleLine = true,
                placeholder = { Text("Search commands and help", fontSize = 12.sp) },
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                shape = RoundedCornerShape(18.dp),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                ),
                modifier = Modifier
                    .padding(start = 6.dp)
                    .width(170.dp)
                    .height(46.dp),
            )
            Spacer(Modifier.width(10.dp))

            // Blue Share button.
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable(onClick = onShare)
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    Icons.Filled.Share,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(14.dp),
                )
                Text("Share", color = MaterialTheme.colorScheme.onPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
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
        val lowerName = nameFor(uri, activity).toLowerCase(); val name = if (lowerName.endsWith(".pdf")) lowerName.removeSuffix(".pdf") else nameFor(uri, activity)
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

/**
 * Copy the picked file into the app's cache and return its path.
 *
 * The engine's commands take a *path* (a text file, a recipient list, a document to compare
 * with); the SAF URI cannot be read by the native engine, so the bytes are staged first.
 */
private fun copyToCache(uri: Uri, activity: MainActivity, prefix: String): String? {
    return try {
        val bytes = activity.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: return null
        val dir = File(activity.cacheDir, "picked").apply { mkdirs() }
        val name = "${prefix}_${System.currentTimeMillis()}.bin"
        val out = File(dir, name)
        out.writeBytes(bytes)
        out.absolutePath
    } catch (e: Exception) {
        null
    }
}

/** Print the current document through the Android print service. */
private fun printDocument(activity: MainActivity, onError: (String) -> Unit) {    try {
        val bytes = PdfExport.export(EditorController.state)
        val printManager = activity.getSystemService(android.content.Context.PRINT_SERVICE) as android.print.PrintManager
        val job = printManager.print(
            EditorController.fileName.substringBeforeLast('.').ifBlank { "document" },
            DocumentPrintAdapter(bytes),
            android.print.PrintAttributes.Builder().build(),
        )
        if (job == null) onError("No print service available")
    } catch (e: Exception) {
        onError(e.message ?: "Could not print the document")
    }
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

/** Open a web page (the Help tab's "Help Topics" and "Project Page"). */
private fun openUrl(url: String, activity: MainActivity) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        activity.startActivity(intent)
    } catch (e: Exception) {
        EditorController.showStatus(url)
    }
}

private fun shareDocument(bytes: ByteArray, name: String, activity: MainActivity, done: (String?) -> Unit) {    try {
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
