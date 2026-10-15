package com.wordcraft.documentcore

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StateParserTest {

    private val sample = """
        {
          "canUndo": true,
          "canRedo": false,
          "dirty": true,
          "selectedText": "ell",
          "wordCount": 42,
          "selection": {
            "anchor": {"story":"body","path":[0],"off":1},
            "focus": {"story":"body","path":[0],"off":4}
          },
          "format": {
            "font": "Calibri", "size": 14.0, "bold": true, "italic": false,
            "underline": true, "strike": false, "color": "FF0000", "highlight": null,
            "style": "Normal", "styleName": "Normal", "subscript": false, "superscript": false
          },
          "para": {
            "style": "Heading1", "styleName": "Heading 1", "align": "center",
            "indentLeft": 0, "indentFirst": 0, "spaceBefore": 12, "spaceAfter": 6,
            "lineSpacing": {"kind": "multiple", "value": 1.15},
            "inList": false
          },
          "find": {"query": "cat", "replace": "dog", "matches": 3, "current": 1},
          "blocks": [
            {
              "k": "p", "path": [0], "text": "Hello",
              "spans": [{"s0":0,"s1":5,"b":true,"sz":14.0,"font":"Calibri","color":"FF0000"}],
              "style": "Heading1", "styleName": "Heading 1", "align": "center",
              "indentLeft": 0, "indentFirst": 0, "spaceBefore": 12, "spaceAfter": 6,
              "lineSpacing": {"kind":"multiple","value":1.15},
              "pageBreak": false, "rtl": false, "outline": 0, "shading": null,
              "list": null, "imgs": [], "empty": false
            },
            {
              "k": "p", "path": [1], "text": "item",
              "spans": [],
              "style": "Normal", "styleName": "Normal", "align": "left",
              "indentLeft": 18, "indentFirst": -18, "spaceBefore": 0, "spaceAfter": 0,
              "lineSpacing": {"kind":"multiple","value":1.0},
              "pageBreak": false, "rtl": false, "outline": null, "shading": null,
              "list": {"label": "2.", "level": 0, "hanging": 18, "indent": 18},
              "imgs": [{"kind":"image","w":72.0,"h":48.0,"alt":"logo","b64":"aGVsbG8="}],
              "empty": false
            },
            {
              "k": "t", "path": [2], "grid": [100.0, 50.0],
              "rows": [
                [
                  {"span": 1, "blocks": [
                    {"k":"p","path":[2,0,0,0],"text":"A1","spans":[],"style":"Normal","styleName":"Normal",
                     "align":"left","indentLeft":0,"indentFirst":0,"spaceBefore":0,"spaceAfter":0,
                     "pageBreak":false,"rtl":false,"list":null,"imgs":[],"empty":false}
                  ]},
                  {"span": 1, "blocks": [
                    {"k":"p","path":[2,0,1,0],"text":"B1","spans":[],"style":"Normal","styleName":"Normal",
                     "align":"left","indentLeft":0,"indentFirst":0,"spaceBefore":0,"spaceAfter":0,
                     "pageBreak":false,"rtl":false,"list":null,"imgs":[],"empty":false}
                  ]}
                ]
              ]
            }
          ]
        }
    """.trimIndent()

    private val state = StateParser.parse(sample)

    @Test
    fun topLevelFields() {
        assertTrue(state.canUndo)
        assertEquals(false, state.canRedo)
        assertTrue(state.dirty)
        assertEquals("ell", state.selectedText)
        assertEquals(42, state.wordCount)
    }

    @Test
    fun selection() {
        assertEquals(1, state.selection.anchor.off)
        assertEquals(4, state.selection.focus.off)
        assertEquals(listOf(0), state.selection.focus.path)
        val (a, b) = state.selection.ordered()
        assertEquals(1, a.off)
        assertEquals(4, b.off)
    }

    @Test
    fun formatState() {
        assertEquals("Calibri", state.format.font)
        assertEquals(14f, state.format.sizePt)
        assertTrue(state.format.bold)
        assertTrue(state.format.underline)
        assertEquals("FF0000", state.format.color)
    }

    @Test
    fun paraState() {
        assertEquals("Heading1", state.para.style)
        assertEquals("Heading 1", state.para.styleName)
        assertEquals("center", state.para.align)
        assertEquals(1.15f, state.para.lineSpacing?.value)
    }

    @Test
    fun blocks() {
        assertEquals(3, state.blocks.size)
        val p0 = state.blocks[0].asPara!!
        assertEquals("Hello", p0.text)
        assertEquals("Heading1", p0.style)
        assertEquals(1, p0.spans.size)
        assertEquals(0, p0.spans[0].start)
        assertEquals(5, p0.spans[0].end)
        assertTrue(p0.spans[0].bold)
        assertEquals("FF0000", p0.spans[0].color)
        assertEquals("center", p0.align)
    }

    @Test
    fun listParagraph() {
        val p1 = state.blocks[1].asPara!!
        assertEquals("2.", p1.list?.label)
        assertEquals(18f, p1.list?.hanging)
        assertEquals("image", p1.imgs[0].kind)
        assertEquals("aGVsbG8=", p1.imgs[0].b64)
    }

    @Test
    fun nestedTableCells() {
        val t = state.blocks[2].asTable!!
        assertEquals(listOf(2), t.path)
        assertEquals(listOf(100f, 50f), t.grid)
        assertEquals(1, t.rows.size)
        assertEquals(2, t.rows[0].size)
        val cell = t.rows[0][1]
        assertEquals(1, cell.span)
        val cp = cell.blocks[0].asPara!!
        assertEquals("B1", cp.text)
        assertEquals(listOf(2, 0, 1, 0), cp.path)
    }

    @Test
    fun sectionKeyGroupsSameStyle() {
        val a = state.blocks[0].asPara!!
        val b = state.blocks[1].asPara!!
        assertTrue(a.sectionKey() != b.sectionKey()) // heading vs list item
    }
}
