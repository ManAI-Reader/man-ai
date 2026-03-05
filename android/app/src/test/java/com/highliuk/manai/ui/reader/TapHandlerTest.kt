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

    @Test
    fun `tap while zoomed always toggles bars regardless of position`() {
        var barsToggled = false
        var navigatedTo: Int? = null
        val handler = TapHandler(
            tapToNavigate = true, isZoomed = true, isRtl = false,
            currentPage = 5, pageCount = 10
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
    fun `tap with tapToNavigate disabled always toggles bars`() {
        var barsToggled = false
        var navigatedTo: Int? = null
        val handler = TapHandler(
            tapToNavigate = false, isZoomed = false, isRtl = false,
            currentPage = 5, pageCount = 10
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
    fun `center tap toggles bars`() {
        var barsToggled = false
        var navigatedTo: Int? = null
        val handler = TapHandler(
            tapToNavigate = true, isZoomed = false, isRtl = false,
            currentPage = 5, pageCount = 10
        )
        handler.handle(
            offset = Offset(450f, 500f),
            containerWidth = 900f,
            toggleBars = { barsToggled = true },
            navigateToPage = { navigatedTo = it }
        )
        assertTrue(barsToggled)
        assertNull(navigatedTo)
    }

    @Test
    fun `LTR right tap on middle page navigates forward`() {
        var navigatedTo: Int? = null
        val handler = TapHandler(
            tapToNavigate = true, isZoomed = false, isRtl = false,
            currentPage = 5, pageCount = 10
        )
        handler.handle(
            offset = Offset(850f, 500f),
            containerWidth = 900f,
            toggleBars = {},
            navigateToPage = { navigatedTo = it }
        )
        assertEquals(6, navigatedTo)
    }

    @Test
    fun `LTR left tap on middle page navigates backward`() {
        var navigatedTo: Int? = null
        val handler = TapHandler(
            tapToNavigate = true, isZoomed = false, isRtl = false,
            currentPage = 5, pageCount = 10
        )
        handler.handle(
            offset = Offset(50f, 500f),
            containerWidth = 900f,
            toggleBars = {},
            navigateToPage = { navigatedTo = it }
        )
        assertEquals(4, navigatedTo)
    }
}
