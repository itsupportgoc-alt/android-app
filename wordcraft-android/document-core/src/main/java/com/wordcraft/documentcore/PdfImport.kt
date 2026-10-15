package com.wordcraft.documentcore

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.InflaterInputStream

/** Thrown when a PDF cannot be imported as editable text (encrypted, image-only, …). */
class PdfImportException(message: String) : Exception(message)

/**
 * Minimal PDF text extraction — enough to open common PDFs as an editable WordCraft document.
 *
 * Pure Kotlin (JVM-testable, no Android dependencies). Supports:
 *  - classic top-level objects and compressed object streams (ObjStm, PDF 1.5+)
 *  - FlateDecode / uncompressed content streams
 *  - literal `(...)` and hex `<...>` strings with escapes
 *  - per-font ToUnicode CMaps (bfchar / bfrange) with 1–2 byte codes
 *  - Latin-1 / WinAnsi fallback when no CMap exists
 *  - line/paragraph assembly from the text matrix (Tm, Td, T-star, Tj, TJ)
 *
 * Not supported: encrypted PDFs, scanned/image-only PDFs (no OCR), inline images,
 * Type3/XObject text, vertical writing modes. Extracted text is imported into a fresh
 * document as paragraphs; formatting fidelity is not attempted.
 */
object PdfImport {

    fun extractText(pdf: ByteArray): String {
        val src = pdf.toString(Charsets.ISO_8859_1)
        if (Regex("/Encrypt\\b").containsMatchIn(src)) {
            throw PdfImportException("This PDF is password-protected; remove the password and try again")
        }

        val objects = parseObjects(pdf)
        if (objects.isEmpty()) throw PdfImportException("Not a readable PDF file")

        val rootRef = Regex("/Root\\s+(\\d+)\\s+0\\s+R").find(src)?.groupValues?.get(1)?.toIntOrNull()
        val pages = collectPages(objects, rootRef)
        if (pages.isEmpty()) throw PdfImportException("No document pages found in this PDF")

        val out = StringBuilder()
        for (page in pages) {
            val text = pageText(page, objects)
            if (text.isNotBlank()) {
                if (out.isNotEmpty()) out.append('\n')
                out.append(text)
            }
        }
        val result = out.toString().trim()
        if (result.isEmpty()) {
            throw PdfImportException("This PDF has no selectable text (scanned/image PDFs need OCR)")
        }
        return result
    }

    // ------------------------------------------------------------------ objects

    /** Parse `N G obj … endobj` (byte offsets are 1:1 in ISO-8859-1) and expand ObjStm. */
    private fun parseObjects(pdf: ByteArray): Map<Int, ByteArray> {
        val s = pdf.toString(Charsets.ISO_8859_1)
        val header = Regex("(\\d+)\\s+\\d+\\s+obj\\b").findAll(s).toList()
        val out = HashMap<Int, ByteArray>(header.size)
        header.forEachIndexed { i, m ->
            val num = m.groupValues[1].toInt()
            val start = m.range.last + 1
            val end = if (i + 1 < header.size) header[i + 1].range.first else s.length
            if (start <= end) out[num] = pdf.copyOfRange(start, end)
        }
        // Expand compressed object streams (page/font dicts live inside them in PDF 1.5+).
        val stms = out.filterValues { bytes ->
            bytes.toString(Charsets.ISO_8859_1).take(4096).contains("/ObjStm")
        }
        for ((_, bytes) in stms) {
            expandObjStm(bytes)?.forEach { (num, body) -> out.putIfAbsent(num, body) }
        }
        return out
    }

