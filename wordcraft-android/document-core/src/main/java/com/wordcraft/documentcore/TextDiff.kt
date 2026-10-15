package com.wordcraft.documentcore

/**
 * Pure editing logic: turning text-field changes into engine commands.
 *
 * The editor renders *sections* — consecutive paragraphs that share a style/alignment/list
 * signature — inside one text field whose text is the paragraphs joined with `'\n'`, prefixed
 * by a zero-width sentinel so a Backspace at the very start is observable (the engine's
 * `text.backspace` at offset 0 is what merges paragraphs / leaves lists, exactly as in Word).
 *
 * Everything here is pure and unit-tested on the JVM.
 */

const val SENTINEL = "\u200B"

/** A single contiguous change between two versions of a section's text (field coordinates). */
sealed class Edit {
    data class Insert(val at: Int, val text: String) : Edit()
    data class Delete(val from: Int, val to: Int) : Edit()
    data class Replace(val from: Int, val to: Int, val text: String) : Edit()
}

/** An engine command to dispatch (`id` = command registry id, `params` = JSON object). */
data class EngineOp(val id: String, val params: String) {
    override fun toString(): String = "$id$params"
}

/**
 * The one contiguous region that changed between [old] and [new]
 * (IME composition, typing and deletion all produce a single region), `null` if identical.
 */
fun diffText(old: String, new: String): Edit? {
    if (old == new) return null
    val minLen = minOf(old.length, new.length)
    var pre = 0
    while (pre < minLen && old[pre] == new[pre]) pre++
    // Don't split a surrogate pair between "same" and "changed".
    if (pre > 0 && pre < old.length && old[pre].isLowSurrogate() && old[pre - 1].isHighSurrogate()) pre--
    var suf = 0
    while (suf < minLen - pre && old[old.length - 1 - suf] == new[new.length - 1 - suf]) suf++
    if (suf > 0 && old.length - suf > pre && new.length - suf > pre) {
        val o = old.length - suf
        if (o < old.length && old[o].isLowSurrogate() && old[o - 1].isHighSurrogate()) suf--
    }
    val oldMid = old.substring(pre, old.length - suf)
    val newMid = new.substring(pre, new.length - suf)
    return when {
        oldMid.isEmpty() -> Edit.Insert(pre, newMid)
        newMid.isEmpty() -> Edit.Delete(pre, pre + oldMid.length)
        else -> Edit.Replace(pre, pre + oldMid.length, newMid)
    }
}

/**
 * Maps offsets between the joined section text and individual paragraphs.
 * [paraTexts] are the paragraphs' plain texts (UTF-16 coordinates); they join with `'\n'`.
 */
class SectionMapper(private val paraTexts: List<String>) {

    /** The field body (paragraphs joined with `'\n'`, no sentinel). */
    val body: String = paraTexts.joinToString("\n")

    /** Start offset of each paragraph inside [body]. */
    private val starts: IntArray = IntArray(paraTexts.size).also { arr ->
        var at = 0
        for (i in paraTexts.indices) {
            arr[i] = at
            at += paraTexts[i].length + 1 // + '\n'
        }
    }

    /** A position inside the section: paragraph index + local UTF-16 offset. */
    data class Local(val para: Int, val off: Int)

    /** Start offset of paragraph [para] inside [body]. */
    fun startOf(para: Int): Int = starts[para.coerceIn(0, starts.size - 1)]

    /** Body offset → paragraph + local offset (clamped into range). */
    fun localOf(bodyOff: Int): Local {
        val off = bodyOff.coerceIn(0, body.length)
        // Last paragraph whose start <= off.
        var lo = 0; var hi = starts.size - 1; var idx = 0
        while (lo <= hi) {
            val mid = (lo + hi) / 2
            if (starts[mid] <= off) { idx = mid; lo = mid + 1 } else hi = mid - 1
        }
        return Local(idx, off - starts[idx])
    }

    /** Paragraph + local offset → body offset. */
    fun offsetOf(para: Int, localOff: Int): Int {
        val i = para.coerceIn(0, starts.size - 1)
        return starts[i] + localOff.coerceIn(0, paraTexts[i].length)
    }

    /** Body offset → engine position (byte offset inside the paragraph). */
    fun posOf(bodyOff: Int, pathOf: (Int) -> List<Int>): PosVm {
        val l = localOf(bodyOff)
        val text = paraTexts[l.para]
        val byte = utf16ToUtf8Bytes(text, clampUtf16Boundary(text, l.off))
        return PosVm("body", pathOf(l.para), byte)
    }
}

