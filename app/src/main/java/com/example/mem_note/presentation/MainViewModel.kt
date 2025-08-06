package com.example.mem_note.presentation

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mem_note.data.repository.SpeechRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MainUiState(
    val isListening: Boolean = false,
    val recognizedText: String = "",
    val errorMessage: String = "",
    val isSending: Boolean = false,
    val lastSentText: String = "",
    val shouldStartNewRecognition: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    
    private val speechRepository = SpeechRepository(application)
    
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
    
    fun setListening(listening: Boolean) {
        _uiState.value = _uiState.value.copy(isListening = listening)
    }
    
    fun setError(error: String) {
        _uiState.value = _uiState.value.copy(
            isListening = false,
            errorMessage = error
        )
    }
    
    fun createSpeechIntent(): Intent {
        return speechRepository.createSpeechIntent()
    }
    
    fun processSpeechResult(data: Intent?): String? {
        return speechRepository.processSpeechResult(data)
    }
    
    fun sendRecognizedText(text: String) {
        _uiState.value = _uiState.value.copy(
            isListening = false,
            recognizedText = text
        )
        sendNoteToApi(text)
    }
    
    fun resetRecognitionFlag() {
        _uiState.value = _uiState.value.copy(shouldStartNewRecognition = false)
    }
    
    private fun sendNoteToApi(text: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSending = true)
            
            val success = speechRepository.sendNoteToApi(text)
            
            _uiState.value = _uiState.value.copy(
                isSending = false,
                lastSentText = if (success) text else "",
                errorMessage = if (!success) "Ошибка отправки данных" else "",
                recognizedText = if (success) "" else _uiState.value.recognizedText,
                shouldStartNewRecognition = success
            )
        }
    }
} 