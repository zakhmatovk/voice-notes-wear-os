/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter to find the
 * most up to date changes to the libraries and their usages.
 */

package com.example.mem_note.presentation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.*
import com.example.mem_note.presentation.theme.MemnoteTheme

class MainActivity : ComponentActivity() {
    
    private val viewModel: MainViewModel by viewModels()
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Разрешение получено
        } else {
            // Пользователь отказал в разрешении
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Проверяем разрешение на запись аудио
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Разрешение уже есть
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
        
        setContent {
            MemnoteTheme {
                SpeechRecognitionScreen(viewModel)
            }
        }
    }
}

@Composable
fun SpeechRecognitionScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // Launcher для распознавания речи согласно официальной документации
    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { activityResult ->
        if (activityResult.resultCode == android.app.Activity.RESULT_OK) {
            val recognizedText = viewModel.processSpeechResult(activityResult.data)
            if (recognizedText != null) {
                viewModel.sendRecognizedText(recognizedText)
            }
        } else {
            viewModel.setError("Распознавание речи было отменено")
        }
    }
    
    // Автоматический запуск распознавания при открытии приложения
    LaunchedEffect(Unit) {
        viewModel.setListening(true)
        voiceLauncher.launch(viewModel.createSpeechIntent())
    }
    
    // Автоматический перезапуск распознавания после успешной отправки
    LaunchedEffect(uiState.shouldStartNewRecognition) {
        if (uiState.shouldStartNewRecognition) {
            viewModel.resetRecognitionFlag()
            viewModel.setListening(true)
            voiceLauncher.launch(viewModel.createSpeechIntent())
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Статус распознавания
            Text(
                text = when {
                    uiState.isListening -> "Слушаю..."
                    uiState.isSending -> "Отправляю..."
                    uiState.errorMessage.isNotEmpty() -> "Ошибка: ${uiState.errorMessage}"
                    uiState.lastSentText.isNotEmpty() -> "Отправлено: ${uiState.lastSentText}"
                    else -> "Нажмите кнопку для записи"
                },
                style = MaterialTheme.typography.body1,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.primary,
                modifier = Modifier.padding(16.dp)
            )
            
            // Распознанный текст
            if (uiState.recognizedText.isNotEmpty()) {
                Text(
                    text = uiState.recognizedText,
                    style = MaterialTheme.typography.body2,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.onBackground,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            
            // Кнопка записи
            Button(
                onClick = { 
                    viewModel.setListening(true)
                    voiceLauncher.launch(viewModel.createSpeechIntent())
                },
                enabled = !uiState.isListening && !uiState.isSending,
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
            ) {
                Text(
                    text = if (uiState.isListening) "⏹" else "🎤",
                    fontSize = 24.sp
                )
            }
        }
    }
}