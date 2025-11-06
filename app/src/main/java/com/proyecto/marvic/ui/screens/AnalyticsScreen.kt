package com.proyecto.marvic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.proyecto.marvic.ui.theme.MarvicOrange
import com.proyecto.marvic.viewmodel.AssistantViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    vm: AssistantViewModel = viewModel(),
    onBack: () -> Unit = {}
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    // Scroll al final cuando hay nuevos mensajes
    LaunchedEffect(vm.messages.size) {
        if (vm.messages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(vm.messages.size - 1)
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Asistente de IA", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Volver", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { vm.clearChat() }) {
                        Icon(Icons.Default.DeleteSweep, "Limpiar chat", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MarvicOrange,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF121212))
                .padding(padding)
        ) {
            // Área de mensajes
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                if (vm.messages.isEmpty()) {
                    item {
                        WelcomeMessage()
                    }
                }
                
                items(vm.messages) { message ->
                    ChatBubble(message = message)
                }
                
                if (vm.isLoading) {
                    item {
                        TypingIndicator()
                    }
                }
            }
            
            // Área de entrada
            ChatInputBar(
                userInput = vm.userInput,
                onInputChange = { vm.updateUserInput(it) },
                onSend = { vm.sendMessage() },
                enabled = !vm.isLoading && vm.userInput.isNotBlank()
            )
        }
    }
}

@Composable
fun WelcomeMessage() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.SmartToy,
                contentDescription = null,
                tint = MarvicOrange,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "¡Hola! Soy tu Asistente de IA",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Puedo ayudarte con:\n" +
                "• Consultas sobre inventario\n" +
                "• Análisis de movimientos\n" +
                "• Recomendaciones de stock\n" +
                "• Información sobre materiales",
                color = Color(0xFFBDBDBD),
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun ChatBubble(message: com.proyecto.marvic.viewmodel.ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!message.isUser) {
            Icon(
                Icons.Default.SmartToy,
                contentDescription = null,
                tint = MarvicOrange,
                modifier = Modifier
                    .size(32.dp)
                    .padding(end = 8.dp, top = 4.dp)
            )
        }
        
        Card(
            modifier = Modifier.widthIn(max = 280.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (message.isUser) MarvicOrange else Color(0xFF2A2A2A)
            ),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isUser) 16.dp else 4.dp,
                bottomEnd = if (message.isUser) 4.dp else 16.dp
            )
        ) {
            Text(
                text = message.text,
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier.padding(12.dp),
                lineHeight = 20.sp
            )
        }
        
        if (message.isUser) {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                tint = Color(0xFFBDBDBD),
                modifier = Modifier
                    .size(32.dp)
                    .padding(start = 8.dp, top = 4.dp)
            )
        }
    }
}

@Composable
fun TypingIndicator() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.SmartToy,
            contentDescription = null,
            tint = MarvicOrange,
            modifier = Modifier
                .size(32.dp)
                .padding(end = 8.dp)
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(3) { index ->
                    CircularProgressIndicator(
                        modifier = Modifier.size(8.dp),
                        color = MarvicOrange,
                        strokeWidth = 2.dp
                    )
                }
            }
        }
    }
}

@Composable
fun ChatInputBar(
    userInput: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    enabled: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF1A1A1A),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = userInput,
                onValueChange = onInputChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Escribe tu pregunta...", color = Color(0xFFBDBDBD)) },
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = MarvicOrange,
                    unfocusedBorderColor = Color(0xFF424242),
                    cursorColor = MarvicOrange
                ),
                shape = RoundedCornerShape(24.dp),
                maxLines = 3,
                singleLine = false
            )
            
            FloatingActionButton(
                onClick = onSend,
                modifier = Modifier.size(48.dp),
                containerColor = if (enabled) MarvicOrange else Color(0xFF424242),
                contentColor = Color.White
            ) {
                if (enabled) {
                    Icon(Icons.Default.Send, contentDescription = "Enviar")
                } else {
                    Icon(Icons.Default.Send, contentDescription = "Enviar", tint = Color(0xFF757575))
                }
            }
        }
    }
}
