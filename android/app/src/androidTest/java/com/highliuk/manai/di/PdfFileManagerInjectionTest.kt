package com.highliuk.manai.di

import com.highliuk.manai.data.pdf.PdfFileManager
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

@HiltAndroidTest
class PdfFileManagerInjectionTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var pdfFileManager: PdfFileManager

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun pdfFileManagerIsInjectable() {
        assertNotNull(pdfFileManager)
    }
}
