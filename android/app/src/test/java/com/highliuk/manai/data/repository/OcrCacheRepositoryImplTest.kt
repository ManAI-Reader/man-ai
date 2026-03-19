package com.highliuk.manai.data.repository

import com.highliuk.manai.data.local.dao.PageOcrResultDao
import com.highliuk.manai.data.local.entity.PageOcrResultEntity
import com.highliuk.manai.domain.ml.TextRegion
import com.highliuk.manai.domain.model.PageRegion
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OcrCacheRepositoryImplTest {

    private val dao = mockk<PageOcrResultDao>(relaxed = true)
    private val repository = OcrCacheRepositoryImpl(dao)

    @Test
    fun `observeRegions maps entities to domain models`() = runTest {
        val entities = listOf(
            PageOcrResultEntity(
                mangaId = 1L, pageIndex = 0, regionIndex = 0,
                normX1 = 0.1f, normY1 = 0.2f, normX2 = 0.3f, normY2 = 0.4f,
                confidence = 0.95f, ocrText = "hello"
            )
        )
        coEvery { dao.getByPage(1L, 0) } returns flowOf(entities)

        val result = repository.observeRegions(1L, 0).first()

        assertEquals(1, result.size)
        assertEquals(
            PageRegion(0, 0.1f, 0.2f, 0.3f, 0.4f, 0.95f, "hello"),
            result[0]
        )
    }

    @Test
    fun `hasDetectionResults delegates to dao`() = runTest {
        coEvery { dao.hasDetectionResults(1L, 0) } returns true
        assertTrue(repository.hasDetectionResults(1L, 0))

        coEvery { dao.hasDetectionResults(1L, 1) } returns false
        assertFalse(repository.hasDetectionResults(1L, 1))
    }

    @Test
    fun `getRegions maps entities to domain models`() = runTest {
        val entities = listOf(
            PageOcrResultEntity(
                mangaId = 1L, pageIndex = 0, regionIndex = 0,
                normX1 = 0.5f, normY1 = 0.5f, normX2 = 1.0f, normY2 = 1.0f,
                confidence = 0.8f, ocrText = null
            )
        )
        coEvery { dao.getByPageOnce(1L, 0) } returns entities

        val result = repository.getRegions(1L, 0)

        assertEquals(1, result.size)
        assertEquals(0, result[0].regionIndex)
        assertEquals(0.5f, result[0].normX1)
        assertNull(result[0].ocrText)
    }

    @Test
    fun `saveDetectionResults normalizes coordinates by bitmap dimensions`() = runTest {
        val regions = listOf(
            TextRegion(x1 = 100f, y1 = 200f, x2 = 300f, y2 = 400f, confidence = 0.9f),
            TextRegion(x1 = 50f, y1 = 100f, x2 = 150f, y2 = 300f, confidence = 0.85f)
        )
        val entitiesSlot = slot<List<PageOcrResultEntity>>()
        coEvery { dao.insertAll(capture(entitiesSlot)) } returns Unit

        repository.saveDetectionResults(
            mangaId = 1L, pageIndex = 0,
            regions = regions, bitmapWidth = 1000, bitmapHeight = 2000
        )

        val saved = entitiesSlot.captured
        assertEquals(2, saved.size)

        // First region: 100/1000=0.1, 200/2000=0.1, 300/1000=0.3, 400/2000=0.2
        assertEquals(0.1f, saved[0].normX1, 0.001f)
        assertEquals(0.1f, saved[0].normY1, 0.001f)
        assertEquals(0.3f, saved[0].normX2, 0.001f)
        assertEquals(0.2f, saved[0].normY2, 0.001f)
        assertEquals(0.9f, saved[0].confidence, 0.001f)
        assertNull(saved[0].ocrText)
        assertEquals(0, saved[0].regionIndex)

        // Second region: 50/1000=0.05, 100/2000=0.05, 150/1000=0.15, 300/2000=0.15
        assertEquals(0.05f, saved[1].normX1, 0.001f)
        assertEquals(0.05f, saved[1].normY1, 0.001f)
        assertEquals(0.15f, saved[1].normX2, 0.001f)
        assertEquals(0.15f, saved[1].normY2, 0.001f)
        assertEquals(1, saved[1].regionIndex)
    }

    @Test
    fun `saveDetectionResults inserts sentinel row when regions empty`() = runTest {
        val entitiesSlot = slot<List<PageOcrResultEntity>>()
        coEvery { dao.insertAll(capture(entitiesSlot)) } returns Unit

        repository.saveDetectionResults(
            mangaId = 1L, pageIndex = 0,
            regions = emptyList(), bitmapWidth = 100, bitmapHeight = 100
        )

        val saved = entitiesSlot.captured
        assertEquals(1, saved.size)
        assertEquals(-1, saved[0].regionIndex)
    }

    @Test
    fun `getRegions filters out sentinel rows`() = runTest {
        coEvery { dao.getByPageOnce(1L, 0) } returns listOf(
            PageOcrResultEntity(1L, 0, -1, 0f, 0f, 0f, 0f, 0f, null),
            PageOcrResultEntity(1L, 0, 0, 0.1f, 0.1f, 0.5f, 0.5f, 0.9f, "text"),
        )

        val result = repository.getRegions(1L, 0)

        assertEquals(1, result.size)
        assertEquals(0, result[0].regionIndex)
    }

    @Test
    fun `observeRegions filters out sentinel rows`() = runTest {
        coEvery { dao.getByPage(1L, 0) } returns flowOf(listOf(
            PageOcrResultEntity(1L, 0, -1, 0f, 0f, 0f, 0f, 0f, null),
            PageOcrResultEntity(1L, 0, 0, 0.1f, 0.1f, 0.5f, 0.5f, 0.9f, "hello"),
        ))

        val result = repository.observeRegions(1L, 0).first()

        assertEquals(1, result.size)
        assertEquals(0, result[0].regionIndex)
    }

    @Test
    fun `saveOcrResult delegates to dao`() = runTest {
        repository.saveOcrResult(1L, 0, 2, "recognized text")

        coVerify { dao.updateOcrText(1L, 0, 2, "recognized text") }
    }

    @Test
    fun `observeRegions includes pageIndex from entity`() = runTest {
        val entities = listOf(
            PageOcrResultEntity(
                mangaId = 1L, pageIndex = 5, regionIndex = 0,
                normX1 = 0.1f, normY1 = 0.2f, normX2 = 0.3f, normY2 = 0.4f,
                confidence = 0.95f, ocrText = "hello"
            )
        )
        coEvery { dao.getByPage(1L, 5) } returns flowOf(entities)

        val result = repository.observeRegions(1L, 5).first()

        assertEquals(5, result[0].pageIndex)
    }

    @Test
    fun `getRegions includes pageIndex from entity`() = runTest {
        val entities = listOf(
            PageOcrResultEntity(
                mangaId = 1L, pageIndex = 7, regionIndex = 0,
                normX1 = 0.1f, normY1 = 0.2f, normX2 = 0.3f, normY2 = 0.4f,
                confidence = 0.9f, ocrText = null
            )
        )
        coEvery { dao.getByPageOnce(1L, 7) } returns entities

        val result = repository.getRegions(1L, 7)

        assertEquals(7, result[0].pageIndex)
    }
}
