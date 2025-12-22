package com.example.military_holidays_tv_calendar

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.example.military_holidays_tv_calendar.repository.HolidayRepository
import com.example.military_holidays_tv_calendar.ui.UiState
import com.example.military_holidays_tv_calendar.ui.theme.MilitaryholidaystvcalendarTheme
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import android.graphics.BitmapFactory
import java.io.IOException

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
    
    val moscowZone = ZoneId.of("Europe/Moscow")
    val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    
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
        
        // Дата в левом верхнем углу
        Text(
            text = uiState.currentDate.format(dateFormatter),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(24.dp),
            style = androidx.tv.material3.MaterialTheme.typography.headlineMedium
        )
        
        // Время в правом верхнем углу
        Text(
            text = uiState.currentTime.format(timeFormatter),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(24.dp),
            style = androidx.tv.material3.MaterialTheme.typography.headlineMedium
        )
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
    
    return if (holidayImage != null) {
        "images/$holidayImage"
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
        if (path != "images/russia_flag.png") {
            try {
                val defaultStream = context.assets.open("images/russia_flag.png")
                BitmapFactory.decodeStream(defaultStream)
            } catch (e2: IOException) {
                e2.printStackTrace()
                null
            }
        } else {
            // Если уже пытались загрузить russia_flag.png и не получилось
            null
        }
    }
}
