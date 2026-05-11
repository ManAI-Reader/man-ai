package com.highliuk.manai.ui.reader

import android.text.Spanned
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.highliuk.manai.domain.model.FuriganaPart
import com.highliuk.manai.domain.model.FuriganaToken
import com.highliuk.manai.domain.model.PageRegion
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class OcrBottomSheetFuriganaTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

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
