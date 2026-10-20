package com.wordcraft.editor

// GENERATED FILE - do not edit by hand.
//
// Source: the WordCraft engine's own command registry (crates/engine/src/cmd/*.rs), whose
// `CommandSpec::new(id, label, location)` entries carry the ribbon location ("Tab > Group").
// Regenerate with:
//   python3 tools/build_ribbon_catalog.py <cmd-src-dir> <libwordcraft_core.so> <out.kt>
//
// Every id below was verified to be present in the shipped libwordcraft_core.so, so each
// button is backed by a command the app can really run.

/** Tabs whose ribbon is generated from the engine's command registry. */
val EngineRibbonTabs = listOf("Insert", "Draw", "Design", "Layout", "References", "Mailings", "Review")

private val insertRibbon = listOf(
    RibbonGroup("Illustrations", listOf(
        ribbonCmd("insert.canvas", "Drawing Canvas", "canvas", fields = listOf(ribbonField("width", FieldKind.NUMBER, "width", false), ribbonField("fill", FieldKind.TEXT, "fill", false), ribbonField("outline", FieldKind.TEXT, "outline", false))),
        ribbonCmd("insert.chart", "Chart", "chart", fields = listOf(ribbonField("type", FieldKind.CHOICE, "type", false, null, "column", "columnStacked", "bar", "barStacked", "line", "area", "pie", "doughnut", "scatter"), ribbonField("title", FieldKind.TEXT, "title", false), ribbonField("legend", FieldKind.CHOICE, "legend", false, null, "right", "left", "top", "bottom", "none"), ribbonField("dataLabels", FieldKind.BOOL, "dataLabels", false), ribbonField("width", FieldKind.NUMBER, "width", false), ribbonField("height", FieldKind.NUMBER, "height", false))),
        ribbonCmd("insert.picture", "Pictures", "picture", appAction = AppActionKind.PICK_IMAGE, large = true),
        ribbonCmd("insert.icon", "Icons", "icons", fields = listOf(ribbonField("id", FieldKind.TEXT, "id", false), ribbonField("color", FieldKind.TEXT, "color", false), ribbonField("size", FieldKind.NUMBER, "size", false), ribbonField("query", FieldKind.TEXT, "query", false), ribbonField("category", FieldKind.TEXT, "category", false)), large = true),
        ribbonCmd("insert.shape", "Shapes", "shapes", fields = listOf(ribbonField("kind", FieldKind.CHOICE, "kind", true, null, "rectangle", "roundedRectangle", "ellipse", "triangle", "diamond", "line", "arrow", "star", "heart", "straightConnector", "elbowConnector", "curvedConnector"), ribbonField("width", FieldKind.NUMBER, "width", false), ribbonField("height", FieldKind.NUMBER, "height", false), ribbonField("fill", FieldKind.TEXT, "fill", false), ribbonField("stroke", FieldKind.TEXT, "stroke", false), ribbonField("arrow", FieldKind.CHOICE, "arrow", false, null, "none", "start", "end", "both")), large = true),
        ribbonCmd("insert.smartArt", "SmartArt", "smartart", fields = listOf(ribbonField("layout", FieldKind.CHOICE, "layout", false, null, "basicList", "process", "cycle", "hierarchy", "pyramid", "radial", "matrix", "venn"), ribbonField("colors", FieldKind.CHOICE, "colors", false, null, "colorful"), ribbonField("width", FieldKind.NUMBER, "width", false), ribbonField("height", FieldKind.NUMBER, "height", false))),
    )),
    RibbonGroup("Links", listOf(
        ribbonCmd("insert.crossReference", "Cross-reference", "crossref", fields = listOf(ribbonField("to", FieldKind.CHOICE, "to", true, null, "heading", "bookmark", "figure", "table", "equation", "label:<Name>"), ribbonField("target", FieldKind.TEXT, "target", true), ribbonField("show", FieldKind.CHOICE, "show", false, null, "text", "page", "number", "aboveBelow"))),
        ribbonCmd("insert.link", "Link", "link", fields = listOf(ribbonField("url", FieldKind.TEXT, "url", true)), large = true),
        ribbonCmd("insert.removeLink", "Remove Hyperlink", "generic"),
        ribbonCmd("insert.bookmark", "Bookmark", "bookmark", fields = listOf(ribbonField("name", FieldKind.TEXT, "name", true))),
    )),
    RibbonGroup("Symbols", listOf(
        ribbonCmd("insert.equation", "Equation", "equation", fields = listOf(ribbonField("linear", FieldKind.TEXT, "linear", false), ribbonField("latex", FieldKind.TEXT, "latex", false), ribbonField("builtin", FieldKind.CHOICE, "builtin", false, null, "pythagoras", "area", "quadratic", "binomial", "expansion", "euler", "derivative", "taylor", "gaussian", "fourier", "trig1", "trig2", "normal", "bayes"), ribbonField("display", FieldKind.BOOL, "display", false), ribbonField("edit", FieldKind.BOOL, "edit", false)), large = true),
        ribbonCmd("insert.symbol", "Symbol", "symbol", fields = listOf(ribbonField("char", FieldKind.TEXT, "char", true), ribbonField("font", FieldKind.TEXT, "font", false)), large = true),
        ribbonCmd("text.nbsp", "Nonbreaking Space", "generic"),
        ribbonCmd("text.nbHyphen", "Nonbreaking Hyphen", "generic"),
        ribbonCmd("text.optionalHyphen", "Optional Hyphen", "generic"),
        ribbonCmd("text.emDash", "Em Dash", "generic"),
        ribbonCmd("text.copyright", "Copyright Sign", "generic"),
        ribbonCmd("text.registered", "Registered Sign", "generic"),
        ribbonCmd("text.trademark", "Trademark Sign", "generic"),
        ribbonCmd("text.ellipsis", "Ellipsis", "generic"),
    )),
    RibbonGroup("Text", listOf(
        ribbonCmd("fields.toggleCode", "Toggle Field Codes", "field"),
        ribbonCmd("fields.setCode", "Edit Field Code", "field", fields = listOf(ribbonField("code", FieldKind.TEXT, "code", true))),
        ribbonCmd("fields.unlink", "Unlink Fields", "field"),
        ribbonCmd("fields.lock", "Lock Fields", "field"),
        ribbonCmd("fields.unlock", "Unlock Fields", "field"),
        ribbonCmd("fields.next", "Next Field", "field"),
        ribbonCmd("fields.previous", "Previous Field", "field"),
        ribbonCmd("fields.selected", "Selected Fields", "field"),
        ribbonCmd("insert.textBox", "Text Box", "textbox", fields = listOf(ribbonField("text", FieldKind.TEXT, "text", false), ribbonField("width", FieldKind.NUMBER, "width", false), ribbonField("height", FieldKind.NUMBER, "height", false)), large = true),
        ribbonCmd("insert.dateTime", "Date & Time", "dateTime", fields = listOf(ribbonField("format", FieldKind.TEXT, "format", false), ribbonField("update", FieldKind.BOOL, "update", false))),
        ribbonCmd("insert.field", "Field", "field", fields = listOf(ribbonField("instr", FieldKind.TEXT, "instr", true), ribbonField("result", FieldKind.TEXT, "result", false))),
        ribbonCmd("insert.dateField", "Insert Date Field", "dateTime"),
        ribbonCmd("insert.timeField", "Insert Time Field", "dateTime"),
        ribbonCmd("insert.pageField", "Insert Page Field", "generic"),
        ribbonCmd("insert.dropCap", "Drop Cap", "dropCap", fields = listOf(ribbonField("position", FieldKind.CHOICE, "position", false, null, "none", "dropped", "margin"), ribbonField("distance", FieldKind.NUMBER, "distance", false), ribbonField("font", FieldKind.TEXT, "font", false))),
        ribbonCmd("insert.textFromFile", "Text from File", "textFile", appAction = AppActionKind.PICK_TEXT_FILE),
        ribbonCmd("insert.quickParts", "Quick Parts", "quickParts", fields = listOf(ribbonField("save", FieldKind.TEXT, "save", false), ribbonField("in", FieldKind.TEXT, "in", false), ribbonField("insert", FieldKind.TEXT, "insert", false), ribbonField("delete", FieldKind.TEXT, "delete", false))),
        ribbonCmd("insert.autoText", "AutoText", "quickParts", fields = listOf(ribbonField("save", FieldKind.TEXT, "save", false), ribbonField("in", FieldKind.TEXT, "in", false), ribbonField("insert", FieldKind.TEXT, "insert", false), ribbonField("delete", FieldKind.TEXT, "delete", false))),
        ribbonCmd("insert.docProperty", "Document Property", "docProperty", fields = listOf(ribbonField("name", FieldKind.CHOICE, "name", true, null, "Title", "Author", "Subject", "Keywords", "Comments", "Category"))),
        ribbonCmd("insert.signatureLine", "Signature Line", "signature", fields = listOf(ribbonField("signer", FieldKind.TEXT, "signer", false), ribbonField("title", FieldKind.TEXT, "title", false))),
        ribbonCmd("insert.object", "Object", "object", fields = listOf(ribbonField("path", FieldKind.TEXT, "path", true)), large = true),
        ribbonCmd("insert.wordArt", "WordArt", "wordart", fields = listOf(ribbonField("text", FieldKind.TEXT, "text", false), ribbonField("style", FieldKind.TEXT, "style", false), ribbonField("transform", FieldKind.CHOICE, "transform", false, null, "textArchUp", "textArchDown", "textCircle", "textWave1")), large = true),
    )),
    RibbonGroup("Pages", listOf(
        ribbonCmd("insert.pageBreak", "Page Break", "pagebreak", large = true),
        ribbonCmd("insert.blankPage", "Blank Page", "coverPage", large = true),
        ribbonCmd("insert.coverPage", "Cover Page", "coverPage", fields = listOf(ribbonField("title", FieldKind.TEXT, "title", false), ribbonField("subtitle", FieldKind.TEXT, "subtitle", false), ribbonField("author", FieldKind.TEXT, "author", false)), large = true),
        ribbonCmd("text.pageBreak", "Page Break", "pagebreak"),
    )),
    RibbonGroup("Tables", listOf(
        ribbonCmd("insert.table", "Table", "generic", fields = listOf(ribbonField("rows", FieldKind.NUMBER, "rows", true), ribbonField("cols", FieldKind.NUMBER, "cols", true), ribbonField("style", FieldKind.TEXT, "style", false)), large = true),
        ribbonCmd("table.fromText", "Convert Text to Table", "table", fields = listOf(ribbonField("separator", FieldKind.CHOICE, "separator", false, null, "tab", "comma", "paragraph", "other"), ribbonField("char", FieldKind.TEXT, "char", false), ribbonField("columns", FieldKind.NUMBER, "columns", false), ribbonField("autofit", FieldKind.CHOICE, "autofit", false, null, "initial", "fixed", "contents", "window"), ribbonField("width", FieldKind.NUMBER, "width", false)), large = true),
        ribbonCmd("table.quick", "Quick Tables", "table", fields = listOf(ribbonField("kind", FieldKind.CHOICE, "kind", false, null, "calendar", "tabular", "matrix"))),
        ribbonCmd("table.draw", "Draw Table", "table", fields = listOf(ribbonField("page", FieldKind.NUMBER, "page", true), ribbonField("x0", FieldKind.NUMBER, "x0", true), ribbonField("y0", FieldKind.NUMBER, "y0", true), ribbonField("x1", FieldKind.NUMBER, "x1", true), ribbonField("y1", FieldKind.NUMBER, "y1", true))),
        ribbonCmd("insert.spreadsheet", "Spreadsheet Table", "spreadsheet", fields = listOf(ribbonField("csv", FieldKind.TEXT, "csv", false))),
    )),
    RibbonGroup("Header & Footer", listOf(
        ribbonCmd("insert.header", "Header", "header", fields = listOf(ribbonField("text", FieldKind.TEXT, "text", false), ribbonField("preset", FieldKind.CHOICE, "preset", false, null, "blank", "blankThree", "title")), large = true),
        ribbonCmd("insert.footer", "Footer", "footer", fields = listOf(ribbonField("text", FieldKind.TEXT, "text", false), ribbonField("preset", FieldKind.CHOICE, "preset", false, null, "blank", "blankThree", "pageNumber")), large = true),
        ribbonCmd("insert.pageNumber", "Page Number", "pageNumber", fields = listOf(ribbonField("position", FieldKind.CHOICE, "position", false, null, "top", "bottom", "current"), ribbonField("align", FieldKind.CHOICE, "align", false, null, "left", "center", "right"), ribbonField("format", FieldKind.TEXT, "format", false)), large = true),
        ribbonCmd("insert.editHeader", "Edit Header", "header", fields = listOf(ribbonField("page", FieldKind.NUMBER, "page", false))),
        ribbonCmd("insert.editFooter", "Edit Footer", "footer", fields = listOf(ribbonField("page", FieldKind.NUMBER, "page", false))),
        ribbonCmd("insert.removeHeader", "Remove Header", "header"),
        ribbonCmd("insert.removeFooter", "Remove Footer", "footer"),
        ribbonCmd("layout.pageNumberFormat", "Format Page Numbers", "pageNumber", fields = listOf(ribbonField("format", FieldKind.CHOICE, "format", false, null, "decimal", "lowerRoman", "upperRoman", "lowerLetter", "upperLetter"), ribbonField("start", FieldKind.NUMBER, "start", false))),
    )),
)

