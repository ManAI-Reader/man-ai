package com.highliuk.manai.domain.logging

interface Logger {
    fun e(tag: String, message: String, throwable: Throwable? = null)
}
