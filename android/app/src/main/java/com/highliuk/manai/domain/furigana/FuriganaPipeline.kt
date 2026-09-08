package com.highliuk.manai.domain.furigana

import com.highliuk.manai.domain.ml.JapaneseTokenizer
import com.highliuk.manai.domain.usecase.ParseFuriganaUseCase
import javax.inject.Inject

/**
 * The components needed to produce furigana for a piece of Japanese text,
 * bundled so consumers can inject the pipeline as a single dependency.
 */
class FuriganaPipeline @Inject constructor(
    val tokenizer: JapaneseTokenizer,
    val kanjiReadings: KanjiReadingsDataSource,
    val parseFurigana: ParseFuriganaUseCase,
)