private val drawRibbon = listOf(
    RibbonGroup("Tools", listOf(
        ribbonCmd("draw.select", "Select", "ink", large = true),
        ribbonCmd("draw.lasso", "Lasso Select", "lasso", fields = listOf(ribbonField("page", FieldKind.NUMBER, "page", false), ribbonField("clear", FieldKind.BOOL, "clear", false)), large = true),
        ribbonCmd("draw.lassoMove", "Move Selected Ink", "lasso", fields = listOf(ribbonField("dx", FieldKind.NUMBER, "dx", true), ribbonField("dy", FieldKind.NUMBER, "dy", true))),
        ribbonCmd("draw.lassoDelete", "Delete Selected Ink", "lasso"),
        ribbonCmd("draw.lassoColor", "Recolor Selected Ink", "lasso", fields = listOf(ribbonField("color", FieldKind.TEXT, "color", true))),
        ribbonCmd("draw.eraser", "Eraser", "eraser", fields = listOf(ribbonField("page", FieldKind.NUMBER, "page", false), ribbonField("x", FieldKind.NUMBER, "x", false), ribbonField("y", FieldKind.NUMBER, "y", false)), large = true),
        ribbonCmd("draw.erase", "Erase Ink", "eraser", fields = listOf(ribbonField("page", FieldKind.NUMBER, "page", true), ribbonField("x", FieldKind.NUMBER, "x", true), ribbonField("y", FieldKind.NUMBER, "y", true))),
    )),
    RibbonGroup("Pens", listOf(
        ribbonCmd("draw.pen", "Pen", "pen", fields = listOf(ribbonField("color", FieldKind.TEXT, "color", false), ribbonField("width", FieldKind.NUMBER, "width", false)), large = true),
        ribbonCmd("draw.pencil", "Pencil", "pen", fields = listOf(ribbonField("color", FieldKind.TEXT, "color", false), ribbonField("width", FieldKind.NUMBER, "width", false)), large = true),
        ribbonCmd("draw.highlighter", "Highlighter", "highlighter", fields = listOf(ribbonField("color", FieldKind.TEXT, "color", false), ribbonField("width", FieldKind.NUMBER, "width", false)), large = true),
        ribbonCmd("draw.addPen", "Add Pen", "pen", fields = listOf(ribbonField("kind", FieldKind.CHOICE, "kind", false, null, "pen", "pencil", "highlighter"), ribbonField("color", FieldKind.TEXT, "color", false), ribbonField("width", FieldKind.NUMBER, "width", false))),
        ribbonCmd("draw.removePen", "Remove Pen", "pen", fields = listOf(ribbonField("index", FieldKind.NUMBER, "index", true))),
        ribbonCmd("draw.stroke", "Draw Ink", "ink", fields = listOf(ribbonField("page", FieldKind.NUMBER, "page", true), ribbonField("tool", FieldKind.CHOICE, "tool", false, null, "pen", "pencil", "highlighter"), ribbonField("color", FieldKind.TEXT, "color", false), ribbonField("width", FieldKind.NUMBER, "width", false))),
    )),
    RibbonGroup("Convert", listOf(
        ribbonCmd("draw.inkToShape", "Ink to Shape", "convert", fields = listOf(ribbonField("value", FieldKind.BOOL, "value", false)), large = true),
        ribbonCmd("draw.convertInk", "Convert to Shapes", "convert"),
    )),
    RibbonGroup("Replay", listOf(
        ribbonCmd("draw.replay", "Ink Replay", "replay", fields = listOf(ribbonField("value", FieldKind.BOOL, "value", false)), large = true),
    )),
)

