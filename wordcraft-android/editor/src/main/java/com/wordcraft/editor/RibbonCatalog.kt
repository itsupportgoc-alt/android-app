package com.wordcraft.editor

/**
 * The ribbon's data model.
 *
 * The engine's own command registry carries the ribbon layout (every command declares the
 * tab and group it belongs to), so the ribbon is *generated* from that registry — see
 * [EngineRibbon] (generated) and the `tools/build_ribbon_catalog.py` script.
 */

/** How one field of a command's parameter contract is edited. */
enum class FieldKind {
    /** Free text. */
    TEXT,

    /** A number (points, rows, columns…). */
    NUMBER,

    /** A switch. */
    BOOL,

    /** One of a fixed set of values. */
    CHOICE,
}

/**
 * One editable field of an engine command's parameter contract.
 *
 * @param name the parameter name the engine expects
 * @param required the engine refuses the command without it
 * @param optionsProvider an engine command that lists the possible values (a live gallery)
 */
data class RibbonField(
    val name: String,
    val kind: FieldKind,
    val label: String = name,
    val required: Boolean = false,
    val optionsProvider: String? = null,
    val options: List<String> = emptyList(),
)

/**
 * One ribbon button.
 *
 * @param id the engine command id (e.g. `insert.table`)
 * @param appAction set when the button needs a platform service the editor cannot provide
 * @param large true when Word draws this command as a big button (icon above the label)
 */
data class RibbonCommand(
    val id: String,
    val label: String,
    val icon: String,
    val appAction: AppActionKind? = null,
    val fields: List<RibbonField> = emptyList(),
    val large: Boolean = false,
)

/** A captioned ribbon group (Word draws a thin separator between groups). */
data class RibbonGroup(val caption: String, val commands: List<RibbonCommand>)

/**
 * The constructors the generated catalog uses.
 *
 * They live here (not in the generated file) because two generated files share them: the
 * ribbon catalog and the command index.
 */
internal fun ribbonCmd(
    id: String,
    label: String,
    icon: String,
    appAction: AppActionKind? = null,
    fields: List<RibbonField> = emptyList(),
    large: Boolean = false,
) = RibbonCommand(id, label, icon, appAction, fields, large)

internal fun ribbonField(
    name: String,
    kind: FieldKind,
    label: String = name,
    required: Boolean = false,
    optionsProvider: String? = null,
    vararg options: String,
) = RibbonField(name, kind, label, required, optionsProvider, options.toList())

/** The ribbon as the app knows it: tab -> groups -> commands. */
object RibbonCatalog {

    /** Tab -> its captioned groups, in the engine's own order. */
    val tabs: Map<String, List<RibbonGroup>> = EngineRibbon.tabs

    /** The groups of [tab] (empty when the tab has no engine commands). */
    fun groups(tab: String): List<RibbonGroup> = tabs[tab].orEmpty()

    /** Every command of every tab. */
    val commands: List<RibbonCommand> = tabs.values.flatten().flatMap { it.commands }

    /** The command with this id, if the ribbon has one. */
    fun command(id: String): RibbonCommand? = commands.firstOrNull { it.id == id }
}
