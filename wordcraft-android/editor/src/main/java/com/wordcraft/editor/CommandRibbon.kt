package com.wordcraft.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject

/**
 * The ribbon for the tabs the engine drives.
 *
 * Every button is a real engine command: the generated catalog ([RibbonCatalog]) carries the
 * command id, the label and the parameter contract, so a button opens the same options dialog
 * Word opens and then runs the command. No button is decorative.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CommandTabGroups(tab: String) {
    val groups = RibbonCatalog.groups(tab)
    Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.Top,
        ) {
            groups.forEachIndexed { index, group ->
                if (index > 0) CommandGroupDivider()
                CommandGroup(group)
            }
        }
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
        )
    }
}

/** Thin vertical separator between ribbon groups (as in Word). */
@Composable
private fun CommandGroupDivider() {
    Box(
        modifier = Modifier
            .padding(horizontal = 6.dp)
            .width(1.dp)
            .height(GROUP_HEIGHT.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
    )
}

/** A captioned group, laid out the way Word lays a group out.
 *
 * Word puts the group's most-used commands up top as *large* buttons (icon above the label)
 * and the rest underneath as small icon-and-label buttons, two to a row, with a "More"
 * expander for whatever does not fit. The catalog marks the large ones; the remaining commands
 * fill the small grid and the rest go into the "More" menu, so every command of the group
 * stays reachable without the ribbon growing taller than the rest of the tabs.
 */
@Composable
private fun CommandGroup(group: RibbonGroup) {
    val commands = group.commands
    // The commands the catalog marks as prominent get the big button; the rest become the
    // small icon-and-label rows, and whatever does not fit goes into the "More" expander.
    val large = commands.filter { it.large }.take(MAX_LARGE_BUTTONS)
    val rest = commands.filterNot { it.large }
    val small = rest.take(MAX_SMALL_BUTTONS)
    val overflow = rest.drop(MAX_SMALL_BUTTONS)

    Column(
        modifier = Modifier
            .width(GROUP_WIDTH.dp)
            .height(GROUP_HEIGHT.dp)
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (large.isNotEmpty()) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                large.forEach { LargeCommandButton(it) }
            }
        }
        if (small.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                // Word's small buttons form a grid of two, not one tall column.
                small.chunked(SMALL_PER_ROW).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        row.forEach { cmd ->
                            SmallCommandButton(cmd, Modifier.weight(1f))
                        }
                        // Keep the last row's buttons aligned with the ones above.
                        repeat(SMALL_PER_ROW - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
        if (overflow.isNotEmpty()) {
            MoreCommandButton(overflow, Modifier.fillMaxWidth().padding(top = 2.dp))
        }
        // Word pins the group's caption to the bottom, so every caption lines up.
        Spacer(Modifier.weight(1f))
        Text(
            text = group.caption,
            fontSize = 10.sp,
            letterSpacing = 0.4.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(4.dp))
    }
}

/** Width of one ribbon group. */
private val GROUP_WIDTH = 208

/** How many large buttons a group shows side by side (Word shows at most three). */
private const val MAX_LARGE_BUTTONS = 3

/** How many small buttons a group shows before the "More" expander takes over. */
private const val MAX_SMALL_BUTTONS = 4

/** How many small buttons share one row (Word's groups are two columns wide). */
private const val SMALL_PER_ROW = 2

/**
 * Height of the ribbon group area, so the separators match the groups and every group's caption
 * sits on the same line. Tall enough for the widest group (three large buttons, a two-row
 * small grid and the "More" expander).
 */
private val GROUP_HEIGHT = RIBBON_GROUP_HEIGHT

/** A large ribbon button: icon above the label, as in Word. */
@Composable
private fun LargeCommandButton(cmd: RibbonCommand) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable { RibbonRunner.run(cmd) }
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .width(64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = RibbonIcons.of(cmd.icon),
            contentDescription = cmd.label,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = cmd.label,
            fontSize = 11.sp,
            lineHeight = 12.sp,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

/** A small ribbon button: icon left, label right, as in Word. */
@Composable
private fun SmallCommandButton(cmd: RibbonCommand, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable { RibbonRunner.run(cmd) }
            .padding(horizontal = 3.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = RibbonIcons.of(cmd.icon),
            contentDescription = cmd.label,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = cmd.label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Word's "More" expander: the commands that did not fit in the group. */
@Composable
private fun MoreCommandButton(overflow: List<RibbonCommand>, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .clickable { open = true }
                .padding(horizontal = 3.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("More", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface)
            Icon(
                Icons.Filled.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(14.dp),
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            overflow.forEach { cmd ->
                DropdownMenuItem(
                    text = { Text(cmd.label, fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(
                            RibbonIcons.of(cmd.icon),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                    onClick = {
                        open = false
                        RibbonRunner.run(cmd)
                    },
                )
            }
        }
    }
}

// ------------------------------------------------------------------ running commands

/** Runs a ribbon command: app action, options dialog, or straight to the engine. */
object RibbonRunner {

    /**
     * Commands whose result the user should read: the ribbon opens a result window for them,
     * every other command just reports that it ran.
     */
    private val RESULT_COMMANDS = setOf(
        "design.themes",
        "insert.icon",
        "references.sources",
        "review.wordCount",
        "review.spelling",
        "review.issues",
        "review.thesaurus",
        "file.accessibility",
        "file.info",
    )

    /** Run [cmd]: an app action, the options dialog, or the engine itself. */
    fun run(cmd: RibbonCommand) {
        val action = cmd.appAction
        if (action != null) {
            AppHost.perform(action)
            return
        }
        if (needsOptions(cmd)) {
            CommandDialogs.open(cmd)
        } else {
            execute(cmd, JSONObject())
        }
    }

    /**
     * A command whose only parameters are optional switches toggles by itself (Word's
     * Track Changes, Ink Replay, Hyphenation…), so the ribbon runs it straight away.
     */
    private fun needsOptions(cmd: RibbonCommand): Boolean =
        cmd.fields.any { it.kind != FieldKind.BOOL || it.required }

    /** Run [cmd] with [params] and show whatever the engine answers. */
    fun execute(cmd: RibbonCommand, params: JSONObject) {
        val result = EditorController.runForResult(cmd.id, params)
        if (result == null) return // the engine's error is already reported
        if (cmd.id in RESULT_COMMANDS) {
            ResultDialogs.show(cmd, result)
        } else {
            EditorController.showStatus(cmd.label)
        }
    }
}

// ------------------------------------------------------------------ the options dialog

/** The options dialog a ribbon button opens (Word's "Table > Insert Table" style). */
object CommandDialogs {
    var pending by mutableStateOf<RibbonCommand?>(null)
    fun open(cmd: RibbonCommand) {
        pending = cmd
    }

    fun dismiss() {
        pending = null
    }
}

@Composable
fun CommandParamDialog() {
    val cmd = CommandDialogs.pending ?: return
    var values by remember(cmd.id) { mutableStateOf<Map<String, String>>(emptyMap()) }
    var flags by remember(cmd.id) { mutableStateOf<Map<String, Boolean?>>(emptyMap()) }
    var error by remember(cmd.id) { mutableStateOf<String?>(null) }

    // Live option lists (the engine's own galleries: themes, icons, …).
    val dynamic = remember(cmd.id) {
        cmd.fields.mapNotNull { field ->
            val provider = field.optionsProvider ?: return@mapNotNull null
            val result = EditorController.runForResult(provider, JSONObject())
            field.name to extractOptions(result)
        }.toMap()
    }

    AlertDialog(
        onDismissRequest = { CommandDialogs.dismiss() },
        title = { Text(cmd.label) },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 380.dp).verticalScrollCompat(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {                cmd.fields.forEach { field ->
                    val options = dynamic[field.name].orEmpty().ifEmpty { field.options }
                    when {
                        field.kind == FieldKind.BOOL -> BoolField(
                            label = field.label,
                            state = flags[field.name],
                            onChange = { flags = flags + (field.name to it) },
                        )

                        field.kind == FieldKind.CHOICE || options.isNotEmpty() -> ChoiceField(
                            label = field.label,
                            options = options,
                            selected = values[field.name],
                            required = field.required,
                            onSelect = { values = values + (field.name to it) },
                        )

                        field.kind == FieldKind.NUMBER -> TextFieldRow(
                            label = field.label,
                            value = values[field.name].orEmpty(),
                            required = field.required,
                            keyboard = KeyboardType.Decimal,
                            onChange = { values = values + (field.name to it) },
                        )

                        else -> TextFieldRow(
                            label = field.label,
                            value = values[field.name].orEmpty(),
                            required = field.required,
                            keyboard = KeyboardType.Text,
                            onChange = { values = values + (field.name to it) },
                        )
                    }
                }
                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val params = buildParams(cmd, values, flags)
                if (params == null) {
                    error = "Fill in the required values."
                } else {
                    CommandDialogs.dismiss()
                    RibbonRunner.execute(cmd, params)
                }
            }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = { CommandDialogs.dismiss() }) { Text("Cancel") }
        },
    )
}

/** Build the parameter object; null when a required value is missing or invalid. */
fun buildParams(
    cmd: RibbonCommand,
    values: Map<String, String>,
    flags: Map<String, Boolean?>,
): JSONObject? {
    val json = JSONObject()
    for (field in cmd.fields) {
        when (field.kind) {
            FieldKind.BOOL -> {
                val value = flags[field.name]
                if (value != null) json.put(field.name, value) else if (field.required) json.put(field.name, false)
            }

            FieldKind.NUMBER -> {
                val raw = values[field.name]?.trim().orEmpty()
                if (raw.isEmpty()) {
                    if (field.required) return null
                } else {
                    val number = raw.toDoubleOrNull() ?: return null
                    json.put(field.name, number)
                }
            }

            else -> {
                val raw = values[field.name]?.trim().orEmpty()
                if (raw.isEmpty()) {
                    if (field.required) return null
                } else {
                    json.put(field.name, raw)
                }
            }
        }
    }
    return json
}

@Composable
private fun TextFieldRow(
    label: String,
    value: String,
    required: Boolean,
    keyboard: KeyboardType,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        label = { Text(if (required) "$label *" else label) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun BoolField(label: String, state: Boolean?, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = state == true, onCheckedChange = onChange)
        Text(label, fontSize = 13.sp)
    }
}

@Composable
private fun ChoiceField(
    label: String,
    options: List<String>,
    selected: String?,
    required: Boolean,
    onSelect: (String) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(if (required) "$label *" else label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
                    .clickable { open = true }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = selected ?: "Choose…",
                    fontSize = 13.sp,
                    color = if (selected == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                Icon(
                    Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onSelect(option)
                            open = false
                        },
                    )
                }
            }
        }
    }
}

/** A plain vertical scroll for the dialog body. */
@Composable
private fun Modifier.verticalScrollCompat(): Modifier =
    this.then(verticalScroll(rememberScrollState()))

/**
 * Turn an engine answer into a list of choices: an array of names, or an array of objects with
 * an `id`/`name`, or an object that holds one of those arrays.
 */
fun extractOptions(value: Any?): List<String> = when (value) {
    is JSONArray -> (0 until value.length()).mapNotNull { i ->
        when (val element = value.opt(i)) {
            is String -> element
            is JSONObject -> {
                element.optString("id").takeIf { it.isNotEmpty() }
                    ?: element.optString("name").takeIf { it.isNotEmpty() }
                    ?: element.optString("tag").takeIf { it.isNotEmpty() }
            }

            else -> null
        }
    }

    is JSONObject -> {
        val keys = value.keys()
        var found: List<String> = emptyList()
        while (keys.hasNext() && found.isEmpty()) {
            val key = keys.next()
            val nested = value.opt(key)
            if (nested is JSONArray) found = extractOptions(nested)
        }
        found
    }

    else -> emptyList()
}
