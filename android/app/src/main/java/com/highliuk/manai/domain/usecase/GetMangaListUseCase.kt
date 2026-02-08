package com.highliuk.manai.domain.usecase

import com.highliuk.manai.domain.model.Manga
import com.highliuk.manai.domain.repository.MangaRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetMangaListUseCase @Inject constructor(
    private val repository: MangaRepository,
) {
    operator fun invoke(): Flow<List<Manga>> = repository.getMangaList()
}
