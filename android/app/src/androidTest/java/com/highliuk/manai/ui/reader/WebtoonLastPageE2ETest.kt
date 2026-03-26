package com.highliuk.manai.ui.reader

import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.highliuk.manai.MainActivity
import com.highliuk.manai.data.local.ManAiDatabase
import com.highliuk.manai.data.local.dao.MangaDao
import com.highliuk.manai.data.local.entity.MangaEntity
import com.highliuk.manai.domain.model.ReadingMode
import com.highliuk.manai.domain.repository.UserPreferencesRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

private const val TIMEOUT = 3_000L

@HiltAndroidTest
class WebtoonLastPageE2ETest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject lateinit var mangaDao: MangaDao
    @Inject lateinit var database: ManAiDatabase
    @Inject lateinit var userPreferencesRepository: UserPreferencesRepository

    private lateinit var device: UiDevice

    @Before
    fun setUp() {
        hiltRule.inject()
        database.clearAllTables()
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        runBlocking {
            userPreferencesRepository.setReadingMode(ReadingMode.WEBTOON)
            mangaDao.insert(
                MangaEntity(
                    uri = "content://webtoon-save-test",
                    title = "Webtoon Save Test",
                    pageCount = 10,
                )
            )
        }
    }

    @After
    fun tearDown() {
        device.setOrientationNatural()
        device.unfreezeRotation()
    }

    private fun showBarsAndFindIndicator(): String {
        device.click(device.displayWidth / 2, device.displayHeight / 2)
        val indicator = device.wait(Until.findObject(By.textContains("/ 10")), TIMEOUT)
        assertNotNull("Page indicator not visible after tap", indicator)
        return indicator.text
    }

    private fun goToPageViaDialog(pageNumber: Int) {
        device.findObject(By.textContains("/ 10")).click()
        val input = device.wait(
            Until.findObject(By.clazz("android.widget.EditText")),
            TIMEOUT,
        )
        input.click()
        input.clear()
        for (digit in pageNumber.toString()) {
            device.pressKeyCode(android.view.KeyEvent.KEYCODE_0 + (digit - '0'))
        }
        device.pressBack()
        device.wait(Until.findObject(By.text("OK")), TIMEOUT).click()
    }

    @Test
    fun webtoonLastPage_isSavedCorrectly() {
        ActivityScenario.launch(MainActivity::class.java)

        // 1. Tap manga
        device.wait(Until.findObject(By.text("Webtoon Save Test")), TIMEOUT).click()

        // 2. Show bars, tap page indicator → GoToPageDialog
        Thread.sleep(500)
        showBarsAndFindIndicator()
        device.findObject(By.textContains("/ 10")).click()

        // 3. Type "10" and confirm
        val input = device.wait(
            Until.findObject(By.clazz("android.widget.EditText")),
            TIMEOUT,
        )
        input.click()
        input.clear()
        device.pressKeyCode(android.view.KeyEvent.KEYCODE_1)
        device.pressKeyCode(android.view.KeyEvent.KEYCODE_0)
        device.pressBack()
        device.wait(Until.findObject(By.text("OK")), TIMEOUT).click()

        // 4. Bars still visible — verify page 10
        val indicatorBefore = device.wait(
            Until.findObject(By.text("10 / 10")),
            TIMEOUT,
        )
        assertNotNull("Should show 10 / 10 before leaving", indicatorBefore)

        // 5. Back → home
        device.pressBack()

        // 6. Reopen
        device.wait(Until.findObject(By.text("Webtoon Save Test")), TIMEOUT).click()

        // 7. Show bars, verify page restored correctly
        Thread.sleep(500)
        val indicatorText = showBarsAndFindIndicator()
        assertEquals("10 / 10", indicatorText)

        // 8. Hide bars so they don't interfere with scrolling
        device.click(device.displayWidth / 2, device.displayHeight / 2)
        Thread.sleep(500)

        // 9. Record Y of the bottom-most placeholder before swiping
        val before = device.findObjects(By.desc("PDF placeholder"))
            .maxByOrNull { it.visibleBounds.top }
        assertNotNull("Should find placeholder pages before swipe", before)
        val yBefore = before!!.visibleBounds.top

        // 10. Swipe up — should be a no-op at the bottom
        device.swipe(
            device.displayWidth / 2,
            device.displayHeight * 3 / 4,
            device.displayWidth / 2,
            device.displayHeight / 4,
            10,
        )
        Thread.sleep(500)

        // 11. Record Y after swiping
        val after = device.findObjects(By.desc("PDF placeholder"))
            .maxByOrNull { it.visibleBounds.top }
        assertNotNull("Should find placeholder pages after swipe", after)
        val yAfter = after!!.visibleBounds.top

        assertEquals(
            "Content should not have moved (already at bottom)",
            yBefore,
            yAfter,
        )
    }

    // --- Landscape ---

    private fun setLandscape() {
        device.setOrientationLeft()
        Thread.sleep(500)
    }

    @Test
    fun webtoonLastPage_isSavedCorrectly_landscape() {
        ActivityScenario.launch(MainActivity::class.java)
        device.wait(Until.findObject(By.text("Webtoon Save Test")), TIMEOUT).click()
        Thread.sleep(500)
        setLandscape()

        showBarsAndFindIndicator()
        goToPageViaDialog(10)
        val indicatorBefore = device.wait(
            Until.findObject(By.text("10 / 10")),
            TIMEOUT,
        )
        assertNotNull("Should show 10 / 10 before leaving", indicatorBefore)

        device.pressBack()
        device.wait(Until.findObject(By.text("Webtoon Save Test")), TIMEOUT).click()

        Thread.sleep(500)
        val indicatorText = showBarsAndFindIndicator()
        assertEquals("10 / 10", indicatorText)
    }
}
