package com.wordcraft.editor

/**
 * The lists and palettes the ribbon's galleries share:
 * the font names, the font sizes, Word's text colours and its highlighter colours.
 */

/** Word's highlighter colours (engine `format.highlight` accepts these names). */
val HighlightPalette: List<Pair<String, String>> = listOf(
    "yellow" to "FFFF00",
    "brightGreen" to "00FF00",
    "turquoise" to "00FFFF",
    "pink" to "FF00FF",
    "blue" to "0000FF",
    "red" to "FF0000",
    "darkBlue" to "000080",
    "teal" to "008080",
    "green" to "008000",
    "violet" to "800080",
    "darkRed" to "800000",
    "darkYellow" to "808000",
    "gray50" to "808080",
    "gray25" to "C0C0C0",
    "black" to "000000",
)

/** Text colours for `format.color` (RRGGBB). */
val TextColorPalette: List<String?> = listOf(
    null, // auto
    "000000", "434343", "666666", "999999", "BFBFBF",
    "C00000", "FF0000", "FF6600", "FFC000", "FFFF00",
    "92D050", "00B050", "00B0F0", "0070C0", "002060", "7030A0", "FFFFFF",
)

val FontSizes: List<Float> = listOf(8f, 9f, 10f, 10.5f, 11f, 12f, 14f, 16f, 18f, 20f, 22f, 24f, 26f, 28f, 36f, 48f, 72f)

val FontNames: List<String> = listOf(
    "Aptos", "Calibri", "Cambria", "Arial", "Times New Roman", "Courier New", "Verdana", "Georgia", "Comic Sans MS",
)
