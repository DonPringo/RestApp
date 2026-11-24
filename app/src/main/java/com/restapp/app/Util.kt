package com.restapp.app

import java.util.Calendar

fun fmtDur(ms: Long): String {
    val m = ms / 60000
    return when {
        ms <= 0L -> "0 min"
        m < 1 -> "<1 min"
        m < 60 -> "$m min"
        else -> "${m / 60} h ${m % 60} min"
    }
}

fun fmtClock(min: Int): String {
    val h = (min / 60) % 24
    val m = min % 60
    return (if (h < 10) "0$h" else "$h") + ":" + (if (m < 10) "0$m" else "$m")
}

fun startOfDay(ts: Long): Long {
    val c = Calendar.getInstance()
    c.timeInMillis = ts
    c.set(Calendar.HOUR_OF_DAY, 0)
    c.set(Calendar.MINUTE, 0)
    c.set(Calendar.SECOND, 0)
    c.set(Calendar.MILLISECOND, 0)
    return c.timeInMillis
}

fun nowMinutes(): Int {
    val c = Calendar.getInstance()
    return c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE)
}

/** true si [nowMin] está entre la hora de dormir y la de despertar (cruza medianoche). */
fun inSleepWindow(bedMin: Int, wakeMin: Int, nowMin: Int): Boolean =
    if (bedMin <= wakeMin) nowMin in bedMin until wakeMin
    else nowMin >= bedMin || nowMin < wakeMin