    private fun expandObjStm(bytes: ByteArray): Map<Int, ByteArray>? {
        val dict = bytes.toString(Charsets.ISO_8859_1)
        val first = Regex("/First\\s+(\\d+)").find(dict)?.groupValues?.get(1)?.toIntOrNull() ?: return null
        val count = Regex("/Count\\s+(\\d+)").find(dict)?.groupValues?.get(1)?.toIntOrNull() ?: return null
        val stream = extractStream(bytes) ?: return null
        val inflated = inflateIfDeflated(stream, dict) ?: return null
        val head = String(inflated, 0, minOf(first, inflated.size), Charsets.ISO_8859_1)
        val nums = head.trim().split(Regex("\\s+")).mapNotNull { it.toIntOrNull() }
        val out = HashMap<Int, ByteArray>()
        var i = 0
        while (i + 1 < nums.size && out.size < count) {
            val objNum = nums[i]
            val off = nums[i + 1]
            val begin = first + off
            val end = if (i + 3 < nums.size) first + nums[i + 3] else inflated.size
            if (begin in 0..end && end <= inflated.size) {
                out[objNum] = inflated.copyOfRange(begin, end)
            }
            i += 2
        }
        return out
    }

    private fun extractStream(objBytes: ByteArray): ByteArray? {
        val s = objBytes.toString(Charsets.ISO_8859_1)
        val m = Regex("stream(\\r\\n|\\n|\\r)").find(s) ?: return null
        val start = m.range.last + 1
        val end = s.lastIndexOf("endstream")
        if (end < start) return null
        return objBytes.copyOfRange(start, end)
    }

    private fun inflateIfDeflated(data: ByteArray, dict: String): ByteArray? {
        if (!dict.contains("/FlateDecode")) return data
        return try {
            InflaterInputStream(ByteArrayInputStream(data)).use { it.readBytes() }
        } catch (_: Exception) {
            null
        }
    }

    private fun dictOf(bytes: ByteArray): String {
        val s = bytes.toString(Charsets.ISO_8859_1)
        val end = s.indexOf("stream").let { if (it < 0) s.length else it }
        return s.substring(0, end)
    }

    // ------------------------------------------------------------------ pages

    private fun collectPages(objects: Map<Int, ByteArray>, rootRef: Int?): List<ByteArray> {
        val pages = ArrayList<ByteArray>()
        val visited = HashSet<Int>()

        fun walk(num: Int) {
            if (!visited.add(num)) return
            val bytes = objects[num] ?: return
            val dict = dictOf(bytes)
            when {
                Regex("/Type\\s*/Page(?!s)").containsMatchIn(dict) -> pages.add(bytes)
                dict.contains("/Kids") -> {
                    val kids = Regex("(\\d+)\\s+0\\s+R").findAll(
                        Regex("/Kids\\s*\\[(.*?)\\]", RegexOption.DOT_MATCHES_ALL).find(dict)?.groupValues?.get(1) ?: ""
                    ).map { it.groupValues[1].toInt() }
                    kids.forEach { walk(it) }
                }
            }
        }

        if (rootRef != null) {
            val catalog = objects[rootRef]?.toString(Charsets.ISO_8859_1) ?: ""
            Regex("/Pages\\s+(\\d+)\\s+0\\s+R").find(catalog)?.groupValues?.get(1)?.toIntOrNull()?.let { walk(it) }
        }
        if (pages.isEmpty()) {
            // Fallback: any page object, in file order.
            objects.entries
                .sortedBy { it.key }
                .filter { Regex("/Type\\s*/Page(?!s)").containsMatchIn(dictOf(it.value)) }
                .forEach { pages.add(it.value) }
        }
        return pages
    }

    // ------------------------------------------------------------------ page text

