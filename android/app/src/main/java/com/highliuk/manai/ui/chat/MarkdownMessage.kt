package com.highliuk.manai.ui.chat

import android.graphics.Typeface
import android.os.LocaleList
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.highliuk.manai.domain.model.FuriganaToken
import com.highliuk.manai.ui.chat.markdown.FuriganaRunResolver
import com.highliuk.manai.ui.chat.markdown.MarkdownBlock
import com.highliuk.manai.ui.chat.markdown.MarkdownInline
import com.highliuk.manai.ui.chat.markdown.MarkdownParser
import com.highliuk.manai.ui.chat.markdown.RichTextPiece
import com.highliuk.manai.ui.chat.markdown.RichTextPlanner
import com.highliuk.manai.ui.chat.markdown.StyledRun
import com.highliuk.manai.ui.chat.markdown.toStyledRuns
import com.highliuk.manai.ui.reader.RubySpan
import java.util.Locale

/** Furigana resolver: maps a closed Japanese run to its parsed tokens. */
typealias FuriganaResolver = suspend (String) -> List<FuriganaToken>

private val TABLE_CELL_WIDTH = 140.dp
private val BLOCK_SPACING = 12.dp
private val HEADING_EXTRA_TOP_SPACING = 4.dp
private val LIST_INDENT_PER_LEVEL = 16.dp
private val BLOCKQUOTE_BAR_WIDTH = 3.dp
private const val BOLD_WEIGHT_THRESHOLD = 600

// Chat body typography: roomier than M3 bodyLarge (16sp/24sp/0.5sp) for
// long-form reading; the TextView multiplier approximates the same ~26sp
// line height for the furigana-capable native text path.
private val CHAT_BODY_FONT_SIZE = 17.sp
private val CHAT_BODY_LINE_HEIGHT = 26.sp
private val CHAT_BODY_LETTER_SPACING = 0.2.sp
private const val CHAT_LINE_SPACING_MULTIPLIER = 1.3f

/**
 * Renders assistant markdown with ruby furigana over Japanese runs.
 *
 * The raw [text] is re-parsed on every streaming emission; closed Japanese
 * runs are resolved once through [resolveFurigana] and cached, while the
 * still-open tail run renders as plain text until it closes.
 */
@Composable
fun MarkdownMessageContent(
    text: String,
    isComplete: Boolean,
    resolveFurigana: FuriganaResolver?,
    modifier: Modifier = Modifier,
) {
    val blocks = remember(text) { MarkdownParser.parse(text) }
    val furiganaCache = remember { mutableStateMapOf<String, List<FuriganaToken>>() }
    if (resolveFurigana != null) {
        LaunchedEffect(text, isComplete) {
            FuriganaRunResolver.resolveMissing(
                runs = RichTextPlanner.closedDocumentRuns(blocks, isComplete),
                cache = furiganaCache,
                resolve = resolveFurigana,
            )
        }
    }
    val lookup: (String) -> List<FuriganaToken>? =
        if (resolveFurigana != null) {
            { run -> furiganaCache[run] }
        } else {
            { null }
        }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(BLOCK_SPACING)) {
        blocks.forEachIndexed { index, block ->
            MarkdownBlockView(
                block = block,
                isTail = !isComplete && index == blocks.lastIndex,
                lookup = lookup,
            )
        }
    }
}

@Composable
private fun MarkdownBlockView(
    block: MarkdownBlock,
    isTail: Boolean,
    lookup: (String) -> List<FuriganaToken>?,
) {
    when (block) {
        is MarkdownBlock.Paragraph -> FuriganaRichText(
            runs = block.inlines.toStyledRuns(),
            isTail = isTail,
            lookup = lookup,
            style = chatBodyStyle(),
            modifier = Modifier.fillMaxWidth(),
        )

        is MarkdownBlock.Heading -> FuriganaRichText(
            runs = block.inlines.toStyledRuns(),
            isTail = isTail,
            lookup = lookup,
            style = headingStyle(block.level).copy(fontWeight = FontWeight.Bold),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = HEADING_EXTRA_TOP_SPACING),
        )

        is MarkdownBlock.ListItem -> MarkdownListItemView(block, isTail, lookup)

        is MarkdownBlock.Table -> MarkdownTableView(block, lookup)

        is MarkdownBlock.CodeBlock -> MarkdownCodeBlockView(block)

        is MarkdownBlock.HorizontalRule -> HorizontalDivider()

        is MarkdownBlock.Blockquote -> MarkdownBlockquoteView(block, isTail, lookup)
    }
}

@Composable
private fun MarkdownBlockquoteView(
    block: MarkdownBlock.Blockquote,
    isTail: Boolean,
    lookup: (String) -> List<FuriganaToken>?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .testTag("markdown_blockquote"),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(BLOCKQUOTE_BAR_WIDTH)
                .background(MaterialTheme.colorScheme.outlineVariant),
        )
        CompositionLocalProvider(
            LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant,
        ) {
            FuriganaRichText(
                runs = block.inlines.toStyledRuns(),
                isTail = isTail,
                lookup = lookup,
                style = chatBodyStyle(),
                modifier = Modifier
                    .weight(1f)
                    .padding(start = BLOCK_SPACING),
            )
        }
    }
}

@Composable
private fun chatBodyStyle(): TextStyle =
    MaterialTheme.typography.bodyLarge.copy(
        fontSize = CHAT_BODY_FONT_SIZE,
        lineHeight = CHAT_BODY_LINE_HEIGHT,
        letterSpacing = CHAT_BODY_LETTER_SPACING,
    )

