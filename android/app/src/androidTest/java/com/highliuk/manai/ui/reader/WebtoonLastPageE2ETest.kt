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
import com.highliuk.manai.ui.testutil.clickOn
import com.highliuk.manai.ui.testutil.onFreshObject
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

private const val TIMEOUT = 5_000L

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

    /** Block until the WebtoonViewer composable is on screen. */
    private fun waitForReader() {
        assertNotNull(
            "WebtoonViewer should be visible",
            device.wait(Until.findObject(By.res("webtoon_viewer")), TIMEOUT),
        )
    }

    private fun showBarsAndFindIndicator(): String {
        waitForReader()
        device.click(device.displayWidth / 2, device.displayHeight / 2)
        return device.onFreshObject(By.textContains("/ 10"), TIMEOUT) { it.text }
    }

    private fun goToPageViaDialog(pageNumber: Int) {
        device.clickOn(By.textContains("/ 10"), TIMEOUT)
        device.onFreshObject(By.clazz("android.widget.EditText"), TIMEOUT) {
            it.click()
            it.clear()
        }
        for (digit in pageNumber.toString()) {
            device.pressKeyCode(android.view.KeyEvent.KEYCODE_0 + (digit - '0'))
        }
        device.pressBack()
        device.clickOn(By.text("OK"), TIMEOUT)
    }

    @Test
    fun webtoonLastPage_isSavedCorrectly() {
        ActivityScenario.launch(MainActivity::class.java)

        // 1. Tap manga
        device.clickOn(By.text("Webtoon Save Test"), TIMEOUT)

        // 2. Show bars, tap page indicator → GoToPageDialog, type "10" and confirm
        showBarsAndFindIndicator()
        goToPageViaDialog(10)

        // 4. Bars still visible — verify page 10
        val indicatorBefore = device.wait(
            Until.findObject(By.text("10 / 10")),
            TIMEOUT,
        )
        assertNotNull("Should show 10 / 10 before leaving", indicatorBefore)

        // 5. Back → home, wait for home to settle
        device.pressBack()
        device.wait(Until.hasObject(By.text("Webtoon Save Test")), TIMEOUT)
        device.waitForIdle()

        // 6. Reopen
        device.clickOn(By.text("Webtoon Save Test"), TIMEOUT)

        // 7. Show bars, verify page restored correctly
        val indicatorText = showBarsAndFindIndicator()
        assertEquals("10 / 10", indicatorText)

        // 8. Hide bars so they don't interfere with scrolling
        device.click(device.displayWidth / 2, device.displayHeight / 2)
        device.wait(Until.gone(By.textContains("/ 10")), TIMEOUT)

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
        device.waitForIdle()

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
        waitForReader()
    }

    @Test
    fun webtoonLastPage_isSavedCorrectly_landscape() {
        ActivityScenario.launch(MainActivity::class.java)
        device.clickOn(By.text("Webtoon Save Test"), TIMEOUT)
        waitForReader()
        setLandscape()

        showBarsAndFindIndicator()
        goToPageViaDialog(10)
        val indicatorBefore = device.wait(
            Until.findObject(By.text("10 / 10")),
            TIMEOUT,
        )
        assertNotNull("Should show 10 / 10 before leaving", indicatorBefore)

        device.pressBack()
        device.wait(Until.hasObject(By.text("Webtoon Save Test")), TIMEOUT)
        device.waitForIdle()
        device.clickOn(By.text("Webtoon Save Test"), TIMEOUT)

        val indicatorText = showBarsAndFindIndicator()
        assertEquals("10 / 10", indicatorText)
    }
}
