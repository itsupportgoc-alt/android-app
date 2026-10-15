package com.wordcraft.documentcore

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.DeflaterOutputStream

/**
 * PdfImport tests with hand-crafted PDFs: literal strings and gap-based paragraphs,
 * FlateDecode + ToUnicode CMap, encrypted rejection.
 */
class PdfImportTest {

    /** Build a minimal valid PDF around one content stream (+ optional ToUnicode font). */
    private fun buildPdf(
        content: String,
        fontExtra: String = "",
        cmap: String? = null,
        deflate: Boolean = false,
    ): ByteArray {
        val contentBytes = if (deflate) {
            ByteArrayOutputStream().also { bos ->
                DeflaterOutputStream(bos).use { it.write(content.toByteArray(Charsets.ISO_8859_1)) }
            }.toByteArray()
        } else content.toByteArray(Charsets.ISO_8859_1)
        val filter = if (deflate) " /Filter /FlateDecode" else ""
        val streamDictLen = contentBytes.size

        val objs = LinkedHashMap<Int, String>()
        objs[1] = "<< /Type /Catalog /Pages 2 0 R >>"
        objs[2] = "<< /Type /Pages /Kids [3 0 R] /Count 1 >>"
        objs[3] = "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] " +
            "/Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>"
        objs[4] = "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica $fontExtra >>"
        objs[6] = if (cmap != null) "__STREAM6__" else ""

        val sb = StringBuilder("%PDF-1.4\n")
        val offsets = HashMap<Int, Int>()
        fun put(num: Int, body: String) {
            offsets[num] = sb.length
            sb.append("$num 0 obj\n$body\nendobj\n")
        }
        put(1, objs[1]!!); put(2, objs[2]!!); put(3, objs[3]!!); put(4, objs[4]!!)
        // Font object may reference ToUnicode (6) — injected here so numbering is stable.
        if (cmap != null) {
            objs[4] = "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica $fontExtra /ToUnicode 6 0 R >>"
            sb.setLength(0)
            sb.append("%PDF-1.4\n")
            put(1, objs[1]!!); put(2, objs[2]!!); put(3, objs[3]!!); put(4, objs[4]!!)
        }
        offsets[5] = sb.length
        sb.append("5 0 obj\n<< /Length $streamDictLen$filter >>\nstream\n")
        sb.append(String(contentBytes, Charsets.ISO_8859_1))
        sb.append("\nendstream\nendobj\n")
        if (cmap != null) {
            val cmapBytes = cmap.toByteArray(Charsets.ISO_8859_1)
            offsets[6] = sb.length
            sb.append("6 0 obj\n<< /Length ${cmapBytes.size} >>\nstream\n")
            sb.append(cmap)
            sb.append("\nendstream\nendobj\n")
        }
        val xrefPos = sb.length
        val maxObj = if (cmap != null) 6 else 5
        sb.append("xref\n0 ${maxObj + 1}\n")
        sb.append("0000000000 65535 f \n")
        for (i in 1..maxObj) {
            sb.append(String.format("%010d 00000 n \n", offsets[i] ?: 0))
        }
        sb.append("trailer\n<< /Size ${maxObj + 1} /Root 1 0 R >>\nstartxref\n$xrefPos\n%%EOF")
        return sb.toString().toByteArray(Charsets.ISO_8859_1)
    }

    @Test
    fun `literal strings join lines into paragraphs by vertical gap`() {
        val content = """
            BT
            /F1 12 Tf
            72 720 Td
            (Hello PDF world) Tj
            0 -14 Td
            (Line two continues the same paragraph) Tj
            0 -40 Td
            (Second paragraph starts here) Tj
            ET
        """.trimIndent()
        val text = PdfImport.extractText(buildPdf(content))
        assertEquals(
            "Hello PDF world Line two continues the same paragraph\nSecond paragraph starts here",
            text,
        )
    }

    @Test
    fun `TJ arrays and hex strings are decoded`() {
        val content = "BT /F1 10 Tf 50 700 Td [(Hel) -120 (lo) <20> (there)] TJ 0 -30 Td <576F7264> Tj ET"
        val text = PdfImport.extractText(buildPdf(content))
        assertEquals("Hello there\nWord", text)
    }

    @Test
    fun `flate content plus ToUnicode cmap maps codes to unicode`() {
        val content = "BT /F1 12 Tf 72 700 Td <010203> Tj ET"
        val cmap = """
            /CIDInit /ProcSet findresource begin
            12 dict begin begincmap
            1 beginbfchar
            <01> <0058>
            endbfchar
            1 beginbfrange
            <02> <03> <0059>
            endbfrange
            endcmap
            end end
        """.trimIndent()
        val text = PdfImport.extractText(buildPdf(content, cmap = cmap, deflate = true))
        assertEquals("XYZ", text)
    }

    @Test
    fun `encrypted pdf is rejected with a clear message`() {
        val pdf = "%PDF-1.4\n1 0 obj << /Type /Catalog >> endobj\ntrailer << /Encrypt 9 0 R >>\n%%EOF"
        try {
            PdfImport.extractText(pdf.toByteArray())
            throw AssertionError("expected PdfImportException")
        } catch (e: PdfImportException) {
            assertTrue(e.message!!.contains("password"))
        }
    }

    @Test
    fun `image-only pdf reports no selectable text`() {
        val pdf = "%PDF-1.4\n1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n" +
            "2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj\n" +
            "3 0 obj << /Type /Page /Parent 2 0 R /Contents 5 0 R >> endobj\n" +
            "5 0 obj << /Length 0 >>\nstream\n\nendstream\nendobj\n" +
            "trailer << /Root 1 0 R >>\n%%EOF"
        try {
            PdfImport.extractText(pdf.toByteArray())
            throw AssertionError("expected PdfImportException")
        } catch (e: PdfImportException) {
            assertTrue(e.message!!.contains("text"))
        }
    }
}