@Composable
private fun headingStyle(level: Int): TextStyle = when (level) {
    1 -> MaterialTheme.typography.headlineSmall
    2 -> MaterialTheme.typography.titleLarge
    3 -> MaterialTheme.typography.titleMedium
    else -> MaterialTheme.typography.titleSmall
}

@Composable
private fun MarkdownListItemView(
    item: MarkdownBlock.ListItem,
    isTail: Boolean,
    lookup: (String) -> List<FuriganaToken>?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = LIST_INDENT_PER_LEVEL * item.level),
    ) {
        Text(
            text = if (item.ordered) "${item.index}." else "•",
            style = chatBodyStyle(),
        )
        Spacer(modifier = Modifier.width(BLOCK_SPACING))
        FuriganaRichText(
            runs = item.inlines.toStyledRuns(),
            isTail = isTail,
            lookup = lookup,
            style = chatBodyStyle(),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MarkdownTableView(
    table: MarkdownBlock.Table,
    lookup: (String) -> List<FuriganaToken>?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
    ) {
        MarkdownTableRow(cells = table.header, bold = true, lookup = lookup)
        HorizontalDivider()
        table.rows.forEach { row ->
            MarkdownTableRow(cells = row, bold = false, lookup = lookup)
        }
    }
}

@Composable
private fun MarkdownTableRow(
    cells: List<List<MarkdownInline>>,
    bold: Boolean,
    lookup: (String) -> List<FuriganaToken>?,
) {
    Row {
        cells.forEach { cell ->
            val runs = cell.toStyledRuns().map { run ->
                if (bold) run.copy(bold = true) else run
            }
            FuriganaRichText(
                runs = runs,
                isTail = false,
                lookup = lookup,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .width(TABLE_CELL_WIDTH)
                    .padding(end = BLOCK_SPACING, top = 2.dp, bottom = 2.dp),
            )
        }
    }
}

@Composable
private fun MarkdownCodeBlockView(block: MarkdownBlock.CodeBlock) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(BLOCK_SPACING),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = block.text,
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(BLOCK_SPACING),
        )
    }
}

/**
 * One rich-text unit (paragraph, heading, list item content or table cell)
 * drawn by an Android [TextView] so kanji can carry [RubySpan] furigana,
 * exactly like the reader's OCR sheet.
 */
@Composable
internal fun FuriganaRichText(
    runs: List<StyledRun>,
    isTail: Boolean,
    lookup: (String) -> List<FuriganaToken>?,
    style: TextStyle,
    modifier: Modifier = Modifier,
) {
    val pieces = RichTextPlanner.plan(runs, isTail, lookup)
    val plainText = pieces.joinToString("") { it.text }
    val textColor = LocalContentColor.current
    val fontSizeSp = style.fontSize.value
    // TextView.setLetterSpacing wants em units, Compose styles carry sp.
    val letterSpacingEm = when {
        style.letterSpacing.isSp && fontSizeSp > 0f -> style.letterSpacing.value / fontSizeSp
        style.letterSpacing.isEm -> style.letterSpacing.value
        else -> 0f
    }
    val baseBold = (style.fontWeight?.weight ?: 0) >= BOLD_WEIGHT_THRESHOLD
    AndroidView(
        factory = { ctx ->
            TextView(ctx).apply {
                // An ID is required by TextView.canProcessText(): without it
                // the selection toolbar drops the PROCESS_TEXT actions other
                // apps contribute (dictionaries, translators, ...).
                id = View.generateViewId()
                setTextIsSelectable(true)
                setLineSpacing(0f, CHAT_LINE_SPACING_MULTIPLIER)
                textLocales = LocaleList(Locale("ja"))
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
            }
        },
        update = { view ->
            view.text = buildPieceSpannable(pieces)
            view.setTextColor(textColor.toArgb())
            view.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSizeSp)
            view.letterSpacing = letterSpacingEm
            view.setTypeface(if (baseBold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT)
        },
        modifier = modifier
            .semantics { text = AnnotatedString(plainText) }
            .layout { measurable, constraints ->
                // Reading the current content keeps this lambda capture-bound to it, so
                // the element is recreated whenever the spannable content changes:
                // AndroidView does not remeasure on the TextView's own requestLayout
                // once streaming has settled, so a content-keyed layout element forces
                // Compose to re-read the view size (otherwise the last chat lines stay
                // clipped when furigana rubies raise the line height after measure).
                @Suppress("UNUSED_EXPRESSION")
                pieces
                val placeable = measurable.measure(constraints)
                layout(placeable.width, placeable.height) { placeable.place(0, 0) }
            },
    )
}

internal fun buildPieceSpannable(pieces: List<RichTextPiece>): SpannableStringBuilder {
    val builder = SpannableStringBuilder()
    for (piece in pieces) {
        val start = builder.length
        builder.append(piece.text)
        val end = builder.length
        if (piece.ruby != null) {
            builder.setSpan(RubySpan(piece.ruby), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        styleFlag(piece)?.let { flag ->
            builder.setSpan(StyleSpan(flag), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        if (piece.code) {
            builder.setSpan(
                TypefaceSpan("monospace"),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
        }
    }
    return builder
}

private fun styleFlag(piece: RichTextPiece): Int? = when {
    piece.bold && piece.italic -> Typeface.BOLD_ITALIC
    piece.bold -> Typeface.BOLD
    piece.italic -> Typeface.ITALIC
    else -> null
}
