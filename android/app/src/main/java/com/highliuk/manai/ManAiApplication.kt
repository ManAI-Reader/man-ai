package com.highliuk.manai

import android.app.Application
import com.highliuk.manai.domain.usecase.WarmUpOnnxUseCase
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class ManAiApplication : Application() {

    @Inject
    lateinit var warmUpOnnxUseCase: WarmUpOnnxUseCase

    override fun onCreate() {
        super.onCreate()
        CoroutineScope(Dispatchers.Default + SupervisorJob()).launch {
            warmUpOnnxUseCase.execute()
        }
    }
}
