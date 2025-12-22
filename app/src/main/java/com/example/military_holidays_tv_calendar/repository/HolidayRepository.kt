package com.example.military_holidays_tv_calendar.repository

import android.content.Context
import org.json.JSONObject
import java.io.IOException

class HolidayRepository(private val context: Context) {
    
    private var holidaysMap: Map<String, String>? = null
    
    /**
     * Загружает и парсит JSON файл с праздниками из assets
     */
    fun loadHolidays(): Map<String, String> {
        if (holidaysMap != null) {
            return holidaysMap!!
        }
        
        return try {
            val jsonString = context.assets.open("holidays/holidays.json")
                .bufferedReader()
                .use { it.readText() }
            
            val jsonObject = JSONObject(jsonString)
            val map = mutableMapOf<String, String>()
            
            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                map[key] = jsonObject.getString(key)
            }
            
            holidaysMap = map
            map
        } catch (e: IOException) {
            e.printStackTrace()
            emptyMap()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyMap()
        }
    }
    
    /**
     * Получает имя файла изображения для указанной даты (формат MM-DD)
     * Возвращает null, если праздника нет
     */
    fun getHolidayImage(dateKey: String): String? {
        val holidays = loadHolidays()
        return holidays[dateKey]
    }
}

