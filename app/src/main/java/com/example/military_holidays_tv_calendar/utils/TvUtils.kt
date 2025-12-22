package com.example.military_holidays_tv_calendar.utils

import android.content.Context
import android.content.pm.PackageManager

object TvUtils {
    /**
     * Проверяет, является ли устройство Android TV
     */
    fun isAndroidTv(context: Context): Boolean {
        val packageManager = context.packageManager
        return packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK) ||
               packageManager.hasSystemFeature(PackageManager.FEATURE_TELEVISION)
    }
}

