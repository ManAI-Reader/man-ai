package com.highliuk.manai.ui.reader

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class GoToPageDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun dialog_displaysTitle() {
        composeTestRule.setContent {
            GoToPageDialog(
                onConfirm = {},
                onDismiss = {}
            )
        }
        composeTestRule.onNodeWithText("Go to page").assertIsDisplayed()
    }

    @Test
    fun textField_hasAutoFocus() {
        composeTestRule.setContent {
            GoToPageDialog(
                onConfirm = {},
                onDismiss = {}
            )
        }
        composeTestRule.onNodeWithTag("go_to_page_input").assertIsFocused()
    }

    @Test
    fun confirm_callsOnConfirmWithEnteredValue() {
        var confirmedPage = -1
        composeTestRule.setContent {
            GoToPageDialog(
                onConfirm = { confirmedPage = it },
                onDismiss = {}
            )
        }
        composeTestRule.onNodeWithTag("go_to_page_input").performTextInput("7")
        composeTestRule.onNodeWithText("OK").performClick()
        assertEquals(7, confirmedPage)
    }

    @Test
    fun dismiss_callsOnDismiss() {
        var dismissed = false
        composeTestRule.setContent {
            GoToPageDialog(
                onConfirm = {},
                onDismiss = { dismissed = true }
            )
        }
        composeTestRule.onNodeWithText("Cancel").performClick()
        assertTrue(dismissed)
    }

    @Test
    fun confirm_withEmptyInput_doesNotCallOnConfirm() {
        var confirmCalled = false
        composeTestRule.setContent {
            GoToPageDialog(
                onConfirm = { confirmCalled = true },
                onDismiss = {}
            )
        }
        composeTestRule.onNodeWithText("OK").performClick()
        assertTrue(!confirmCalled)
    }

    @Test
    fun confirm_withNonNumericInput_doesNotCallOnConfirm() {
        var confirmCalled = false
        composeTestRule.setContent {
            GoToPageDialog(
                onConfirm = { confirmCalled = true },
                onDismiss = {}
            )
        }
        composeTestRule.onNodeWithTag("go_to_page_input").performTextInput("abc")
        composeTestRule.onNodeWithText("OK").performClick()
        assertTrue(!confirmCalled)
    }
}
