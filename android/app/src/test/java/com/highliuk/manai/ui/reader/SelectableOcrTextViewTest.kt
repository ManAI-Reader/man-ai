package com.highliuk.manai.ui.reader

import android.view.Menu
import android.view.MenuItem
import com.highliuk.manai.domain.model.FuriganaPart
import com.highliuk.manai.domain.model.FuriganaToken
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test

class SelectableOcrTextViewTest {

    // "私は食べる" — tokens: 私[0..1), は[1..2), 食べる[2..5)
    private val sampleTokens = listOf(
        FuriganaToken(
            surface = "私",
            reading = "わたし",
            parts = listOf(FuriganaPart.kanji("私", "わたし")),
        ),
        FuriganaToken(
            surface = "は",
            reading = null,
            parts = listOf(FuriganaPart.kana("は")),
        ),
        FuriganaToken(
            surface = "食べる",
            reading = "たべる",
            parts = listOf(
                FuriganaPart.kanji("食", "た"),
                FuriganaPart.kana("べる"),
            ),
        ),
    )

    // --- resolveTappedCharOffset tests ---

    @Test
    fun tappedCharOffsetReturnsPreviousCharWhenTapFallsOnItsRightHalf() {
        // Layout has chars A[0..10), B[10..20), C[20..30), D[30..40).
        // Tap on right half of B (tapX=18) → Android's getOffsetForHorizontal
        // returns cursor offset 2 (cursor after B). The right edge of B
        // (= left edge of C = getPrimaryHorizontal(2)) is 20.
        // The actually-tapped char is B (index 1), not C (index 2).
        val result = SelectableOcrTextView.resolveTappedCharOffset(
            cursorOffset = 2,
            tapX = 18f,
            cursorOffsetX = 20f,
        )
        assertEquals(1, result)
    }

    @Test
    fun tappedCharOffsetReturnsCursorOffsetWhenTapFallsOnLeftHalf() {
        // Tap on left half of B (tapX=11). Cursor offset = 1 (cursor before B),
        // cursorOffsetX = getPrimaryHorizontal(1) = 10. Tap is to the right of
        // that edge → tapped char is B (index 1).
        val result = SelectableOcrTextView.resolveTappedCharOffset(
            cursorOffset = 1,
            tapX = 11f,
            cursorOffsetX = 10f,
        )
        assertEquals(1, result)
    }

    @Test
    fun tappedCharOffsetReturnsZeroWhenCursorOffsetIsZero() {
        // Tap on first char's left half: cursor offset clamps to 0.
        val result = SelectableOcrTextView.resolveTappedCharOffset(
            cursorOffset = 0,
            tapX = 2f,
            cursorOffsetX = 0f,
        )
        assertEquals(0, result)
    }

    @Test
    fun tappedCharOffsetReturnsLastCharWhenTapOnRightHalfOfLastChar() {
        // 4-char text, tap on right half of D (tapX=38). Cursor offset = 4
        // (cursor after D), cursorOffsetX = getPrimaryHorizontal(4) = 40.
        // Tapped char is D (index 3).
        val result = SelectableOcrTextView.resolveTappedCharOffset(
            cursorOffset = 4,
            tapX = 38f,
            cursorOffsetX = 40f,
        )
        assertEquals(3, result)
    }

    // --- computeWordSelectionAdjustment tests ---

    @Test
    fun adjustmentReturnsNullWhenNotPending() {
        val result = SelectableOcrTextView.computeWordSelectionAdjustment(
            pending = false,
            touchedOffset = 3,
            nativeSelStart = 0,
            nativeSelEnd = 5,
            tokens = sampleTokens,
        )
        assertEquals(null, result)
    }

    @Test
    fun adjustmentReturnsNullForCursorSelection() {
        val result = SelectableOcrTextView.computeWordSelectionAdjustment(
            pending = true,
            touchedOffset = 3,
            nativeSelStart = 3,
            nativeSelEnd = 3,
            tokens = sampleTokens,
        )
        assertEquals(null, result)
    }

    @Test
    fun adjustmentSelectsWholeTokenWhenTappingFirstCharOfMultiCharToken() {
        // Tap on 食 (offset 2) — first char of 食べる.
        // The whole token "食べる" must be selected → (2, 5).
        val result = SelectableOcrTextView.computeWordSelectionAdjustment(
            pending = true,
            touchedOffset = 2,
            nativeSelStart = 0,
            nativeSelEnd = 5,
            tokens = sampleTokens,
        )

        assertEquals(2 to 5, result)
    }

