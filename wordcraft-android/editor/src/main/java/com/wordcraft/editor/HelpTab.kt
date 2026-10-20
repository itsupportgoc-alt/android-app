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
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Spellcheck
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
 * Word's **Help** tab and the command search.
 *
 * The search is the engine's own (`tools.search`): it looks through every command's label,
 * location and id, so the search finds the whole engine, not just the ribbon.
 */
object CommandSearch {
    var visible by mutableStateOf(false)
        private set

    /** The query the search box starts with. */
    var query by mutableStateOf("")
        private set

    fun open(query: String = "") {
        this.query = query
        visible = true
    }

    fun dismiss() {
        visible = false
    }
}

@Composable
fun HelpTabGroups() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        HelpGroup(
            "Get Help",
            listOf(
                HelpItem("Search Commands", "search") { CommandSearch.open() },
                HelpItem("Help Topics", "help") { AppHost.openUrl(UPSTREAM_URL) },
            ),
        )
        HelpDivider()
        HelpGroup(
            "Word Tools",
            listOf(
                HelpItem("Word Count", "wordCount") { runEngineCommand("review.wordCount", "Word Count", "wordCount") },
                HelpItem("Spelling & Grammar", "proofing") { runEngineCommand("review.spelling", "Spelling & Grammar", "proofing") },
                HelpItem("Proofing Issues", "proofing") { runEngineCommand("review.issues", "Proofing Issues", "proofing") },
                HelpItem("Thesaurus", "thesaurus") { runEngineCommand("review.thesaurus", "Thesaurus", "thesaurus") },
                HelpItem("Accessibility Checker", "accessibility") { runEngineCommand("file.accessibility", "Check Accessibility", "accessibility") },
            ),
        )
        HelpDivider()
        HelpGroup(
            "Speech",
            listOf(
                HelpItem("Read Aloud", "readAloud") { runEngineCommand("review.readAloud", "Read Aloud", "readAloud") },
                HelpItem("Stop Reading", "readAloud") { runEngineCommand("readAloud.stop", "Stop Reading", "readAloud") },
                HelpItem("Reading Speed", "readAloud") { runEngineCommand("readAloud.speed", "Reading Speed", "readAloud") },
                HelpItem("Set Proofing Language", "language") { runEngineCommand("review.language", "Language", "language") },
            ),
        )
        HelpDivider()
        HelpGroup(
            "About",
            listOf(
                HelpItem("About DevWord", "about") { AboutDialog.show() },
                HelpItem("Project Page", "help") { AppHost.openUrl(APP_URL) },
            ),
        )
    }
    HorizontalDivider(
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
    )
}

/** The upstream engine's home page (the Help topics). */
const val UPSTREAM_URL = "https://github.com/storytold/wordcraft"

/** This app's home page. */
const val APP_URL = "https://github.com/itsupportgoc-alt/android-app"

private data class HelpItem(val label: String, val icon: String, val onClick: () -> Unit)

@Composable
private fun HelpGroup(caption: String, items: List<HelpItem>) {
    Column(
        modifier = Modifier.width(196.dp).padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            items.forEach { item -> HelpButton(item) }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = caption,
            fontSize = 10.sp,
            letterSpacing = 0.4.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun HelpDivider() {
    Box(
        modifier = Modifier
            .padding(horizontal = 6.dp)
            .width(1.dp)
            .height(96.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
    )
}

@Composable
private fun HelpButton(item: HelpItem) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = item.onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = RibbonIcons.of(item.icon),
            contentDescription = item.label,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(18.dp),
        )
        Text(item.label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
    }
}

// ------------------------------------------------------------------ command search

/** One hit of the engine's command search. */
data class CommandMatch(val id: String, val label: String, val location: String)

/** Ask the engine for the commands matching [query]. */
fun searchCommands(query: String): List<CommandMatch> {
    if (query.isBlank()) return emptyList()
    val result = EditorController.runForResult("tools.search", JSONObject().put("query", query))
    val matches = (result as? JSONObject)?.optJSONArray("matches") ?: return emptyList()
    val out = ArrayList<CommandMatch>()
    for (i in 0 until matches.length()) {
        val obj = matches.optJSONObject(i) ?: continue
        out.add(
            CommandMatch(
                id = obj.optString("id"),
                label = obj.optString("label"),
                location = obj.optString("location"),
            ),
        )
    }
    return out
}

@Composable
fun CommandSearchDialog() {
    if (!CommandSearch.visible) return
    var query by remember { mutableStateOf(CommandSearch.query) }
    val matches = remember(query) { searchCommands(query) }

    AlertDialog(
        onDismissRequest = { CommandSearch.dismiss() },
        title = { Text("Search commands and help") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    placeholder = { Text("e.g. table, header, spelling") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Column(
                    modifier = Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    if (query.isNotBlank() && matches.isEmpty()) {
                        Text("No command matches \"$query\".", fontSize = 12.sp)
                    }
                    matches.forEach { match ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .clickable {
                                    CommandSearch.dismiss()
                                    RibbonRunner.run(
                                        RibbonCommand(
                                            id = match.id,
                                            label = match.label,
                                            icon = "generic",
                                            fields = EngineCommandIndex.fieldsOf(match.id),
                                        ),
                                    )
                                }
                                .padding(horizontal = 6.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(match.label, fontSize = 13.sp)
                                if (match.location.isNotEmpty()) {
                                    Text(
                                        match.location,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Icon(
                                Icons.Filled.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { CommandSearch.dismiss() }) { Text("Close") }
        },
    )
}

// ------------------------------------------------------------------ about

object AboutDialog {
    var visible by mutableStateOf(false)
        private set

    fun show() {
        visible = true
    }

    fun dismiss() {
        visible = false
    }
}

@Composable
fun AboutDialogSurface() {
    if (!AboutDialog.visible) return
    val state = EditorController.state
    AlertDialog(
        onDismissRequest = { AboutDialog.dismiss() },
        title = { Text("About DevWord") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("DevWord for Android", fontWeight = FontWeight.SemiBold)
                Text("Version ${AppHost.appVersion}", fontSize = 12.sp)
                Text("Engine: WordCraft document engine (Rust)", fontSize = 12.sp)
                Text("Words: ${state.wordCount}", fontSize = 12.sp)
                Text("Paragraphs: ${state.blocks.size}", fontSize = 12.sp)
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = { AppHost.openUrl(APP_URL) }) { Text("Project page") }
                TextButton(onClick = { AppHost.openUrl(UPSTREAM_URL) }) { Text("Engine home page") }
            }
        },
        confirmButton = {
            TextButton(onClick = { AboutDialog.dismiss() }) { Text("Close") }
        },
    )
}
