package com.highliuk.manai.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import com.highliuk.manai.domain.model.ThemeMode
import com.highliuk.manai.ui.theme.ManAiTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ReaderBottomBarThemeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun bottomBarBackgroundIsLightInLightTheme() {
        composeTestRule.setContent {
            ManAiTheme(themeMode = ThemeMode.LIGHT, dynamicColor = false) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    ReaderBottomBar(
                        currentPage = 0,
                        pageCount = 5,
                        onPageSelected = {}
                    )
                }
            }
        }

        composeTestRule.waitForIdle()

        val image = composeTestRule.onNode(hasTestTag("reader_bottom_bar"))
            .captureToImage()
        val pixelMap = image.toPixelMap()
        val pixelColor: Color = pixelMap[2, 2]

        assertTrue(
            "Background should be light in light theme but got red=${pixelColor.red}",
            pixelColor.red > 0.5f
        )
    }
}
