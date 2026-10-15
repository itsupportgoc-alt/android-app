package com.wordcraft.documentcore

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TextDiffTest {

    @Test
    fun identicalIsNull() {
        assertNull(diffText("abc", "abc"))
    }

    @Test
    fun simpleInsert() {
        assertEquals(Edit.Insert(3, "X"), diffText("abc", "abcX"))
        assertEquals(Edit.Insert(0, "X"), diffText("", "X"))
    }

    @Test
    fun simpleDelete() {
        assertEquals(Edit.Delete(1, 2), diffText("abc", "ac"))
        assertEquals(Edit.Delete(0, 3), diffText("abc", ""))
    }

    @Test
    fun replaceMiddle() {
        assertEquals(Edit.Replace(1, 4, "XY"), diffText("aOLDz", "aXYz"))
    }

    @Test
    fun newlineInsertion() {
        assertEquals(Edit.Insert(5, "\nNext"), diffText("Hello", "Hello\nNext"))
    }

    @Test
    fun deleteParagraphBreak() {
        // Two paragraphs joined by '\n' become one.
        assertEquals(Edit.Delete(5, 6), diffText("Hello\nWorld", "HelloWorld"))
    }

    @Test
    fun surrogatePairNotSplit() {
        // Change right after an emoji must not split the pair in "same" prefix.
        val old = "\uD83D\uDE00abc" // 😀abc
        val new = "\uD83D\uDE00xyz"
        val e = diffText(old, new)!!
        assertTrue(e is Edit.Replace)
        (e as Edit.Replace).let {
            assertEquals(2, it.from) // after the full emoji (2 UTF-16 units)
            assertEquals("xyz", it.text)
        }
    }

    @Test
    fun emojiReplacement() {
        val e = diffText("a😀b", "a🙂b")!!
        assertTrue(e is Edit.Replace)
        // Both emoji are 2 UTF-16 units; the change sits between them.
        assertEquals(1, (e as Edit.Replace).from)
        assertEquals(3, e.to)
    }
}

class SectionMapperTest {

    private val mapper = SectionMapper(listOf("Hello", "World", ""))

    @Test
    fun joinedBody() {
        assertEquals("Hello\nWorld\n", mapper.body)
    }

    @Test
    fun localOfAcrossParas() {
        assertEquals(SectionMapper.Local(0, 5), mapper.localOf(5)) // at the '\n'
        assertEquals(SectionMapper.Local(1, 0), mapper.localOf(6))
        assertEquals(SectionMapper.Local(1, 2), mapper.localOf(8))
        assertEquals(SectionMapper.Local(2, 0), mapper.localOf(12))
    }

    @Test
    fun offsetOfRoundTrip() {
        for (off in 0..mapper.body.length) {
            val l = mapper.localOf(off)
            // Offset inside a paragraph maps back exactly; offsets on '\n' clamp to para end.
            if (l.off < mapper.body.length && mapper.body.getOrNull(off) != '\n') {
                assertEquals(off, mapper.offsetOf(l.para, l.off))
            }
        }
    }

    @Test
    fun clampsOutOfRange() {
        assertEquals(SectionMapper.Local(2, 0), mapper.localOf(999))
        assertEquals(0, mapper.localOf(-5).para)
    }
}

class UtfConversionTest {

    @Test
    fun asciiOffsets() {
        val t = "hello"
        assertEquals(3, utf16ToUtf8Bytes(t, 3))
        assertEquals(3, utf8ToUtf16(t, 3))
        assertEquals(5, utf8ToUtf16(t, 99))
    }

    @Test
    fun multiByteChars() {
        val t = "héllo wörld" // é=2B, ö=2B
        // "héllo " = 1+2+1+1+1+1 = 7 bytes
        assertEquals(7, utf16ToUtf8Bytes(t, 6))
        assertEquals(6, utf8ToUtf16(t, 7))
        // CJK: 世界 = 6 bytes, 2 UTF-16 units each
        assertEquals(8, utf16ToUtf8Bytes("ab世界cd", 4)) // 2 ascii + 3 + 3
        assertEquals(4, utf8ToUtf16("ab世界cd", 8))
    }

    @Test
    fun supplementaryChars() {
        val t = "a😀b" // 😀 = 4 bytes, 2 UTF-16 units
        assertEquals(1, utf16ToUtf8Bytes(t, 1))
        assertEquals(5, utf16ToUtf8Bytes(t, 3)) // a + emoji
        assertEquals(3, utf8ToUtf16(t, 5))
        // Offset inside the emoji clamps to its start.
        assertEquals(1, utf8ToUtf16(t, 3))
    }

    @Test
    fun surrogateBoundaryClamp() {
        val t = "a😀b"
        assertEquals(1, clampUtf16Boundary(t, 2)) // between the pair → back to 1
        assertEquals(3, clampUtf16Boundary(t, 3))
    }
}

class PlanOpsTest {

    private val mapper = SectionMapper(listOf("Hello", "World"))
    private val paths: (Int) -> List<Int> = { i -> listOf(i) }
    private val sel0 = SelectionVm.collapsed(PosVm.body(0, 0))

    @Test
    fun insertSingleRun() {
        val ops = planOps(Edit.Insert(5, " there"), mapper, paths, sel0)
        // Selection moves to (0,5) then inserts.
        assertEquals("select.range", ops[0].id)
        assertTrue(ops[0].params.contains("\"off\":5"))
        assertEquals("text.insert", ops[1].id)
        assertTrue(ops[1].params.contains("there"))
    }

    @Test
    fun insertSkipsRedundantSelection() {
        val at = SelectionVm.collapsed(PosVm.body(0, 5))
        val ops = planOps(Edit.Insert(5, "X"), mapper, paths, at)
        assertEquals(listOf("text.insert"), ops.map { it.id })
    }

    @Test
    fun insertWithNewlineSplits() {
        val ops = planOps(Edit.Insert(5, "\nNext"), mapper, paths, sel0)
        assertEquals(listOf("select.range", "text.newParagraph", "text.insert"), ops.map { it.id })
        assertTrue(ops[2].params.contains("Next"))
    }

    @Test
    fun deleteBecomesBackspaceWithSelection() {
        val ops = planOps(Edit.Delete(1, 3), mapper, paths, sel0)
        assertEquals(listOf("select.range", "text.backspace"), ops.map { it.id })
        assertTrue(ops[0].params.contains("\"off\":1"))
        assertTrue(ops[0].params.contains("\"off\":3"))
    }

    @Test
    fun crossParagraphPositions() {
        // Delete from body offset 4 (para 0) to 8 (para 1, local 1).
        val ops = planOps(Edit.Delete(4, 8), mapper, paths, sel0)
        val sel = ops[0].params
        assertTrue(sel.contains("\"path\":[0]"))
        assertTrue(sel.contains("\"path\":[1]"))
        // Body offset 8 = para 1, local 2 → 2 UTF-8 bytes ("Wo" is ASCII).
        assertTrue(sel.contains("\"off\":2"))
    }

    @Test
    fun replaceReplacesSelection() {
        val ops = planOps(Edit.Replace(1, 4, "XY"), mapper, paths, sel0)
        assertEquals(listOf("select.range", "text.insert"), ops.map { it.id })
    }

    @Test
    fun jsonEscapes() {
        assertEquals("\"a\\\"b\\\\c\\nd\"", jsonStr("a\"b\\c\nd"))
        assertEquals("\"\\u0001\"", jsonStr("\u0001"))
    }
}
