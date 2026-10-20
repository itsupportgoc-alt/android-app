package com.wordcraft.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Asserts the ribbon is *complete*: every tab in the strip resolves to real content.
 *
 * The bug this guards against: the tabs that are not hand-copied (Insert, Draw, Design,
 * Layout, References, Mailings, Review) fell back to a placeholder that said the tab was
 * "not part of the View reference". Every one of them must now resolve to the engine's own
 * command groups, and every button must be a real command with a label.
 */
class RibbonCatalogTest {

    /** The tabs the engine drives (the others are hand-written: File, Home, View, Help). */
    private val engineTabs = listOf("Insert", "Draw", "Design", "Layout", "References", "Mailings", "Review")

    @Test
    fun `every engine tab resolves to at least one ribbon group`() {
        engineTabs.forEach { tab ->
            val groups = RibbonCatalog.groups(tab)
            assertTrue("tab $tab has no ribbon groups", groups.isNotEmpty())
            groups.forEach { group ->
                assertTrue("tab $tab group '${group.caption}' is empty", group.commands.isNotEmpty())
            }
        }
    }

    @Test
    fun `every ribbon button is a real command with a label`() {
        engineTabs.forEach { tab ->
            RibbonCatalog.groups(tab).forEach { group ->
                group.commands.forEach { cmd ->
                    assertTrue("tab $tab has a button without an id", cmd.id.isNotBlank())
                    assertTrue("tab $tab button ${cmd.id} has no label", cmd.label.isNotBlank())
                    assertNotNull(cmd.icon)
                }
            }
        }
    }

    @Test
    fun `ribbon group captions are unique within a tab`() {
        engineTabs.forEach { tab ->
            val captions = RibbonCatalog.groups(tab).map { it.caption }
            assertEquals("tab $tab has duplicate group captions", captions.size, captions.toSet().size)
        }
    }

    @Test
    fun `no command appears twice in the ribbon`() {
        val ids = RibbonCatalog.commands.map { it.id }
        assertEquals("a command is bound to two ribbon buttons", ids.size, ids.toSet().size)
    }

    @Test
    fun `the ribbon covers every tab of the strip except the hand-written ones`() {
        // The hand-written tabs: File (backstage), Home, View and Help.
        val handWritten = setOf("File", "Home", "View", "Help")
        val uncovered = ViewState.tabs.filter { it !in handWritten && RibbonCatalog.groups(it).isEmpty() }
        assertTrue("tabs without a ribbon: $uncovered", uncovered.isEmpty())
    }

    @Test
    fun `the command search index knows the parameters of the ribbon commands`() {
        // A button whose parameters the search does not know would open an empty dialog.
        val missing = RibbonCatalog.commands
            .filter { it.id.startsWith("insert.") || it.id.startsWith("review.") }
            .filter { it.fields.isNotEmpty() }
            .filter { EngineCommandIndex.fieldsOf(it.id).isEmpty() }
            .map { it.id }
        assertTrue("commands with parameters the index does not know: $missing", missing.isEmpty())
    }

    @Test
    fun `every ribbon group has a prominent large button like Word draws them`() {
        // Word draws the group's most-used command as a big button (icon above the label).
        // A group without one would render as a bare list of small buttons.
        engineTabs.forEach { tab ->
            RibbonCatalog.groups(tab).forEach { group ->
                assertTrue(
                    "tab $tab group '${group.caption}' has no large button",
                    group.commands.any { it.large },
                )
            }
        }
    }

    @Test
    fun `a command that needs a platform service is marked as an app action`() {
        // The pickers the app layer owns: a picture, a text file, a recipient list, a compare file.
        val appOwned = setOf(
            "insert.picture",
            "insert.textFromFile",
            "mailings.recipients",
            "review.compare",
            "review.combine",
        )
        appOwned.forEach { id ->
            val cmd = RibbonCatalog.command(id)
            assertNotNull("app-owned command $id is missing from the ribbon", cmd)
            assertNotNull("app-owned command $id has no app action", cmd!!.appAction)
        }
    }

    @Test
    fun `a command with only optional switches runs without a dialog`() {
        // Word's Track Changes / Hyphenation style toggles: the ribbon runs them straight away.
        val trackChanges = RibbonCatalog.command("review.trackChanges")
        assertNotNull(trackChanges)
        assertFalse("review.trackChanges should not open an options dialog", trackChanges!!.fields.any { it.required })
    }
}
