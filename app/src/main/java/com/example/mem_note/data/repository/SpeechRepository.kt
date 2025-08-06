package com.example.mem_note.data.repository

import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import com.example.mem_note.data.api.ApiClient
import com.example.mem_note.data.api.NoteRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

class SpeechRepository(private val context: Context) {
    
    private val apiService = ApiClient.apiService
    
    fun createSpeechIntent(): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Говорите...")
        }
    }
    
    fun processSpeechResult(data: Intent?): String? {
        return data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.get(0)
    }
    
    suspend fun sendNoteToApi(text: String): Boolean {
        return try {
            withContext(Dispatchers.IO) {
                val response = apiService.sendNote(NoteRequest(text))
                response.isSuccessful
            }
        } catch (e: Exception) {
            false
        }
    }
} 