package com.example.military_holidays_tv_calendar.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")

class AppPreferences(private val context: Context) {
    
    companion object {
        private val AUTO_START_ENABLED_KEY = booleanPreferencesKey("auto_start_enabled")
        private val FIRST_LAUNCH_KEY = booleanPreferencesKey("first_launch")
    }
    
    val autoStartEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[AUTO_START_ENABLED_KEY] ?: false
        }
    
    val isFirstLaunch: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[FIRST_LAUNCH_KEY] ?: true
        }
    
    suspend fun setAutoStartEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_START_ENABLED_KEY] = enabled
        }
    }
    
    suspend fun setFirstLaunch(isFirst: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[FIRST_LAUNCH_KEY] = isFirst
        }
    }
    
    suspend fun getAutoStartEnabled(): Boolean {
        return context.dataStore.data.first()[AUTO_START_ENABLED_KEY] ?: false
    }
    
    suspend fun getIsFirstLaunch(): Boolean {
        return context.dataStore.data.first()[FIRST_LAUNCH_KEY] ?: true
    }
}

