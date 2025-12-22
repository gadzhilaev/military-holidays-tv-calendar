package com.example.military_holidays_tv_calendar.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresApi

private const val TAG = "AutostartPermissionHelper"

object AutostartPermissionHelper {
    
    /**
     * Запрашивает разрешение на показ поверх других окон (SYSTEM_ALERT_WINDOW)
     * Это критически важно для автозапуска на Android TV
     * Открывает экран настроек "Allow display over other apps"
     */
    @RequiresApi(Build.VERSION_CODES.M)
    fun requestOverlayPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            Log.d(TAG, "Android версия < 6.0, разрешение на показ поверх окон не требуется")
            return true
        }
        
        val hasPermission = Settings.canDrawOverlays(context)
        Log.d(TAG, "Проверка разрешения на показ поверх окон: $hasPermission")
        
        if (!hasPermission) {
            Log.d(TAG, "Запрашиваем разрешение на показ поверх других окон")
            try {
                // Открываем экран настроек "Allow display over other apps"
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                
                // Проверяем, что Intent может быть обработан
                if (intent.resolveActivity(context.packageManager) != null) {
                    Log.d(TAG, "Открываем экран настроек разрешения на показ поверх окон")
                    context.startActivity(intent)
                    return false
                } else {
                    Log.e(TAG, "Не удалось открыть экран настроек разрешения на показ поверх окон")
                    // Fallback: открываем общие настройки приложения
                    openAppSettings(context)
                    return false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при запросе разрешения на показ поверх окон", e)
                // Fallback: открываем общие настройки приложения
                openAppSettings(context)
                return false
            }
        }
        
        return true
    }
    
    /**
     * Проверяет, есть ли разрешение на показ поверх других окон
     */
    fun hasOverlayPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true // На старых версиях разрешение не требуется
        }
    }
    
    /**
     * Запрашивает разрешение на автозапуск
     * Сначала запрашивает разрешение на показ поверх других окон (критично для автозапуска)
     * Затем открывает системные настройки автозапуска (если нужно)
     */
    fun requestAutostartPermission(context: Context) {
        Log.d(TAG, "Запрос разрешения на автозапуск, Android версия: ${Build.VERSION.SDK_INT}")
        
        // Сначала запрашиваем разрешение на показ поверх других окон (критично для автозапуска)
        // Это открывает экран "Allow display over other apps"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val overlayGranted = requestOverlayPermission(context)
            if (!overlayGranted) {
                // Если разрешение на показ поверх окон не дано, не открываем другие настройки
                // Пользователь должен сначала дать это разрешение
                Log.d(TAG, "Разрешение на показ поверх окон не дано, ожидаем действия пользователя")
                return
            }
        }
        
        // После получения разрешения на показ поверх окон, открываем настройки автозапуска
        try {
            when {
                // Android 8.0+ - открываем настройки автозапуска приложений
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                    // Попытка открыть настройки автозапуска (зависит от производителя)
                    openAutostartSettings(context)
                }
                // Старые версии Android
                else -> {
                    // Для старых версий просто открываем настройки приложения
                    openAppSettings(context)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при запросе разрешения на автозапуск", e)
            // В случае ошибки открываем общие настройки приложения
            openAppSettings(context)
        }
    }
    
    /**
     * Открывает настройки автозапуска (для разных производителей)
     */
    private fun openAutostartSettings(context: Context) {
        val packageName = context.packageName
        Log.d(TAG, "Открываем настройки автозапуска для пакета: $packageName")
        
        // Попытка открыть настройки автозапуска через разные Intent'ы
        val intents = listOf(
            // Стандартный способ для Android 8.0+
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
            },
            // Для Xiaomi/MIUI
            Intent().apply {
                setClassName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
            },
            // Для Huawei
            Intent().apply {
                setClassName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity")
            },
            // Для Oppo/ColorOS
            Intent().apply {
                setClassName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")
            },
            // Для Vivo
            Intent().apply {
                setClassName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")
            },
            // Для Samsung
            Intent().apply {
                setClassName("com.samsung.android.sm", "com.samsung.android.sm.app.dashboard.SmartManagerDashBoardActivity")
            },
            // Для OnePlus
            Intent().apply {
                setClassName("com.oneplus.security", "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity")
            }
        )
        
        // Пробуем открыть каждый Intent
        for (intent in intents) {
            try {
                if (intent.resolveActivity(context.packageManager) != null) {
                    Log.d(TAG, "Открываем настройки через: ${intent.component?.className}")
                    context.startActivity(intent)
                    return
                }
            } catch (e: Exception) {
                Log.d(TAG, "Не удалось открыть настройки через ${intent.component?.className}: ${e.message}")
            }
        }
        
        // Если ничего не сработало, открываем общие настройки приложения
        Log.d(TAG, "Не найдены специфичные настройки автозапуска, открываем общие настройки")
        openAppSettings(context)
    }
    
    /**
     * Открывает общие настройки приложения
     */
    private fun openAppSettings(context: Context) {
        val packageName = context.packageName
        Log.d(TAG, "Открываем общие настройки приложения: $packageName")
        
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при открытии настроек приложения", e)
        }
    }
    
    /**
     * Запрашивает игнорирование оптимизации батареи
     */
    fun requestIgnoreBatteryOptimizations(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val packageName = context.packageName
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                
                if (intent.resolveActivity(context.packageManager) != null) {
                    Log.d(TAG, "Запрашиваем игнорирование оптимизации батареи")
                    context.startActivity(intent)
                } else {
                    Log.d(TAG, "Не удалось открыть настройки оптимизации батареи")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при запросе игнорирования оптимизации батареи", e)
            }
        }
    }
}

