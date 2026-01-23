package com.example.military_holidays_tv_calendar

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.atan2
import kotlin.math.sqrt
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.example.military_holidays_tv_calendar.data.AppPreferences
import com.example.military_holidays_tv_calendar.repository.HolidayRepository
import com.example.military_holidays_tv_calendar.ui.FirstLaunchDialog
import com.example.military_holidays_tv_calendar.ui.SettingsDialog
import com.example.military_holidays_tv_calendar.ui.UiState
import com.example.military_holidays_tv_calendar.ui.theme.MilitaryholidaystvcalendarTheme
import com.example.military_holidays_tv_calendar.utils.TvUtils
import com.example.military_holidays_tv_calendar.utils.AutostartPermissionHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import android.graphics.BitmapFactory
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import java.io.IOException

private const val TAG = "MainActivity"

// Дата начала СВО
private val SVO_START_DATE = LocalDate.of(2022, 2, 24)

/**
 * Получает название дня недели на русском языке
 */
private fun getDayOfWeekInRussian(date: LocalDate): String {
    val dayOfWeekFormatter = DateTimeFormatter.ofPattern("EEEE", Locale("ru", "RU"))
    val dayName = date.format(dayOfWeekFormatter)
    // Делаем первую букву заглавной
    return if (dayName.isNotEmpty()) {
        dayName.substring(0, 1).uppercase(Locale("ru", "RU")) + dayName.substring(1)
    } else {
        dayName
    }
}

/**
 * Рассчитывает количество дней с начала СВО
 * 24.02.2022 считается первым днем, поэтому добавляем 1
 */
private fun getDaysSinceSVOStart(currentDate: LocalDate): Long {
    return ChronoUnit.DAYS.between(SVO_START_DATE, currentDate) + 1
}

/**
 * Форматирует количество дней СВО в строку
 */
private fun formatSvoDays(days: Long): String {
    return "$days-й день СВО"
}

class MainActivity : ComponentActivity() {
    
    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Установка полноэкранного режима
        setupFullscreen()
        
