package com.wordcraft.documentcore

import org.json.JSONArray
import org.json.JSONObject

/** A document position: story + path to the paragraph + UTF-8 byte offset inside it. */
data class PosVm(val story: String, val path: List<Int>, val off: Int) {
    val block: Int get() = path.firstOrNull() ?: 0

    /** Engine JSON for `select.range` / `caret.set` (`Pos` serde shape). */
    fun toJson(): JSONObject = JSONObject().put("story", story).put("path", JSONArray(path)).put("off", off)

    companion object {
        fun body(block: Int, off: Int) = PosVm("body", listOf(block), off)
    }
}

data class SelectionVm(val anchor: PosVm, val focus: PosVm) {
    /** (start, end) in document order. */
    fun ordered(): Pair<PosVm, PosVm> = if (anchor <= focus) anchor to focus else focus to anchor

    companion object {
        fun collapsed(p: PosVm) = SelectionVm(p, p)
    }
}

private fun comparePos(a: PosVm, b: PosVm): Int {
    val sa = a.story; val sb = b.story
    if (sa != sb) return sa.compareTo(sb)
    val n = maxOf(a.path.size, b.path.size)
    for (i in 0 until n) {
        val x = a.path.getOrNull(i) ?: return -1
        val y = b.path.getOrNull(i) ?: return 1
        if (x != y) return x.compareTo(y)
    }
    return a.off.compareTo(b.off)
}

private operator fun PosVm.compareTo(other: PosVm): Int = comparePos(this, other)

/** Character formatting for one run of text (UTF-16 indices into the paragraph text). */
data class SpanVm(
    val start: Int,
    val end: Int,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: String? = null,
    val strike: Boolean = false,
    val sizePt: Float? = null,
    val font: String? = null,
    val color: String? = null,
    val highlight: String? = null,
    val vertAlign: String? = null,
    val caps: Boolean = false,
    val smallCaps: Boolean = false,
    val rtl: Boolean = false,
    val link: String? = null,
)

data class LineSpacingVm(val kind: String, val value: Float)

data class ListVm(val label: String, val level: Int, val hanging: Float, val indent: Float)

data class ImageVm(val kind: String, val w: Float, val h: Float, val alt: String, val b64: String?)

/** A paragraph block (path = indices from the story root; inside a table it starts with the table block). */
data class ParaVm(
    override val path: List<Int>,
    val text: String,
    val spans: List<SpanVm>,
    val style: String,
    val styleName: String,
    val align: String,
    val indentLeft: Float,
    val indentFirst: Float,
    val spaceBefore: Float,
    val spaceAfter: Float,
    val lineSpacing: LineSpacingVm?,
    val pageBreak: Boolean,
    val rtl: Boolean,
    val outline: Int?,
    val shading: String?,
    val list: ListVm?,
    val imgs: List<ImageVm>,
) : BlockVm {
    /** Paragraphs with the same editing/rendering signature can share one text field. */
    fun sectionKey(): String = "$style|$align|$rtl|${list != null}"
}

data class CellVm(val span: Int, val blocks: List<BlockVm>)

/** A table block. */
data class TableVm(
    override val path: List<Int>,
    val grid: List<Float>,
    val rows: List<List<CellVm>>,
) : BlockVm

sealed interface BlockVm {
    val path: List<Int>
    val asPara: ParaVm? get() = this as? ParaVm
    val asTable: TableVm? get() = this as? TableVm
}

data class FormatVm(
    val font: String? = null,
    val sizePt: Float? = null,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val strike: Boolean = false,
    val color: String? = null,
    val highlight: String? = null,
    val style: String? = null,
    val styleName: String? = null,
    val subscript: Boolean = false,
    val superscript: Boolean = false,
)

data class ParaStateVm(
    val style: String,
    val styleName: String,
    val align: String,
    val indentLeft: Float,
    val indentFirst: Float,
    val spaceBefore: Float,
    val spaceAfter: Float,
    val lineSpacing: LineSpacingVm?,
    val inList: Boolean,
)

data class FindVm(val query: String, val replace: String, val matches: Int, val current: Int)

/** Everything the UI needs after a mutation. */
data class EditorState(
    val blocks: List<BlockVm>,
    val canUndo: Boolean,
    val canRedo: Boolean,
    val dirty: Boolean,
    val selection: SelectionVm,
    val selectedText: String,
    val wordCount: Int,
    val format: FormatVm,
    val para: ParaStateVm,
    val find: FindVm,
)

/** Parses the engine's `state()` JSON ([StateParser]). */
object StateParser {

    fun parse(raw: String): EditorState {
        val o = JSONObject(raw)
        val sel = o.getJSONObject("selection")
        return EditorState(
            blocks = parseBlocks(o.getJSONArray("blocks")),
            canUndo = o.optBoolean("canUndo"),
            canRedo = o.optBoolean("canRedo"),
            dirty = o.optBoolean("dirty"),
            selection = SelectionVm(parsePos(sel.getJSONObject("anchor")), parsePos(sel.getJSONObject("focus"))),
            selectedText = o.optString("selectedText"),
            wordCount = o.optInt("wordCount"),
            format = parseFormat(o.optJSONObject("format") ?: JSONObject()),
            para = parseParaState(o.optJSONObject("para") ?: JSONObject()),
            find = parseFind(o.optJSONObject("find") ?: JSONObject()),
        )
    }

    fun parsePos(o: JSONObject): PosVm {
        val story = o.optString("story", "body")
        val path = o.optJSONArray("path")?.let { arr -> (0 until arr.length()).map { arr.getInt(it) } }
            ?: listOf(o.optInt("block", 0))
        return PosVm(story, path, o.optInt("off", 0))
    }

