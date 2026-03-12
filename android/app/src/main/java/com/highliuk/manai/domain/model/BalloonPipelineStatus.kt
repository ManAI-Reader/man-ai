package com.highliuk.manai.domain.model

sealed interface BalloonPipelineStatus {
    val overlayColor: Int

    data class OcrQueued(val position: Int) : BalloonPipelineStatus {
        override val overlayColor: Int = 0x4DFF9800.toInt()
    }
    data object OcrProcessing : BalloonPipelineStatus {
        override val overlayColor: Int = 0x4DFFEB3B.toInt()
    }
    data object OcrDone : BalloonPipelineStatus {
        override val overlayColor: Int = 0x4D4CAF50.toInt()
    }
    data object OcrCacheHit : BalloonPipelineStatus {
        override val overlayColor: Int = 0x4D2196F3.toInt()
    }
    data class OcrError(val message: String) : BalloonPipelineStatus {
        override val overlayColor: Int = 0x4DF44336.toInt()
    }
}
