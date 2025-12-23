package com.example.military_holidays_tv_calendar.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.Checkbox
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text

private const val TAG = "SettingsDialog"

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SettingsDialog(
    autoStartEnabled: Boolean,
    onSave: (Boolean) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentAutoStart by remember { mutableStateOf(autoStartEnabled) }
    val checkboxFocusRequester = remember { FocusRequester() }
    val cancelButtonFocusRequester = remember { FocusRequester() }
    val saveButtonFocusRequester = remember { FocusRequester() }
    
    // Логируем показ диалога и устанавливаем фокус на чекбокс
    LaunchedEffect(Unit) {
        Log.d(TAG, "SettingsDialog показан, autoStartEnabled=$autoStartEnabled")
        checkboxFocusRequester.requestFocus()
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth(0.7f)
            .padding(16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(androidx.tv.material3.MaterialTheme.colorScheme.surface)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "Настройки",
                style = androidx.tv.material3.MaterialTheme.typography.headlineMedium,
                color = androidx.tv.material3.MaterialTheme.colorScheme.onSurface
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(checkboxFocusRequester)
                    .focusable(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Checkbox(
                    checked = currentAutoStart,
                    onCheckedChange = { newValue ->
                        Log.d(TAG, "Чекбокс автозапуска изменен: $currentAutoStart -> $newValue")
                        currentAutoStart = newValue
                    }
                )
                Text(
                    text = "Автозапуск при включении телевизора",
                    style = androidx.tv.material3.MaterialTheme.typography.bodyLarge,
                    color = androidx.tv.material3.MaterialTheme.colorScheme.onSurface
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.End)
            ) {
                Button(
                    onClick = {
                        Log.d(TAG, "Кнопка 'Отмена' нажата")
                        onCancel()
                    },
                    modifier = Modifier.focusRequester(cancelButtonFocusRequester)
                ) {
                    Text("Отмена")
                }
                Button(
                    onClick = {
                        Log.d(TAG, "Кнопка 'Сохранить' нажата, autoStartEnabled=$currentAutoStart")
                        onSave(currentAutoStart)
                    },
                    modifier = Modifier.focusRequester(saveButtonFocusRequester)
                ) {
                    Text("Сохранить")
                }
            }
        }
    }
}