        setContent {
            MilitaryholidaystvcalendarTheme {
                MainScreen()
            }
        }
    }
    
    
    private fun setupFullscreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Предотвращаем засыпание экрана пока приложение активно
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController?.apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val holidayRepository = remember { HolidayRepository(context) }
    val preferences = remember { AppPreferences(context) }
    val scope = rememberCoroutineScope()
    
    val moscowZone = ZoneId.of("Europe/Moscow")
    val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    
    // Состояния для диалогов
    var showFirstLaunchDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showToast by remember { mutableStateOf<String?>(null) }
    
    // Загружаем настройки
    val isFirstLaunchFlow by preferences.isFirstLaunch.collectAsState(initial = true)
    val autoStartEnabled by preferences.autoStartEnabled.collectAsState(initial = false)
    
    // Проверяем первый запуск синхронно при инициализации
    var isFirstLaunchChecked by remember { mutableStateOf<Boolean?>(null) }
    
    LaunchedEffect(Unit) {
        val isFirst = preferences.getIsFirstLaunch()
        isFirstLaunchChecked = isFirst
        Log.d(TAG, "Проверка первого запуска: isFirstLaunch=$isFirst")
        
        if (isFirst && TvUtils.isAndroidTv(context) && !showFirstLaunchDialog) {
            Log.d(TAG, "Первый запуск обнаружен, показываем диалог первого запуска")
            showFirstLaunchDialog = true
        } else if (!isFirst) {
            Log.d(TAG, "Это не первый запуск, диалог не показываем")
        }
    }
    
    // Обновляем состояние при изменении Flow и проверяем снова
    LaunchedEffect(isFirstLaunchFlow) {
        if (isFirstLaunchChecked != isFirstLaunchFlow) {
            val oldValue = isFirstLaunchChecked
            isFirstLaunchChecked = isFirstLaunchFlow
            Log.d(TAG, "isFirstLaunch изменен через Flow: $oldValue -> $isFirstLaunchFlow")
            
            // Если значение изменилось на false, скрываем диалог
            if (!isFirstLaunchFlow && showFirstLaunchDialog) {
                Log.d(TAG, "firstLaunch стал false, скрываем диалог")
                showFirstLaunchDialog = false
            }
        }
    }
    
    // Логируем изменения состояния диалогов
    LaunchedEffect(showFirstLaunchDialog) {
        if (showFirstLaunchDialog) {
            Log.d(TAG, "Диалог первого запуска показан")
        } else {
            Log.d(TAG, "Диалог первого запуска скрыт")
        }
    }
    
    LaunchedEffect(showSettingsDialog) {
        if (showSettingsDialog) {
            Log.d(TAG, "Диалог настроек показан")
        } else {
            Log.d(TAG, "Диалог настроек скрыт")
        }
    }
    
    var uiState by remember {
        mutableStateOf(
            UiState(
                currentDate = LocalDate.now(moscowZone),
                currentTime = LocalTime.now(moscowZone),
                backgroundImagePath = getBackgroundImagePath(
                    holidayRepository,
                    LocalDate.now(moscowZone)
                )
            )
        )
    }
    
    // Обновление времени каждую секунду и проверка праздников в полночь
    LaunchedEffect(Unit) {
        var lastCheckedDate = LocalDate.now(moscowZone)
        
        while (true) {
            delay(1000) // Обновление каждую секунду
            
            val now = LocalDate.now(moscowZone)
            val currentTime = LocalTime.now(moscowZone)
            
            // Проверяем, наступила ли полночь (новая дата)
            if (now != lastCheckedDate) {
                lastCheckedDate = now
                uiState = UiState(
                    currentDate = now,
                    currentTime = currentTime,
                    backgroundImagePath = getBackgroundImagePath(holidayRepository, now)
                )
            } else {
                // Обновляем только время
                uiState = uiState.copy(currentTime = currentTime)
            }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.tv.material3.MaterialTheme.colorScheme.background)
            .onKeyEvent { event ->
                // Обработка кнопки Назад для закрытия диалогов
                if (event.type == KeyEventType.KeyUp && event.key == Key.Back) {
                    if (showSettingsDialog) {
                        Log.d(TAG, "Кнопка Назад нажата, закрываем диалог настроек")
                        showSettingsDialog = false
                        true
                    } else if (showFirstLaunchDialog) {
                        Log.d(TAG, "Кнопка Назад нажата, закрываем диалог первого запуска (как 'Нет')")
                        scope.launch {
                            try {
                                preferences.setFirstLaunch(false)
                                preferences.setAutoStartEnabled(false)
                                showFirstLaunchDialog = false
                                Log.d(TAG, "Диалог первого запуска закрыт через кнопку Назад")
                            } catch (e: Exception) {
                                Log.e(TAG, "Ошибка при сохранении настроек", e)
                            }
                        }
                        true
                    } else {
                        false
                    }
                }
                // Обработка нажатия OK/Enter для открытия настроек
                else if (event.type == KeyEventType.KeyUp && !showFirstLaunchDialog && !showSettingsDialog) {
                    when (event.key) {
                        Key.DirectionCenter,
                        Key.Enter -> {
                            Log.d(TAG, "Нажатие OK/Enter обнаружено, открываем диалог настроек")
                            showSettingsDialog = true
                            true
                        }
                        else -> false
                    }
                } else {
                    false
                }
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                // Обработка тапа для открытия настроек
                if (!showFirstLaunchDialog && !showSettingsDialog) {
                    Log.d(TAG, "Тап по экрану обнаружен, открываем диалог настроек")
                    showSettingsDialog = true
                }
            }
    ) {
        // Фоновое изображение
        val bitmap = remember(uiState.backgroundImagePath) {
            loadImageFromAssets(context, uiState.backgroundImagePath)
        }
        
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        
        // Дата в левом верхнем углу без подложки, с крупным зелёным текстом и лёгкой обводкой
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(28.dp)
        ) {
            Column {
                Text(
                    text = uiState.currentDate.format(dateFormatter),
                    style = androidx.tv.material3.MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 80.sp,
                        fontWeight = FontWeight.SemiBold,
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.9f),
                            offset = Offset(3f, 3f),
                            blurRadius = 10f
                        )
                    ),
                    color = Color.White
                )
                Text(
                    text = getDayOfWeekInRussian(uiState.currentDate),
                    style = androidx.tv.material3.MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Medium,
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.9f),
                            offset = Offset(3f, 3f),
                            blurRadius = 10f
                        )
                    ),
                    color = Color.White,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
        
        // Время в правом верхнем углу без подложки, с крупным зелёным текстом и лёгкой обводкой
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(28.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = uiState.currentTime.format(timeFormatter),
                    style = androidx.tv.material3.MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 80.sp,
                        fontWeight = FontWeight.Bold,
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.9f),
                            offset = Offset(3f, 3f),
                            blurRadius = 10f
                        )
                    ),
                    color = Color.White
                )
                Text(
                    text = formatSvoDays(getDaysSinceSVOStart(uiState.currentDate)),
                    style = androidx.tv.material3.MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Medium,
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.9f),
                            offset = Offset(3f, 3f),
                            blurRadius = 10f
                        )
                    ),
                    color = Color.White,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
        
        val dateKey = String.format("%02d-%02d", uiState.currentDate.monthValue, uiState.currentDate.dayOfMonth)
        val holidayText = holidayRepository.getHolidayText(dateKey)
        
        if (holidayText != null) {
            val density = LocalDensity.current
            var screenWidth by remember { mutableStateOf(0.dp) }
            var screenHeight by remember { mutableStateOf(0.dp) }
            
            val goldenColor = Color(0xFFFFD700)
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { coordinates ->
                        screenWidth = with(density) { coordinates.size.width.toDp() }
                        screenHeight = with(density) { coordinates.size.height.toDp() }
                    }
            ) {
                val startX = 0.dp
                val startY = screenHeight / 3f
                val endX = screenWidth
                val endY = screenHeight * 1.55f / 3f
                
                val dx = with(density) { (endX - startX).toPx() }
                val dy = with(density) { (endY - startY).toPx() }
                val angleRadians = atan2(dy.toDouble(), dx.toDouble())
                val angle = (angleRadians * 180.0 / kotlin.math.PI).toFloat() * 1.2f
                
                val screenDiagonal = with(density) { 
                    sqrt(screenWidth.toPx() * screenWidth.toPx() + screenHeight.toPx() * screenHeight.toPx())
                }
                val fontSize = with(density) { 
                    (screenDiagonal / 5f).coerceIn(100f, 350f).toSp() 
                }
                
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .rotate(angle)
                ) {
                    Text(
                        text = holidayText,
                        style = androidx.tv.material3.MaterialTheme.typography.headlineLarge.copy(
                            fontSize = fontSize,
                            fontWeight = FontWeight.Bold,
                            shadow = Shadow(
                                color = Color.Black.copy(alpha = 0.9f),
                                offset = Offset(3f, 3f),
                                blurRadius = 10f
                            )
                        ),
                        color = goldenColor
                    )
                }
            }
        }
        
        // Диалог первого запуска
        if (showFirstLaunchDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.7f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* Блокируем клики на фоне */ },
                contentAlignment = Alignment.Center
            ) {
                FirstLaunchDialog(
                    onYesClick = {
                        Log.d(TAG, "Обработка нажатия 'Да' в диалоге первого запуска")
                        scope.launch {
                            try {
                                preferences.setFirstLaunch(false)
                                Log.d(TAG, "firstLaunch установлен в false")
                                
                                preferences.setAutoStartEnabled(true)
                                Log.d(TAG, "autoStartEnabled установлен в true")
                                
                                // Проверяем что сохранилось
                                val savedFirstLaunch = preferences.getIsFirstLaunch()
                                val savedAutoStart = preferences.getAutoStartEnabled()
                                Log.d(TAG, "Проверка сохранения: firstLaunch=$savedFirstLaunch, autoStartEnabled=$savedAutoStart")
                                
                                showFirstLaunchDialog = false
                                
                                // Запрашиваем разрешение на показ поверх других окон (критично для автозапуска)
                                // Это откроет экран "Allow display over other apps"
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    Log.d(TAG, "Запрашиваем разрешение на показ поверх других окон")
                                    AutostartPermissionHelper.requestOverlayPermission(context)
                                    showToast = "Включите разрешение 'Показ поверх других окон' для автозапуска."
                                } else {
                                    // Для старых версий Android просто запрашиваем автозапуск
                                    AutostartPermissionHelper.requestAutostartPermission(context)
                                    showToast = "Включите автозапуск в открывшихся настройках системы."
                                }
                                
                                Log.d(TAG, "Автозапуск включен, запрошено разрешение, диалог первого запуска закрыт")
                            } catch (e: Exception) {
                                Log.e(TAG, "Ошибка при сохранении настроек", e)
                            }
                        }
                    },
                    onNoClick = {
                        Log.d(TAG, "Обработка нажатия 'Нет' в диалоге первого запуска")
                        scope.launch {
                            try {
                                preferences.setFirstLaunch(false)
                                Log.d(TAG, "firstLaunch установлен в false")
                                
                                preferences.setAutoStartEnabled(false)
                                Log.d(TAG, "autoStartEnabled установлен в false")
                                
                                // Проверяем что сохранилось
                                val savedFirstLaunch = preferences.getIsFirstLaunch()
                                val savedAutoStart = preferences.getAutoStartEnabled()
                                Log.d(TAG, "Проверка сохранения: firstLaunch=$savedFirstLaunch, autoStartEnabled=$savedAutoStart")
                                
                                showFirstLaunchDialog = false
                                Log.d(TAG, "Автозапуск отключен, диалог первого запуска закрыт")
                            } catch (e: Exception) {
                                Log.e(TAG, "Ошибка при сохранении настроек", e)
                            }
                        }
                    }
                )
            }
        }
        
        // Окно настроек
        if (showSettingsDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.7f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* Блокируем клики на фоне */ },
                contentAlignment = Alignment.Center
            ) {
                SettingsDialog(
                    autoStartEnabled = autoStartEnabled,
                    onSave = { enabled ->
                        Log.d(TAG, "Обработка нажатия 'Сохранить' в диалоге настроек, autoStartEnabled=$enabled")
                        scope.launch {
                            preferences.setAutoStartEnabled(enabled)
                            showSettingsDialog = false
                            
                            // Если автозапуск включен, запрашиваем системное разрешение
                            if (enabled) {
                                Log.d(TAG, "Автозапуск включен, запрашиваем системное разрешение")
                                
                                // Запрашиваем разрешение на показ поверх других окон (критично для автозапуска)
                                // Это откроет экран "Allow display over other apps"
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    Log.d(TAG, "Запрашиваем разрешение на показ поверх других окон")
                                    AutostartPermissionHelper.requestOverlayPermission(context)
                                    showToast = "Включите разрешение 'Показ поверх других окон' для автозапуска."
                                } else {
                                    // Для старых версий Android просто запрашиваем автозапуск
                                    AutostartPermissionHelper.requestAutostartPermission(context)
                                    showToast = "Включите автозапуск в открывшихся настройках системы."
                                }
                            } else {
                                Log.d(TAG, "Автозапуск отключен")
                            }
                            
                            Log.d(TAG, "Настройки сохранены, диалог настроек закрыт")
                        }
                    },
                    onCancel = {
                        Log.d(TAG, "Обработка нажатия 'Отмена' в диалоге настроек")
                        showSettingsDialog = false
                        Log.d(TAG, "Диалог настроек закрыт без сохранения")
                    }
                )
            }
        }
    }
    
    // Показ Toast сообщения
    showToast?.let { message ->
        LaunchedEffect(message) {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            showToast = null
        }
    }
}

