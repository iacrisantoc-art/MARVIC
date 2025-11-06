package com.proyecto.marvic.ai

import com.google.ai.client.generativeai.GenerativeModel
import com.proyecto.marvic.BuildConfig

class GeminiAIEngine {

    private val generativeModel = GenerativeModel(
        modelName = "gemini-pro",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    suspend fun getResponse(prompt: String): String {
        return try {
            val response = generativeModel.generateContent(prompt)
            response.text ?: "No se pudo obtener una respuesta del modelo."
        } catch (e: Exception) {
            "Error al contactar al modelo de IA: ${e.message}"
        }
    }
}