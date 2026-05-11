package com.highliuk.manai.domain.furigana

interface KanjiReadingsDataSource {
    suspend fun load()
    fun getReadings(kanji: Char): List<String>
}
