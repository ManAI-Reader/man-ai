package com.highliuk.manai.ui.home

import org.junit.Assert.assertNotNull
import org.junit.Test

class PdfThumbnailKtTest {

    @Test
    fun thumbnailCacheSingletonIsInitialized() {
        assertNotNull(thumbnailCache)
    }
}
