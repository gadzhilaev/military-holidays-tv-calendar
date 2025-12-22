package com.example.military_holidays_tv_calendar.ui

import java.time.LocalDate
import java.time.LocalTime

data class UiState(
    val currentDate: LocalDate,
    val currentTime: LocalTime,
    val backgroundImagePath: String
)