    @Test
    fun adjustmentSelectsSingleCharTokenAtStart() {
        // Tap on 私 (offset 0) — single-char token → (0, 1).
        val result = SelectableOcrTextView.computeWordSelectionAdjustment(
            pending = true,
            touchedOffset = 0,
            nativeSelStart = 0,
            nativeSelEnd = 5,
            tokens = sampleTokens,
        )

        assertEquals(0 to 1, result)
    }

    @Test
    fun adjustmentReturnsNullWhenBoundaryMatchesNative() {
        // Tap on 食 (offset 2) → token boundary (2, 5).
        // Native already at (2, 5), no adjustment needed.
        val result = SelectableOcrTextView.computeWordSelectionAdjustment(
            pending = true,
            touchedOffset = 2,
            nativeSelStart = 2,
            nativeSelEnd = 5,
            tokens = sampleTokens,
        )
        assertEquals(null, result)
    }

    @Test
    fun adjustmentReturnsNullWhenTokensEmpty() {
        // No tokens → no word boundary → no adjustment.
        val result = SelectableOcrTextView.computeWordSelectionAdjustment(
            pending = true,
            touchedOffset = 3,
            nativeSelStart = 0,
            nativeSelEnd = 5,
            tokens = emptyList(),
        )
        assertEquals(null, result)
    }

    // --- wordBoundaryFromTokens tests ---

    @Test
    fun wordBoundaryReturnsTokenRangeForTapInsideToken() {
        // Tap on べ (offset 3) → inside 食べる → (2, 5).
        val result = SelectableOcrTextView.wordBoundaryFromTokens(
            offset = 3,
            tokens = sampleTokens,
        )
        assertEquals(2 to 5, result)
    }

    @Test
    fun wordBoundaryReturnsSecondTokenAtItsStart() {
        // Tap at offset 1 — start of は (boundary belongs to next token).
        val result = SelectableOcrTextView.wordBoundaryFromTokens(
            offset = 1,
            tokens = sampleTokens,
        )
        assertEquals(1 to 2, result)
    }

    @Test
    fun wordBoundaryReturnsNullForOffsetAtEnd() {
        // Offset == total length (5) is past the last char.
        val result = SelectableOcrTextView.wordBoundaryFromTokens(
            offset = 5,
            tokens = sampleTokens,
        )
        assertEquals(null, result)
    }

    @Test
    fun wordBoundaryReturnsNullForEmptyTokens() {
        val result = SelectableOcrTextView.wordBoundaryFromTokens(
            offset = 0,
            tokens = emptyList(),
        )
        assertEquals(null, result)
    }

    // --- isSingleTapGesture tests ---

    @Test
    fun quickTouchWithinSlopIsASingleTap() {
        val result = SelectableOcrTextView.isSingleTapGesture(
            durationMillis = 120L,
            dxPx = 4f,
            dyPx = -3f,
            touchSlopPx = 16f,
            longPressTimeoutMillis = 400L,
        )
        assertEquals(true, result)
    }

    @Test
    fun touchHeldUpToLongPressTimeoutIsNotASingleTap() {
        val result = SelectableOcrTextView.isSingleTapGesture(
            durationMillis = 400L,
            dxPx = 0f,
            dyPx = 0f,
            touchSlopPx = 16f,
            longPressTimeoutMillis = 400L,
        )
        assertEquals(false, result)
    }

    @Test
    fun touchMovedBeyondSlopIsNotASingleTap() {
        val result = SelectableOcrTextView.isSingleTapGesture(
            durationMillis = 120L,
            dxPx = 17f,
            dyPx = 0f,
            touchSlopPx = 16f,
            longPressTimeoutMillis = 400L,
        )
        assertEquals(false, result)
    }

    @Test
    fun diagonalMoveBeyondSlopIsNotASingleTapEvenIfEachAxisIsWithinSlop() {
        // dx and dy are each below the slop but the euclidean distance
        // (sqrt(13^2 + 13^2) ≈ 18.4) exceeds it — this is a drag, not a tap.
        val result = SelectableOcrTextView.isSingleTapGesture(
            durationMillis = 120L,
            dxPx = 13f,
            dyPx = 13f,
            touchSlopPx = 16f,
            longPressTimeoutMillis = 400L,
        )
        assertEquals(false, result)
    }

    @Test
    fun touchExactlyAtSlopStillCountsAsASingleTap() {
        val result = SelectableOcrTextView.isSingleTapGesture(
            durationMillis = 120L,
            dxPx = 16f,
            dyPx = 0f,
            touchSlopPx = 16f,
            longPressTimeoutMillis = 400L,
        )
        assertEquals(true, result)
    }