private val designRibbon = listOf(
    RibbonGroup("Document Formatting", listOf(
        ribbonCmd("design.theme", "Themes", "theme", fields = listOf(ribbonField("name", FieldKind.TEXT, "name", true, "design.themes")), large = true),
        ribbonCmd("design.themeFonts", "Fonts", "fonts", fields = listOf(ribbonField("heading", FieldKind.TEXT, "heading", true), ribbonField("body", FieldKind.TEXT, "body", false)), large = true),
        ribbonCmd("design.themeColors", "Colors", "colors", fields = listOf(ribbonField("name", FieldKind.TEXT, "name", true, "design.themes")), large = true),
        ribbonCmd("design.styleSet", "Style Set", "styleSet", fields = listOf(ribbonField("name", FieldKind.CHOICE, "name", true, null, "default", "basic", "lines", "shaded", "casual", "centered", "minimalist", "title"))),
        ribbonCmd("design.paragraphSpacing", "Paragraph Spacing", "spacing", fields = listOf(ribbonField("value", FieldKind.CHOICE, "value", true, null, "default", "none", "compact", "tight", "open", "relaxed", "double"))),
        ribbonCmd("design.setDefault", "Set as Default", "save"),
        ribbonCmd("design.themes", "Theme List", "theme"),
        ribbonCmd("design.effects", "Effects", "effects"),
    )),
    RibbonGroup("Page Background", listOf(
        ribbonCmd("design.watermark", "Watermark", "watermark", fields = listOf(ribbonField("text", FieldKind.TEXT, "text", false), ribbonField("remove", FieldKind.BOOL, "remove", false), ribbonField("diagonal", FieldKind.BOOL, "diagonal", false), ribbonField("color", FieldKind.TEXT, "color", false)), large = true),
        ribbonCmd("design.pageColor", "Page Color", "pageColor", fields = listOf(ribbonField("color", FieldKind.TEXT, "color", true)), large = true),
        ribbonCmd("design.pageBorders", "Page Borders", "pageBorders", fields = listOf(ribbonField("kind", FieldKind.CHOICE, "kind", false, null, "box", "none"), ribbonField("width", FieldKind.NUMBER, "width", false), ribbonField("color", FieldKind.TEXT, "color", false), ribbonField("style", FieldKind.CHOICE, "style", false, null, "single", "double", "dotted", "dashed", "thick", "triple", "dotDash", "wave"), ribbonField("applyTo", FieldKind.CHOICE, "applyTo", false, null, "section", "document")), large = true),
    )),
)

