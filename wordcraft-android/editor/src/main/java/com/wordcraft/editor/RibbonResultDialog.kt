package com.wordcraft.editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject

/**
 * The window a ribbon command's answer opens.
 *
 * Most commands answer with a selection or a confirmation — the ribbon shows a short status
 * instead. The commands listed in [RibbonRunner] answer with something the user wants to
 * read (the theme list, the icon library, the word count, the proofing issues), and those open
 * this dialog.
 */
object ResultDialogs {
    var pending by mutableStateOf<Pair<RibbonCommand, Any?>?>(null)
    fun show(cmd: RibbonCommand, result: Any?) {
        pending = cmd to result
    }

    fun dismiss() {
        pending = null
    }
}

@Composable
fun ResultDialog() {
    val entry = ResultDialogs.pending ?: return
    val cmd = entry.first
    val result = entry.second

    // The engine's icon library: the dialog doubles as a picker.
    val icons = (result as? JSONObject)?.optJSONArray("icons")

    AlertDialog(
        onDismissRequest = { ResultDialogs.dismiss() },
        title = { Text(cmd.label) },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                if (icons != null) {
                    for (i in 0 until icons.length()) {
                        val obj = icons.optJSONObject(i)
                        val id = obj?.optString("id").orEmpty()
                        val name = obj?.optString("name").orEmpty()
                        val category = obj?.optString("category").orEmpty()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    ResultDialogs.dismiss()
                                    RibbonRunner.execute(
                                        RibbonCommand("insert.icon", "Icon", "icons"),
                                        JSONObject().put("id", id),
                                    )
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(name, fontSize = 13.sp, modifier = Modifier.weight(1f))
                            if (category.isNotEmpty()) {
                                Text(
                                    category,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                } else {
                    ResultBody(result)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { ResultDialogs.dismiss() }) { Text("Close") }
        },
    )
}

/** Render an engine answer as rows: key/value pairs, or one line per list entry. */
@Composable
private fun ResultBody(result: Any?) {
    when (result) {
        null -> Text("Done.")

        is JSONArray -> {
            if (result.length() == 0) {
                Text("Nothing to show.", fontSize = 13.sp)
            }
            for (i in 0 until result.length()) {
                val element = result.opt(i)
                if (element is JSONObject) {
                    KeyValueRows(element)
                } else {
                    Text(element?.toString().orEmpty(), fontSize = 13.sp, modifier = Modifier.padding(vertical = 2.dp))
                }
            }
        }

        is JSONObject -> KeyValueRows(result)

        else -> Text(result.toString(), fontSize = 13.sp)
    }
}

@Composable
private fun KeyValueRows(obj: JSONObject) {
    val keys = obj.keys()
    while (keys.hasNext()) {
        val key = keys.next()
        val value = obj.opt(key)
        val text = when (value) {
            is JSONArray -> value.toString()
            null -> ""
            else -> value.toString()
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = key,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(text = text, fontSize = 12.sp, modifier = Modifier.weight(1f))
        }
        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    }
}
