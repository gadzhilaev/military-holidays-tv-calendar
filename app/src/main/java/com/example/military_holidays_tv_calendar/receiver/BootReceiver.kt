package com.example.military_holidays_tv_calendar.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.military_holidays_tv_calendar.MainActivity
import com.example.military_holidays_tv_calendar.data.AppPreferences
import kotlinx.coroutines.runBlocking

private const val TAG = "BootReceiver"

class BootReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "BootReceiver получил событие: ${intent.action}")
        Log.d(TAG, "Android версия: ${Build.VERSION.SDK_INT}")
        
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || 
            intent.action == "android.intent.action.QUICKBOOT_POWERON" ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            intent.action == Intent.ACTION_PACKAGE_REPLACED) {
            
            Log.d(TAG, "Обработка события загрузки системы")
            val preferences = AppPreferences(context)
            
            // Используем runBlocking для синхронного получения значения
            try {
                val autoStartEnabled = runBlocking {
                    preferences.getAutoStartEnabled()
                }
                
                Log.d(TAG, "Автозапуск включен: $autoStartEnabled")
                
                if (autoStartEnabled) {
                    Log.d(TAG, "Запускаем MainActivity при загрузке системы")
                    
                    // Добавляем небольшую задержку для стабильности
                    Handler(Looper.getMainLooper()).postDelayed({
                        try {
                            val launchIntent = Intent(context, MainActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                            }
                            context.startActivity(launchIntent)
                            Log.d(TAG, "MainActivity запущена успешно")
                        } catch (e: Exception) {
                            Log.e(TAG, "Ошибка при запуске MainActivity", e)
                        }
                    }, 2000) // Задержка 2 секунды
                } else {
                    Log.d(TAG, "Автозапуск отключен, MainActivity не запускается")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при проверке автозапуска", e)
            }
        } else {
            Log.d(TAG, "Неизвестное действие: ${intent.action}")
        }
    }
}