    private fun parseFind(o: JSONObject) =
        FindVm(o.optString("query"), o.optString("replace"), o.optInt("matches"), o.optInt("current"))

    private fun parseFormat(o: JSONObject) = FormatVm(
        font = o.optStringOrNull("font"),
        sizePt = o.optFloatOrNull("size"),
        bold = o.optBoolean("bold"),
        italic = o.optBoolean("italic"),
        underline = o.optBoolean("underline"),
        strike = o.optBoolean("strike"),
        color = o.optColorOrNull("color"),
        highlight = o.optColorOrNull("highlight"),
        style = o.optStringOrNull("style"),
        styleName = o.optStringOrNull("styleName"),
        subscript = o.optBoolean("subscript"),
        superscript = o.optBoolean("superscript"),
    )

    private fun parseParaState(o: JSONObject) = ParaStateVm(
        style = o.optString("style", "Normal"),
        styleName = o.optString("styleName", "Normal"),
        align = o.optString("align", "left"),
        indentLeft = o.optFloat("indentLeft"),
        indentFirst = o.optFloat("indentFirst"),
        spaceBefore = o.optFloat("spaceBefore"),
        spaceAfter = o.optFloat("spaceAfter"),
        lineSpacing = o.optJSONObject("lineSpacing")?.let { LineSpacingVm(it.optString("kind"), it.optFloat("value")) },
        inList = o.optBoolean("inList"),
    )

    private fun parseBlocks(arr: JSONArray): List<BlockVm> {
        val out = ArrayList<BlockVm>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            when (o.optString("k")) {
                "t" -> out.add(parseTable(o))
                else -> out.add(parsePara(o))
            }
        }
        return out
    }

    private fun parseTable(o: JSONObject): TableVm {
        val path = path(o)
        val gridArr = o.optJSONArray("grid") ?: JSONArray()
        val grid = (0 until gridArr.length()).map { gridArr.getDouble(it).toFloat() }
        val rowsArr = o.optJSONArray("rows") ?: JSONArray()
        val rows = (0 until rowsArr.length()).map { r ->
            val cellsArr = rowsArr.getJSONArray(r)
            (0 until cellsArr.length()).map { c ->
                val cell = cellsArr.getJSONObject(c)
                CellVm(cell.optInt("span", 1), parseBlocks(cell.optJSONArray("blocks") ?: JSONArray()))
            }
        }
        return TableVm(path, grid, rows)
    }

    private fun parsePara(o: JSONObject): ParaVm {
        val spansArr = o.optJSONArray("spans") ?: JSONArray()
        val spans = (0 until spansArr.length()).map { s ->
            val sO = spansArr.getJSONObject(s)
            SpanVm(
                start = sO.getInt("s0"),
                end = sO.getInt("s1"),
                bold = sO.optBoolean("b"),
                italic = sO.optBoolean("i"),
                underline = sO.optStringOrNull("u"),
                strike = sO.optBoolean("s"),
                sizePt = sO.optFloatOrNull("sz"),
                font = sO.optStringOrNull("font"),
                color = sO.optStringOrNull("color"),
                highlight = sO.optStringOrNull("hl"),
                vertAlign = sO.optStringOrNull("va"),
                caps = sO.optBoolean("caps"),
                smallCaps = sO.optBoolean("scaps"),
                rtl = sO.optBoolean("rtl"),
                link = sO.optStringOrNull("link"),
            )
        }
        val imgsArr = o.optJSONArray("imgs") ?: JSONArray()
        val imgs = (0 until imgsArr.length()).map { i ->
            val iO = imgsArr.getJSONObject(i)
            ImageVm(
                kind = iO.optString("kind", "image"),
                w = iO.optFloat("w"),
                h = iO.optFloat("h"),
                alt = iO.optString("alt"),
                b64 = iO.optStringOrNull("b64"),
            )
        }
        val listO = o.optJSONObject("list")
        return ParaVm(
            path = path(o),
            text = o.optString("text"),
            spans = spans,
            style = o.optString("style", "Normal"),
            styleName = o.optString("styleName", "Normal"),
            align = o.optString("align", "left"),
            indentLeft = o.optFloat("indentLeft"),
            indentFirst = o.optFloat("indentFirst"),
            spaceBefore = o.optFloat("spaceBefore"),
            spaceAfter = o.optFloat("spaceAfter"),
            lineSpacing = o.optJSONObject("lineSpacing")?.let { LineSpacingVm(it.optString("kind"), it.optFloat("value")) },
            pageBreak = o.optBoolean("pageBreak"),
            rtl = o.optBoolean("rtl"),
            outline = if (o.has("outline") && !o.isNull("outline")) o.getInt("outline") else null,
            shading = o.optStringOrNull("shading"),
            list = listO?.let { ListVm(it.optString("label", "•"), it.optInt("level"), it.optFloat("hanging"), it.optFloat("indent")) },
            imgs = imgs,
        )
    }

    private fun path(o: JSONObject): List<Int> {
        val arr = o.optJSONArray("path") ?: return emptyList()
        return (0 until arr.length()).map { arr.getInt(it) }
    }

    // Tiny helpers (org.json's opt* returns "" / 0 / 0.0 rather than null).
    private fun JSONObject.optStringOrNull(key: String): String? =
        if (isNull(key) || !has(key)) null else optString(key).takeIf { it.isNotEmpty() }

    private fun JSONObject.optFloatOrNull(key: String): Float? =
        if (isNull(key) || !has(key)) null else getDouble(key).toFloat()

    private fun JSONObject.optColorOrNull(key: String): String? =
        if (isNull(key) || !has(key)) null else optString(key).takeIf { it.isNotEmpty() && it != "auto" }

    private fun JSONObject.optFloat(key: String): Float =
        if (isNull(key) || !has(key)) 0f else getDouble(key).toFloat()
}
