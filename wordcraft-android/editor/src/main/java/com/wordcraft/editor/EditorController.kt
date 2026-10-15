package com.wordcraft.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.wordcraft.documentcore.EditorState
import com.wordcraft.documentcore.EngineOp
import com.wordcraft.documentcore.ParaVm
import com.wordcraft.documentcore.WordCraftEngine
import com.wordcraft.documentcore.WordCraftException
import org.json.JSONObject

/** One rendered top-level run: consecutive paragraphs sharing a style/alignment/list signature. */
data class SectionItem(val key: String, val paras: List<ParaVm>)

/** What the editor renders between text sections. */
sealed interface EditorItem {
    data class Section(val item: SectionItem) : EditorItem
    data class Table(val vm: com.wordcraft.documentcore.TableVm) : EditorItem
}

/**
 * The single editor controller: owns the engine session (a process-wide singleton so rotation
 * never loses the document), the current [EditorState], and every action the UI can request.
 * All mutations go through the engine's command registry.
 */
object EditorController {

    var engine: WordCraftEngine = WordCraftEngine.newDocument()
        private set

    var state: EditorState by mutableStateOf(engine.state())
        private set

    var fileName: String by mutableStateOf("Untitled")
        private set

    var lastError: String? by mutableStateOf(null)
        private set

    var statusMessage: String? by mutableStateOf(null)
        private set

    /** Find & replace visibility (UI-local). */
    var findVisible by mutableStateOf(false)
        private set

    /** Set by the app: opens the system save flow (Ctrl+S / Save menu). */
    var requestSave: (() -> Unit)? = null

    fun refresh() {
        state = engine.state()
    }

    // ------------------------------------------------------------------ documents

    fun newDocument() {
        engine.close()
        engine = WordCraftEngine.newDocument()
        fileName = "Untitled"
        lastError = null
        findVisible = false
        refresh()
    }

    fun open(name: String, bytes: ByteArray): Boolean = try {
        val next = WordCraftEngine.open(name, bytes)
        engine.close()
        engine = next
        fileName = name.substringAfterLast('/')
        lastError = null
        findVisible = false
        refresh()
        true
    } catch (e: WordCraftException) {
        lastError = e.error
        false
    }

    /** Serialize the document; extension of [name] picks the format (default .docx). */
    fun saveBytes(name: String = fileName): ByteArray = try {
        val withExt = if (name.contains('.')) name else "$name.docx"
        val bytes = engine.save(withExt)
        statusMessage = "Saved $withExt"
        bytes
    } catch (e: WordCraftException) {
        lastError = e.error
        throw e
    }

    fun rename(name: String) {
        fileName = name
    }

    // ------------------------------------------------------------------ commands

    /** Dispatch a prepared list of engine ops in order, then refresh the state. */
    fun dispatch(ops: List<EngineOp>) {
        if (ops.isEmpty()) return
        for (op in ops) {
            try {
                engine.run(op.id, JSONObject(op.params))
            } catch (e: WordCraftException) {
                lastError = e.error
                break
            }
        }
        refresh()
    }

    /** Run one command (toolbar/menu), refreshing afterwards. */
    fun run(id: String, params: JSONObject? = null): Boolean = try {
        engine.run(id, params)
        refresh()
        true
    } catch (e: WordCraftException) {
        lastError = e.error
        refresh()
        false
    }

    /** Run a command that may legitimately be unavailable; returns false without an error. */
    fun runQuiet(id: String, params: JSONObject? = null): Boolean = try {
        engine.run(id, params)
        refresh()
        true
    } catch (_: WordCraftException) {
        refresh()
        false
    }

    // ------------------------------------------------------------------ editing actions

    fun undo() = run("edit.undo")
    fun redo() = run("edit.redo")

    fun toggleBold() = run("format.bold")
    fun toggleItalic() = run("format.italic")
    fun toggleUnderline() = run("format.underline")
    fun toggleStrike() = run("format.strikethrough")

    fun setAlign(value: String) = run("para.align", JSONObject().put("value", value))
    fun indent() = run("para.indent")
    fun outdent() = run("para.outdent")

