package com.highliuk.manai.domain.debug

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton

sealed interface DebugMlEvent {
    val toastMessage: String

    data class ModelLoading(val modelName: String) : DebugMlEvent {
        override val toastMessage: String = "Loading $modelName..."
    }
    data class ModelReady(val modelName: String) : DebugMlEvent {
        override val toastMessage: String = "$modelName ready"
    }
    data class PipelineError(val message: String) : DebugMlEvent {
        override val toastMessage: String = "Pipeline error: $message"
    }
}

@Singleton
class DebugMlEventHolder @Inject constructor() {
    private val _events = Channel<DebugMlEvent>(Channel.BUFFERED)
    val events: Flow<DebugMlEvent> = _events.receiveAsFlow()

    fun emit(event: DebugMlEvent) {
        _events.trySend(event)
    }
}