    private fun pageText(page: ByteArray, objects: Map<Int, ByteArray>): String {
        val dict = dictOf(page)

        // Contents (ref or array of refs).
        val contents = ArrayList<ByteArray>()
        val single = Regex("/Contents\\s+(\\d+)\\s+0\\s+R").find(dict)?.groupValues?.get(1)?.toIntOrNull()
        if (single != null) {
            objects[single]?.let { contents.add(it) }
        } else {
            val arr = Regex("/Contents\\s*\\[(.*?)\\]", RegexOption.DOT_MATCHES_ALL).find(dict)
            if (arr != null) {
                Regex("(\\d+)\\s+0\\s+R").findAll(arr.groupValues[1]).forEach { m ->
                    objects[m.groupValues[1].toInt()]?.let { contents.add(it) }
                }
            }
        }

        // Fonts: page resources (inline or by reference) → font name → font dict.
        val resources = Regex("/Resources\\s+(\\d+)\\s+0\\s+R").find(dict)?.groupValues?.get(1)?.toIntOrNull()
            ?.let { objects[it]?.toString(Charsets.ISO_8859_1) ?: "" } ?: dict
        val fonts = HashMap<String, Map<String, String>>() // font name → ToUnicode code map
        Regex("/Font\\s*<<(.*?)>>", RegexOption.DOT_MATCHES_ALL).find(resources)?.groupValues?.get(1)
            ?.let { block ->
                Regex("/(\\S+)\\s+(\\d+)\\s+0\\s+R").findAll(block).forEach { m ->
                    val name = m.groupValues[1]
                    val fontDict = objects[m.groupValues[2].toInt()]?.toString(Charsets.ISO_8859_1) ?: return@forEach
                    val tuRef = Regex("/ToUnicode\\s+(\\d+)\\s+0\\s+R").find(fontDict)?.groupValues?.get(1)?.toIntOrNull()
                    val cmapBytes = tuRef?.let { r -> objects[r] } ?: return@forEach
                    val cmapStream = extractStream(cmapBytes)?.let { inflateIfDeflated(it, cmapBytes.toString(Charsets.ISO_8859_1)) } ?: return@forEach
                    val cmap = parseCMap(cmapStream.toString(Charsets.ISO_8859_1))
                    if (cmap.isNotEmpty()) fonts[name] = cmap
                }
            }

        val extractor = ContentExtractor(fonts)
        for (c in contents) {
            val d = c.toString(Charsets.ISO_8859_1)
            val stream = extractStream(c) ?: continue
            val bytes = inflateIfDeflated(stream, d) ?: continue
            extractor.run(bytes.toString(Charsets.ISO_8859_1))
        }
        return extractor.text()
    }

    // ------------------------------------------------------------------ ToUnicode CMap

    private fun parseCMap(s: String): Map<String, String> {
        val out = HashMap<String, String>()
        Regex("beginbfchar(.*?)endbfchar", setOf(RegexOption.DOT_MATCHES_ALL)).findAll(s).forEach { block ->
            Regex("<([0-9A-Fa-f]+)>\\s*<([0-9A-Fa-f]+)>").findAll(block.groupValues[1]).forEach { m ->
                out[m.groupValues[1].uppercase()] = hexToText(m.groupValues[2])
            }
        }
        Regex("beginbfrange(.*?)endbfrange", setOf(RegexOption.DOT_MATCHES_ALL)).findAll(s).forEach { block ->
            val body = block.groupValues[1]
            Regex("<([0-9A-Fa-f]+)>\\s*<([0-9A-Fa-f]+)>\\s*<([0-9A-Fa-f]+)>").findAll(body).forEach { m ->
                val lo = m.groupValues[1].toInt(16)
                val hi = m.groupValues[2].toInt(16)
                val dst = m.groupValues[3]
                if (hi - lo <= 4096) {
                    val base = hexToText(dst)
                    val lastUnit = base.lastOrNull() ?: '\u0000'
                    val prefix = base.dropLast(1)
                    for (code in lo..hi) {
                        val delta = code - lo
                        out[code.toString(16).uppercase().padStart(m.groupValues[1].length, '0')] =
                            prefix + (lastUnit.code + delta).toChar()
                    }
                }
            }
            Regex("<([0-9A-Fa-f]+)>\\s*<([0-9A-Fa-f]+)>\\s*\\[(.*?)\\]", setOf(RegexOption.DOT_MATCHES_ALL))
                .findAll(body).forEach { m ->
                    val lo = m.groupValues[1].toInt(16)
                    val hi = m.groupValues[2].toInt(16)
                    val targets = Regex("<([0-9A-Fa-f]+)>").findAll(m.groupValues[3]).map { it.groupValues[1] }.toList()
                    for ((i, t) in targets.withIndex()) {
                        if (lo + i > hi) break
                        out[(lo + i).toString(16).uppercase().padStart(m.groupValues[1].length, '0')] = hexToText(t)
                    }
                }
        }
        return out
    }