private val layoutRibbon = listOf(
    RibbonGroup("Arrange", listOf(
        ribbonCmd("arrange.rotate", "Rotate", "rotate", fields = listOf(ribbonField("direction", FieldKind.CHOICE, "direction", true, null, "right", "left", "flipVertical", "flipHorizontal"))),
        ribbonCmd("arrange.rotation", "Rotation", "rotate", fields = listOf(ribbonField("degrees", FieldKind.NUMBER, "degrees", true), ribbonField("flipH", FieldKind.BOOL, "flipH", false), ribbonField("flipV", FieldKind.BOOL, "flipV", false))),
        ribbonCmd("arrange.wrap", "Wrap Text", "wrap", fields = listOf(ribbonField("wrap", FieldKind.CHOICE, "wrap", true, null, "inline", "square", "tight", "through", "topAndBottom", "behindText", "inFrontOfText")), large = true),
        ribbonCmd("arrange.wrapText", "Wrap Text Sides", "wrap", fields = listOf(ribbonField("side", FieldKind.CHOICE, "side", true, null, "bothSides", "left", "right", "largest"))),
        ribbonCmd("arrange.moveWithText", "Move with Text", "position", fields = listOf(ribbonField("value", FieldKind.BOOL, "value", true))),
        ribbonCmd("arrange.position", "Position", "position", fields = listOf(ribbonField("preset", FieldKind.CHOICE, "preset", false, null, "topLeft", "topCenter", "topRight", "middleLeft", "middleCenter", "middleRight", "bottomLeft", "bottomCenter", "bottomRight"), ribbonField("x", FieldKind.NUMBER, "x", false), ribbonField("y", FieldKind.NUMBER, "y", false)), large = true),
        ribbonCmd("arrange.bringForward", "Bring Forward", "position"),
        ribbonCmd("arrange.sendBackward", "Send Backward", "position"),
        ribbonCmd("arrange.align", "Align", "align", fields = listOf(ribbonField("value", FieldKind.CHOICE, "value", true, null, "left", "center", "right")), large = true),
        ribbonCmd("arrange.selectionPane", "Selection Pane", "list"),
        ribbonCmd("arrange.bounds", "Move or Resize", "size", fields = listOf(ribbonField("width", FieldKind.NUMBER, "width", false), ribbonField("height", FieldKind.NUMBER, "height", false), ribbonField("x", FieldKind.NUMBER, "x", false), ribbonField("y", FieldKind.NUMBER, "y", false), ribbonField("page", FieldKind.NUMBER, "page", false))),
        ribbonCmd("arrange.nudge", "Nudge", "position", fields = listOf(ribbonField("dx", FieldKind.NUMBER, "dx", false), ribbonField("dy", FieldKind.NUMBER, "dy", false))),
        ribbonCmd("arrange.group", "Group", "group"),
        ribbonCmd("arrange.ungroup", "Ungroup", "group"),
    )),
    RibbonGroup("Page Setup", listOf(
        ribbonCmd("layout.margins", "Margins", "margins", fields = listOf(ribbonField("preset", FieldKind.CHOICE, "preset", false, null, "normal", "narrow", "moderate", "wide", "mirrored", "office2003"), ribbonField("top", FieldKind.NUMBER, "top", false), ribbonField("bottom", FieldKind.NUMBER, "bottom", false), ribbonField("left", FieldKind.NUMBER, "left", false), ribbonField("right", FieldKind.NUMBER, "right", false), ribbonField("gutter", FieldKind.NUMBER, "gutter", false), ribbonField("mirror", FieldKind.BOOL, "mirror", false)), large = true),
        ribbonCmd("layout.orientation", "Orientation", "orientation", fields = listOf(ribbonField("value", FieldKind.CHOICE, "value", true, null, "portrait", "landscape")), large = true),
        ribbonCmd("layout.size", "Size", "size", fields = listOf(ribbonField("name", FieldKind.CHOICE, "name", false, null, "Letter", "Legal", "A4"), ribbonField("width", FieldKind.NUMBER, "width", false), ribbonField("height", FieldKind.NUMBER, "height", false)), large = true),
        ribbonCmd("layout.columns", "Columns", "columns", fields = listOf(ribbonField("count", FieldKind.NUMBER, "count", true), ribbonField("space", FieldKind.NUMBER, "space", false), ribbonField("separator", FieldKind.BOOL, "separator", false), ribbonField("preset", FieldKind.CHOICE, "preset", false, null, "left", "right"), ribbonField("apply", FieldKind.CHOICE, "apply", false, null, "section", "document", "forward", "selection"))),
        ribbonCmd("layout.break", "Breaks", "breaks", fields = listOf(ribbonField("kind", FieldKind.CHOICE, "kind", true, null, "page", "column", "textWrapping", "nextPage", "continuous", "evenPage", "oddPage"))),
        ribbonCmd("layout.lineNumbers", "Line Numbers", "lineNumbers", fields = listOf(ribbonField("value", FieldKind.CHOICE, "value", false, null, "none", "continuous", "restartPage", "restartSection"), ribbonField("start", FieldKind.NUMBER, "start", false), ribbonField("countBy", FieldKind.NUMBER, "countBy", false), ribbonField("distance", FieldKind.NUMBER, "distance", false))),
        ribbonCmd("layout.hyphenation", "Hyphenation", "hyphenation", fields = listOf(ribbonField("value", FieldKind.BOOL, "value", false), ribbonField("zone", FieldKind.NUMBER, "zone", false), ribbonField("caps", FieldKind.BOOL, "caps", false), ribbonField("limit", FieldKind.NUMBER, "limit", false))),
        ribbonCmd("layout.manualHyphenation", "Manual Hyphenation", "hyphenation"),
        ribbonCmd("layout.pageSetup", "Page Setup", "margins"),
        ribbonCmd("layout.verticalAlign", "Vertical Alignment", "align", fields = listOf(ribbonField("value", FieldKind.CHOICE, "value", true, null, "top", "center", "justify", "bottom"))),
        ribbonCmd("layout.sectionRtl", "Right-to-Left Section", "margins", fields = listOf(ribbonField("value", FieldKind.BOOL, "value", false))),
        ribbonCmd("layout.textDirection", "Text Direction", "align", fields = listOf(ribbonField("value", FieldKind.CHOICE, "value", false, null, "horizontal", "down", "up"))),
        ribbonCmd("layout.documentGrid", "Document Grid", "grid", fields = listOf(ribbonField("type", FieldKind.CHOICE, "type", false, null, "none", "lines", "linesAndChars", "snapToChars"), ribbonField("linesPerPage", FieldKind.NUMBER, "linesPerPage", false), ribbonField("charsPerLine", FieldKind.NUMBER, "charsPerLine", false), ribbonField("linePitch", FieldKind.NUMBER, "linePitch", false), ribbonField("charSpace", FieldKind.NUMBER, "charSpace", false))),
        ribbonCmd("layout.section", "Section Properties", "margins"),
    )),
    RibbonGroup("Paragraph", listOf(
        ribbonCmd("para.indents", "Indents", "indents", fields = listOf(ribbonField("left", FieldKind.NUMBER, "left", false), ribbonField("right", FieldKind.NUMBER, "right", false), ribbonField("firstLine", FieldKind.NUMBER, "firstLine", false)), large = true),
        ribbonCmd("para.spacing", "Paragraph Spacing", "spacing", fields = listOf(ribbonField("before", FieldKind.NUMBER, "before", false), ribbonField("after", FieldKind.NUMBER, "after", false), ribbonField("beforeAuto", FieldKind.BOOL, "beforeAuto", false), ribbonField("afterAuto", FieldKind.BOOL, "afterAuto", false)), large = true),
    )),
    RibbonGroup("Breaks", listOf(
        ribbonCmd("text.columnBreak", "Column Break", "breaks", large = true),
    )),
)

