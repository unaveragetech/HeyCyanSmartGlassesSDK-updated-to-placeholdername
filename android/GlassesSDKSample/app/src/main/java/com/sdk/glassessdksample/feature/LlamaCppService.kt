package com.sdk.glassessdksample.feature

import android.os.Environment
import java.io.File

object LlamaCppService : LLMProvider {
    private val memory = mutableListOf<Pair<String, String>>()

    private val modelBaseDir = File(Environment.getExternalStorageDirectory(), "llama-model")

    override fun ask(question: String, imageHint: String?): String {
        val response = if (isAvailable()) {
            // In a complete version, run the actual llama.cpp inference with JNI wrapper
            "[llama.cpp local model] Simulated result for '$question'"
        } else {
            "llama.cpp model not found. Please place model files under ${modelBaseDir.absolutePath}."
        }
        remember(question, response)
        return response
    }

    override fun remember(user: String, response: String) {
        memory.add(user to response)
        if (memory.size > 30) memory.removeAt(0)
    }

    override fun conversationHistory(): List<Pair<String, String>> = memory.toList()

    override fun isAvailable(): Boolean {
        val modelFile1 = File(modelBaseDir, "ggml-model-q4_0.bin")
        val modelFile2 = File(modelBaseDir, "ggml-model.bin")
        return modelFile1.exists() || modelFile2.exists()
    }
}
