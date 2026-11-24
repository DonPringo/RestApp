package com.restapp.app

import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import androidx.core.app.NotificationCompat

/**
 * Vigilante opcional anti-scroll. Cada 20 s mira qué app está en primer plano (UsageStatsManager) y:
 *  1) tras N minutos seguidos en redes, sugiere una pausa;
 *  2) al llegar al límite diario que el usuario puso para esa app, avisa;
 *  3) si ya pasó la hora de dormir, avisa que cierre la red social.
 * Solo manda notificaciones: no bloquea ni cierra apps (límites voluntarios).
 */
class MonitorService : Service() {
    private lateinit var thread: HandlerThread
    private lateinit var handler: Handler
    private lateinit var prefs: Prefs
    private lateinit var repo: UsageRepo
    private lateinit var usm: UsageStatsManager

    private var lastQuery = 0L
    private var cur: String? = null
    private val openActs = HashSet<String>()
    private var streakStart = 0L
    private var lastSocialSeen = 0L
    private var lastBreakAlert = 0L
    private var lastBedAlert = 0L
    private val lastLimitAlert = HashMap<String, Long>()

    private val tick = object : Runnable {
        override fun run() {
            try {
                doTick()
            } catch (e: Exception) {
            }
            handler.postDelayed(this, 20_000L)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        repo = UsageRepo(this)
        usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        thread = HandlerThread("rest-monitor").also { it.start() }
        handler = Handler(thread.looper)
        lastQuery = System.currentTimeMillis() - 30 * 60_000L
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Notifier.ensureChannels(this)
        val n = NotificationCompat.Builder(this, Notifier.CH_MONITOR)
            .setSmallIcon(R.drawable.ic_stat_moon)
            .setContentTitle("RestApp cuida tu descanso")
            .setContentText("Avisos de pausa y límites activos")
            .setOngoing(true)
            .setContentIntent(Notifier.openAppIntent(this))
            .build()
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(1001, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(1001, n)
            }
        } catch (e: Exception) {
            stopSelf()
            return START_NOT_STICKY
        }
        handler.removeCallbacksAndMessages(null)
        handler.post(tick)
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        thread.quitSafely()
        super.onDestroy()
    }

    private fun consume(from: Long, to: Long) {
        val ev = usm.queryEvents(from, to)
        val e = UsageEvents.Event()
        while (ev.hasNextEvent()) {
            ev.getNextEvent(e)
            val p = e.packageName ?: continue
            val key = p + "/" + (e.className ?: "")
            when (e.eventType) {
                1 -> { openActs.add(key); cur = p }
                2, 23 -> {
                    openActs.remove(key)
                    if (cur == p && openActs.none { it.startsWith("$p/") }) cur = null
                }
                16, 26 -> { openActs.clear(); cur = null }
            }
        }
    }

    private fun doTick() {
        if (!prefs.monitorOn) {
            stopSelf()
            return
        }
        if (!Perms.usage(this)) return
        val now = System.currentTimeMillis()
        consume(lastQuery, now)
        lastQuery = now

        val pkg = cur
        val isSocial = pkg != null && repo.isSocial(pkg)
        if (isSocial) {
            if (streakStart == 0L) streakStart = now
            lastSocialSeen = now
        } else if (streakStart != 0L && now - lastSocialSeen > 60_000L) {
            streakStart = 0L
        }
        if (!isSocial || pkg == null) return

        val label = repo.label(pkg)

        // 1) Pausa tras uso continuo
        val breakMs = prefs.breakMin * 60_000L
        if (now - streakStart >= breakMs && now - lastBreakAlert >= breakMs) {
            lastBreakAlert = now
            Notifier.show(
                this, 2001, "Pausa para tu cerebro 🧠",
                "Llevas ${fmtDur(now - streakStart)} seguidos en redes. Suelta el celular, estírate y respira 1 minuto.",
                Notifier.CH_ALERT
            )
        }

        // 2) Límite diario voluntario
        val limit = prefs.limits()[pkg]
        if (limit != null && limit > 0 && now - (lastLimitAlert[pkg] ?: 0L) > 10 * 60_000L) {
            val used = repo.query(startOfDay(now), now).perApp[pkg] ?: 0L
            if (used >= limit * 60_000L) {
                lastLimitAlert[pkg] = now
                Notifier.show(
                    this, 2002, "Llegaste a tu límite en $label",
                    "Hoy llevas ${fmtDur(used)} (tu límite era $limit min). Tú lo pusiste: cumple tu palabra 💪",
                    Notifier.CH_ALERT
                )
            }
        }

        // 3) Después de la hora de dormir
        if (prefs.remindersOn && inSleepWindow(prefs.bedtimeMin, prefs.wakeMin, nowMinutes()) &&
            now - lastBedAlert > 10 * 60_000L
        ) {
            lastBedAlert = now
            Notifier.show(
                this, 2003, "Ya es hora de dormir 😴",
                "Cierra $label. El sueño es lo que más te ayuda mañana.",
                Notifier.CH_ALERT
            )
        }
    }
}
