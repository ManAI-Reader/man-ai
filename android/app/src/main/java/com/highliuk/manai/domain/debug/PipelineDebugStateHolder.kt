package com.highliuk.manai.domain.debug

import com.highliuk.manai.domain.model.BalloonPipelineStatus
import com.highliuk.manai.domain.model.PagePipelineState
import com.highliuk.manai.domain.model.PipelineStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PipelineDebugStateHolder @Inject constructor() {

    private val _states = MutableStateFlow<Map<Int, PagePipelineState>>(emptyMap())
    val states: StateFlow<Map<Int, PagePipelineState>> = _states.asStateFlow()

    fun setPageStatus(pageIndex: Int, status: PipelineStatus) {
        _states.value = _states.value.toMutableMap().apply {
            val existing = get(pageIndex) ?: PagePipelineState(pageIndex = pageIndex)
            put(pageIndex, existing.copy(pageStatus = status))
        }
    }

    fun setBalloonStatus(pageIndex: Int, regionIndex: Int, status: BalloonPipelineStatus) {
        _states.value = _states.value.toMutableMap().apply {
            val existing = get(pageIndex) ?: PagePipelineState(pageIndex = pageIndex)
            put(pageIndex, existing.copy(
                balloonStatuses = existing.balloonStatuses + (regionIndex to status)
            ))
        }
    }

    fun setBalloonStatuses(pageIndex: Int, statuses: Map<Int, BalloonPipelineStatus>) {
        _states.value = _states.value.toMutableMap().apply {
            val existing = get(pageIndex) ?: PagePipelineState(pageIndex = pageIndex)
            put(pageIndex, existing.copy(
                balloonStatuses = existing.balloonStatuses + statuses
            ))
        }
    }
}
