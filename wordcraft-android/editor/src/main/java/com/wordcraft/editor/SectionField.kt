package com.wordcraft.editor

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wordcraft.documentcore.EditorState
import com.wordcraft.documentcore.ParaVm
import com.wordcraft.documentcore.SectionMapper
import com.wordcraft.documentcore.SENTINEL
import com.wordcraft.documentcore.SpanVm
import com.wordcraft.documentcore.clampUtf16Boundary
import com.wordcraft.documentcore.diffText
import com.wordcraft.documentcore.planOps
import com.wordcraft.documentcore.utf8ToUtf16
import com.wordcraft.ui.parseHexColor
import kotlin.math.roundToInt

/** pt → dp-ish display units (document points rendered ~1:1 at mdpi, scaled for reading). */
private fun pt(v: Float): Float = v * 1.33f

/**
 * One editable run of consecutive paragraphs sharing a style/alignment/list signature,
 * rendered as a single text field (paragraphs joined with `'\n'`) behind a zero-width sentinel.
 */
@Composable
fun SectionField(
    item: SectionItem,
    state: EditorState,
    modifier: Modifier = Modifier,
) {
    val paras = item.paras
    val mapper = remember(item.key, paras) { SectionMapper(paras.map { it.text }) }
    val pathOf: (Int) -> List<Int> = { i -> paras[i].path }

    val focusPath = state.selection.focus.path
    val focused = paras.any { it.path == focusPath }
    val focusRequester = remember { FocusRequester() }
    var lastFocused by remember { mutableStateOf(false) }

    // Field value: sentinel + joined body text. Engine truth wins when the text differs;
    // when it matches, the local value (incl. IME composition) is left untouched.
    var fieldValue by remember(item.key) {
        mutableStateOf(TextFieldValue(SENTINEL + mapper.body, TextRange(SENTINEL.length)))
    }
    LaunchedEffect(item.key, paras) {
        val engineText = SENTINEL + mapper.body
        if (fieldValue.text != engineText) {
            val caret = if (focused) fieldOffsetFromPos(state, mapper, paras) ?: SENTINEL.length else {
                fieldValue.selection.start.coerceIn(0, engineText.length)
            }
            fieldValue = TextFieldValue(engineText, TextRange(caret.coerceIn(0, engineText.length)))
        } else if (focused && fieldValue.composition == null) {
            // Text unchanged: still mirror engine-driven selection moves (find next, select all).
            val a = fieldOffsetOf(state.selection.anchor, mapper, paras)
            val b = fieldOffsetOf(state.selection.focus, mapper, paras)
            val want = when {
                a != null && b != null -> TextRange(a, b)
                b != null -> TextRange(b, b)
                else -> null
            }
            if (want != null && want != fieldValue.selection) {
                fieldValue = fieldValue.copy(selection = want)
            }
        }
    }
    LaunchedEffect(focused) {
        if (focused && !lastFocused) {
            focusRequester.requestFocus()
        } else if (!focused && lastFocused) {
            fieldValue = fieldValue.copy(composition = null)
        }
        lastFocused = focused
    }

    val first = paras.first()
    val baseSize = 16.sp
    val ls = first.lineSpacing
    val lineHeightMul = if (ls != null && ls.kind == "multiple") ls.value else 1.35f

    val textStyle = TextStyle(
        fontSize = baseSize,
        lineHeight = (baseSize.value * 1.3f * lineHeightMul).sp,
        textAlign = alignOf(first.align),
        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
    )

    val transformation = remember(item.key, paras) { spanTransformation(paras, mapper) }

    val onValueChange: (TextFieldValue) -> Unit = { newValue ->
        val hadSentinel = newValue.text.startsWith(SENTINEL)
        val newBody = if (hadSentinel) newValue.text.substring(1) else newValue.text
        val oldBody = fieldValue.text.let { if (it.startsWith(SENTINEL)) it.substring(1) else it }

        val edit = diffText(oldBody, newBody)
        if (edit != null) {
            EditorController.dispatch(planOps(edit, mapper, pathOf, state.selection))
        } else if (hadSentinel && newValue.selection != fieldValue.selection) {
            // Selection-only change: mirror it into the engine (pure command, no undo step).
            val bodySel = TextRange(
                (newValue.selection.start - 1).coerceAtLeast(0),
                (newValue.selection.end - 1).coerceAtLeast(0),
            )
            val a = mapper.posOf(bodySel.min, pathOf)
            val b = mapper.posOf(bodySel.max, pathOf)
            if (state.selection.anchor != a || state.selection.focus != b) {
                EditorController.dispatch(listOf(com.wordcraft.documentcore.selectRangeOp(a, b)))
            }
        }
        // Keep the local value in sync; restore the sentinel if it was swept away.
        val text = if (hadSentinel) newValue.text else SENTINEL + newBody
        val shift = if (hadSentinel) 0 else 1
        fieldValue = newValue.copy(
            text = text,
            selection = TextRange(
                (newValue.selection.start + shift).coerceIn(0, text.length),
                (newValue.selection.end + shift).coerceIn(0, text.length),
            ),
        )
    }

    val content: @Composable () -> Unit = {
        BasicTextField(
            value = fieldValue,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            textStyle = textStyle,
            visualTransformation = transformation,
            cursorBrush = SolidColor(androidx.compose.material3.MaterialTheme.colorScheme.primary),
            // The soft keyboard's Enter inserts '\n', which planOps turns into newParagraph.
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = pt(first.spaceBefore).dp.coerceAtLeast(0.dp), bottom = pt(first.spaceAfter).dp),
    ) {
        if (first.rtl) {
            androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Content(first, content)
            }
        } else {
            Content(first, content)
        }
        if (first.imgs.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                first.imgs.forEach { img -> InlineImage(img) }
            }
        }
    }
}

