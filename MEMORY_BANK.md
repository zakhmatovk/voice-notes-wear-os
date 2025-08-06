# Мемори Банк - Приложение Распознавания Речи для Wear OS

## Обзор проекта

**Цель**: Создать простое приложение для Wear OS, которое автоматически распознает речь и отправляет данные в API.

**Технологии**: Kotlin, Jetpack Compose, Speech Recognition API, Retrofit, Coroutines

## Архитектура проекта

### Структура пакетов
```
com.example.mem_note/
├── data/
│   ├── api/           # API клиент и интерфейсы
│   ├── repository/    # Репозитории для работы с данными
│   └── queue/         # Очередь для оффлайн режима
├── presentation/      # UI компоненты и ViewModels
└── domain/           # Бизнес-логика (будущее расширение)
```

### Ключевые компоненты

#### 1. API слой
- **ApiService**: Интерфейс для HTTP запросов
- **ApiClient**: Конфигурация Retrofit клиента
- **NoteRequest**: DTO для отправки данных

#### 2. Репозиторий
- **SpeechRepository**: Управление распознаванием речи и отправкой данных
- Использование `startActivityForResult()` согласно официальной документации
- Обработка результатов распознавания

#### 3. Presentation слой
- **MainViewModel**: Управление состоянием UI
- **MainActivity**: Основной экран приложения
- **MainUiState**: Состояние интерфейса

## Первый этап - Базовая функциональность

### Реализованные возможности:
1. **Автоматический запуск** распознавания при открытии приложения
2. **Кнопка записи** по центру экрана для ручного запуска
3. **Автоматический перезапуск** распознавания после успешной отправки
4. **Отправка данных** в API после успешного распознавания
5. **Обработка ошибок** и статусов
6. **Проверка разрешений** и доступности распознавания

### Ключевые файлы первого этапа:
- `MainActivity.kt` - UI с кнопкой записи и автоматическим запуском
- `MainViewModel.kt` - Логика управления состоянием
- `SpeechRepository.kt` - Работа с распознаванием речи
- `ApiService.kt` - Интерфейс API
- `ApiClient.kt` - Конфигурация HTTP клиента

### Разрешения:
- `RECORD_AUDIO` - для распознавания речи
- `INTERNET` - для отправки данных
- `ACCESS_NETWORK_STATE` - для проверки соединения

## Реализация распознавания речи согласно официальной документации

### Использование `startActivityForResult()`:

