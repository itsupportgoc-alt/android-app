package com.wordcraft.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.util.Base64
import com.wordcraft.documentcore.EditorState
import com.wordcraft.documentcore.ImageVm
import com.wordcraft.documentcore.ParaVm
import com.wordcraft.documentcore.SpanVm
import com.wordcraft.documentcore.TableVm
import java.io.ByteArrayOutputStream

/**
 * Save as PDF: renders the current [EditorState] into an A4 PDF with `android.graphics.pdf`.
 * Pure Android — no engine or upstream-crate involvement (the upstream crates stay untouched).
 *
 * Fidelity: paragraphs with runs (bold/italic/underline/strike/colour/size/highlight),
 * alignment, headings, lists (bullet/number labels), page breaks, images and basic tables.
 * Justified text is written left-aligned; complex layout (tabs, columns) is flattened.
 */
object PdfExport {

    private const val PAGE_W = 595f   // A4 width in points (1/72")
    private const val PAGE_H = 842f   // A4 height in points
    private const val MARGIN = 56f    // ~2 cm
    private const val BODY_SIZE = 11f
    private const val LINE_FACTOR = 1.25f

    fun export(state: EditorState): ByteArray {
        val doc = PdfDocument()
        val writer = PageWriter(doc)
        for (b in state.blocks) {
            b.asPara?.let { writer.writePara(it) }
            b.asTable?.let { writer.writeTable(it) }
        }
        writer.close()
        return ByteArrayOutputStream().also { doc.writeTo(it) }.toByteArray()
    }

    // ------------------------------------------------------------------ page canvas

