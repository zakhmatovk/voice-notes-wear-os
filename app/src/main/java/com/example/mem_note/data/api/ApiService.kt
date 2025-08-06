package com.example.mem_note.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("notes")
    suspend fun sendNote(@Body note: NoteRequest): Response<Unit>
}

data class NoteRequest(
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
) 