/** List marker / hanging indent gutter to the left of the field. */
@Composable
private fun Content(first: ParaVm, field: @Composable () -> Unit) {
    val list = first.list
    val indentDp = pt(if (list != null) list.indent else first.indentLeft).dp
    Row(modifier = Modifier.fillMaxWidth()) {
        if (list != null) {
            val hang = pt(list.hanging).dp
            Box(
                modifier = Modifier.width(hang.coerceAtLeast(18.dp)),
                contentAlignment = Alignment.CenterEnd,
            ) {
                androidx.compose.material3.Text(
                    text = list.label,
                    style = TextStyle(fontSize = 16.sp),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                )
            }
        } else if (indentDp.value > 0f) {
            Spacer(Modifier.width(indentDp))
        }
        Box(modifier = Modifier.weight(1f)) { field() }
    }
}

@Composable
private fun InlineImage(img: com.wordcraft.documentcore.ImageVm) {
    val bmp = remember(img.b64) {
        try {
            val bytes = Base64.decode(img.b64 ?: "", Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Exception) {
            null
        }
    }
    if (bmp != null) {
        val w = pt(img.w).dp
        val h = pt(img.h).dp
        Image(bitmap = bmp.asImageBitmap(), contentDescription = img.alt.ifEmpty { "image" }, modifier = Modifier.width(w).height(h))
    } else {
        Box(
            modifier = Modifier
                .width(pt(img.w).dp)
                .height(pt(img.h).dp)
                .background(androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.material3.Text("🖼", fontSize = 18.sp)
        }
    }
}

/** Engine position → field offset inside this section (null when the position is elsewhere). */
private fun fieldOffsetOf(
    pos: com.wordcraft.documentcore.PosVm,
    mapper: SectionMapper,
    paras: List<ParaVm>,
): Int? {
    if (pos.story != "body") return null
    val idx = paras.indexOfFirst { it.path == pos.path }
    if (idx < 0) return null
    val local = utf8ToUtf16(paras[idx].text, pos.off)
    return mapper.offsetOf(idx, local) + SENTINEL.length
}

/** Engine caret position → field offset inside this section (null when not here). */
private fun fieldOffsetFromPos(state: EditorState, mapper: SectionMapper, paras: List<ParaVm>): Int? =
    fieldOffsetOf(state.selection.focus, mapper, paras)

private fun alignOf(align: String): TextAlign = when (align) {
    "center" -> TextAlign.Center
    "right" -> TextAlign.End
    "justify" -> TextAlign.Justify
    "distribute" -> TextAlign.Justify
    else -> TextAlign.Start
}

/** Per-run styling: engine spans (paragraph-local, body coords) → field spans (+1 sentinel). */
private fun spanTransformation(paras: List<ParaVm>, mapper: SectionMapper): VisualTransformation =
    object : VisualTransformation {
        override fun filter(text: AnnotatedString): TransformedText {
            val builder = AnnotatedString.Builder(text.length)
            builder.append(text.text)
            for ((pi, para) in paras.withIndex()) {
                val base = mapper.startOf(pi) + SENTINEL.length
                for (span in para.spans) {
                    val s = (base + span.start).coerceIn(0, text.length)
                    val e = (base + span.end).coerceIn(0, text.length)
                    if (e <= s) continue
                    builder.addStyle(spanToStyle(span), s, e)
                }
            }
            return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
        }
    }

private fun spanToStyle(span: SpanVm): SpanStyle {
    var style = SpanStyle(
        fontWeight = if (span.bold) FontWeight.Bold else null,
        fontStyle = if (span.italic) FontStyle.Italic else null,
        textDecoration = when {
            span.underline != null && span.strike -> TextDecoration.combine(
                listOf(TextDecoration.Underline, TextDecoration.LineThrough),
            )
            span.underline != null -> TextDecoration.Underline
            span.strike -> TextDecoration.LineThrough
            else -> null
        },
        fontSize = span.sizePt?.let { (it * 1.33f).sp } ?: TextUnit.Unspecified,
        fontFamily = span.font?.let { mapFontFamily(it) },
        color = span.color?.let { parseHexColor(it) } ?: Color.Unspecified,
        background = span.highlight?.let { parseHexColor(it) } ?: Color.Unspecified,
    )
    if (span.vertAlign == "sup") {
        style = style.copy(
            fontSize = span.sizePt?.let { (it * 0.9f).sp } ?: 13.sp,
            baselineShift = androidx.compose.ui.text.style.BaselineShift.Superscript,
        )
    } else if (span.vertAlign == "sub") {
        style = style.copy(
            fontSize = span.sizePt?.let { (it * 0.9f).sp } ?: 13.sp,
            baselineShift = androidx.compose.ui.text.style.BaselineShift.Subscript,
        )
    }
    return style
}

/** Font families by name; unknown names fall back to the platform default. */
private fun mapFontFamily(name: String): androidx.compose.ui.text.font.FontFamily? = when (name.lowercase()) {
    "times new roman", "times", "georgia", "serif", "cambria", "caladea", "liberation serif" -> androidx.compose.ui.text.font.FontFamily.Serif
    "calibri", "carlito", "arial", "helvetica", "liberation sans", "aptos", "sans-serif" -> androidx.compose.ui.text.font.FontFamily.SansSerif
    "courier new", "consolas", "liberation mono", "monospace" -> androidx.compose.ui.text.font.FontFamily.Monospace
    "comic sans ms", "cursive" -> androidx.compose.ui.text.font.FontFamily.Cursive
    else -> null
}