    // --- promptOnlyWordForTap tests ---

    @Test
    fun singleTapInsideTokenStartsPromptOnlySelectionWithWordBounds() {
        // Tap on べ (offset 3) → inside 食べる → prompt-only selection of (2, 5).
        val result = SelectableOcrTextView.promptOnlyWordForTap(
            offset = 3,
            tokens = sampleTokens,
            hasPrompts = true,
        )
        assertEquals(2 to 5, result)
    }

    @Test
    fun singleTapPastLastTokenDoesNotStartPromptOnlySelection() {
        // Offset == total length (5) is past the last char → no selection, no menu.
        val result = SelectableOcrTextView.promptOnlyWordForTap(
            offset = 5,
            tokens = sampleTokens,
            hasPrompts = true,
        )
        assertEquals(null, result)
    }

    @Test
    fun singleTapWithoutTokensDoesNotStartPromptOnlySelection() {
        val result = SelectableOcrTextView.promptOnlyWordForTap(
            offset = 0,
            tokens = emptyList(),
            hasPrompts = true,
        )
        assertEquals(null, result)
    }

    @Test
    fun singleTapWithNoConfiguredPromptsDoesNotStartPromptOnlySelection() {
        // No prompts configured → a prompt-only toolbar would be empty,
        // so the tap must not select anything at all.
        val result = SelectableOcrTextView.promptOnlyWordForTap(
            offset = 3,
            tokens = sampleTokens,
            hasPrompts = false,
        )
        assertEquals(null, result)
    }

    // --- shouldExitPromptOnlyMode tests ---

    @Test
    fun exitsPromptOnlyModeWhenSelectionDiffersFromTappedWord() {
        // Word tapped is 食べる (2, 5); user dragged a handle to (0, 5).
        val result = SelectableOcrTextView.shouldExitPromptOnlyMode(
            selStart = 0,
            selEnd = 5,
            wordStart = 2,
            wordEnd = 5,
        )
        assertEquals(true, result)
    }

    @Test
    fun staysInPromptOnlyModeWhenSelectionEqualsTappedWord() {
        val result = SelectableOcrTextView.shouldExitPromptOnlyMode(
            selStart = 2,
            selEnd = 5,
            wordStart = 2,
            wordEnd = 5,
        )
        assertEquals(false, result)
    }

    @Test
    fun staysInPromptOnlyModeWhenInvertedSelectionEqualsTappedWord() {
        // Native selection can report inverted bounds; (5, 2) is still the word (2, 5).
        val result = SelectableOcrTextView.shouldExitPromptOnlyMode(
            selStart = 5,
            selEnd = 2,
            wordStart = 2,
            wordEnd = 5,
        )
        assertEquals(false, result)
    }

    @Test
    fun staysInPromptOnlyModeForCollapsedSelection() {
        // A collapsed (cursor) selection must not kick us out of prompt-only mode.
        val result = SelectableOcrTextView.shouldExitPromptOnlyMode(
            selStart = 3,
            selEnd = 3,
            wordStart = 2,
            wordEnd = 5,
        )
        assertEquals(false, result)
    }

    // --- promptMenuItemId / promptIdForMenuItem tests ---

    private val samplePrompts = listOf(10L to "Explain", 20L to "Grammar", 30L to "Nuance")

    @Test
    fun promptMenuItemIdRoundTripsThroughPromptIdForMenuItem() {
        samplePrompts.forEachIndexed { index, (promptId, _) ->
            val menuItemId = SelectableOcrTextView.promptMenuItemId(index)
            assertEquals(promptId, SelectableOcrTextView.promptIdForMenuItem(menuItemId, samplePrompts))
        }
    }

    @Test
    fun promptIdForMenuItemReturnsNullForOutOfRangeIds() {
        val beyondLast = SelectableOcrTextView.promptMenuItemId(samplePrompts.size)
        assertEquals(null, SelectableOcrTextView.promptIdForMenuItem(beyondLast, samplePrompts))

        val beforeFirst = SelectableOcrTextView.promptMenuItemId(0) - 1
        assertEquals(null, SelectableOcrTextView.promptIdForMenuItem(beforeFirst, samplePrompts))
    }

