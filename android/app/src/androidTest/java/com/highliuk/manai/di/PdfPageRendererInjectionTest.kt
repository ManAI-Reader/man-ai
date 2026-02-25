package com.highliuk.manai.di

import com.highliuk.manai.data.pdf.PdfPageRenderer
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

@HiltAndroidTest
class PdfPageRendererInjectionTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var pdfPageRenderer: PdfPageRenderer

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun pdfPageRendererIsInjected() {
        assertNotNull(pdfPageRenderer)
    }
}
