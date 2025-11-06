package com.proyecto.marvic.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import java.net.HttpURLConnection
import java.net.URL

@Serializable
data class GeminiRequest(
    val contents: List<Content>
)

@Serializable
data class Content(
    val parts: List<Part>
)

@Serializable
data class Part(
    val text: String
)

@Serializable
data class GeminiResponse(
    val candidates: List<Candidate>? = null,
    val error: GeminiError? = null
)

@Serializable
data class Candidate(
    val content: Content? = null,
    val finishReason: String? = null
)

@Serializable
data class GeminiError(
    val code: Int? = null,
    val message: String? = null,
    val status: String? = null
)

object GeminiService {
    private const val API_KEY = "AIzaSyC0ew0UaA5fZ6rhVtIVGQuqHhQItiUFFJk"
    private const val API_URL = "https://generativelanguage.googleapis.com/v1/models/gemini-1.5-flash-latest:generateContent?key=$API_KEY"
    
    suspend fun chat(prompt: String, context: String = ""): Result<String> {
        return try {
            withContext(Dispatchers.IO) {
                val fullPrompt = if (context.isNotEmpty()) {
                    """
                    Eres un asistente de IA especializado en gestión de inventario para la empresa Grupo Marvic.
                    
                    Contexto del sistema:
                    $context
                    
                    Usuario pregunta: $prompt
                    
                    Responde de manera clara, concisa y útil. Si la pregunta está relacionada con inventario, materiales o movimientos, usa el contexto proporcionado.
                    """.trimIndent()
                } else {
                    """
                    Eres un asistente de IA especializado en gestión de inventario para la empresa Grupo Marvic.
                    
                    Usuario pregunta: $prompt
                    
                    Responde de manera clara, concisa y útil sobre temas de inventario, materiales, movimientos y gestión de almacén.
                    """.trimIndent()
                }
                
                val request = GeminiRequest(
                    contents = listOf(
                        Content(
                            parts = listOf(Part(text = fullPrompt))
                        )
                    )
                )
                
                val json = Json { 
                    ignoreUnknownKeys = true
                    isLenient = true
                }
                val requestBody = json.encodeToString(request)
                
                println("📤 [GeminiService] Enviando petición a Gemini API...")
                println("📤 [GeminiService] Request body: $requestBody")
                
                val url = URL(API_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Accept", "application/json")
                connection.doOutput = true
                connection.connectTimeout = 30000
                connection.readTimeout = 30000
                
                connection.outputStream.use { output ->
                    output.write(requestBody.toByteArray(Charsets.UTF_8))
                }
                
                val responseCode = connection.responseCode
                println("📥 [GeminiService] Response code: $responseCode")
                
                when {
                    responseCode == HttpURLConnection.HTTP_OK -> {
                        val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
                        println("📥 [GeminiService] Response body: $responseBody")
                        
                        try {
                            val response = json.decodeFromString<GeminiResponse>(responseBody)
                            
                            // Verificar si hay error en la respuesta
                            if (response.error != null) {
                                val errorMsg = response.error.message ?: "Error desconocido de Gemini"
                                println("❌ [GeminiService] Error en respuesta: $errorMsg")
                                Result.failure(Exception("Error de Gemini: $errorMsg"))
                            } else {
                                val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                                if (text != null && text.isNotBlank()) {
                                    println("✅ [GeminiService] Respuesta exitosa: ${text.take(100)}...")
                                    Result.success(text)
                                } else {
                                    println("⚠️ [GeminiService] Respuesta vacía o sin texto")
                                    Result.failure(Exception("No se pudo generar una respuesta válida."))
                                }
                            }
                        } catch (e: Exception) {
                            println("❌ [GeminiService] Error parseando respuesta: ${e.message}")
                            println("❌ [GeminiService] Response body completo: $responseBody")
                            e.printStackTrace()
                            Result.failure(Exception("Error parseando respuesta: ${e.message}"))
                        }
                    }
                    else -> {
                        val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Error desconocido"
                        println("❌ [GeminiService] Error HTTP $responseCode: $errorBody")
                        Result.failure(Exception("Error HTTP $responseCode: $errorBody"))
                    }
                }
            }
        } catch (e: Exception) {
            println("❌ [GeminiService] Error: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    suspend fun getInventoryContext(): String {
        // Obtener contexto del inventario para mejorar las respuestas
        return try {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val materialsSnapshot = db.collection("materials").limit(10).get().await()
            val movementsSnapshot = db.collection("movements").orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING).limit(5).get().await()
            
            val materials = materialsSnapshot.documents.joinToString("\n") { doc ->
                "- ${doc.getString("nombre") ?: "Sin nombre"}: ${doc.getLong("cantidad") ?: 0} unidades"
            }
            
            val movements = movementsSnapshot.documents.joinToString("\n") { doc ->
                val delta = doc.getLong("delta") ?: 0L
                val tipo = if (delta > 0) "Entrada" else "Salida"
                "- $tipo: ${kotlin.math.abs(delta.toInt())} unidades"
            }
            
            """
            Materiales en inventario:
            $materials
            
            Movimientos recientes:
            $movements
            """.trimIndent()
        } catch (e: Exception) {
            println("⚠️ [GeminiService] Error obteniendo contexto: ${e.message}")
            ""
        }
    }
}