    @Test
    fun promptIdForMenuItemMustResolveAgainstMenuSnapshotNotMutatedList() {
        // The action-mode menu is built from a snapshot of the prompt list.
        // If the live list grows while the menu is open (e.g. a translation
        // completes), resolving the clicked index against the mutated list
        // would dispatch the wrong prompt — only the snapshot is correct.
        val snapshot = listOf(10L to "Explain", 20L to "Grammar")
        val mutated = listOf(99L to "Compare") + snapshot

        val clickedItemId = SelectableOcrTextView.promptMenuItemId(1)

        assertEquals(20L, SelectableOcrTextView.promptIdForMenuItem(clickedItemId, snapshot))
        // Same item id against the mutated list resolves to a different prompt:
        assertEquals(10L, SelectableOcrTextView.promptIdForMenuItem(clickedItemId, mutated))
    }

    @Test
    fun promptIdForMenuItemReturnsNullForEmptyPrompts() {
        val menuItemId = SelectableOcrTextView.promptMenuItemId(0)
        assertEquals(null, SelectableOcrTextView.promptIdForMenuItem(menuItemId, emptyList()))
    }

    // --- prepareSelectionMenu tests ---

    private fun menuItemWithGroup(group: Int): MenuItem =
        mockk(relaxed = true) { every { groupId } returns group }

    private fun menuWithItems(vararg items: MenuItem): Menu {
        val menu = mockk<Menu>(relaxed = true)
        every { menu.size() } returns items.size
        items.forEachIndexed { index, item -> every { menu.getItem(index) } returns item }
        return menu
    }

    @Test
    fun promptOnlyModeHidesNonPromptItemsWithoutClearingTheMenu() {
        val copyItem = menuItemWithGroup(0)
        val processTextItem = menuItemWithGroup(0)
        val menu = menuWithItems(copyItem, processTextItem)

        SelectableOcrTextView.prepareSelectionMenu(menu, promptOnly = true, prompts = samplePrompts)

        verify { copyItem.setVisible(false) }
        verify { processTextItem.setVisible(false) }
        // AOSP adds Cut/Copy/Share and PROCESS_TEXT items only in
        // onCreateActionMode; onPrepareActionMode re-adds only Select All/
        // Replace/assist. clear() would therefore drop Copy/Share/PROCESS_TEXT
        // for the whole lifetime of the action mode — it must never be called.
        verify(exactly = 0) { menu.clear() }
    }

    @Test
    fun fullMenuModeRestoresVisibilityOfNonPromptItems() {
        val copyItem = menuItemWithGroup(0)
        val menu = menuWithItems(copyItem)

        SelectableOcrTextView.prepareSelectionMenu(menu, promptOnly = false, prompts = samplePrompts)

        verify { copyItem.setVisible(true) }
        verify(exactly = 0) { menu.clear() }
    }

    @Test
    fun prepareAddsOnePromptMenuItemPerPromptInBothModes() {
        listOf(true, false).forEach { promptOnly ->
            val menu = menuWithItems()

            SelectableOcrTextView.prepareSelectionMenu(menu, promptOnly = promptOnly, prompts = samplePrompts)

            samplePrompts.forEachIndexed { index, (_, name) ->
                verify {
                    menu.add(
                        SelectableOcrTextView.PROMPT_GROUP,
                        SelectableOcrTextView.promptMenuItemId(index),
                        any(),
                        name,
                    )
                }
            }
        }
    }

    @Test
    fun prepareNeverTouchesVisibilityOfPromptGroupItems() {
        val promptItem = menuItemWithGroup(SelectableOcrTextView.PROMPT_GROUP)
        val menu = menuWithItems(promptItem)

        SelectableOcrTextView.prepareSelectionMenu(menu, promptOnly = true, prompts = samplePrompts)

        verify(exactly = 0) { promptItem.setVisible(any()) }
    }

    /**
     * Stateful stand-in replicating the AOSP MenuBuilder semantics our code
     * depends on: removeGroup only removes the CONTIGUOUS run of items
     * starting at the first match, and removeItem removes the first item
     * with the given id. PROCESS_TEXT items share the prompt items' order
     * range, so prompt items can end up interleaved with them.
     */
    private class FakeMenu {
        data class Entry(val groupId: Int, val itemId: Int, val order: Int, val item: MenuItem)

        val entries = mutableListOf<Entry>()

