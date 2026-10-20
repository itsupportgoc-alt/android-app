package com.wordcraft.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONObject

/**
 * Word's **File** tab: the backstage.
 *
 * The left column is the backstage menu, the right pane the page for the selected entry. Every
 * entry is live: the file entries go through the app's platform services (open, save, share,
 * print), the document entries through the engine's own commands.
 */
enum class FilePage(val title: String) {
    INFO("Info"),
    NEW("New"),
    OPEN("Open"),
    SAVE("Save"),
    SAVE_AS("Save As"),
    PDF("Save as PDF"),
    PRINT("Print"),
    SHARE("Share"),
    RECENT("Recent"),
    OPTIONS("Options"),
}

/** The backstage's current page. */
object FileBackstage {
    var page by mutableStateOf(FilePage.INFO)
        private set

    fun show(page: FilePage) {
        this.page = page
    }
}

@Composable
fun FileTabGroups() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .height(248.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier
                .width(132.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(vertical = 4.dp),
        ) {
            FilePage.entries.forEach { page ->
                Text(
                    text = page.title,
                    fontSize = 13.sp,
                    fontWeight = if (page == FileBackstage.page) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (page == FileBackstage.page) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { FileBackstage.show(page) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            when (FileBackstage.page) {
                FilePage.INFO -> InfoPage()
                FilePage.NEW -> NewPage()
                FilePage.OPEN -> OpenPage()
                FilePage.SAVE -> SavePage()
                FilePage.SAVE_AS -> SaveAsPage()
                FilePage.PDF -> PdfPage()
                FilePage.PRINT -> PrintPage()
                FilePage.SHARE -> SharePage()
                FilePage.RECENT -> RecentPage()
                FilePage.OPTIONS -> OptionsPage()
            }
        }
    }
    HorizontalDivider(
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
    )
}

// ------------------------------------------------------------------ pages

@Composable
private fun InfoPage() {
    val state = EditorController.state
    BackstageTitle("Info")
    Text(EditorController.fileName, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    InfoRow("Format", "Word document (.docx)")
    InfoRow("Words", state.wordCount.toString())
    InfoRow("Paragraphs", state.blocks.size.toString())
    InfoRow("Saved", if (state.dirty) "No — unsaved changes" else "Yes")
    BackstageActions {
        BackstageAction("Protect Document", Icons.Filled.Lock) {
            runEngineCommand("file.protect", "Protect Document", "protect")
        }
        BackstageAction("Inspect Document", Icons.Filled.Info) {
            runEngineCommand("file.inspect", "Inspect Document", "info")
        }
        BackstageAction("Check Compatibility", Icons.Filled.Settings) {
            runEngineCommand("file.compatibility", "Check Compatibility", "settings")
        }
        BackstageAction("Document Properties", Icons.Filled.Description) {
            runEngineCommand("file.properties", "Document Properties", "docProperty")
        }
    }
}

@Composable
private fun NewPage() {
    BackstageTitle("New")
    Text("Start a new document.", fontSize = 13.sp)
    BackstageActions {
        BackstageAction("Blank Document", Icons.Filled.Description) {
            AppHost.perform(AppActionKind.NEW)
        }
        BackstageAction("From a template on this device", Icons.Filled.FolderOpen) {
            AppHost.perform(AppActionKind.OPEN)
        }
    }
}

@Composable
private fun OpenPage() {
    BackstageTitle("Open")
    Text("Open a document stored on this device.", fontSize = 13.sp)
    BackstageActions {
        BackstageAction("Browse", Icons.Filled.FolderOpen) {
            AppHost.perform(AppActionKind.OPEN)
        }
        BackstageAction("Open a PDF", Icons.Filled.Description) {
            AppHost.perform(AppActionKind.OPEN_PDF)
        }
        BackstageAction("Recent documents", Icons.Filled.FolderOpen) {
            AppHost.perform(AppActionKind.RECENTS)
        }
    }
}

@Composable
private fun SavePage() {
    BackstageTitle("Save")
    Text("Save the document.", fontSize = 13.sp)
    BackstageActions {
        BackstageAction("Save", Icons.Filled.Save) {
            AppHost.perform(AppActionKind.SAVE)
        }
        BackstageAction("Save as…", Icons.Filled.Save) {
            AppHost.perform(AppActionKind.SAVE_AS)
        }
        BackstageAction("Save as PDF", Icons.Filled.Description) {
            AppHost.perform(AppActionKind.SAVE_AS_PDF)
        }
    }
}

@Composable
private fun SaveAsPage() {
    BackstageTitle("Save As")
    Text("Save the document under a new name or in another format.", fontSize = 13.sp)
    BackstageActions {
        BackstageAction("Save as…", Icons.Filled.Save) {
            AppHost.perform(AppActionKind.SAVE_AS)
        }
        BackstageAction("Save as PDF", Icons.Filled.Description) {
            AppHost.perform(AppActionKind.SAVE_AS_PDF)
        }
    }
}

@Composable
private fun PdfPage() {
    BackstageTitle("Save as PDF")
    Text("Export the document as a PDF file.", fontSize = 13.sp)
    BackstageActions {
        BackstageAction("Save as PDF", Icons.Filled.Description) {
            AppHost.perform(AppActionKind.SAVE_AS_PDF)
        }
    }
}

@Composable
private fun PrintPage() {
    BackstageTitle("Print")
    Text("Print the document through the Android print service.", fontSize = 13.sp)
    BackstageActions {
        BackstageAction("Print", Icons.Filled.Print) {
            AppHost.perform(AppActionKind.PRINT)
        }
    }
}

@Composable
private fun SharePage() {
    BackstageTitle("Share")
    Text("Share the document with another app.", fontSize = 13.sp)
    BackstageActions {
        BackstageAction("Share", Icons.Filled.Share) {
            AppHost.perform(AppActionKind.SHARE)
        }
    }
}

@Composable
private fun RecentPage() {
    BackstageTitle("Recent")
    Text("Documents you opened before.", fontSize = 13.sp)
    BackstageActions {
        BackstageAction("Show recent documents", Icons.Filled.FolderOpen) {
            AppHost.perform(AppActionKind.RECENTS)
        }
    }
}

@Composable
private fun OptionsPage() {
    BackstageTitle("Options")
    Text("Document and editing options.", fontSize = 13.sp)
    BackstageActions {
        BackstageAction("User Name", Icons.Filled.Settings) {
            runEngineCommand("file.setAuthor", "User Name", "settings")
        }
        BackstageAction("AutoCorrect Options", Icons.Filled.Settings) {
            runEngineCommand("tools.autocorrect", "AutoCorrect Options", "settings")
        }
        BackstageAction("Customize Keyboard", Icons.Filled.Settings) {
            runEngineCommand("tools.customizeKeyboard", "Customize Keyboard", "settings")
        }
        BackstageAction("Check Compatibility", Icons.Filled.Settings) {
            runEngineCommand("file.compatibility", "Check Compatibility", "settings")
        }
    }
}

// ------------------------------------------------------------------ shared bits

/** Run an engine command, opening its options dialog when it takes parameters. */
fun runEngineCommand(id: String, label: String, icon: String = "generic") {
    RibbonRunner.run(
        RibbonCommand(
            id = id,
            label = label,
            icon = icon,
            fields = EngineCommandIndex.fieldsOf(id),
        ),
    )
}

@Composable
private fun BackstageTitle(text: String) {
    Text(text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(110.dp),
        )
        Text(text = value, fontSize = 12.sp)
    }
}

@Composable
private fun BackstageActions(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().heightIn(max = 130.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) { content() }
}

@Composable
private fun BackstageAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp))
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}
