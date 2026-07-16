package com.mychat.app.utils

import java.text.SimpleDateFormat
import java.util.*

fun chatKey(u1: String, u2: String): String = listOf(u1, u2).sorted().joinToString("_")

fun formatTime(ts: String): String {
    if (ts.isEmpty()) return ""
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val date = inputFormat.parse(ts.take(19)) ?: return ""
        val now = Calendar.getInstance()
        val msgTime = Calendar.getInstance().apply { time = date }
        when {
            now.get(Calendar.YEAR) == msgTime.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == msgTime.get(Calendar.DAY_OF_YEAR) ->
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
            now.get(Calendar.YEAR) == msgTime.get(Calendar.YEAR) ->
                SimpleDateFormat("dd MMM", Locale.getDefault()).format(date)
            else ->
                SimpleDateFormat("dd.MM.yy", Locale.getDefault()).format(date)
        }
    } catch (e: Exception) { "" }
}
