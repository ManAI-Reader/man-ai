package com.highliuk.manai.ui.theme

import com.highliuk.manai.domain.model.ThemeMode
import com.highliuk.manai.domain.model.isDark
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeModeResolutionTest {

    @Test
    fun `LIGHT returns false for isDark`() {
        val result = ThemeMode.LIGHT.isDark()

        assertFalse(result!!)
    }

    @Test
    fun `DARK returns true for isDark`() {
        val result = ThemeMode.DARK.isDark()

        assertTrue(result!!)
    }

    @Test
    fun `SYSTEM returns null for isDark`() {
        val result = ThemeMode.SYSTEM.isDark()

        assertNull(result)
    }
}
