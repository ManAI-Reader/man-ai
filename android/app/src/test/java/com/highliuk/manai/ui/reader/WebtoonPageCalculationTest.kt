package com.highliuk.manai.ui.reader

import org.junit.Assert.assertEquals
import org.junit.Test

class WebtoonPageCalculationTest {

    @Test
    fun `when can scroll forward returns firstVisibleItemIndex`() {
        val result = computeWebtoonCurrentPage(
            firstVisibleItemIndex = 5,
            canScrollForward = true,
            lastVisibleItemIndex = 6,
        )
        assertEquals(5, result)
    }

    @Test
    fun `when cannot scroll forward returns last visible item index`() {
        val result = computeWebtoonCurrentPage(
            firstVisibleItemIndex = 8,
            canScrollForward = false,
            lastVisibleItemIndex = 9,
        )
        assertEquals(9, result)
    }

    @Test
    fun `when cannot scroll forward and no visible items returns firstVisibleItemIndex`() {
        val result = computeWebtoonCurrentPage(
            firstVisibleItemIndex = 8,
            canScrollForward = false,
            lastVisibleItemIndex = null,
        )
        assertEquals(8, result)
    }

    @Test
    fun `last page of 10 page document is reachable`() {
        // Simulates: 10-page PDF, scrolled to bottom, last page visible but not first
        val result = computeWebtoonCurrentPage(
            firstVisibleItemIndex = 8,
            canScrollForward = false,
            lastVisibleItemIndex = 9,
        )
        assertEquals(9, result)
    }

    @Test
    fun `single page document shows page 0`() {
        val result = computeWebtoonCurrentPage(
            firstVisibleItemIndex = 0,
            canScrollForward = false,
            lastVisibleItemIndex = 0,
        )
        assertEquals(0, result)
    }

    @Test
    fun `middle of document still uses firstVisibleItemIndex`() {
        val result = computeWebtoonCurrentPage(
            firstVisibleItemIndex = 3,
            canScrollForward = true,
            lastVisibleItemIndex = 5,
        )
        assertEquals(3, result)
    }
}
