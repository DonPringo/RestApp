package com.restapp.app

import android.content.Context
import android.provider.Settings

/** Baja el brillo poco a poco antes de dormir. Requiere el permiso WRITE_SETTINGS. */
object Dimmer {
    const val STEPS = 6

    fun canDim(ctx: Context) = Settings.System.canWrite(ctx)

    fun begin(ctx: Context, p: Prefs) {
        if (!canDim(ctx) || p.dimActive) return
        try {
            val cr = ctx.contentResolver
            p.savedBrightness = Settings.System.getInt(cr, Settings.System.SCREEN_BRIGHTNESS, 128)
            p.savedMode = Settings.System.getInt(cr, Settings.System.SCREEN_BRIGHTNESS_MODE, 0)
            p.dimActive = true
        } catch (e: Exception) {
        }
    }

    /** [fraction] 0..1: cuánto del camino hacia el brillo objetivo ya se recorrió. */
    fun step(ctx: Context, p: Prefs, fraction: Float) {
        if (!canDim(ctx) || !p.dimActive) return
        try {
            val orig = p.savedBrightness
            val target = minOf((p.dimTargetPct / 100f * 255f).toInt().coerceIn(3, 255), orig)
            val v = (orig + (target - orig) * fraction.coerceIn(0f, 1f)).toInt().coerceIn(1, 255)
            val cr = ctx.contentResolver
            Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
            Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS, v)
        } catch (e: Exception) {
        }
    }

    fun restore(ctx: Context, p: Prefs) {
        if (!p.dimActive) return
        try {
            if (canDim(ctx)) {
                val cr = ctx.contentResolver
                Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS, p.savedBrightness)
                Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS_MODE, p.savedMode)
            }
        } catch (e: Exception) {
        }
        p.dimActive = false
    }
}
