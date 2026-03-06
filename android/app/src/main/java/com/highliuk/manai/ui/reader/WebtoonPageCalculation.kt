package com.highliuk.manai.ui.reader

/**
 * Computes the current page index in webtoon (vertical scroll) mode.
 *
 * When the user has scrolled to the bottom of the document, [firstVisibleItemIndex]
 * may still point to the second-to-last page because the last page doesn't fill
 * the entire viewport. In that case we use [lastVisibleItemIndex] instead.
 */
internal fun computeWebtoonCurrentPage(
    firstVisibleItemIndex: Int,
    canScrollForward: Boolean,
    lastVisibleItemIndex: Int?,
): Int = if (!canScrollForward && lastVisibleItemIndex != null) {
    lastVisibleItemIndex
} else {
    firstVisibleItemIndex
}
