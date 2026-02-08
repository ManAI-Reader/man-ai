package com.highliuk.manai.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.highliuk.manai.domain.model.Manga
import com.highliuk.manai.domain.usecase.GetMangaListUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data object Empty : HomeUiState
    data class Success(val mangaList: List<Manga>) : HomeUiState
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    getMangaList: GetMangaListUseCase,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = getMangaList()
        .map { list ->
            if (list.isEmpty()) HomeUiState.Empty else HomeUiState.Success(list)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState.Loading,
        )
}