Согласно [официальной документации Android](https://developer.android.com/training/wearables/user-input/voice?hl=ru), для Wear OS рекомендуется использовать `startActivityForResult()` с `ACTION_RECOGNIZE_SPEECH` вместо `SpeechRecognizer`.

### Ключевые изменения:

#### 1. SpeechRepository
```kotlin
class SpeechRepository(private val context: Context) {
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
}
```

#### 2. MainActivity с rememberLauncherForActivityResult
```kotlin
val voiceLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.StartActivityForResult()
) { activityResult ->
    if (activityResult.resultCode == Activity.RESULT_OK) {
        val recognizedText = viewModel.processSpeechResult(activityResult.data)
        if (recognizedText != null) {
            viewModel.sendRecognizedText(recognizedText)
        }
    } else {
        viewModel.setError("Распознавание речи было отменено")
    }
}
```

#### 3. Автоматический запуск и перезапуск
```kotlin
// Автоматический запуск при открытии приложения
LaunchedEffect(Unit) {
    viewModel.setListening(true)
    voiceLauncher.launch(viewModel.createSpeechIntent())
}

// Автоматический перезапуск после успешной отправки
LaunchedEffect(uiState.shouldStartNewRecognition) {
    if (uiState.shouldStartNewRecognition) {
        viewModel.resetRecognitionFlag()
        viewModel.setListening(true)
        voiceLauncher.launch(viewModel.createSpeechIntent())
    }
}
```

### Преимущества нового подхода:
1. **Официальная рекомендация**: Следует документации Android для Wear OS
2. **Простота**: Не требует управления жизненным циклом SpeechRecognizer
3. **Надежность**: Использует встроенную систему распознавания
4. **Совместимость**: Работает на всех устройствах Wear OS
5. **Автоматизация**: Непрерывное распознавание речи

## Второй этап - Очередь и оффлайн режим

### Планируемые компоненты:

#### 1. Очередь данных
```kotlin
class NoteQueue {
    private val queue = ConcurrentLinkedQueue<NoteRequest>()
    
    fun addNote(note: NoteRequest)
    fun getNextNote(): NoteRequest?
    fun isEmpty(): Boolean
    fun size(): Int
}
```

#### 2. Фоновый процесс
```kotlin
class BackgroundProcessor(
    private val queue: NoteQueue,
    private val apiService: ApiService
) {
    suspend fun processQueue()
    suspend fun sendNote(note: NoteRequest): Boolean
}
```

#### 3. Сохранение в файл
```kotlin
class QueueStorage(private val context: Context) {
    fun saveQueue(queue: List<NoteRequest>)
    fun loadQueue(): List<NoteRequest>
}
```

### Логика работы:
1. При распознавании → добавление в очередь
2. Фоновый процесс → отправка из очереди
3. При закрытии → сохранение очереди в файл
4. При запуске → восстановление очереди

## Тестирование

### Структура тестов:
```
androidTest/
├── MainActivityTest.kt      # UI тесты
├── SpeechRepositoryTest.kt  # Тесты репозитория
├── NoteQueueTest.kt        # Тесты очереди
└── BackgroundProcessorTest.kt # Тесты фонового процесса
```

### Ключевые тестовые сценарии:
1. **UI тесты**: Проверка кнопки, статусов
2. **Интеграционные тесты**: Распознавание → Очередь → API
3. **Оффлайн тесты**: Сохранение/восстановление очереди
4. **Автоматизация тесты**: Проверка автоматического запуска

## Зависимости

### Основные библиотеки:
```kotlin
// HTTP requests
implementation("com.squareup.okhttp3:okhttp:4.12.0")
implementation("com.squareup.retrofit2:retrofit:2.9.0")
implementation("com.squareup.retrofit2:converter-gson:2.9.0")

// Coroutines
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

// ViewModel
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
```

**Примечание**: Убрана зависимость `androidx.speech:speech:1.0.0-alpha01` - используем стандартный Android Speech Recognition API с `startActivityForResult()`.

## Состояния приложения

### MainUiState:
```kotlin
data class MainUiState(
    val isListening: Boolean = false,           // Распознавание активно
    val recognizedText: String = "",            // Распознанный текст
    val errorMessage: String = "",              // Сообщение об ошибке
    val isSending: Boolean = false,            // Отправка в процессе
    val lastSentText: String = "",             // Последний отправленный текст
    val shouldStartNewRecognition: Boolean = false, // Флаг для автоматического перезапуска
    val queueSize: Int = 0                     // Размер очереди (этап 2)
)
```

## Важные детали реализации

### 1. Обработка разрешений
- Запрос `RECORD_AUDIO` при первом запуске
- Проверка разрешений перед распознаванием
- Обработка отказа в разрешении

### 2. Управление жизненным циклом
- Использование `rememberLauncherForActivityResult` для управления результатами
- Простое управление состоянием без сложных ресурсов
- Автоматическая очистка при уничтожении компонентов

### 3. Обработка ошибок
- Обработка отмены распознавания речи
- Ошибки сетевых запросов
- Ошибки сохранения/загрузки
- Проверка доступности распознавания

### 4. Производительность
- Использование Coroutines для асинхронных операций
- Минимальное количество файлов
- Простой UI без излишеств
- Эффективное использование системных ресурсов

### 5. Автоматизация
- **Автоматический запуск**: При открытии приложения сразу начинается распознавание
- **Непрерывное распознавание**: После успешной отправки автоматически запускается новое распознавание
- **Флаг перезапуска**: `shouldStartNewRecognition` управляет автоматическим перезапуском

## Конфигурация API

### Текущая конфигурация:
```kotlin
private const val BASE_URL = "https://api.example.com/"
```

### Замена на реальный API:
1. Изменить `BASE_URL` в `ApiClient.kt`
2. Настроить эндпоинты в `ApiService.kt`
3. Добавить аутентификацию при необходимости

## Планы развития

### Этап 1 ✅ (Завершен)
- [x] Базовая распознавание речи
- [x] Отправка в API
- [x] Простой UI
- [x] Базовые тесты
- [x] Использование официального API
- [x] Обработка недоступности распознавания
- [x] Автоматический запуск при открытии
- [x] Автоматический перезапуск после отправки

### Этап 2 🔄 (В разработке)
- [ ] Реализация очереди
- [ ] Фоновый процесс
- [ ] Сохранение/восстановление
- [ ] Оффлайн режим
- [ ] Расширенные тесты

### Будущие улучшения:
- [ ] Настройки приложения
- [ ] Статистика использования
- [ ] Поддержка разных языков
- [ ] Улучшенный UI

## Команды для сборки и тестирования

```bash
# Сборка проекта
./gradlew assembleDebug

# Запуск тестов
./gradlew connectedAndroidTest

# Очистка и пересборка
./gradlew clean build
```

## Ключевые принципы разработки

1. **Простота**: Минимальное количество файлов и зависимостей
2. **Надежность**: Обработка всех возможных ошибок
3. **Тестируемость**: Покрытие тестами критических компонентов
4. **Производительность**: Эффективное использование ресурсов Wear OS
5. **Оффлайн-первый**: Работа без интернета с последующей синхронизацией
6. **Безопасность**: Проверка разрешений и доступности функций
7. **Соответствие стандартам**: Использование официальных API Android
8. **Автоматизация**: Непрерывное распознавание без вмешательства пользователя 