    /** For simple bfrange runs: increment the last UTF-16 unit (good enough for ASCII ranges). */

    private fun hexToText(hex: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i + 3 < hex.length + 1) {
            val chunk = hex.substring(i, minOf(i + 4, hex.length))
            if (chunk.length < 4) break
            sb.append(chunk.toInt(16).toChar())
            i += 4
        }
        return sb.toString()
    }

    // ------------------------------------------------------------------ content stream

    /** Stateful text-op interpreter: Tj/TJ/Td/Tm/T* → lines with gap-based paragraphs. */
    private class ContentExtractor(private val fonts: Map<String, Map<String, String>>) {
        private val paragraphs = StringBuilder()
        private var currentFont: Map<String, String>? = null
        private var fontSize = 12f
        private var leading = 12f
        private var y: Float? = null
        private var lastShowY: Float? = null
        private var lineBuf = StringBuilder()
        /** True when the next joined line starts a new paragraph (a gap break just happened). */
        private var newPara = false

        fun text(): String {
            flushLine()
            return paragraphs.toString()
        }

        fun run(content: String) {
            var i = 0
            val n = content.length
            val operands = ArrayList<Any>() // String(bytes), Float, String(name)

            while (i < n) {
                val c = content[i]
                when {
                    c == '%' -> { while (i < n && content[i] != '\n') i++ }
                    c == '(' -> {
                        val (bytes, next) = readLiteral(content, i)
                        operands.add(bytes); i = next
                    }
                    c == '<' && i + 1 < n && content[i + 1] == '<' -> { // dict — skip
                        val end = content.indexOf(">>", i)
                        i = if (end < 0) n else end + 2
                    }
                    c == '<' -> {
                        val end = content.indexOf('>', i)
                        val hex = content.substring(i + 1, if (end < 0) n else end).filter { it.isLetterOrDigit() }
                        operands.add(hexToBytes(hex)); i = if (end < 0) n else end + 1
                    }
                    c == '[' -> { i = readArray(content, i, operands) }
                    c == '/' -> {
                        var j = i + 1
                        while (j < n && !content[j].isWhitespace() && content[j] !in "()[]<>/") j++
                        operands.add(content.substring(i + 1, j)); i = j
                    }
                    c.isDigit() || c == '-' || c == '+' || c == '.' -> {
                        var j = i + 1
                        while (j < n && (content[j].isDigit() || content[j] == '-' || content[j] == '+' || content[j] == '.' || content[j] == 'e' || content[j] == 'E')) j++
                        operands.add(content.substring(i, j).toFloatOrNull() ?: 0f); i = j
                    }
                    c.isWhitespace() -> i++
                    c == 'B' && content.startsWith("BI", i) -> { // inline image — skip to EI
                        val end = content.indexOf("EI", i)
                        i = if (end < 0) n else end + 2
                    }
                    else -> {
                        var j = i
                        while (j < n && content[j].isLetter()) j++
                        val op = content.substring(i, j)
                        if (j == i) i++ else { handleOp(op, operands); operands.clear(); i = j }
                    }
                }
            }
        }

        private fun handleOp(op: String, operands: List<Any>) {
            when (op) {
                "BT" -> { /* keep lastShowY so cross-BT gaps still segment paragraphs */ }
                "Tf" -> {
                    val name = operands.getOrNull(operands.size - 2) as? String
                    currentFont = name?.let { fonts[it] } ?: currentFont
                    fontSize = operands.getOrNull(operands.size - 1) as? Float ?: fontSize
                    leading = fontSize
                }
                "TL" -> leading = operands.getOrNull(operands.size - 1) as? Float ?: leading
                "Td", "TD" -> {
                    val ty = operands.getOrNull(operands.size - 1) as? Float ?: 0f
                    if (op == "TD") leading = -ty
                    val before = y
                    val newY = (before ?: 0f) + ty
                    y = newY
                    // Downward move: -ty is the vertical gap after the line we buffered.
                    val last = lastShowY
                    val gap = if (before == null && last != null) last - newY else -ty
                    if (ty != 0f || gap != 0f) breakLine(gap)
                }
                "Tm" -> {
                    val a = operands.getOrNull(operands.size - 5) as? Float ?: 1f
                    val b = operands.getOrNull(operands.size - 4) as? Float ?: 0f
                    val d = operands.getOrNull(operands.size - 2) as? Float ?: 1f
                    val f = operands.getOrNull(operands.size - 1) as? Float ?: 0f
                    if (b == 0f && d == 0f && a != 0f) {
                        val before = y
                        if (before != null && before != f) breakLine(before - f)
                        y = f
                    }
                }
                "T*" -> { y = (y ?: 0f) - leading; breakLine(leading) }
                "Tj", "'" -> {
                    if (op == "'") { y = (y ?: 0f) - leading; breakLine(leading) }
                    val s = operands.getOrNull(operands.size - 1) as? ByteArray ?: return
                    show(s)
                }
                "\"" -> {
                    y = (y ?: 0f) - leading; breakLine(leading)
                    val s = operands.getOrNull(operands.size - 1) as? ByteArray ?: return
                    show(s)
                }
                "TJ" -> {
                    val arr = operands.getOrNull(operands.size - 1) as? List<*> ?: return
                    for (part in arr) when (part) {
                        is ByteArray -> show(part)
                        // Positive TJ displacement ≈ word gap (text space, ×1000/1000 em).
                        is Float -> if (part >= fontSize * 0.3f) lineBuf.append(' ')
                    }
                }
                "ET" -> { flushLine() }
            }
        }

        /** Append a shown string to the current line (line breaks are driven by Td/Tm/T*). */
        private fun show(bytes: ByteArray) {
            val text = decode(bytes, currentFont)
            if (text.isEmpty()) return
            lineBuf.append(text)
            lastShowY = y ?: lastShowY
        }

        /**
         * Close the buffered line. [gap] = vertical distance to the *next* line:
         * a large gap ends the paragraph (newPara), a small one joins with a space
         * (hyphen-aware), a non-positive one is a plain flush.
         */
        private fun breakLine(gap: Float = 0f) {
            if (lineBuf.isEmpty()) return
            val line = lineBuf.toString().trim()
            lineBuf = StringBuilder()
            if (line.isEmpty()) return
            join(line)
            if (gap > fontSize * 1.5f) newPara = true
        }

        /** Append [line] to the paragraph buffer honouring paragraph/join/hyphen state. */
        private fun join(line: String) {
            when {
                paragraphs.isEmpty() -> paragraphs.append(line)
                newPara -> { paragraphs.append('\n'); paragraphs.append(line); newPara = false }
                paragraphs.last() == '-' -> { paragraphs.setLength(paragraphs.length - 1); paragraphs.append(line) }
                else -> { paragraphs.append(' '); paragraphs.append(line) }
            }
        }

        private fun flushLine() {
            if (lineBuf.isNotEmpty()) breakLine(0f)
        }

        private fun decode(bytes: ByteArray, cmap: Map<String, String>?): String {
            val sb = StringBuilder()
            var i = 0
            while (i < bytes.size) {
                var decoded: String? = null
                if (cmap != null && i + 1 < bytes.size) {
                    val two = ((bytes[i].toInt() and 0xFF) shl 8 or (bytes[i + 1].toInt() and 0xFF))
                        .toString(16).uppercase().padStart(4, '0')
                    decoded = cmap[two]
                    if (decoded != null) { i += 2; sb.append(decoded); continue }
                }
                val b = bytes[i].toInt() and 0xFF
                if (cmap != null) {
                    decoded = cmap[b.toString(16).uppercase().padStart(2, '0')]
                }
                sb.append(decoded ?: cp1252Char(b))
                i++
            }
            return sb.toString()
        }

        private fun cp1252Char(b: Int): Char = when {
            b < 0x80 -> b.toChar()
            b < 0xA0 -> CP1252[b - 0x80] ?: b.toChar()
            else -> b.toChar()
        }
    }

    // ------------------------------------------------------------------ helpers

    private val CP1252 = intArrayOf(
        0x20AC, 0x201A, 0x0192, 0x201E, 0x2026, 0x2020, 0x2021, 0x02C6,
        0x2030, 0x0160, 0x2039, 0x0152, 0x017D, 0x0000, 0x018D, 0x0000,
        0x0000, 0x2018, 0x2019, 0x201C, 0x201D, 0x2022, 0x2013, 0x2014,
        0x02DC, 0x2122, 0x0161, 0x203A, 0x0153, 0x0000, 0x017E, 0x0178,
    ).map { if (it == 0) '�'.code.toChar() else it.toChar() }.toCharArray()

    private fun hexToBytes(hex: String): ByteArray {
        val clean = hex.filter { it.isLetterOrDigit() }
        val out = ByteArray(clean.length / 2)
        var i = 0
        while (i + 1 < clean.length) {
            out[i / 2] = clean.substring(i, i + 2).toInt(16).toByte()
            i += 2
        }
        return out
    }

    /** Read a literal string starting at '('; returns bytes and index after ')'. */
    private fun readLiteral(s: String, start: Int): Pair<ByteArray, Int> {
        val out = ArrayList<Byte>()
        var i = start + 1
        var depth = 1
        while (i < s.length) {
            val c = s[i]
            when (c) {
                '\\' -> {
                    val nx = s.getOrNull(i + 1)
                    when (nx) {
                        'n' -> { out.add('\n'.code.toByte()); i += 2 }
                        'r' -> { out.add('\r'.code.toByte()); i += 2 }
                        't' -> { out.add('\t'.code.toByte()); i += 2 }
                        'b' -> { out.add(8); i += 2 }
                        'f' -> { out.add(12); i += 2 }
                        '(' , ')', '\\' -> { out.add(nx.code.toByte()); i += 2 }
                        '\n' -> { i += 2 }
                        in '0'..'7' -> {
                            val end = (i + 1..minOf(i + 3, s.length - 1)).takeWhile { s[it] in '0'..'7' }.last() + 1
                            out.add(s.substring(i + 1, end).toInt(8).toByte())
                            i = end
                        }
                        else -> i += 2
                    }
                }
                '(' -> { depth++; out.add('('.code.toByte()); i++ }
                ')' -> {
                    depth--
                    if (depth == 0) return out.toByteArray() to (i + 1)
                    out.add(')'.code.toByte()); i++
                }
                else -> { out.add(c.code.toByte()); i++ }
            }
        }
        return out.toByteArray() to s.length
    }

    /** Read a `[ … ]` array; pushes a List<Any> onto operands. Returns index after ']'. */
    private fun readArray(s: String, start: Int, operands: MutableList<Any>): Int {
        var i = start + 1
        val parts = ArrayList<Any>()
        while (i < s.length) {
            val c = s[i]
            when {
                c == ']' -> { operands.add(parts); return i + 1 }
                c == '(' -> { val (b, next) = readLiteral(s, i); parts.add(b); i = next }
                c == '<' -> {
                    val end = s.indexOf('>', i)
                    parts.add(hexToBytes(s.substring(i + 1, end.coerceAtLeast(i + 1))))
                    i = if (end < 0) s.length else end + 1
                }
                c.isDigit() || c == '-' || c == '+' || c == '.' -> {
                    var j = i + 1
                    while (j < s.length && (s[j].isDigit() || s[j] == '-' || s[j] == '+' || s[j] == '.')) j++
                    parts.add(s.substring(i, j).toFloatOrNull() ?: 0f); i = j
                }
                c.isWhitespace() -> i++
                else -> i++
            }
        }
        operands.add(parts)
        return s.length
    }
}
