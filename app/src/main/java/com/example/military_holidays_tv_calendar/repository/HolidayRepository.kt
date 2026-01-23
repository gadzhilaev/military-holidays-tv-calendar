package com.example.military_holidays_tv_calendar.repository

import android.content.Context
import org.json.JSONObject
import java.io.IOException

data class HolidayData(
    val image: String,
    val text: String? = null
)

class HolidayRepository(private val context: Context) {
    
    private var holidaysMap: Map<String, HolidayData>? = null
    
    fun loadHolidays(): Map<String, HolidayData> {
        if (holidaysMap != null) {
            return holidaysMap!!
        }
        
        return try {
            val jsonString = context.assets.open("holidays/holidays.json")
                .bufferedReader()
                .use { it.readText() }
            
            val jsonObject = JSONObject(jsonString)
            val map = mutableMapOf<String, HolidayData>()
            
            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val value = jsonObject.get(key)
                
                when (value) {
                    is String -> {
                        map[key] = HolidayData(image = value)
                    }
                    is JSONObject -> {
                        val image = value.optString("image", "")
                        val text = value.optString("text", null).takeIf { !it.isNullOrEmpty() }
                        map[key] = HolidayData(image = image, text = text)
                    }
                }
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
    
    fun getHolidayImage(dateKey: String): String? {
        val holidays = loadHolidays()
        return holidays[dateKey]?.image
    }
    
    fun getHolidayText(dateKey: String): String? {
        val holidays = loadHolidays()
        return holidays[dateKey]?.text
    }
}

