package com.proyecto.marvic.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proyecto.marvic.ai.GeminiService
import kotlinx.coroutines.launch

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

class AssistantViewModel : ViewModel() {
    var messages by mutableStateOf<List<ChatMessage>>(emptyList())
        private set
    
    var isLoading by mutableStateOf(false)
        private set
    
    var errorMessage by mutableStateOf<String?>(null)
        private set
    
    var userInput by mutableStateOf("")
        private set
    
    fun updateUserInput(input: String) {
        userInput = input
    }
    
    fun sendMessage() {
        if (userInput.isBlank() || isLoading) return
        
        val userMessage = ChatMessage(userInput, isUser = true)
        messages = messages + userMessage
        val currentInput = userInput
        userInput = ""
        isLoading = true
        errorMessage = null
        
        viewModelScope.launch {
            try {
                // Obtener contexto del inventario
                val context = GeminiService.getInventoryContext()
                
                // Enviar mensaje a Gemini
                val result = GeminiService.chat(currentInput, context)
                
                if (result.isSuccess) {
                    val responseText = result.getOrDefault("No se pudo generar una respuesta.")
                    val assistantMessage = ChatMessage(
                        responseText,
                        isUser = false
                    )
                    messages = messages + assistantMessage
                    println("✅ [AssistantViewModel] Mensaje agregado exitosamente")
                } else {
                    val exception = result.exceptionOrNull()
                    val errorMsgText = exception?.message ?: "Error desconocido"
                    errorMessage = errorMsgText
                    println("❌ [AssistantViewModel] Error: $errorMsgText")
                    exception?.printStackTrace()
                    
                    val errorMsg = ChatMessage(
                        "Lo siento, hubo un error al procesar tu mensaje.\n\nDetalle: $errorMsgText\n\nPor favor intenta de nuevo.",
                        isUser = false
                    )
                    messages = messages + errorMsg
                }
            } catch (e: Exception) {
                errorMessage = e.message
                val errorMsg = ChatMessage(
                    "Error: ${e.message}",
                    isUser = false
                )
                messages = messages + errorMsg
            } finally {
                isLoading = false
            }
        }
    }
    
    fun clearChat() {
        messages = emptyList()
        errorMessage = null
    }
}

