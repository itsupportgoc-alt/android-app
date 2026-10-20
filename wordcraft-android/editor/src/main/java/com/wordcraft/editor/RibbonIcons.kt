package com.wordcraft.editor

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessible
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.AutoFixNormal
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BorderColor
import androidx.compose.material.icons.filled.BrandingWatermark
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Difference
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.EmojiSymbols
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.FormatIndentIncrease
import androidx.compose.material.icons.filled.FormatLineSpacing
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Spellcheck
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.TextFormat
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Toc
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material.icons.filled.VerticalSplit
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Workspaces
import androidx.compose.material.icons.filled.WrapText
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * The ribbon's icons.
 *
 * The catalog stores an icon *name* (so the generated file stays free of Compose imports);
 * this maps the name to the Material icon the button draws.
 */
object RibbonIcons {

    /** The icon for [name] (falls back to a neutral one). */
    fun of(name: String): ImageVector = when (name) {
        "picture" -> Icons.Filled.Image
        "icons" -> Icons.Filled.EmojiSymbols
        "symbol" -> Icons.Filled.EmojiSymbols
        "shapes" -> Icons.Filled.DragHandle
        "chart" -> Icons.Filled.BarChart
        "smartart" -> Icons.Filled.Hub
        "canvas" -> Icons.Filled.Dashboard
        "table" -> Icons.Filled.TableChart
        "grid" -> Icons.Filled.GridOn
        "link" -> Icons.Filled.Link
        "bookmark" -> Icons.Filled.Bookmark
        "crossref" -> Icons.Filled.CompareArrows
        "textbox" -> Icons.Filled.TextFields
        "wordart" -> Icons.Filled.Title
        "equation" -> Icons.Filled.Functions
        "field" -> Icons.Filled.DataObject
        "pagebreak" -> Icons.Filled.HorizontalRule
        "header" -> Icons.Filled.Article
        "footer" -> Icons.Filled.List
        "pageNumber" -> Icons.Filled.Tag
        "dateTime" -> Icons.Filled.Tag
        "dropCap" -> Icons.Filled.TextFormat
        "coverPage" -> Icons.Filled.Description
        "textFile" -> Icons.Filled.FolderOpen
        "quickParts" -> Icons.Filled.Article
        "docProperty" -> Icons.Filled.Info
        "signature" -> Icons.Filled.Draw
        "object" -> Icons.Filled.InsertDriveFile
        "spreadsheet" -> Icons.Filled.GridOn
        "theme" -> Icons.Filled.Palette
        "colors" -> Icons.Filled.Colorize
        "fonts" -> Icons.Filled.FontDownload
        "styleSet" -> Icons.Filled.Style
        "spacing" -> Icons.Filled.FormatLineSpacing
        "watermark" -> Icons.Filled.BrandingWatermark
        "pageColor" -> Icons.Filled.Colorize
        "pageBorders" -> Icons.Filled.BorderColor
        "effects" -> Icons.Filled.AutoFixHigh
        "rotate" -> Icons.Filled.RotateRight
        "wrap" -> Icons.Filled.WrapText
        "position" -> Icons.Filled.CenterFocusStrong
        "size" -> Icons.Filled.AspectRatio
        "group" -> Icons.Filled.Workspaces
        "align" -> Icons.Filled.FormatListNumbered
        "margins" -> Icons.Filled.Fullscreen
        "orientation" -> Icons.Filled.ScreenRotation
        "columns" -> Icons.Filled.ViewColumn
        "breaks" -> Icons.Filled.VerticalSplit
        "lineNumbers" -> Icons.Filled.FormatListNumbered
        "hyphenation" -> Icons.Filled.Spellcheck
        "indents" -> Icons.Filled.FormatIndentIncrease
        "citations" -> Icons.Filled.FormatQuote
        "bibliography" -> Icons.Filled.MenuBook
        "caption" -> Icons.Filled.PhotoCamera
        "footnote" -> Icons.Filled.Notes
        "index" -> Icons.Filled.List
        "toc" -> Icons.Filled.Toc
        "research" -> Icons.Filled.TravelExplore
        "mail" -> Icons.Filled.Mail
        "comment" -> Icons.Filled.Comment
        "trackChanges" -> Icons.Filled.EditNote
        "accept" -> Icons.Filled.Done
        "reject" -> Icons.Filled.Close
        "proofing" -> Icons.Filled.Spellcheck
        "readAloud" -> Icons.Filled.VolumeUp
        "compare" -> Icons.Filled.Difference
        "accessibility" -> Icons.Filled.Accessible
        "language" -> Icons.Filled.Language
        "protect" -> Icons.Filled.Lock
        "pen" -> Icons.Filled.Edit
        "pencil" -> Icons.Filled.Create
        "highlighter" -> Icons.Filled.Brush
        "eraser" -> Icons.Filled.AutoFixNormal
        "lasso" -> Icons.Filled.Opacity
        "ink" -> Icons.Filled.Gesture
        "convert" -> Icons.Filled.Autorenew
        "replay" -> Icons.Filled.Replay
        "wordCount" -> Icons.Filled.Numbers
        "thesaurus" -> Icons.Filled.MenuBook
        "list" -> Icons.Filled.List
        else -> Icons.Filled.Extension
    }
}
