package com.highliuk.manai.data.furigana

import android.content.Context
import com.highliuk.manai.domain.furigana.KanaUtils
import com.highliuk.manai.domain.furigana.KanjiReadingsDataSource
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

private const val ASSET_PATH = "kanji_readings.csv"

class KanjiReadingsDataSourceImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : KanjiReadingsDataSource {

    private var readings: Map<Char, List<String>> = emptyMap()

    override suspend fun load() {
        if (readings.isNotEmpty()) return
        val csv = context.assets.open(ASSET_PATH).bufferedReader().use { it.readText() }
        readings = parseCsv(csv)
    }

    override fun getReadings(kanji: Char): List<String> = readings[kanji].orEmpty()

    internal fun loadFromString(csv: String) {
        readings = parseCsv(csv)
    }
}

internal fun parseKanjiReadings(csv: String): Map<Char, List<String>> = parseCsv(csv)

private fun parseCsv(csv: String): Map<Char, List<String>> =
    csv.lines()
        .filter { it.isNotBlank() }
        .mapNotNull { line ->
            val parts = line.split(",")
            if (parts.size < 2) return@mapNotNull null
            val kanji = parts[0].first()
            val processed = mutableSetOf<String>()
            for (raw in parts.drop(1)) {
                val reading = processReading(raw)
                if (reading.isNotEmpty()) {
                    processed.add(reading)
                    addVariants(reading, processed)
                    addDualKana(reading, processed)
                }
            }
            kanji to processed.toList()
        }
        .toMap()

private fun processReading(raw: String): String {
    var reading = raw.trim()
    while (reading.startsWith("-")) reading = reading.drop(1)
    while (reading.endsWith("-")) reading = reading.dropLast(1)
    if (reading.contains(".")) reading = reading.split(".").first()
    return reading
}

private fun addVariants(reading: String, set: MutableSet<String>) {
    if (reading.length >= 2 && reading.endsWith("ツ")) {
        set.add(reading.dropLast(1) + "ッ")
    }
    if (reading.startsWith("ア")) {
        set.add("ワ" + reading.drop(1))
    }
}

private fun addDualKana(reading: String, set: MutableSet<String>) {
    if (KanaUtils.isKatakana(reading)) {
        set.add(KanaUtils.toHiragana(reading))
    } else if (reading.all { KanaUtils.isKana(it) }) {
        set.add(KanaUtils.toKatakana(reading))
    }
}
