package com.miniichat.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

fun newId(): String = UUID.randomUUID().toString()

fun formatTime(ts: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - ts
    val oneDay = 24 * 60 * 60 * 1000L
    val sameDay = SimpleDateFormat("HH:mm", Locale.getDefault())
    val withDate = SimpleDateFormat("MMM d HH:mm", Locale.getDefault())
    return if (diff < oneDay) sameDay.format(Date(ts)) else withDate.format(Date(ts))
}