/** Clamp a UTF-16 index off the middle of a surrogate pair (backwards). */
fun clampUtf16Boundary(text: String, idx: Int): Int {
    var i = idx.coerceIn(0, text.length)
    if (i in 1..<text.length && text[i].isLowSurrogate() && text[i - 1].isHighSurrogate()) i--
    return i
}

/** UTF-8 byte offset of the first [utf16Idx] UTF-16 units of [text]. */
fun utf16ToUtf8Bytes(text: String, utf16Idx: Int): Int {
    val i = clampUtf16Boundary(text, utf16Idx)
    return text.substring(0, i).toByteArray(Charsets.UTF_8).size
}

/** UTF-16 index of a UTF-8 byte offset (clamped to the containing code point). */
fun utf8ToUtf16(text: String, byteOff: Int): Int {
    if (byteOff <= 0) return 0
    var bytes = 0
    var i = 0
    while (i < text.length) {
        if (bytes >= byteOff) return i
        val cp = text.codePointAt(i)
        val units = Character.charCount(cp)
        var cpBytes = if (cp < 0x80) 1 else if (cp < 0x800) 2 else if (cp < 0x10000) 3 else 4
        if (bytes + cpBytes > byteOff) return i // offset lands inside this code point
        bytes += cpBytes
        i += units
    }
    return text.length
}

/**
 * Turn a field edit into engine commands.
 *
 * @param edit the change in **body** coordinates (sentinel already stripped)
 * @param mapper section offset mapping
 * @param pathOf paragraph index → engine block path
 * @param selection the engine selection as it is *now* (so a redundant `select.range` is skipped)
 */
fun planOps(
    edit: Edit,
    mapper: SectionMapper,
    pathOf: (Int) -> List<Int>,
    selection: SelectionVm,
): List<EngineOp> {
    val ops = mutableListOf<EngineOp>()
    val needSelect: (PosVm, PosVm) -> Unit = { a, b ->
        val same = selection.anchor == a && selection.focus == b
        if (!same) ops.add(selectRangeOp(a, b))
    }

    when (edit) {
        is Edit.Insert -> {
            needSelect(mapper.posOf(edit.at, pathOf), mapper.posOf(edit.at, pathOf))
            ops.addAll(insertOps(edit.text, mapper, pathOf))
        }
        is Edit.Delete -> {
            needSelect(mapper.posOf(edit.from, pathOf), mapper.posOf(edit.to, pathOf))
            ops.add(EngineOp("text.backspace", "{}"))
        }
        is Edit.Replace -> {
            needSelect(mapper.posOf(edit.from, pathOf), mapper.posOf(edit.to, pathOf))
            ops.addAll(insertOps(edit.text, mapper, pathOf))
        }
    }
    return ops
}

/** `text.insert` (plus `text.newParagraph` for each newline). The active selection is replaced. */
private fun insertOps(text: String, mapper: SectionMapper, pathOf: (Int) -> List<Int>): List<EngineOp> {
    if (text.isEmpty()) return emptyList()
    val parts = text.split('\n')
    val ops = mutableListOf<EngineOp>()
    if (parts[0].isNotEmpty()) ops.add(EngineOp("text.insert", """{"text":${jsonStr(parts[0])}}"""))
    for (i in 1 until parts.size) {
        ops.add(EngineOp("text.newParagraph", "{}"))
        if (parts[i].isNotEmpty()) ops.add(EngineOp("text.insert", """{"text":${jsonStr(parts[i])}}"""))
    }
    return ops
}

fun selectRangeOp(a: PosVm, b: PosVm): EngineOp =
    EngineOp("select.range", """{"anchor":${a.toJson()},"focus":${b.toJson()}}""")

fun caretOp(p: PosVm): EngineOp = EngineOp("caret.set", """{"pos":${p.toJson()}}""")

/** JSON string literal escaping (control chars, quotes, backslashes). */
fun jsonStr(s: String): String {
    val sb = StringBuilder(s.length + 2)
    sb.append('"')
    for (c in s) {
        when (c) {
            '"' -> sb.append("\\\"")
            '\\' -> sb.append("\\\\")
            '\n' -> sb.append("\\n")
            '\r' -> sb.append("\\r")
            '\t' -> sb.append("\\t")
            else -> if (c < ' ') sb.append("\\u").append(String.format("%04x", c.code)) else sb.append(c)
        }
    }
    sb.append('"')
    return sb.toString()
}
