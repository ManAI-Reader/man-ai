package com.highliuk.manai.ui.reader

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.highliuk.manai.domain.model.PageRegion
import com.highliuk.manai.domain.model.PromptTemplate
import org.junit.Rule
import org.junit.Test

class OcrBottomSheetPromptsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val region = PageRegion(0, 0.1f, 0.1f, 0.5f, 0.5f, 0.9f, "テスト")

    private val explain = PromptTemplate(id = 1L, name = "Explain", template = "Explain {text}")
    private val compare = PromptTemplate(
        id = 2L,
        name = "Compare",
        template = "Compare {text} with {translation}",
    )

    @Test
    fun promptChipsAreNeverShownEvenWithTemplates() {
        composeTestRule.setContent {
            OcrBottomSheet(
                region = region,
                onDismiss = {},
                promptTemplates = listOf(explain, compare),
                translationState = ReaderViewModel.TranslationState.Translated("Test"),
            )
        }

        composeTestRule.onNodeWithTag("prompt_chips").assertDoesNotExist()
        composeTestRule.onNodeWithTag("prompt_chip_1").assertDoesNotExist()
        composeTestRule.onNodeWithTag("prompt_chip_2").assertDoesNotExist()
    }

    @Test
    fun ocrTextRemainsDisplayedWhenTemplatesAreProvided() {
        composeTestRule.setContent {
            OcrBottomSheet(
                region = region,
                onDismiss = {},
                promptTemplates = listOf(explain),
            )
        }

        composeTestRule.onNodeWithTag("ocr_text").assertIsDisplayed()
    }
}
