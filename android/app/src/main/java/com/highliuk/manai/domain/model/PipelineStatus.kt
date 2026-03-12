package com.highliuk.manai.domain.model

sealed interface PipelineStatus {
    val overlayColor: Int

    data object Queued : PipelineStatus {
        override val overlayColor: Int = 0x4DFF9800.toInt()
    }
    data object Processing : PipelineStatus {
        override val overlayColor: Int = 0x4DFFEB3B.toInt()
    }
    data object Done : PipelineStatus {
        override val overlayColor: Int = 0x4D4CAF50.toInt()
    }
    data object CacheHit : PipelineStatus {
        override val overlayColor: Int = 0x4D2196F3.toInt()
    }
    data class Error(val message: String) : PipelineStatus {
        override val overlayColor: Int = 0x4DF44336.toInt()
    }
}
