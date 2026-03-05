package com.highliuk.manai.domain.model

data class PagePipelineState(
    val pageIndex: Int,
    val pageStatus: PipelineStatus = PipelineStatus.Queued,
    val balloonStatuses: Map<Int, BalloonPipelineStatus> = emptyMap(),
)
