package com.highliuk.manai.ui.reader

import androidx.compose.ui.layout.ContentScale
import org.junit.Assert.assertEquals
import org.junit.Test

class ContentScaleChooserTest {

    @Test
    fun `portrait image in portrait container returns FillWidth`() {
        val result = chooseContentScale(
            imageWidth = 1000f,
            imageHeight = 1500f,
            containerWidth = 1080f,
            containerHeight = 1920f
        )
        assertEquals(ContentScale.FillWidth, result)
    }

    @Test
    fun `portrait image in landscape container returns FillHeight`() {
        val result = chooseContentScale(
            imageWidth = 1000f,
            imageHeight = 1500f,
            containerWidth = 1920f,
            containerHeight = 1080f
        )
        assertEquals(ContentScale.FillHeight, result)
    }

    @Test
    fun `wide image in portrait container returns FillHeight`() {
        val result = chooseContentScale(
            imageWidth = 2000f,
            imageHeight = 1500f,
            containerWidth = 1080f,
            containerHeight = 1920f
        )
        assertEquals(ContentScale.FillHeight, result)
    }

    @Test
    fun `square image in landscape container returns FillHeight`() {
        val result = chooseContentScale(
            imageWidth = 1000f,
            imageHeight = 1000f,
            containerWidth = 1920f,
            containerHeight = 1080f
        )
        assertEquals(ContentScale.FillHeight, result)
    }

    @Test
    fun `image matching container aspect ratio returns FillWidth`() {
        val result = chooseContentScale(
            imageWidth = 1080f,
            imageHeight = 1920f,
            containerWidth = 540f,
            containerHeight = 960f
        )
        assertEquals(ContentScale.FillWidth, result)
    }

    @Test
    fun `zero container height returns FillWidth as fallback`() {
        val result = chooseContentScale(
            imageWidth = 1000f,
            imageHeight = 1500f,
            containerWidth = 1080f,
            containerHeight = 0f
        )
        assertEquals(ContentScale.FillWidth, result)
    }

    @Test
    fun `zero image dimensions returns FillWidth as fallback`() {
        val result = chooseContentScale(
            imageWidth = 0f,
            imageHeight = 0f,
            containerWidth = 1080f,
            containerHeight = 1920f
        )
        assertEquals(ContentScale.FillWidth, result)
    }
}
