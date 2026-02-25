package com.highliuk.manai.data.ml

import com.highliuk.manai.domain.ml.TextDetector
import com.highliuk.manai.domain.ml.TextRecognizer
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class MlModuleInjectionTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var textDetector: TextDetector

    @Inject
    lateinit var textRecognizer: TextRecognizer

    @Inject
    lateinit var sessionManager: OnnxSessionManager

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun hiltProvidesTextDetector() {
        assertNotNull(textDetector)
    }

    @Test
    fun hiltProvidesTextRecognizer() {
        assertNotNull(textRecognizer)
    }

    @Test
    fun hiltProvidesSessionManager() {
        assertNotNull(sessionManager)
    }
}