/**
 * Определяет путь к фоновому изображению на основе текущей даты
 */
private fun getBackgroundImagePath(
    repository: HolidayRepository,
    date: LocalDate
): String {
    val dateKey = String.format("%02d-%02d", date.monthValue, date.dayOfMonth)
    val holidayImage = repository.getHolidayImage(dateKey)
    
    return if (holidayImage != null && holidayImage.isNotEmpty()) {
        "holidays/$holidayImage"
    } else {
        "images/russia_flag.png"
    }
}

/**
 * Загружает изображение из assets
 * Если указанное изображение не найдено, пытается загрузить russia_flag.png
 * Если и его нет, возвращает null (будет показан цветной фон)
 */
private fun loadImageFromAssets(context: android.content.Context, path: String): android.graphics.Bitmap? {
    return try {
        // Пытаемся загрузить указанное изображение
        val inputStream = context.assets.open(path)
        BitmapFactory.decodeStream(inputStream)
    } catch (e: IOException) {
        e.printStackTrace()
        // Если это не было изображение по умолчанию, пытаемся загрузить его
        if (path != "images/russia_flag.png" && !path.startsWith("images/")) {
            // Если путь начинается с holidays/, пробуем также в images/
            if (path.startsWith("holidays/")) {
                val imageName = path.substringAfter("holidays/")
                try {
                    val alternativeStream = context.assets.open("images/$imageName")
                    BitmapFactory.decodeStream(alternativeStream)
                } catch (e2: IOException) {
                    // Если не найдено в images, пробуем russia_flag.png
                    try {
                        val defaultStream = context.assets.open("images/russia_flag.png")
                        BitmapFactory.decodeStream(defaultStream)
                    } catch (e3: IOException) {
                        e3.printStackTrace()
                        null
                    }
                }
            } else {
                // Пробуем загрузить russia_flag.png
                try {
                    val defaultStream = context.assets.open("images/russia_flag.png")
                    BitmapFactory.decodeStream(defaultStream)
                } catch (e2: IOException) {
                    e2.printStackTrace()
                    null
                }
            }
        } else {
            // Если уже пытались загрузить russia_flag.png и не получилось
            null
        }
    }
}
