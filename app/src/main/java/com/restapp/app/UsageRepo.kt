package com.restapp.app

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import java.util.Calendar

data class DayStat(val dayStart: Long, val total: Long, val social: Long)

class Snapshot(
    val today: Map<String, Long>,
    val hourly: LongArray,
    val week: List<DayStat>,
    val weekApps: Map<String, Long>
)

/**
 * Lee el uso REAL del teléfono con UsageStatsManager.queryEvents y calcula el tiempo en
 * primer plano de cada app (pares ACTIVITY_RESUMED / ACTIVITY_PAUSED). Nada se simula.
 */
class UsageRepo(private val ctx: Context) {
    private val usm = ctx.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    private val pm = ctx.packageManager
    private val counted = HashMap<String, Boolean>()
    private val social = HashMap<String, Boolean>()
    private val homePkg: String? = try {
        pm.resolveActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)?.activityInfo?.packageName
    } catch (e: Exception) {
        null
    }

    class Result(val perApp: Map<String, Long>, val hourly: LongArray)

    /** Solo cuentan apps con icono de lanzador (excluye servicios del sistema, el launcher y RestApp). */
    fun isCounted(pkg: String): Boolean = counted.getOrPut(pkg) {
        pkg != ctx.packageName && pkg != homePkg && pm.getLaunchIntentForPackage(pkg) != null
    }

    fun isSocial(pkg: String): Boolean = social.getOrPut(pkg) { Social.isSocial(pm, pkg) }

    fun label(pkg: String): String = try {
        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
    } catch (e: Exception) {
        pkg
    }

    fun installedSocial(): List<String> {
        val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(i, 0)
            .map { it.activityInfo.packageName }
            .distinct()
            .filter { it != ctx.packageName && isSocial(it) }
    }

    private fun addInterval(pkg: String, s: Long, e: Long, totals: HashMap<String, Long>, hourly: LongArray) {
        if (e <= s || !isCounted(pkg)) return
        totals[pkg] = (totals[pkg] ?: 0L) + (e - s)
        var cur = s
        val cal = Calendar.getInstance()
        while (cur < e) {
            cal.timeInMillis = cur
            val h = cal.get(Calendar.HOUR_OF_DAY)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.add(Calendar.HOUR_OF_DAY, 1)
            val next = minOf(cal.timeInMillis, e)
            if (next <= cur) break
            hourly[h] += next - cur
            cur = next
        }
    }

    fun query(start: Long, end: Long): Result {
        val totals = HashMap<String, Long>()
        val hourly = LongArray(24)
        val open = HashMap<String, Pair<String, Long>>() // "pkg/clase" -> (pkg, inicio)
        val events = usm.queryEvents(start, end)
        val e = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            val pkg = e.packageName ?: continue
            val key = pkg + "/" + (e.className ?: "")
            when (e.eventType) {
                1 -> open[key] = pkg to e.timeStamp                       // ACTIVITY_RESUMED
                2, 23 -> open.remove(key)?.let {                           // PAUSED / STOPPED
                    addInterval(it.first, it.second, e.timeStamp, totals, hourly)
                }
                16, 26 -> {                                                // pantalla apagada / apagado
                    for (v in open.values) addInterval(v.first, v.second, e.timeStamp, totals, hourly)
                    open.clear()
                }
            }
        }
        for (v in open.values) addInterval(v.first, v.second, end, totals, hourly)
        return Result(totals, hourly)
    }

    /** Hoy + los 6 días anteriores. */
    fun snapshot(prefs: Prefs): Snapshot {
        val now = System.currentTimeMillis()
        val todayStart = startOfDay(now)
        val days = ArrayList<DayStat>()
        val weekApps = HashMap<String, Long>()
        var todayRes: Result? = null

        for (off in 6 downTo 0) {
            val c = Calendar.getInstance()
            c.timeInMillis = todayStart
            c.add(Calendar.DAY_OF_YEAR, -off)
            val s = c.timeInMillis
            c.add(Calendar.DAY_OF_YEAR, 1)
            val e = minOf(c.timeInMillis, now)
            val r = query(s, e)

            var total = r.perApp.values.sum()
            var soc = r.perApp.filterKeys { isSocial(it) }.values.sum()
            if (total == 0L) {
                // Android solo conserva eventos unos días: si ya no hay, usamos lo que guardamos antes.
                prefs.savedDay(s)?.let { total = it.first; soc = it.second }
            } else {
                prefs.saveDay(s, total, soc)
            }
            for ((k, v) in r.perApp) weekApps[k] = (weekApps[k] ?: 0L) + v
            if (off == 0) todayRes = r
            days.add(DayStat(s, total, soc))
        }
        return Snapshot(todayRes?.perApp ?: emptyMap(), todayRes?.hourly ?: LongArray(24), days, weekApps)
    }
}
