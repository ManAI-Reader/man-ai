package com.highliuk.manai.ui.reader

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TapHandlerTest {

    @Test
    fun `tap left on first page toggles bars instead of navigating`() {
        var barsToggled = false
        var navigatedTo: Int? = null
        val handler = TapHandler(
            tapToNavigate = true, isZoomed = false, isRtl = false,
            currentPage = 0, pageCount = 10
        )
        handler.handle(
            offset = Offset(50f, 500f),
            containerWidth = 900f,
            toggleBars = { barsToggled = true },
            navigateToPage = { navigatedTo = it }
        )
        assertTrue(barsToggled)
        assertNull(navigatedTo)
    }

    @Test
    fun `tap right on last page toggles bars instead of navigating`() {
        var barsToggled = false
        var navigatedTo: Int? = null
        val handler = TapHandler(
            tapToNavigate = true, isZoomed = false, isRtl = false,
            currentPage = 9, pageCount = 10
        )
        handler.handle(
            offset = Offset(850f, 500f),
            containerWidth = 900f,
            toggleBars = { barsToggled = true },
            navigateToPage = { navigatedTo = it }
        )
        assertTrue(barsToggled)
        assertNull(navigatedTo)
    }

    @Test
    fun `tap left on first page in RTL navigates to next page`() {
        var navigatedTo: Int? = null
        val handler = TapHandler(
            tapToNavigate = true, isZoomed = false, isRtl = true,
            currentPage = 0, pageCount = 10
        )
        handler.handle(
            offset = Offset(50f, 500f),
            containerWidth = 900f,
            toggleBars = {},
            navigateToPage = { navigatedTo = it }
        )
        assertEquals(1, navigatedTo)
    }

    @Test
    fun `tap right on last page in RTL navigates to previous page`() {
        var navigatedTo: Int? = null
        val handler = TapHandler(
            tapToNavigate = true, isZoomed = false, isRtl = true,
            currentPage = 9, pageCount = 10
        )
        handler.handle(
            offset = Offset(850f, 500f),
            containerWidth = 900f,
            toggleBars = {},
            navigateToPage = { navigatedTo = it }
        )
        assertEquals(8, navigatedTo)
    }
}
