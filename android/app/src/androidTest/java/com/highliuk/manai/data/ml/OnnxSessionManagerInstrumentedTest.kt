package com.highliuk.manai.data.ml

import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class OnnxSessionManagerInstrumentedTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var sessionManager: OnnxSessionManager

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun hiltProvidesSessionManager() {
        assertNotNull(sessionManager)
    }
}
