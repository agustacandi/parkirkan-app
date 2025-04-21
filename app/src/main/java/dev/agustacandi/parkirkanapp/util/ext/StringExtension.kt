package dev.agustacandi.parkirkanapp.util.ext

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun String.checkHttps(): String {
    return if (this.startsWith("http://")) {
        this.replace("http://", "https://")
    } else {
        this
    }
}

fun String.formatDateTime(): String {
    return try {
        val inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val outputFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
        val dateTime = LocalDateTime.parse(this, inputFormatter)
        dateTime.format(outputFormatter)
    } catch (e: Exception) {
        this
    }
}
