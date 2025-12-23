package com.example.military_holidays_tv_calendar.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text

private const val TAG = "FirstLaunchDialog"

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun FirstLaunchDialog(
    onYesClick: () -> Unit,
    onNoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val yesButtonFocusRequester = remember { FocusRequester() }
    val noButtonFocusRequester = remember { FocusRequester() }
    
    // Логируем показ диалога и устанавливаем фокус на первую кнопку
    LaunchedEffect(Unit) {
        Log.d(TAG, "FirstLaunchDialog показан")
        yesButtonFocusRequester.requestFocus()
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth(0.6f)
            .padding(16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(androidx.tv.material3.MaterialTheme.colorScheme.surface)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Включить автозапуск при включении телевизора?",
                style = androidx.tv.material3.MaterialTheme.typography.headlineSmall,
                color = androidx.tv.material3.MaterialTheme.colorScheme.onSurface
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
            ) {
                Button(
                    onClick = {
                        Log.d(TAG, "Кнопка 'Да' нажата")
                        onYesClick()
                    },
                    modifier = Modifier.focusRequester(yesButtonFocusRequester)
                ) {
                    Text("Да")
                }
                Button(
                    onClick = {
                        Log.d(TAG, "Кнопка 'Нет' нажата")
                        onNoClick()
                    },
                    modifier = Modifier.focusRequester(noButtonFocusRequester)
                ) {
                    Text("Нет")
                }
            }
        }
    }
}