private val referencesRibbon = listOf(
    RibbonGroup("Citations & Bibliography", listOf(
        ribbonCmd("references.sources", "Manage Sources", "bibliography", fields = listOf(ribbonField("remove", FieldKind.TEXT, "remove", false), ribbonField("import", FieldKind.TEXT, "import", false)), large = true),
        ribbonCmd("references.citation", "Insert Citation", "citations", fields = listOf(ribbonField("tag", FieldKind.TEXT, "tag", false), ribbonField("pages", FieldKind.TEXT, "pages", false)), large = true),
        ribbonCmd("references.citationStyle", "Style", "citations", fields = listOf(ribbonField("style", FieldKind.CHOICE, "style", true, null, "APA", "Chicago", "GB/T 7714", "GOST", "GOST", "Harvard", "IEEE", "ISO 690", "ISO 690", "MLA", "SIST02", "Turabian"))),
        ribbonCmd("references.bibliography", "Bibliography", "bibliography", fields = listOf(ribbonField("title", FieldKind.TEXT, "title", false)), large = true),
    )),
    RibbonGroup("Captions", listOf(
        ribbonCmd("references.tableOfFigures", "Insert Table of Figures", "caption", fields = listOf(ribbonField("label", FieldKind.CHOICE, "label", false, null, "Figure", "Table", "Equation"))),
        ribbonCmd("references.updateFigures", "Update Table", "caption"),
        ribbonCmd("references.caption", "Insert Caption", "caption", fields = listOf(ribbonField("label", FieldKind.CHOICE, "label", false, null, "Figure", "Table", "Equation", "any label"), ribbonField("text", FieldKind.TEXT, "text", false), ribbonField("position", FieldKind.CHOICE, "position", false, null, "below", "above"), ribbonField("excludeLabel", FieldKind.BOOL, "excludeLabel", false), ribbonField("format", FieldKind.CHOICE, "format", false, null, "ARABIC", "alphabetic", "ALPHABETIC", "roman", "ROMAN"), ribbonField("chapter", FieldKind.TEXT, "chapter", false), ribbonField("separator", FieldKind.CHOICE, "separator", false, null, "-", ".", ":", "—", "–")), large = true),
    )),
    RibbonGroup("Table of Authorities", listOf(
        ribbonCmd("references.markCitation", "Mark Citation", "citations", fields = listOf(ribbonField("entry", FieldKind.TEXT, "entry", false))),
        ribbonCmd("references.tableOfAuthorities", "Insert Table of Authorities", "citations", large = true),
        ribbonCmd("references.updateAuthorities", "Update Table", "citations"),
    )),
    RibbonGroup("Footnotes", listOf(
        ribbonCmd("references.noteOptions", "Footnote and Endnote", "footnote", fields = listOf(ribbonField("kind", FieldKind.CHOICE, "kind", false, null, "footnote", "endnote"), ribbonField("pos", FieldKind.CHOICE, "pos", false, null, "pageBottom", "beneathText"), ribbonField("numFmt", FieldKind.CHOICE, "numFmt", false, null, "decimal", "lowerRoman", "upperRoman", "lowerLetter", "upperLetter"), ribbonField("numStart", FieldKind.NUMBER, "numStart", false), ribbonField("numRestart", FieldKind.CHOICE, "numRestart", false, null, "continuous", "eachSect", "eachPage"), ribbonField("scope", FieldKind.CHOICE, "scope", false, null, "document", "section"))),
        ribbonCmd("references.footnote", "Insert Footnote", "footnote", fields = listOf(ribbonField("text", FieldKind.TEXT, "text", false), ribbonField("mark", FieldKind.TEXT, "mark", false)), large = true),
        ribbonCmd("references.endnote", "Insert Endnote", "footnote", fields = listOf(ribbonField("text", FieldKind.TEXT, "text", false), ribbonField("mark", FieldKind.TEXT, "mark", false)), large = true),
        ribbonCmd("references.convertNotes", "Convert Notes", "footnote", fields = listOf(ribbonField("to", FieldKind.TEXT, "to", true))),
        ribbonCmd("references.nextFootnote", "Next Footnote", "footnote"),
        ribbonCmd("references.notes", "Show Notes", "footnote"),
    )),
    RibbonGroup("Research", listOf(
        ribbonCmd("references.researcher", "Researcher", "research", large = true),
    )),
    RibbonGroup("Index", listOf(
        ribbonCmd("references.markEntry", "Mark Entry", "index", fields = listOf(ribbonField("entry", FieldKind.TEXT, "entry", false), ribbonField("subentry", FieldKind.TEXT, "subentry", false), ribbonField("crossReference", FieldKind.TEXT, "crossReference", false), ribbonField("bookmark", FieldKind.TEXT, "bookmark", false), ribbonField("bold", FieldKind.BOOL, "bold", false), ribbonField("italic", FieldKind.BOOL, "italic", false), ribbonField("all", FieldKind.BOOL, "all", false)), large = true),
        ribbonCmd("references.index", "Insert Index", "index", fields = listOf(ribbonField("type", FieldKind.CHOICE, "type", false, null, "indented", "runIn"), ribbonField("columns", FieldKind.NUMBER, "columns", false), ribbonField("rightAlign", FieldKind.BOOL, "rightAlign", false), ribbonField("tabLeader", FieldKind.CHOICE, "tabLeader", false, null, "dot", "hyphen", "underscore", "none"), ribbonField("headings", FieldKind.BOOL, "headings", false)), large = true),
        ribbonCmd("references.updateIndex", "Update Index", "index"),
    )),
    RibbonGroup("Table of Contents", listOf(
        ribbonCmd("references.toc", "Table of Contents", "toc", fields = listOf(ribbonField("levels", FieldKind.NUMBER, "levels", false), ribbonField("title", FieldKind.TEXT, "title", false), ribbonField("pageNumbers", FieldKind.BOOL, "pageNumbers", false), ribbonField("rightAlign", FieldKind.BOOL, "rightAlign", false), ribbonField("tabLeader", FieldKind.CHOICE, "tabLeader", false, null, "dot", "hyphen", "underscore", "none"), ribbonField("hyperlinks", FieldKind.BOOL, "hyperlinks", false), ribbonField("headingStyles", FieldKind.BOOL, "headingStyles", false), ribbonField("outlineLevels", FieldKind.BOOL, "outlineLevels", false), ribbonField("tcFields", FieldKind.BOOL, "tcFields", false)), large = true),
        ribbonCmd("references.updateToc", "Update Table", "toc", large = true),
        ribbonCmd("references.removeToc", "Remove Table of Contents", "toc"),
        ribbonCmd("references.addText", "Add Text", "toc"),
    )),
    RibbonGroup("Fields", listOf(
        ribbonCmd("references.updateFields", "Update Field", "field", fields = listOf(ribbonField("all", FieldKind.BOOL, "all", false)), large = true),
    )),
)

