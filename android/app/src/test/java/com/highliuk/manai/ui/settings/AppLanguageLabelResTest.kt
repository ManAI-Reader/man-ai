package com.highliuk.manai.ui.settings

import com.highliuk.manai.domain.model.AppLanguage
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLanguageLabelResTest {

    @Test
    fun `every AppLanguage entry has a valid labelRes`() {
        for (language in AppLanguage.entries) {
            val resId = language.labelRes()
            assertTrue(
                "labelRes() for $language should return a non-zero resource ID",
                resId != 0
            )
        }
    }
}
