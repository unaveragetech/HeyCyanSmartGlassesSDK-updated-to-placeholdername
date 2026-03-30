package com.sdk.glassessdksample

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.sdk.glassessdksample.feature.LLMProvider
import com.sdk.glassessdksample.feature.LlamaCppService
import com.sdk.glassessdksample.feature.TinyLLMService

class LLMChatActivity : AppCompatActivity() {
    private lateinit var historyText: TextView
    private lateinit var promptInput: EditText
    private lateinit var sendButton: Button
    private lateinit var clearButton: Button
    private lateinit var explainImageButton: Button
    private lateinit var modelSpinner: Spinner
    private lateinit var modelStatus: TextView

    private var selectedLLM: LLMProvider = TinyLLMService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_llm_chat)

        historyText = findViewById(R.id.tv_history)
        promptInput = findViewById(R.id.et_prompt)
        sendButton = findViewById(R.id.btn_send_prompt)
        clearButton = findViewById(R.id.btn_clear_history)
        explainImageButton = findViewById(R.id.btn_explain_image)
        modelSpinner = findViewById(R.id.spinner_model)
        modelStatus = findViewById(R.id.tv_model_status)

        val modelNames = listOf("Tiny LLM", "llama.cpp local")
        modelSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, modelNames).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        modelSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedLLM = when (position) {
                    0 -> TinyLLMService
                    1 -> LlamaCppService
                    else -> TinyLLMService
                }
                val status = if (selectedLLM.isAvailable()) "available" else "unavailable"
                modelStatus.text = "Model: ${modelNames[position]} ($status)"
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
            }
        }

        refreshHistory()

        sendButton.setOnClickListener {
            val prompt = promptInput.text.toString().trim()
            if (prompt.isNotEmpty()) {
                val answer = selectedLLM.ask(prompt)
                selectedLLM.remember(prompt, answer)
                promptInput.text.clear()
                refreshHistory()
            }
        }

        clearButton.setOnClickListener {
            // clear history by reflection (not ideal but okay for prototype)
            try {
                val memField = TinyLLMService::class.java.getDeclaredField("memory")
                memField.isAccessible = true
                val memory = memField.get(TinyLLMService) as MutableList<*>
                memory.clear()
            } catch (_: Exception) {
            }
            refreshHistory()
        }

        explainImageButton.setOnClickListener {
            val answer = selectedLLM.ask("What is in this image?", "a photo of glasses recording interface")
            selectedLLM.remember("What is in this image?", answer)
            refreshHistory()
        }
    }

    private fun refreshHistory() {
        val lines = TinyLLMService.conversationHistory().joinToString(separator = "\n\n") { (q, a) -> "Q: $q\nA: $a" }
        historyText.text = if (lines.isEmpty()) "Conversation history" else lines
    }
}