    fun setFontSize(size: Float) = run("format.size", JSONObject().put("size", size))
    fun growFont() = run("format.growFont")
    fun shrinkFont() = run("format.shrinkFont")
    fun setFont(name: String) = run("format.font", JSONObject().put("name", name))
    fun setTextColor(hex: String) = run("format.color", JSONObject().put("color", hex))
    fun setHighlight(name: String) = run("format.highlight", JSONObject().put("color", name))
    fun clearFormatting() = run("format.clear")

    fun applyStyle(style: String) = run("para.style", JSONObject().put("style", style))
    fun toggleBullets() = run("para.bullets")
    fun toggleNumbering() = run("para.numbering")

    fun insertTable(rows: Int, cols: Int) =
        run("insert.table", JSONObject().put("rows", rows).put("cols", cols))

    fun insertLink(url: String, text: String?) {
        val p = JSONObject().put("url", url)
        if (!text.isNullOrEmpty()) p.put("text", text)
        run("insert.link", p)
    }

    fun insertPictureBase64(b64: String, widthPt: Float? = null, alt: String? = null) {
        val p = JSONObject().put("data", b64)
        if (widthPt != null) p.put("width", widthPt)
        if (!alt.isNullOrEmpty()) p.put("alt", alt)
        run("insert.picture", p)
    }

    fun insertPageBreak() = run("text.pageBreak")
    fun selectAll() = run("select.all")

    // ------------------------------------------------------------------ find / replace

    fun showFind(visible: Boolean) {
        val v = visible
        findVisible = v
        if (!v) runQuiet("edit.find", JSONObject().put("text", ""))
    }

    fun find(text: String, matchCase: Boolean = false) {
        runQuiet("edit.find", JSONObject().put("text", text).put("matchCase", matchCase))
    }

    fun findNext() = runQuiet("edit.findNext")
    fun findPrevious() = runQuiet("edit.findPrevious")

    fun replace(text: String, with: String) = runQuiet("edit.replace", JSONObject().put("text", text).put("with", with))

    fun replaceAll(text: String, with: String) = runQuiet("edit.replaceAll", JSONObject().put("text", text).put("with", with))

    // ------------------------------------------------------------------ split blocks into render items

    /** Split the top-level blocks into sections (runs of paragraphs with one signature) and tables. */
    fun items(): List<EditorItem> {
        val out = ArrayList<EditorItem>()
        val run = ArrayList<ParaVm>()
        fun flush() {
            if (run.isNotEmpty()) {
                out.add(EditorItem.Section(sectionOf(run)))
                run.clear()
            }
        }
        for (b in state.blocks) {
            val para = b.asPara
            val table = b.asTable
            when {
                table != null -> {
                    flush()
                    out.add(EditorItem.Table(table))
                }
                para != null -> {
                    // Break the run at style/alignment/signature changes and around list items
                    // (each list item gets its own row so its marker can render beside it).
                    val prev = run.lastOrNull()
                    if (prev != null && (prev.sectionKey() != para.sectionKey() || prev.list != null || para.list != null)) {
                        flush()
                    }
                    run.add(para)
                }
            }
        }
        flush()
        return out
    }

    fun dismissError() {
        lastError = null
    }

    fun dismissStatus() {
        statusMessage = null
    }
}

/** Group consecutive paragraphs into sections by rendering signature (shared by body and cells). */
fun splitSections(paras: List<ParaVm>): List<SectionItem> {
    val out = ArrayList<SectionItem>()
    val run = ArrayList<ParaVm>()
    fun flush() {
        if (run.isNotEmpty()) {
            out.add(sectionOf(run))
            run.clear()
        }
    }
    for (p in paras) {
        val prev = run.lastOrNull()
        if (prev != null && (prev.sectionKey() != p.sectionKey() || prev.list != null || p.list != null)) flush()
        run.add(p)
    }
    flush()
    return out
}

private fun sectionOf(run: List<ParaVm>): SectionItem =
    SectionItem(run.first().path.joinToString("/") + "#" + run.size, run.toList())