private val mailingsRibbon = listOf(
    RibbonGroup("Start Mail Merge", listOf(
        ribbonCmd("mailings.start", "Start Mail Merge", "mail", fields = listOf(ribbonField("kind", FieldKind.CHOICE, "kind", true, null, "letters", "emails", "envelopes", "labels", "directory", "normal")), large = true),
        ribbonCmd("mailings.recipients", "Select Recipients", "mail", appAction = AppActionKind.PICK_CSV, large = true),
        ribbonCmd("mailings.editRecipients", "Edit Recipient List", "mail"),
    )),
    RibbonGroup("Write & Insert Fields", listOf(
        ribbonCmd("mailings.insertField", "Insert Merge Field", "mail", fields = listOf(ribbonField("field", FieldKind.TEXT, "field", true)), large = true),
        ribbonCmd("mailings.addressBlock", "Address Block", "mail", large = true),
        ribbonCmd("mailings.greetingLine", "Greeting Line", "mail", large = true),
        ribbonCmd("mailings.rules", "Rules", "mail", fields = listOf(ribbonField("rule", FieldKind.CHOICE, "rule", true, null, "IF", "SKIPIF", "NEXTIF", "NEXT", "MERGEREC"))),
        ribbonCmd("mailings.matchFields", "Match Fields", "mail"),
        ribbonCmd("mailings.highlightFields", "Highlight Merge Fields", "mail"),
    )),
    RibbonGroup("Preview Results", listOf(
        ribbonCmd("mailings.preview", "Preview Results", "mail", large = true),
        ribbonCmd("mailings.next", "Next Record", "mail"),
        ribbonCmd("mailings.previous", "Previous Record", "mail"),
        ribbonCmd("mailings.findRecipient", "Find Recipient", "mail", fields = listOf(ribbonField("text", FieldKind.TEXT, "text", true))),
    )),
    RibbonGroup("Finish", listOf(
        ribbonCmd("mailings.checkErrors", "Check for Errors", "mail"),
        ribbonCmd("mailings.finish", "Finish & Merge", "mail", fields = listOf(ribbonField("path", FieldKind.TEXT, "path", false), ribbonField("from", FieldKind.NUMBER, "from", false), ribbonField("to", FieldKind.NUMBER, "to", false)), large = true),
    )),
    RibbonGroup("Create", listOf(
        ribbonCmd("mailings.envelopes", "Envelopes", "mail", fields = listOf(ribbonField("delivery", FieldKind.TEXT, "delivery", true), ribbonField("return", FieldKind.TEXT, "return", false), ribbonField("size", FieldKind.CHOICE, "size", false, null, "Envelope #10", "#9", "Monarch", "DL", "C4", "C5", "C6", "B5"), ribbonField("width", FieldKind.NUMBER, "width", false), ribbonField("height", FieldKind.NUMBER, "height", false), ribbonField("add", FieldKind.BOOL, "add", false)), large = true),
        ribbonCmd("mailings.labels", "Labels", "mail", fields = listOf(ribbonField("text", FieldKind.TEXT, "text", false), ribbonField("product", FieldKind.CHOICE, "product", false, null, "letter-3x10", "letter-2x10", "letter-2x7", "letter-2x5", "letter-4x20", "a4-3x7", "a4-2x7", "a4-3x8", "a4-2x4"), ribbonField("rows", FieldKind.NUMBER, "rows", false), ribbonField("cols", FieldKind.NUMBER, "cols", false), ribbonField("fromRecipients", FieldKind.BOOL, "fromRecipients", false)), large = true),
    )),
)

