package com.sdk.glassessdksample.feature

interface LLMProvider {
    fun ask(question: String, imageHint: String? = null): String
    fun remember(user: String, response: String)
    fun conversationHistory(): List<Pair<String, String>>
    fun isAvailable(): Boolean
}

object TinyLLMService : LLMProvider {
    private val memory = mutableListOf<Pair<String, String>>()

    override fun ask(question: String, imageHint: String?): String {
        val normalized = question.trim().lowercase()
        if (normalized.contains("what") && normalized.contains("image")) {
            val hint = imageHint ?: "an image of the glasses UI"
            val result = "Looks like $hint. It may contain an AR display and can capture photo/video."
            remember(question, result)
            return result
        }

        if (normalized.contains("battery")) {
            val result = "Use syncBattery() in DeviceCommandService to get current battery state from glasses."
            remember(question, result)
            return result
        }

        if (normalized.contains("how") && normalized.contains("record")) {
            val result = "Use startVideo() and stopVideo() to control video capture, startAudio()/stopAudio() for audio capture."
            remember(question, result)
            return result
        }

        val fallback = "I am a tiny local model. I can answer your platform usage and recommend DeviceCommandService methods."
        remember(question, fallback)
        return fallback
    }

    override fun remember(user: String, response: String) {
        memory.add(user to response)
        if (memory.size > 20) memory.removeAt(0)
    }

    override fun conversationHistory(): List<Pair<String, String>> = memory.toList()

    override fun isAvailable(): Boolean = true
}