    private class PageWriter(private val doc: PdfDocument) {
        private var pageNumber = 1
        private var page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W.toInt(), PAGE_H.toInt(), 1).create())
        private var canvas = page.canvas
        private var y = MARGIN
        /** Set per paragraph so heading styles render bold even without explicit spans. */
        private var headingBold = false

        private val remaining: Float get() = PAGE_H - MARGIN - y

        fun newPage() {
            doc.finishPage(page)
            pageNumber++
            page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W.toInt(), PAGE_H.toInt(), pageNumber).create())
            canvas = page.canvas
            y = MARGIN
        }

        fun close() { doc.finishPage(page) }

        fun ensure(height: Float) { if (remaining < height) newPage() }

        // ------------------------------------------------------------ paragraphs

        fun writePara(p: ParaVm) {
            if (p.pageBreak && y > MARGIN) newPage()
            headingBold = p.style in setOf("Title", "Subtitle", "Heading1", "Heading2", "Heading3")

            val baseSize = styleSize(p.style)
            val lineH = lineHeight(p, baseSize)
            y += (p.spaceBefore * 0.75f)

            val prefix = listPrefix(p)
            val avail = PAGE_W - 2 * MARGIN - indentOf(p, prefix.isNotEmpty())

            val lines = wrapRuns(runsOf(p, baseSize), avail, baseSize)
            if (lines.isEmpty()) {
                ensure(lineH)
                y += lineH
            } else {
                for (line in lines) {
                    ensure(lineH)
                    drawLine(p, line, prefix, avail, baseSize, lineH)
                    y += lineH
                }
            }
            y += (p.spaceAfter * 0.75f)
            p.imgs.forEach { writeImage(it) }
            headingBold = false
        }

        private fun indentOf(p: ParaVm, hasPrefix: Boolean): Float =
            p.indentLeft * 0.75f + if (hasPrefix) 16f else 0f

        /** One wrapped visual line = runs (text, span) clipped to that line. */
        private fun wrapRuns(
            runs: List<Pair<String, SpanVm?>>,
            avail: Float,
            baseSize: Float,
        ): List<List<Pair<String, SpanVm?>>> {
            val lines = ArrayList<List<Pair<String, SpanVm?>>>()
            var line = ArrayList<Pair<String, SpanVm?>>()
            var lineWidth = 0f

            fun flush() { if (line.isNotEmpty()) { lines.add(line); line = ArrayList(); lineWidth = 0f } }

            for ((text, span) in runs) {
                val paint = paintFor(span, baseSize)
                var i = 0
                while (i < text.length) {
                    if (text[i] == ' ' && lineWidth == 0f) { i++; continue }
                    val end = if (text[i] == ' ') i + 1
                    else {
                        var e = i
                        while (e < text.length && text[e] != ' ') e++
                        e
                    }
                    val token = text.substring(i, end)
                    val w = paint.measureText(token)
                    if (lineWidth + w > avail && lineWidth > 0f) {
                        flush()
                        if (token.isBlank()) { i = end; continue }
                    }
                    if (paint.measureText(token.trim()) > avail && token.isNotBlank()) {
                        // Word longer than the line: hard-break it by characters.
                        var k = i
                        while (k < text.length && text[k] != ' ') {
                            var e = k + 1
                            while (e < text.length && text[e] != ' ' &&
                                paint.measureText(text.substring(k, e + 1)) <= avail
                            ) e++
                            if (e == k) e++
                            val piece = text.substring(k, e)
                            line.add(piece to span)
                            lineWidth = paint.measureText(piece)
                            flush()
                            k = e
                        }
                        i = k
                        continue
                    }
                    line.add(token to span)
                    lineWidth += w
                    i = end
                }
            }
            flush()
            return lines
        }

        private fun drawLine(
            p: ParaVm,
            runs: List<Pair<String, SpanVm?>>,
            prefix: String,
            avail: Float,
            baseSize: Float,
            lineH: Float,
        ) {
            var total = runs.sumOf { paintFor(it.second, baseSize).measureText(it.first).toDouble() }.toFloat()
            val prefixPaint = paintFor(null, baseSize)
            if (prefix.isNotEmpty()) total += prefixPaint.measureText(prefix)

            val left = MARGIN + indentOf(p, prefix.isNotEmpty())
            var x = when (p.align) {
                "center" -> left + ((avail - total) / 2f).coerceAtLeast(0f)
                "right" -> left + (avail - total).coerceAtLeast(0f)
                else -> left // justify → left
            }
            val baseline = y + lineH * 0.8f

            if (prefix.isNotEmpty()) {
                canvas.drawText(prefix, x, baseline, prefixPaint)
                x += prefixPaint.measureText(prefix)
            }
            for ((text, span) in runs) {
                val paint = paintFor(span, baseSize)
                val hl = span?.highlight?.let { highlightColor(it) }
                if (hl != null) {
                    val bg = Paint().apply { color = hl }
                    canvas.drawRect(RectF(x, baseline - baseSize, x + paint.measureText(text), baseline + baseSize * 0.3f), bg)
                }
                canvas.drawText(text, x, baseline, paint)
                val w = paint.measureText(text)
                if (span?.underline != null) {
                    canvas.drawLine(x, baseline + 2f, x + w, baseline + 2f, paint)
                }
                if (span?.strike == true) {
                    canvas.drawLine(x, baseline - baseSize * 0.35f, x + w, baseline - baseSize * 0.35f, paint)
                }
                x += w
            }
        }

        /** Paragraph text split into styled runs (spans applied by UTF-16 range). */
        private fun runsOf(p: ParaVm, baseSize: Float): List<Pair<String, SpanVm?>> {
            if (p.text.isEmpty()) return emptyList()
            if (p.spans.isEmpty()) return listOf(p.text to null)
            val out = ArrayList<Pair<String, SpanVm?>>()
            var at = 0
            for (span in p.spans.sortedBy { it.start }) {
                val s = span.start.coerceIn(at, p.text.length)
                val e = span.end.coerceIn(s, p.text.length)
                if (s > at) out.add(p.text.substring(at, s) to null)
                if (e > s) out.add(p.text.substring(s, e) to span)
                at = e
            }
            if (at < p.text.length) out.add(p.text.substring(at) to null)
            return out.filter { it.first.isNotEmpty() }
        }

        private fun listPrefix(p: ParaVm): String {
            val list = p.list ?: return ""
            val label = list.label.trim()
            return if (label.isNotEmpty()) "$label " else "• "
        }

        private fun lineHeight(p: ParaVm, baseSize: Float): Float {
            val ls = p.lineSpacing ?: return baseSize * LINE_FACTOR
            return when (ls.kind) {
                "multiple" -> baseSize * (if (ls.value > 0) ls.value else LINE_FACTOR)
                "exact", "atLeast" -> (ls.value * 0.75f).coerceAtLeast(baseSize * 1.1f)
                else -> baseSize * LINE_FACTOR
            }
        }

        private fun styleSize(style: String): Float = when (style) {
            "Title" -> 28f
            "Subtitle" -> 16f
            "Heading1" -> 20f
            "Heading2" -> 16f
            "Heading3" -> 14f
            else -> BODY_SIZE
        }

        private fun paintFor(span: SpanVm?, baseSize: Float): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = span?.sizePt ?: baseSize
            color = span?.color?.let { parseColor(it) } ?: Color.BLACK
            val bold = span?.bold == true || headingBold
            isFakeBoldText = bold
            typeface = Typeface.create(
                typefaceFromName(span?.font),
                if (span?.italic == true) Typeface.ITALIC else Typeface.NORMAL,
            )
        }

        private fun typefaceFromName(name: String?): Typeface = when (name?.lowercase()) {
            "times new roman", "georgia", "cambria" -> Typeface.SERIF
            "courier new" -> Typeface.MONOSPACE
            else -> Typeface.SANS_SERIF
        }

        private fun parseColor(hex: String): Int = try {
            val h = hex.removePrefix("#")
            if (h.length == 6) Color.parseColor("#$h") else Color.BLACK
        } catch (_: Exception) { Color.BLACK }

        private fun highlightColor(name: String): Int = try {
            Color.parseColor("#" + when (name.lowercase()) {
                "yellow" -> "FFFF00"; "brightgreen" -> "00FF00"; "turquoise" -> "00FFFF"
                "pink" -> "FF00FF"; "blue" -> "0000FF"; "red" -> "FF0000"
                "green" -> "008000"; "darkyellow" -> "808000"; "gray50" -> "808080"
                else -> "FFFF00"
            })
        } catch (_: Exception) { Color.YELLOW }

        // ------------------------------------------------------------ images & tables

        private fun writeImage(img: ImageVm) {
            if (img.b64.isNullOrBlank()) return
            val bmp: Bitmap = try {
                val bytes = Base64.decode(img.b64, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return
            } catch (_: Exception) { return }
            val wPt = if (img.w > 0) img.w * 0.75f else (PAGE_W - 2 * MARGIN)
            val hPt = if (img.h > 0) img.h * 0.75f else wPt * bmp.height / bmp.width.toFloat()
            ensure(hPt + 4f)
            canvas.drawBitmap(bmp, null, RectF(MARGIN, y, MARGIN + wPt, y + hPt), Paint())
            bmp.recycle()
            y += hPt + 4f
        }

        fun writeTable(t: TableVm) {
            val colW = t.grid.map { (if (it > 0) it else 60f) * 0.75f }
            val pad = 4f
            val border = Paint().apply { style = Paint.Style.STROKE; strokeWidth = 0.7f; color = Color.GRAY }
            val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 10f; color = Color.BLACK }

            for (row in t.rows) {
                val cellTexts = row.map { cell -> cell.blocks.joinToString(" ") { it.asPara?.text ?: "" } }
                var rowH = 18f
                cellTexts.forEachIndexed { ci, txt ->
                    val w = (colW.getOrElse(ci) { 60f }) - 2 * pad
                    rowH = maxOf(rowH, measureCell(txt, w, cellPaint))
                }
                ensure(rowH + 2f)
                var x = MARGIN
                for ((ci, txt) in cellTexts.withIndex()) {
                    val w = colW.getOrElse(ci) { 60f }
                    canvas.drawRect(x, y, x + w, y + rowH, border)
                    drawCellText(txt, x + pad, y, w - 2 * pad, rowH, cellPaint)
                    x += w
                }
                y += rowH
            }
            y += 6f
        }

        private fun measureCell(text: String, width: Float, paint: Paint): Float {
            var lines = 0
            for (para in text.split('\n')) {
                var w = 0f
                lines++
                for (ch in para) {
                    val cw = paint.measureText(ch.toString())
                    if (w + cw > width) { lines++; w = cw } else w += cw
                }
            }
            return (lines + 1) * 13f + 4f
        }

        private fun drawCellText(text: String, x: Float, top: Float, width: Float, height: Float, paint: Paint) {
            var yy = top + 13f
            for (para in text.split('\n')) {
                var line = ""
                for (word in para.split(' ')) {
                    val candidate = if (line.isEmpty()) word else "$line $word"
                    if (paint.measureText(candidate) > width && line.isNotEmpty()) {
                        canvas.drawText(line, x, yy, paint)
                        yy += 13f
                        line = word
                    } else line = candidate
                    if (yy > top + height) return
                }
                canvas.drawText(line, x, yy, paint)
                yy += 13f
            }
        }
    }
}
