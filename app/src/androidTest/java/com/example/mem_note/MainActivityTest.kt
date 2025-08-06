package com.example.mem_note

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.mem_note.presentation.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testInitialState() {
        // Проверяем, что приложение запускается и показывает кнопку записи
        composeTestRule.onNodeWithText("Нажмите кнопку для записи").assertIsDisplayed()
    }

    @Test
    fun testButtonClick() {
        // Проверяем, что кнопка реагирует на нажатие
        composeTestRule.onNodeWithText("🎤").performClick()
        
        // После нажатия должен появиться статус "Слушаю..."
        composeTestRule.onNodeWithText("Слушаю...").assertIsDisplayed()
    }
} 