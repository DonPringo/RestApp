package com.restapp.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class BedtimeReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val p = Prefs(ctx)
        when (intent.getStringExtra("kind")) {
            "REMIND" -> {
                if (p.remindersOn) {
                    Notifier.show(
                        ctx, 1101, "Se acerca la hora de dormir 🌙",
                        "Faltan unos ${p.reminderMin} min. Ve cerrando TikTok, Instagram y compañía."
                    )
                }
                if (p.dimOn) Dimmer.begin(ctx, p)
            }
            "DIM" -> {
                if (p.dimOn) {
                    Dimmer.begin(ctx, p)
                    val step = intent.getIntExtra("step", 1)
                    Dimmer.step(ctx, p, step / Dimmer.STEPS.toFloat())
                }
            }
            "BED" -> {
                if (p.remindersOn) {
                    Notifier.show(
                        ctx, 1102, "Es hora de dormir 😴",
                        "Deja el celular lejos de la cama. Mañana vas a rendir mucho más.",
                        Notifier.CH_ALERT
                    )
                }
            }
            "WAKE" -> Dimmer.restore(ctx, p)
        }
        // Reprograma el siguiente ciclo.
        Scheduler.scheduleAll(ctx)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        Scheduler.scheduleAll(ctx)
        val p = Prefs(ctx)
        if (p.monitorOn && Perms.usage(ctx)) {
            try {
                ContextCompat.startForegroundService(ctx, Intent(ctx, MonitorService::class.java))
            } catch (e: Exception) {
                // Android puede impedirlo en segundo plano: el usuario lo reactiva al abrir la app.
            }
        }
    }
}
