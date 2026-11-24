package com.restapp.app

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private val tick = mutableIntStateOf(0)
    private val visible = mutableStateOf(true)

    private val notifLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) Prefs(this).notifRejected = true
            tick.intValue++
        }

    private fun safeStart(i: Intent, fallback: Intent? = null) {
        try {
            startActivity(i)
        } catch (e: Exception) {
            if (fallback != null) try { startActivity(fallback) } catch (e2: Exception) { }
        }
    }

    private val actions = Actions(
        requestNotif = {
            val p = Prefs(this)
            if (Build.VERSION.SDK_INT >= 33 && !p.notifRejected) {
                notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                // Ya fue rechazado (o Android < 13): llevamos al usuario a los ajustes de la app.
                val i = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                safeStart(i)
            }
        },
        openUsage = {
            safeStart(
                Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, Uri.parse("package:$packageName")),
                Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            )
        },
        openWrite = {
            safeStart(
                Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:$packageName")),
                Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
            )
        },
        startMonitor = {
            try {
                ContextCompat.startForegroundService(this, Intent(this, MonitorService::class.java))
            } catch (e: Exception) {
            }
        },
        stopMonitor = { stopService(Intent(this, MonitorService::class.java)) }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        Notifier.ensureChannels(this)
        setContent {
            RestAppTheme {
                RestAppRoot(actions, tick.intValue, visible.value)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        visible.value = true
    }

    override fun onResume() {
        super.onResume()
        // Al volver de los ajustes de Android, releemos permisos y datos.
        tick.intValue++
        val p = Prefs(this)
        Scheduler.scheduleAll(this)
        if (p.monitorOn && Perms.usage(this)) actions.startMonitor()
    }

    override fun onStop() {
        visible.value = false
        super.onStop()
    }
}

@Composable
fun RestAppRoot(act: Actions, tick: Int, visible: Boolean) {
    val ctx = LocalContext.current
    val prefs = remember { Prefs(ctx) }
    val perms = remember(tick) { Perms.read(ctx) }
    var onboarding by remember { mutableStateOf(!prefs.onboardingDone) }
    var route by remember { mutableStateOf(Route.Home) }
    var snap by remember { mutableStateOf<Snapshot?>(null) }
    var showBreath by remember { mutableStateOf(false) }

    // Carga y refresca (cada minuto, solo con la app visible) el uso REAL del teléfono.
    LaunchedEffect(tick, perms.usage, visible) {
        if (perms.usage && visible) {
            val repo = UsageRepo(ctx)
            while (true) {
                snap = withContext(Dispatchers.IO) {
                    try { repo.snapshot(prefs) } catch (e: Exception) { null }
                }
                delay(60_000L)
            }
        } else if (!perms.usage) {
            snap = null
        }
    }

    BackHandler(enabled = !route.tab) { route = Route.Settings }

    if (onboarding) {
        Box(Modifier.statusBarsPadding()) {
            OnboardingScreen(prefs, perms, act) { onboarding = false }
        }
        return
    }

    Scaffold(
        containerColor = Navy,
        bottomBar = {
            NavigationBar(containerColor = NavySurface) {
                Route.entries.filter { it.tab }.forEach { r ->
                    val selected = route == r ||
                        (!route.tab && r == Route.Settings)
                    NavigationBarItem(
                        selected = selected,
                        onClick = { route = r },
                        icon = { Text(r.emoji, fontSize = 20.sp) },
                        label = { Text(r.label, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedTextColor = Color.White,
                            unselectedTextColor = Muted,
                            indicatorColor = Purple
                        )
                    )
                }
            }
        }
    ) { inner ->
        Box(Modifier.padding(inner).statusBarsPadding()) {
            when (route) {
                Route.Home -> HomeScreen(snap, perms, prefs, act, { route = it }, { showBreath = true })
                Route.Stats -> StatsScreen(snap, perms, act)
                Route.Apps -> AppsScreen(snap, perms, act)
                Route.Rest -> RestScreen(prefs, perms, act, { route = it }, { showBreath = true })
                Route.Settings -> SettingsScreen { route = it }
                Route.Schedule -> ScheduleScreen(prefs)
                Route.Privacy -> PrivacyScreen(prefs, perms, act) {
                    onboarding = true
                    route = Route.Home
                }
            }
        }
    }

    if (showBreath) BreathDialog { showBreath = false }
}
