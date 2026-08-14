package com.highliuk.manai.ui.reader

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.highliuk.manai.domain.model.PageRegion
import com.highliuk.manai.domain.model.PromptTemplate
import org.junit.Assert.assertEquals
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
    fun promptChipsAreShownForRegionWithText() {
        composeTestRule.setContent {
            OcrBottomSheet(
                region = region,
                onDismiss = {},
                promptTemplates = listOf(explain),
            )
        }

        composeTestRule.onNodeWithTag("prompt_chips").assertIsDisplayed()
        composeTestRule.onNodeWithTag("prompt_chip_1").assertIsDisplayed()
    }

    @Test
    fun translationTemplateIsHiddenWhenTranslationIdle() {
        composeTestRule.setContent {
            OcrBottomSheet(
                region = region,
                onDismiss = {},
                promptTemplates = listOf(explain, compare),
                translationState = ReaderViewModel.TranslationState.Idle,
            )
        }

        composeTestRule.onNodeWithTag("prompt_chip_1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("prompt_chip_2").assertDoesNotExist()
    }

    @Test
    fun translationTemplateIsShownWhenTranslated() {
        composeTestRule.setContent {
            OcrBottomSheet(
                region = region,
                onDismiss = {},
                promptTemplates = listOf(explain, compare),
                translationState = ReaderViewModel.TranslationState.Translated("Test"),
            )
        }

        composeTestRule.onNodeWithTag("prompt_chip_2").assertIsDisplayed()
    }

    @Test
    fun clickingChipInvokesCallbackWithTemplate() {
        var clicked: PromptTemplate? = null

        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            OcrBottomSheet(
                region = region,
                onDismiss = {},
                promptTemplates = listOf(explain),
                onPromptClick = { clicked = it },
            )
        }
        composeTestRule.mainClock.advanceTimeBy(1000)
        composeTestRule.mainClock.autoAdvance = true
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("prompt_chip_1").performClick()
        composeTestRule.waitForIdle()

        assertEquals(explain, clicked)
    }

    @Test
    fun noChipsContainerWhenNoTemplates() {
        composeTestRule.setContent {
            OcrBottomSheet(
                region = region,
                onDismiss = {},
                promptTemplates = emptyList(),
            )
        }

        composeTestRule.onNodeWithTag("prompt_chips").assertDoesNotExist()
    }
}
