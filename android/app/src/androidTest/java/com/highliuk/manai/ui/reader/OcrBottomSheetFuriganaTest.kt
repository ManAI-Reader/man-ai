package com.highliuk.manai.ui.reader

import android.text.Spanned
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import com.highliuk.manai.domain.model.FuriganaPart
import com.highliuk.manai.domain.model.FuriganaToken
import com.highliuk.manai.domain.model.PageRegion
import com.highliuk.manai.domain.model.PromptTemplate
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class OcrBottomSheetFuriganaTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val explainTemplate = PromptTemplate(
        id = 1L,
        name = "Explain",
        template = "Explain {text}",
    )

    @Test
    fun furiganaTokensRendersSpannableInTextView() {
        val region = PageRegion(0, 0.1f, 0.1f, 0.5f, 0.5f, 0.9f, "食べる")
        val tokens = listOf(
            FuriganaToken("食べる", "タベル", listOf(
                FuriganaPart.kanji("食", "た"),
                FuriganaPart.kana("べ"),
                FuriganaPart.kana("る")
            ))
        )

        composeTestRule.setContent {
            OcrBottomSheet(
                region = region,
                furiganaTokens = tokens,
                onDismiss = {},
            )
        }

        composeTestRule.onNodeWithTag("ocr_text").assertExists()
        composeTestRule.waitForIdle()

        var hasRubySpan = false
        composeTestRule.activityRule.scenario.onActivity { activity ->
            val textView = findTextView(activity.window.decorView)
            val spanned = textView?.text as? Spanned
            hasRubySpan = spanned?.getSpans(0, spanned.length, RubySpan::class.java)
                ?.isNotEmpty() == true
        }

        assertTrue("Text should have RubySpan when furigana tokens provided", hasRubySpan)
    }

    @Test
    fun singleTapInsideTokenSelectsWholeWord() {
        val region = PageRegion(0, 0.1f, 0.1f, 0.5f, 0.5f, 0.9f, "私は食べる")
        val tokens = listOf(
            FuriganaToken("私", "ワタシ", listOf(FuriganaPart.kanji("私", "わたし"))),
            FuriganaToken("は", null, listOf(FuriganaPart.kana("は"))),
            FuriganaToken(
                "食べる",
                "タベル",
                listOf(
                    FuriganaPart.kanji("食", "た"),
                    FuriganaPart.kana("べ"),
                    FuriganaPart.kana("る"),
                ),
            ),
        )

        composeTestRule.setContent {
            OcrBottomSheet(
                region = region,
                furiganaTokens = tokens,
                // Through the public API: the AndroidView update block
                // reassigns view.selectionPrompts on every recomposition, so
                // setting them directly on the view would race with it.
                promptTemplates = listOf(explainTemplate),
                onDismiss = {},
            )
        }

        composeTestRule.onNodeWithTag("ocr_text").assertExists()
        composeTestRule.waitForIdle()

        // Compute the tap point (view-local px, same space as the Compose
        // node hosting the AndroidView) at the middle of the 食べる token
        // (chars 2..5).
        var tapPoint = Offset.Zero
        composeTestRule.activityRule.scenario.onActivity { activity ->
            val textView = findTextView(activity.window.decorView) as? SelectableOcrTextView
            requireNotNull(textView) { "SelectableOcrTextView not found" }
            val layout = requireNotNull(textView.layout) { "TextView has no layout yet" }
            val line = layout.getLineForOffset(2)
            val x = (layout.getPrimaryHorizontal(2) + layout.getPrimaryHorizontal(5)) / 2f +
                textView.totalPaddingLeft
            val y = (layout.getLineTop(line) + layout.getLineBottom(line)) / 2f +
                textView.totalPaddingTop
            tapPoint = Offset(x, y)
        }

        // Drive a REAL tap: Editor.selectCurrentWord() refuses selections when
        // standard touch handling was bypassed (min/max touch offsets unset),
        // so the touch stream must be genuine for the word selection to start.
        composeTestRule.onNodeWithTag("ocr_text").performTouchInput { click(tapPoint) }

        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            var selection: Pair<Int, Int>? = null
            composeTestRule.activityRule.scenario.onActivity { activity ->
                val textView = findTextView(activity.window.decorView) as? SelectableOcrTextView
                selection = textView?.let { it.selectionStart to it.selectionEnd }
            }
            selection == 2 to 5
        }
    }

    @Test
    fun singleTapOutsideTokensSelectsNothing() {
        val region = PageRegion(0, 0.1f, 0.1f, 0.5f, 0.5f, 0.9f, "食べる")
        val tokens = listOf(
            FuriganaToken(
                "食べる",
                "タベル",
                listOf(
                    FuriganaPart.kanji("食", "た"),
                    FuriganaPart.kana("べ"),
                    FuriganaPart.kana("る"),
                ),
            ),
        )

        composeTestRule.setContent {
            OcrBottomSheet(
                region = region,
                furiganaTokens = tokens,
                // Prompts are configured (via the public API, see above), so
                // the refusal below comes from the offset being outside every
                // token, not from the empty-prompts guard.
                promptTemplates = listOf(explainTemplate),
                onDismiss = {},
            )
        }

        composeTestRule.onNodeWithTag("ocr_text").assertExists()
        composeTestRule.waitForIdle()

        var handled = true
        var hasSelection = true
        composeTestRule.activityRule.scenario.onActivity { activity ->
            val textView = findTextView(activity.window.decorView) as? SelectableOcrTextView
            requireNotNull(textView) { "SelectableOcrTextView not found" }
            // Offset 3 == text length: past the last char, outside every
            // token. handleSingleTap returns before touching the Editor, so
            // no real touch stream is needed for this negative case.
            handled = textView.handleSingleTap(3)
            hasSelection = textView.selectionStart != textView.selectionEnd
        }
        composeTestRule.waitForIdle()

        assertTrue("Tap outside tokens must not be handled", !handled)
        assertTrue("Tap outside tokens must not create a selection", !hasSelection)
    }

    @Test
    fun noFuriganaTokensRendersPlainText() {
        val region = PageRegion(0, 0.1f, 0.1f, 0.5f, 0.5f, 0.9f, "食べる")

        composeTestRule.setContent {
            OcrBottomSheet(
                region = region,
                furiganaTokens = null,
                onDismiss = {},
            )
        }

        composeTestRule.onNodeWithTag("ocr_text").assertExists()
        composeTestRule.waitForIdle()

        var hasRubySpan = false
        composeTestRule.activityRule.scenario.onActivity { activity ->
            val textView = findTextView(activity.window.decorView)
            val spanned = textView?.text as? android.text.Spanned
            hasRubySpan = spanned?.getSpans(0, spanned.length, RubySpan::class.java)
                ?.isNotEmpty() == true
        }

        assertTrue("Text should have no RubySpan when no furigana tokens", !hasRubySpan)
    }
}

private fun findTextView(view: View): TextView? = when {
    view is SelectableOcrTextView -> view
    view is TextView && view.text?.toString()?.contains("食べる") == true -> view
    view is ViewGroup -> (0 until view.childCount)
        .firstNotNullOfOrNull { findTextView(view.getChildAt(it)) }
    else -> null
}