private val reviewRibbon = listOf(
    RibbonGroup("Ink", listOf(
        ribbonCmd("review.hideInk", "Hide Ink", "generic", fields = listOf(ribbonField("value", FieldKind.BOOL, "value", false)), large = true),
    )),
    RibbonGroup("Protect", listOf(
        ribbonCmd("review.restrict", "Restrict Editing", "protect", fields = listOf(ribbonField("mode", FieldKind.CHOICE, "mode", false, null, "none", "readOnly", "comments", "trackedChanges", "forms"), ribbonField("formatting", FieldKind.BOOL, "formatting", false), ribbonField("password", FieldKind.TEXT, "password", false), ribbonField("stop", FieldKind.BOOL, "stop", false), ribbonField("exception", FieldKind.TEXT, "exception", false)), large = true),
    )),
    RibbonGroup("Comments", listOf(
        ribbonCmd("review.newComment", "New Comment", "comment", fields = listOf(ribbonField("text", FieldKind.TEXT, "text", true)), large = true),
        ribbonCmd("review.reply", "Reply", "comment", fields = listOf(ribbonField("id", FieldKind.NUMBER, "id", true), ribbonField("text", FieldKind.TEXT, "text", true)), large = true),
        ribbonCmd("review.editComment", "Edit Comment", "comment", fields = listOf(ribbonField("id", FieldKind.NUMBER, "id", true), ribbonField("text", FieldKind.TEXT, "text", true))),
        ribbonCmd("review.deleteComment", "Delete", "comment", fields = listOf(ribbonField("id", FieldKind.NUMBER, "id", false), ribbonField("all", FieldKind.BOOL, "all", false))),
        ribbonCmd("review.resolveComment", "Resolve", "comment"),
        ribbonCmd("review.nextComment", "Next", "comment"),
        ribbonCmd("review.previousComment", "Previous", "comment"),
        ribbonCmd("review.comments", "Show Comments", "comment", large = true),
        ribbonCmd("view.commentsPane", "Comments Pane", "comment"),
    )),
    RibbonGroup("Tracking", listOf(
        ribbonCmd("review.trackChanges", "Track Changes", "trackChanges", fields = listOf(ribbonField("value", FieldKind.BOOL, "value", false)), large = true),
        ribbonCmd("review.markup", "Display for Review", "trackChanges"),
        ribbonCmd("review.trackingOptions", "Track Changes Options", "trackChanges", fields = listOf(ribbonField("comments", FieldKind.BOOL, "comments", false), ribbonField("ink", FieldKind.BOOL, "ink", false), ribbonField("insertionsDeletions", FieldKind.BOOL, "insertionsDeletions", false), ribbonField("formatting", FieldKind.BOOL, "formatting", false), ribbonField("balloons", FieldKind.CHOICE, "balloons", false, null, "revisions", "inline", "commentsAndFormatting"), ribbonField("commentDisplay", FieldKind.CHOICE, "commentDisplay", false, null, "margin", "selected"), ribbonField("insertMark", FieldKind.CHOICE, "insertMark", false, null, "underline", "doubleUnderline", "bold", "italic", "strikethrough", "colorOnly", "none"), ribbonField("insertColor", FieldKind.CHOICE, "insertColor", false, null, "byAuthor", "RRGGBB"), ribbonField("deleteMark", FieldKind.CHOICE, "deleteMark", false, null, "strikethrough", "doubleStrikethrough", "hidden", "caret", "hash", "underline", "doubleUnderline", "bold", "italic", "colorOnly", "none"), ribbonField("deleteColor", FieldKind.CHOICE, "deleteColor", false, null, "byAuthor", "RRGGBB"), ribbonField("changedLines", FieldKind.CHOICE, "changedLines", false, null, "outside", "left", "right", "none"), ribbonField("changedLinesColor", FieldKind.CHOICE, "changedLinesColor", false, null, "auto", "RRGGBB"), ribbonField("trackFormatting", FieldKind.BOOL, "trackFormatting", false), ribbonField("moveFromColor", FieldKind.CHOICE, "moveFromColor", false, null, "byAuthor", "RRGGBB"), ribbonField("moveToColor", FieldKind.CHOICE, "moveToColor", false, null, "byAuthor", "RRGGBB"), ribbonField("trackMoves", FieldKind.BOOL, "trackMoves", false))),
        ribbonCmd("review.changes", "Reviewing Pane", "trackChanges"),
        ribbonCmd("review.showMarkup", "Show Markup", "trackChanges", large = true),
    )),
    RibbonGroup("Changes", listOf(
        ribbonCmd("review.acceptAll", "Accept All Changes", "accept"),
        ribbonCmd("review.rejectAll", "Reject All Changes", "reject"),
        ribbonCmd("review.accept", "Accept", "accept", large = true),
        ribbonCmd("review.reject", "Reject", "reject", large = true),
        ribbonCmd("review.nextChange", "Next Change", "trackChanges"),
        ribbonCmd("review.previousChange", "Previous Change", "trackChanges"),
    )),
    RibbonGroup("Proofing", listOf(
        ribbonCmd("review.wordCount", "Word Count", "wordCount", fields = listOf(ribbonField("includeTextBoxes", FieldKind.BOOL, "includeTextBoxes", false)), large = true),
        ribbonCmd("review.spelling", "Spelling & Grammar", "proofing", large = true),
        ribbonCmd("review.issues", "Proofing Issues", "proofing"),
        ribbonCmd("review.suggestions", "Spelling Suggestions", "proofing"),
        ribbonCmd("review.addToDictionary", "Add to Dictionary", "proofing", fields = listOf(ribbonField("word", FieldKind.TEXT, "word", false))),
        ribbonCmd("review.ignoreAll", "Ignore All", "proofing"),
        ribbonCmd("review.applySuggestion", "Change", "proofing", fields = listOf(ribbonField("text", FieldKind.TEXT, "text", true))),
        ribbonCmd("review.thesaurus", "Thesaurus", "thesaurus", large = true),
    )),
    RibbonGroup("Speech", listOf(
        ribbonCmd("review.readAloud", "Read Aloud", "readAloud", fields = listOf(ribbonField("skipCitations", FieldKind.BOOL, "skipCitations", false)), large = true),
        ribbonCmd("readAloud.playPause", "Play/Pause", "readAloud"),
        ribbonCmd("readAloud.next", "Next Sentence", "readAloud"),
        ribbonCmd("readAloud.previous", "Previous Sentence", "readAloud"),
        ribbonCmd("readAloud.stop", "Stop Reading", "readAloud"),
        ribbonCmd("readAloud.speed", "Reading Speed", "readAloud", fields = listOf(ribbonField("value", FieldKind.NUMBER, "value", true))),
        ribbonCmd("readAloud.skipCitations", "Skip Citations and Bibliography", "readAloud", fields = listOf(ribbonField("value", FieldKind.BOOL, "value", false))),
        ribbonCmd("readAloud.status", "Read Aloud Status", "readAloud"),
    )),
    RibbonGroup("Compare", listOf(
        ribbonCmd("review.compare", "Compare", "compare", appAction = AppActionKind.PICK_DOCUMENT, large = true),
        ribbonCmd("review.combine", "Combine", "compare", appAction = AppActionKind.PICK_DOCUMENT, large = true),
    )),
    RibbonGroup("Accessibility", listOf(
        ribbonCmd("file.accessibility", "Check Accessibility", "accessibility", large = true),
        ribbonCmd("view.accessibilityPane", "Accessibility Pane", "accessibility", fields = listOf(ribbonField("value", FieldKind.BOOL, "value", false))),
        ribbonCmd("view.altTextPane", "Alt Text Pane", "accessibility", fields = listOf(ribbonField("value", FieldKind.BOOL, "value", false))),
    )),
    RibbonGroup("Language", listOf(
        ribbonCmd("review.language", "Language", "language", fields = listOf(ribbonField("lang", FieldKind.CHOICE, "lang", false, null, "en-US", "en-GB", "pl-PL", "fr-FR"), ribbonField("noProof", FieldKind.BOOL, "noProof", false), ribbonField("detect", FieldKind.BOOL, "detect", false), ribbonField("default", FieldKind.BOOL, "default", false)), large = true),
    )),
)

object EngineRibbon {
    /** Tab -> its captioned ribbon groups, in the engine's own order. */
    val tabs: Map<String, List<RibbonGroup>> = mapOf(
        "Insert" to insertRibbon,
        "Draw" to drawRibbon,
        "Design" to designRibbon,
        "Layout" to layoutRibbon,
        "References" to referencesRibbon,
        "Mailings" to mailingsRibbon,
        "Review" to reviewRibbon,
    )
}
