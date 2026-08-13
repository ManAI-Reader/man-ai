package com.highliuk.manai.data.logging

import android.util.Log
import com.highliuk.manai.domain.logging.Logger
import javax.inject.Inject

class AndroidLogger @Inject constructor() : Logger {
    override fun e(tag: String, message: String, throwable: Throwable?) {
        Log.e(tag, message, throwable)
    }
}
