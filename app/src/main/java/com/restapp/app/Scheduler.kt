package com.restapp.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Programa con AlarmManager (alarmas inexactas que sí despiertan el teléfono, sin
 * necesitar el permiso de alarmas exactas):
 *  - REMIND: aviso "se acerca la hora de dormir"
 *  - DIM 1..6: bajada progresiva del brillo hasta la hora de dormir
 *  - BED: aviso "es hora de dormir"
 *  - WAKE: restaura el brillo original
 */
object Scheduler {
    private const val RC_REMIND = 1
    private const val RC_BED = 2
    private const val RC_WAKE = 3
    private const val RC_DIM = 10

    private fun pi(ctx: Context, rc: Int, kind: String, step: Int = 0): PendingIntent {
        val i = Intent(ctx, BedtimeReceiver::class.java)
            .putExtra("kind", kind)
            .putExtra("step", step)
        return PendingIntent.getBroadcast(ctx, rc, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    /** Próximo instante en que el reloj marca [minuteOfDay] (puede ser negativo/>1440 respecto al día). */
    private fun next(minuteOfDay: Int): Long {
        val now = ZonedDateTime.now()
        var t = now.toLocalDate().atStartOfDay(ZoneId.systemDefault()).plusMinutes(minuteOfDay.toLong())
        if (!t.isAfter(now)) t = t.plusDays(1)
        return t.toInstant().toEpochMilli()
    }

    fun cancelAll(ctx: Context) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pi(ctx, RC_REMIND, "REMIND"))
        am.cancel(pi(ctx, RC_BED, "BED"))
        am.cancel(pi(ctx, RC_WAKE, "WAKE"))
        for (i in 1..Dimmer.STEPS) am.cancel(pi(ctx, RC_DIM + i, "DIM", i))
    }

    fun scheduleAll(ctx: Context) {
        val p = Prefs(ctx)
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        cancelAll(ctx)

        fun set(at: Long, rc: Int, kind: String, step: Int = 0) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi(ctx, rc, kind, step))
        }

        val bed = p.bedtimeMin
        val lead = p.reminderMin
        if (p.remindersOn || p.dimOn) {
            set(next(bed - lead), RC_REMIND, "REMIND")
            set(next(bed), RC_BED, "BED")
        }
        if (p.dimOn) {
            val stepLen = maxOf(1, lead / Dimmer.STEPS)
            for (i in 1..Dimmer.STEPS) {
                set(next(bed - lead + i * stepLen), RC_DIM + i, "DIM", i)
            }
        }
        if (p.dimOn || p.dimActive) {
            set(next(p.wakeMin), RC_WAKE, "WAKE")
        }
    }
}