        fun asMenu(): Menu {
            val menu = mockk<Menu>(relaxed = true)
            every { menu.size() } answers { entries.size }
            every { menu.getItem(any()) } answers {
                val entry = entries[firstArg<Int>()]
                mockk(relaxed = true) {
                    every { groupId } returns entry.groupId
                    every { itemId } returns entry.itemId
                }
            }
            every { menu.add(any<Int>(), any<Int>(), any<Int>(), any<CharSequence>()) } answers {
                val added = Entry(firstArg(), secondArg(), thirdArg(), mockk(relaxed = true))
                // MenuBuilder inserts sorted by order: after the last entry
                // whose order is <= the new one. This is what interleaves
                // prompt items (order 100/101) with PROCESS_TEXT items
                // occupying the same order range.
                val insertAt = entries.indexOfLast { it.order <= added.order } + 1
                entries.add(insertAt, added)
                added.item
            }
            every { menu.removeItem(any()) } answers {
                val id = firstArg<Int>()
                val index = entries.indexOfFirst { it.itemId == id }
                if (index >= 0) entries.removeAt(index)
            }
            every { menu.removeGroup(any()) } answers {
                val group = firstArg<Int>()
                val first = entries.indexOfFirst { it.groupId == group }
                if (first >= 0) {
                    while (first < entries.size && entries[first].groupId == group) {
                        entries.removeAt(first)
                    }
                }
            }
            return menu
        }
    }

    @Test
    fun repeatedPreparesDoNotDuplicatePromptsInterleavedWithProcessTextItems() {
        val fake = FakeMenu()
        val menu = fake.asMenu()
        // AOSP adds the PROCESS_TEXT items at create time, before the first
        // prepare, starting at menu order 100 — the same order range the
        // prompt items used, which interleaves the two groups.
        menu.add(0, 0, 100, "Other app action A")
        menu.add(0, 0, 101, "Other app action B")

        repeat(4) {
            SelectableOcrTextView.prepareSelectionMenu(
                menu,
                promptOnly = false,
                prompts = samplePrompts,
            )
        }

        val promptCount = fake.entries.count { it.groupId == SelectableOcrTextView.PROMPT_GROUP }
        assertEquals(samplePrompts.size, promptCount)
    }

    // --- safeSelection tests ---

    @Test
    fun safeSelectionReturnsOrderedBoundsForNormalSelection() {
        assertEquals(2 to 5, SelectableOcrTextView.safeSelection(2, 5, 10))
    }

    @Test
    fun safeSelectionReordersInvertedBounds() {
        assertEquals(2 to 5, SelectableOcrTextView.safeSelection(5, 2, 10))
    }

    @Test
    fun safeSelectionClampsNegativeStartToZero() {
        assertEquals(0 to 3, SelectableOcrTextView.safeSelection(-1, 3, 10))
    }

    @Test
    fun safeSelectionClampsEndOverflowToTextLength() {
        assertEquals(2 to 10, SelectableOcrTextView.safeSelection(2, 15, 10))
    }

    @Test
    fun safeSelectionReturnsNullForEmptySelection() {
        assertEquals(null, SelectableOcrTextView.safeSelection(3, 3, 10))
    }

    @Test
    fun safeSelectionReturnsNullWhenBothBoundsInvalid() {
        assertEquals(null, SelectableOcrTextView.safeSelection(-5, -1, 10))
    }

    @Test
    fun safeSelectionReturnsNullForEmptyText() {
        assertEquals(null, SelectableOcrTextView.safeSelection(0, 4, 0))
    }

    private val lineBounds = SelectableOcrTextView.Companion.TextLineBounds(
        leftPx = 10f,
        rightPx = 200f,
        textBottomPx = 300f,
    )

    @Test
    fun tapInsideLineBoundsIsOnText() {
        assertEquals(true, SelectableOcrTextView.isTapOnText(100f, 150f, lineBounds, 8f))
    }

    @Test
    fun tapBelowTextBottomBeyondSlopIsNotOnText() {
        assertEquals(false, SelectableOcrTextView.isTapOnText(100f, 309f, lineBounds, 8f))
    }

    @Test
    fun tapRightOfLineEndBeyondSlopIsNotOnText() {
        assertEquals(false, SelectableOcrTextView.isTapOnText(209f, 150f, lineBounds, 8f))
    }

    @Test
    fun tapLeftOfLineStartBeyondSlopIsNotOnText() {
        assertEquals(false, SelectableOcrTextView.isTapOnText(1f, 150f, lineBounds, 8f))
    }

    @Test
    fun tapWithinSlopOfLineEdgesIsOnText() {
        assertEquals(true, SelectableOcrTextView.isTapOnText(207f, 307f, lineBounds, 8f))
        assertEquals(true, SelectableOcrTextView.isTapOnText(3f, -7f, lineBounds, 8f))
    }
}
