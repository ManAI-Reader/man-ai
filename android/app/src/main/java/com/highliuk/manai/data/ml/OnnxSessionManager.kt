package com.highliuk.manai.data.ml

import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import java.io.File
import java.io.FileOutputStream

class OnnxSessionManager(private val context: Context) {

    private val filesDir: String = context.filesDir.absolutePath

    val ortEnv: OrtEnvironment by lazy { OrtEnvironment.getEnvironment() }

    val detectorSession: OrtSession by lazy {
        createSession("textdetector.onnx", threadCount = 4)
    }

    val encoderSession: OrtSession by lazy {
        createSession("encoder.onnx", threadCount = 2)
    }

    val decoderFirstSession: OrtSession by lazy {
        createSession("decoder_first.onnx", threadCount = 2)
    }

    val decoderWithPastSession: OrtSession by lazy {
        createSession("decoder_with_past.onnx", threadCount = 2)
    }

    val vocab: List<String> by lazy {
        context.assets.open("models/vocab.txt").bufferedReader().readLines()
    }

    fun getModelPath(modelName: String): String {
        return "$filesDir/models/$modelName"
    }

    private fun createSession(fileName: String, threadCount: Int): OrtSession {
        val modelPath = copyAssetToDisk(fileName)
        val opts = OrtSession.SessionOptions().apply {
            setIntraOpNumThreads(threadCount)
        }
        return ortEnv.createSession(modelPath, opts)
    }

    internal fun copyAssetToDisk(fileName: String): String {
        val modelDir = File(context.filesDir, "models").apply { mkdirs() }
        val outFile = File(modelDir, fileName)
        if (!outFile.exists()) {
            context.assets.open("models/$fileName").use { input ->
                FileOutputStream(outFile).use { output -> input.copyTo(output) }
            }
        }
        return outFile.absolutePath
    }
}
