package com.restapp.app

import android.content.Context
import org.json.JSONObject

/** Todo se guarda en SharedPreferences privadas de la app (nadie más puede leerlas). */
class Prefs(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("restapp_private", Context.MODE_PRIVATE)

    var onboardingDone: Boolean
        get() = sp.getBoolean("onboardingDone", false)
        set(v) { sp.edit().putBoolean("onboardingDone", v).apply() }

    // Permisos que el usuario decidió rechazar (para no insistir)
    var usageRejected: Boolean
        get() = sp.getBoolean("usageRejected", false)
        set(v) { sp.edit().putBoolean("usageRejected", v).apply() }
    var notifRejected: Boolean
        get() = sp.getBoolean("notifRejected", false)
        set(v) { sp.edit().putBoolean("notifRejected", v).apply() }
    var writeRejected: Boolean
        get() = sp.getBoolean("writeRejected", false)
        set(v) { sp.edit().putBoolean("writeRejected", v).apply() }

    // Horarios (minutos desde medianoche)
    var bedtimeMin: Int
        get() = sp.getInt("bedtimeMin", 22 * 60 + 30)
        set(v) { sp.edit().putInt("bedtimeMin", v).apply() }
    var wakeMin: Int
        get() = sp.getInt("wakeMin", 6 * 60 + 30)
        set(v) { sp.edit().putInt("wakeMin", v).apply() }
    var reminderMin: Int
        get() = sp.getInt("reminderMin", 30)
        set(v) { sp.edit().putInt("reminderMin", v).apply() }

    var remindersOn: Boolean
        get() = sp.getBoolean("remindersOn", false)
        set(v) { sp.edit().putBoolean("remindersOn", v).apply() }

    // Brillo progresivo
    var dimOn: Boolean
        get() = sp.getBoolean("dimOn", false)
        set(v) { sp.edit().putBoolean("dimOn", v).apply() }
    var dimTargetPct: Int
        get() = sp.getInt("dimTargetPct", 15)
        set(v) { sp.edit().putInt("dimTargetPct", v).apply() }
    var dimActive: Boolean
        get() = sp.getBoolean("dimActive", false)
        set(v) { sp.edit().putBoolean("dimActive", v).apply() }
    var savedBrightness: Int
        get() = sp.getInt("savedBrightness", 128)
        set(v) { sp.edit().putInt("savedBrightness", v).apply() }
    var savedMode: Int
        get() = sp.getInt("savedMode", 0)
        set(v) { sp.edit().putInt("savedMode", v).apply() }

    // Anti-scroll
    var monitorOn: Boolean
        get() = sp.getBoolean("monitorOn", false)
        set(v) { sp.edit().putBoolean("monitorOn", v).apply() }
    var breakMin: Int
        get() = sp.getInt("breakMin", 20)
        set(v) { sp.edit().putInt("breakMin", v).apply() }
    var socialGoalMin: Int
        get() = sp.getInt("socialGoalMin", 120)
        set(v) { sp.edit().putInt("socialGoalMin", v).apply() }

    // Límites diarios por app: paquete -> minutos
    fun limits(): Map<String, Int> {
        val o = JSONObject(sp.getString("limits", "{}") ?: "{}")
        val m = HashMap<String, Int>()
        o.keys().forEach { m[it] = o.getInt(it) }
        return m
    }

    fun setLimit(pkg: String, minutes: Int) {
        val o = JSONObject(sp.getString("limits", "{}") ?: "{}")
        if (minutes <= 0) o.remove(pkg) else o.put(pkg, minutes)
        sp.edit().putString("limits", o.toString()).apply()
    }

    // Historial diario guardado en el dispositivo (total, redes) para conservarlo
    // aunque Android borre los eventos antiguos.
    fun saveDay(dayStart: Long, total: Long, social: Long) {
        val o = JSONObject(sp.getString("history", "{}") ?: "{}")
        o.put(dayStart.toString(), JSONObject().put("t", total).put("s", social))
        val cutoff = dayStart - 60L * 24 * 3600 * 1000
        val old = ArrayList<String>()
        o.keys().forEach { if (it.toLong() < cutoff) old.add(it) }
        old.forEach { o.remove(it) }
        sp.edit().putString("history", o.toString()).apply()
    }

    fun savedDay(dayStart: Long): Pair<Long, Long>? {
        val o = JSONObject(sp.getString("history", "{}") ?: "{}")
        val d = o.optJSONObject(dayStart.toString()) ?: return null
        return d.getLong("t") to d.getLong("s")
    }

    fun clearAll() {
        sp.edit().clear().apply()
    }
}
