package com.restapp.app

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class Actions(
    val requestNotif: () -> Unit,
    val openUsage: () -> Unit,
    val openWrite: () -> Unit,
    val startMonitor: () -> Unit,
    val stopMonitor: () -> Unit
)

enum class Route(val label: String, val emoji: String, val tab: Boolean) {
    Home("Inicio", "🏠", true),
    Stats("Estadísticas", "📊", true),
    Apps("Apps", "📱", true),
    Rest("Descanso", "🌙", true),
    Settings("Ajustes", "⚙️", true),
    Schedule("Horarios", "⏰", false),
    Privacy("Permisos", "🔒", false)
}

// ───────────────────────── Piezas compartidas ─────────────────────────

@Composable
fun NeedUsageCard(act: Actions) {
    RCard {
        Text("Falta un permiso para ver tus datos", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            "RestApp necesita «Acceso de uso» para leer cuánto tiempo pasas en cada app. " +
                "Android te llevará a una lista: busca RestApp y activa el interruptor. Sin esto no mostramos estadísticas (no inventamos datos).",
            color = Muted, fontSize = 14.sp
        )
        Spacer(Modifier.height(14.dp))
        BigButton("Dar acceso de uso", onClick = act.openUsage)
    }
}

@Composable
fun LoadingCard() {
    RCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(color = Purple, modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
            Spacer(Modifier.width(12.dp))
            Text("Leyendo tu uso real…", color = Muted)
        }
    }
}

@Composable
fun PermissionCard(
    emoji: String,
    title: String,
    why: String,
    granted: Boolean,
    rejected: Boolean,
    optional: Boolean,
    onAllow: () -> Unit,
    onDeny: (() -> Unit)?
) {
    RCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 28.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text(
                    when {
                        granted -> "✅ Concedido"
                        rejected -> "Rechazado por ti"
                        optional -> "Opcional"
                        else -> "Necesario para las estadísticas"
                    },
                    color = if (granted) Lilac else Muted, fontSize = 12.sp
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(why, color = Muted, fontSize = 14.sp)
        if (!granted) {
            Spacer(Modifier.height(14.dp))
            BigButton(if (rejected) "Cambiar de opinión: permitir" else "Permitir", onClick = onAllow)
            if (onDeny != null && !rejected) {
                Spacer(Modifier.height(8.dp))
                BigButton("Ahora no", filled = false, onClick = onDeny)
            }
        }
    }
}

@Composable
fun BreathDialog(onClose: () -> Unit) {
    var left by remember { mutableIntStateOf(60) }
    LaunchedEffect(Unit) {
        while (left > 0) {
            delay(1000)
            left -= 1
        }
    }
    val t = rememberInfiniteTransition(label = "breath")
    val s by t.animateFloat(
        initialValue = 0.55f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Reverse),
        label = "scale"
    )
    Dialog(onDismissRequest = onClose) {
        Surface(shape = RoundedCornerShape(28.dp), color = NavySurface) {
            Column(Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Pausa consciente", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(
                    if (left > 0) "Suelta el celular y sigue el círculo" else "Listo 💜 ¿De verdad necesitas seguir deslizando?",
                    color = Muted, textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                Box(Modifier.size(180.dp), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(180.dp).scale(if (left > 0) s else 1f).clip(CircleShape).background(Purple.copy(alpha = 0.75f)))
                    Text(if (left > 0) "$left s" else "✓", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(24.dp))
                BigButton(if (left > 0) "Salir" else "Seguir mi día", onClick = onClose)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeDialog(initial: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    val st = rememberTimePickerState(initialHour = initial / 60, initialMinute = initial % 60, is24Hour = true)
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = NavySurface) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                TimePicker(state = st)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.weight(1f)) { BigButton("Cancelar", filled = false, onClick = onDismiss) }
                    Box(Modifier.weight(1f)) { BigButton("Guardar", onClick = { onConfirm(st.hour * 60 + st.minute) }) }
                }
            }
        }
    }
}

// ───────────────────────── Onboarding de permisos ─────────────────────────

@Composable
fun OnboardingScreen(prefs: Prefs, perms: PermState, act: Actions, onDone: () -> Unit) {
    var tick by remember { mutableIntStateOf(0) }
    ScreenScaffold("Bienvenido a RestApp 🌙", "Duerme mejor y deja de perder horas deslizando. Tú decides qué permisos dar; puedes cambiar todo después.") {
        key(tick) {
            PermissionCard(
                "📊", "Acceso a estadísticas de uso",
                "Para leer cuánto tiempo usas TikTok, Instagram, WhatsApp, etc. Los datos se quedan SOLO en tu teléfono; RestApp ni siquiera tiene permiso de Internet.",
                granted = perms.usage, rejected = prefs.usageRejected, optional = false,
                onAllow = { prefs.usageRejected = false; act.openUsage() },
                onDeny = { prefs.usageRejected = true; tick++ }
            )
            PermissionCard(
                "🔔", "Notificaciones",
                "Para recordarte que se acerca la hora de dormir y avisarte cuando llevas mucho rato deslizando.",
                granted = perms.notif, rejected = prefs.notifRejected, optional = true,
                onAllow = { prefs.notifRejected = false; act.requestNotif() },
                onDeny = { prefs.notifRejected = true; tick++ }
            )
            PermissionCard(
                "🔆", "Modificar el brillo",
                "Para bajar el brillo poco a poco antes de dormir. Solo se usa si activas esa función en Modo descanso.",
                granted = perms.write, rejected = prefs.writeRejected, optional = true,
                onAllow = { prefs.writeRejected = false; act.openWrite() },
                onDeny = { prefs.writeRejected = true; tick++ }
            )
        }
        if (!perms.usage) {
            Text(
                "Sin acceso de uso podrás configurar el modo descanso, pero no verás estadísticas.",
                color = Muted, fontSize = 13.sp
            )
        }
        BigButton("Continuar", onClick = { prefs.onboardingDone = true; onDone() })
    }
}

// ───────────────────────── Inicio ─────────────────────────

@Composable
fun HomeScreen(snap: Snapshot?, perms: PermState, prefs: Prefs, act: Actions, goto: (Route) -> Unit, onBreath: () -> Unit) {
    val ctx = LocalContext.current
    val repo = remember { UsageRepo(ctx) }
    ScreenScaffold("Hola 👋", "Así va tu día") {
        if (!perms.usage) {
            NeedUsageCard(act)
        } else if (snap == null) {
            LoadingCard()
        } else {
            val today = snap.week.last()
            val goalMs = prefs.socialGoalMin * 60_000L
            RCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Ring(if (goalMs > 0) today.social.toFloat() / goalMs else 0f, 140.dp) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(fmtDur(today.social), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Text("en redes", color = Muted, fontSize = 12.sp)
                        }
                    }
                    Spacer(Modifier.width(18.dp))
                    Column {
                        Text("Meta diaria en redes", color = Muted, fontSize = 13.sp)
                        Text(fmtDur(goalMs), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            if (today.social > goalMs) "Ya te pasaste de tu meta" else "Te quedan ${fmtDur(goalMs - today.social)}",
                            color = if (today.social > goalMs) Warn else Lilac, fontSize = 13.sp
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatPill("Tiempo total hoy", fmtDur(today.total), Modifier.weight(1f))
                StatPill("Redes hoy", fmtDur(today.social), Modifier.weight(1f))
            }
            RCard {
                Text("Más usadas hoy", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                val top = snap.today.entries.sortedByDescending { it.value }.take(3)
                if (top.isEmpty()) Text("Aún no hay uso registrado hoy.", color = Muted, fontSize = 14.sp)
                val mx = top.firstOrNull()?.value ?: 0L
                top.forEach { AppRow(it.key, repo.label(it.key), it.value, mx, repo.isSocial(it.key)) }
            }
        }
        val untilBed = ((prefs.bedtimeMin - nowMinutes()) + 1440) % 1440
        RCard {
            Text("Hora de dormir: ${fmtClock(prefs.bedtimeMin)}", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(
                if (inSleepWindow(prefs.bedtimeMin, prefs.wakeMin, nowMinutes())) "Ya deberías estar durmiendo 😴"
                else "Faltan ${fmtDur(untilBed * 60_000L)}",
                color = Muted, fontSize = 14.sp
            )
            Spacer(Modifier.height(14.dp))
            BigButton("Ir al Modo descanso", onClick = { goto(Route.Rest) })
        }
        BigButton("Pausa de 60 segundos", filled = false, onClick = onBreath)
    }
}

// ───────────────────────── Estadísticas ─────────────────────────

@Composable
fun StatsScreen(snap: Snapshot?, perms: PermState, act: Actions) {
    var mode by remember { mutableIntStateOf(0) }
    ScreenScaffold("Estadísticas", "Datos reales de tu celular") {
        if (!perms.usage) {
            NeedUsageCard(act)
        } else if (snap == null) {
            LoadingCard()
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = mode == 0, onClick = { mode = 0 }, label = { Text("Hoy") })
                FilterChip(selected = mode == 1, onClick = { mode = 1 }, label = { Text("Últimos 7 días") })
            }
            if (mode == 0) {
                val today = snap.week.last()
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatPill("Total", fmtDur(today.total), Modifier.weight(1f))
                    StatPill("Redes", fmtDur(today.social), Modifier.weight(1f))
                }
                RCard {
                    Text("Uso por hora (hoy)", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Tiempo con el celular en cada hora del día", color = Muted, fontSize = 13.sp)
                    Spacer(Modifier.height(12.dp))
                    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                    BarChart(
                        values = snap.hourly.toList(), inner = null,
                        labels = (0..23).map { if (it % 3 == 0) "$it" else "" },
                        highlight = hour
                    )
                }
                val night = (0..5).sumOf { snap.hourly[it] } + (23..23).sumOf { snap.hourly[it] }
                RCard {
                    Text("Uso nocturno", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("11 pm – 6 am: ${fmtDur(night)}", color = Lilac, fontSize = 16.sp)
                    Text("Usar el celular de noche retrasa el sueño. Intenta que esta cifra sea 0.", color = Muted, fontSize = 13.sp)
                }
            } else {
                val fmt = SimpleDateFormat("EEE", Locale.getDefault())
                val labels = snap.week.map { fmt.format(Date(it.dayStart)).take(3) }
                val totalW = snap.week.sumOf { it.total }
                val socW = snap.week.sumOf { it.social }
                RCard {
                    Text("Total diario (morado = redes)", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    BarChart(
                        values = snap.week.map { it.total }, inner = snap.week.map { it.social },
                        labels = labels, highlight = snap.week.lastIndex
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatPill("Promedio diario", fmtDur(totalW / 7), Modifier.weight(1f))
                    StatPill("Redes / día", fmtDur(socW / 7), Modifier.weight(1f))
                }
                val worst = snap.week.maxByOrNull { it.social }
                RCard {
                    Text("Resumen semanal", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Total: ${fmtDur(totalW)} · Redes: ${fmtDur(socW)}", color = Lilac, fontSize = 15.sp)
                    if (worst != null && worst.social > 0) {
                        val d = SimpleDateFormat("EEEE d", Locale.getDefault()).format(Date(worst.dayStart))
                        Text("Día con más redes: $d (${fmtDur(worst.social)})", color = Muted, fontSize = 14.sp)
                    }
                    if (totalW > 0) {
                        Text("Las redes son el ${(socW * 100 / totalW)}% de tu tiempo en el celular.", color = Muted, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

// ───────────────────────── Apps más usadas ─────────────────────────

@Composable
fun AppsScreen(snap: Snapshot?, perms: PermState, act: Actions) {
    val ctx = LocalContext.current
    val repo = remember { UsageRepo(ctx) }
    var week by remember { mutableStateOf(false) }
    var onlySocial by remember { mutableStateOf(false) }
    ScreenScaffold("Apps más usadas", "Ordenadas por tiempo real de uso") {
        if (!perms.usage) {
            NeedUsageCard(act)
        } else if (snap == null) {
            LoadingCard()
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !week, onClick = { week = false }, label = { Text("Hoy") })
                FilterChip(selected = week, onClick = { week = true }, label = { Text("7 días") })
                FilterChip(selected = onlySocial, onClick = { onlySocial = !onlySocial }, label = { Text("Solo redes") })
            }
            val src = if (week) snap.weekApps else snap.today
            val list = src.entries
                .filter { it.value >= 30_000L && (!onlySocial || repo.isSocial(it.key)) }
                .sortedByDescending { it.value }
                .take(25)
            RCard {
                if (list.isEmpty()) {
                    Text("Todavía no hay uso para mostrar.", color = Muted)
                } else {
                    val mx = list.first().value
                    list.forEachIndexed { i, e ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${i + 1}", color = Lilac, fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp))
                            Box(Modifier.weight(1f)) {
                                AppRow(e.key, repo.label(e.key), e.value, mx, repo.isSocial(e.key))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ───────────────────────── Modo descanso ─────────────────────────

@Composable
fun RestScreen(prefs: Prefs, perms: PermState, act: Actions, goto: (Route) -> Unit, onBreath: () -> Unit) {
    val ctx = LocalContext.current
    var reminders by remember { mutableStateOf(prefs.remindersOn) }
    var dim by remember { mutableStateOf(prefs.dimOn) }
    var monitor by remember { mutableStateOf(prefs.monitorOn) }
    var dimActive by remember { mutableStateOf(prefs.dimActive) }

    ScreenScaffold("Modo descanso 🌙", "Duerme a las ${fmtClock(prefs.bedtimeMin)} y despierta a las ${fmtClock(prefs.wakeMin)}") {
        RCard {
            SwitchRow(
                "Recordatorios de sueño",
                "Aviso ${prefs.reminderMin} min antes de dormir y a la hora de dormir",
                reminders
            ) { v ->
                reminders = v; prefs.remindersOn = v; Scheduler.scheduleAll(ctx)
                if (v && !perms.notif) act.requestNotif()
            }
            if (reminders && !perms.notif) {
                Text("⚠️ Las notificaciones están desactivadas: no verás los avisos.", color = Warn, fontSize = 13.sp)
            }
            HorizontalDivider(color = NavySurface, modifier = Modifier.padding(vertical = 8.dp))
            SwitchRow(
                "Bajar el brillo poco a poco",
                if (perms.write) "Hasta ${prefs.dimTargetPct}% al llegar la hora de dormir" else "Necesita el permiso «Modificar ajustes del sistema»",
                dim
            ) { v ->
                if (v && !perms.write) {
                    prefs.writeRejected = false
                    act.openWrite()
                } else {
                    dim = v; prefs.dimOn = v; Scheduler.scheduleAll(ctx)
                }
            }
            HorizontalDivider(color = NavySurface, modifier = Modifier.padding(vertical = 8.dp))
            SwitchRow(
                "Vigilante anti-scroll",
                "Avisa tras ${prefs.breakMin} min seguidos en redes, al llegar a tus límites y después de la hora de dormir",
                monitor
            ) { v ->
                if (v && !perms.usage) {
                    act.openUsage()
                } else {
                    monitor = v; prefs.monitorOn = v
                    if (v) act.startMonitor() else act.stopMonitor()
                }
            }
        }
        BigButton("Configurar horarios y límites", onClick = { goto(Route.Schedule) })
        BigButton("Pausa consciente de 60 s", filled = false, onClick = onBreath)
        if (dimActive) {
            BigButton("Restaurar brillo ahora", filled = false, onClick = {
                Dimmer.restore(ctx, prefs); dimActive = false
            })
        }
        RCard {
            Text("¿Cómo funciona el anti-scroll?", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                "Android no deja que una app bloquee a otra sin permisos muy invasivos, así que RestApp usa límites voluntarios: " +
                    "te avisa con una notificación cuando llevas mucho rato deslizando o superas el límite que tú mismo pusiste. " +
                    "Tú decides si cerrar la app.",
                color = Muted, fontSize = 13.sp
            )
        }
    }
}

// ───────────────────────── Configuración de horarios ─────────────────────────

@Composable
fun ScheduleScreen(prefs: Prefs) {
    val ctx = LocalContext.current
    val repo = remember { UsageRepo(ctx) }
    var bed by remember { mutableIntStateOf(prefs.bedtimeMin) }
    var wake by remember { mutableIntStateOf(prefs.wakeMin) }
    var lead by remember { mutableIntStateOf(prefs.reminderMin) }
    var dimPct by remember { mutableIntStateOf(prefs.dimTargetPct) }
    var brk by remember { mutableIntStateOf(prefs.breakMin) }
    var goal by remember { mutableIntStateOf(prefs.socialGoalMin) }
    var editBed by remember { mutableStateOf(false) }
    var editWake by remember { mutableStateOf(false) }
    var limits by remember { mutableStateOf(prefs.limits()) }
    val socials = remember { repo.installedSocial().sortedBy { repo.label(it).lowercase() } }

    if (editBed) TimeDialog(bed, { editBed = false }) { v -> bed = v; prefs.bedtimeMin = v; Scheduler.scheduleAll(ctx); editBed = false }
    if (editWake) TimeDialog(wake, { editWake = false }) { v -> wake = v; prefs.wakeMin = v; Scheduler.scheduleAll(ctx); editWake = false }

    ScreenScaffold("Horarios y límites", "Tú pones las reglas") {
        RCard {
            Text("Horario de sueño", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.weight(1f)) { BigButton("🌙 Dormir ${fmtClock(bed)}", filled = false, onClick = { editBed = true }) }
                Box(Modifier.weight(1f)) { BigButton("☀️ Despertar ${fmtClock(wake)}", filled = false, onClick = { editWake = true }) }
            }
            Spacer(Modifier.height(16.dp))
            Text("Recordarme con anticipación", color = Muted, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(15, 30, 45, 60).forEach { m ->
                    FilterChip(selected = lead == m, onClick = {
                        lead = m; prefs.reminderMin = m; Scheduler.scheduleAll(ctx)
                    }, label = { Text("$m min") })
                }
            }
        }
        RCard {
            Text("Brillo al dormir: $dimPct%", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Slider(
                value = dimPct.toFloat(), onValueChange = { dimPct = it.toInt() },
                onValueChangeFinished = { prefs.dimTargetPct = dimPct },
                valueRange = 5f..50f,
                colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Purple, inactiveTrackColor = NavySurface)
            )
        }
        RCard {
            Text("Anti-scroll", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Sugerir pausa tras ese tiempo seguido en redes", color = Muted, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(10, 15, 20, 30).forEach { m ->
                    FilterChip(selected = brk == m, onClick = { brk = m; prefs.breakMin = m }, label = { Text("$m min") })
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("Meta diaria total en redes", color = Muted, fontSize = 13.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { goal = (goal - 15).coerceAtLeast(15); prefs.socialGoalMin = goal }, modifier = Modifier.size(48.dp), contentPadding = PaddingValues(0.dp)) { Text("−", fontSize = 20.sp, color = Color.White) }
                Text(fmtDur(goal * 60_000L), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                OutlinedButton(onClick = { goal = (goal + 15).coerceAtMost(480); prefs.socialGoalMin = goal }, modifier = Modifier.size(48.dp), contentPadding = PaddingValues(0.dp)) { Text("+", fontSize = 20.sp, color = Color.White) }
            }
        }
        RCard {
            Text("Límite diario por app", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("Voluntario: te avisamos cuando lo alcances", color = Muted, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            if (socials.isEmpty()) Text("No detectamos redes sociales instaladas.", color = Muted, fontSize = 14.sp)
            val steps = listOf(0, 15, 30, 45, 60, 90, 120, 180)
            socials.forEach { pkg ->
                val cur = limits[pkg] ?: 0
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    AppIcon(pkg, 40.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(repo.label(pkg), color = Color.White, fontSize = 15.sp, maxLines = 1)
                        Text(if (cur == 0) "Sin límite" else "$cur min al día", color = Lilac, fontSize = 12.sp)
                    }
                    OutlinedButton(onClick = {
                        val i = steps.indexOf(cur).coerceAtLeast(0)
                        val n = steps[(i - 1).coerceAtLeast(0)]
                        prefs.setLimit(pkg, n); limits = prefs.limits()
                    }, modifier = Modifier.size(44.dp), contentPadding = PaddingValues(0.dp)) { Text("−", color = Color.White) }
                    Spacer(Modifier.width(6.dp))
                    OutlinedButton(onClick = {
                        val i = steps.indexOf(cur).coerceAtLeast(0)
                        val n = steps[(i + 1).coerceAtMost(steps.lastIndex)]
                        prefs.setLimit(pkg, n); limits = prefs.limits()
                    }, modifier = Modifier.size(44.dp), contentPadding = PaddingValues(0.dp)) { Text("+", color = Color.White) }
                }
            }
        }
    }
}

// ───────────────────────── Ajustes ─────────────────────────

@Composable
fun SettingsScreen(goto: (Route) -> Unit) {
    ScreenScaffold("Ajustes", "RestApp 1.0") {
        BigButton("⏰  Horarios y límites", onClick = { goto(Route.Schedule) })
        BigButton("🔒  Permisos y privacidad", onClick = { goto(Route.Privacy) })
        RCard {
            Text("Sobre RestApp", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                "Hecha para ayudarte a usar menos las redes y dormir mejor. Todo ocurre en tu teléfono, sin cuentas ni anuncios.",
                color = Muted, fontSize = 14.sp
            )
        }
    }
}

// ───────────────────────── Permisos y privacidad ─────────────────────────

@Composable
fun PrivacyScreen(prefs: Prefs, perms: PermState, act: Actions, onWiped: () -> Unit) {
    val ctx = LocalContext.current
    var confirm by remember { mutableStateOf(false) }
    ScreenScaffold("Permisos y privacidad", "Tú tienes el control") {
        PermissionCard(
            "📊", "Acceso a estadísticas de uso",
            "Lee el tiempo en primer plano de cada app. Se desactiva en Ajustes de Android > Acceso de uso.",
            granted = perms.usage, rejected = false, optional = false,
            onAllow = { prefs.usageRejected = false; act.openUsage() }, onDeny = null
        )
        PermissionCard(
            "🔔", "Notificaciones",
            "Recordatorios de sueño y avisos de pausa.",
            granted = perms.notif, rejected = false, optional = true,
            onAllow = { prefs.notifRejected = false; act.requestNotif() }, onDeny = null
        )
        PermissionCard(
            "🔆", "Modificar el brillo",
            "Permite bajar el brillo antes de dormir.",
            granted = perms.write, rejected = false, optional = true,
            onAllow = { prefs.writeRejected = false; act.openWrite() }, onDeny = null
        )
        RCard {
            Text("Qué hacemos con tus datos", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                "• Las estadísticas se leen del propio Android y se muestran en tu pantalla.\n" +
                    "• Guardamos solo tus ajustes y un resumen diario (total y redes) en almacenamiento privado de la app.\n" +
                    "• RestApp NO tiene permiso de Internet: no puede enviar nada fuera del teléfono.\n" +
                    "• No hay cuentas, anuncios ni rastreadores.\n" +
                    "• Si desinstalas la app, todo se borra.",
                color = Muted, fontSize = 14.sp
            )
        }
        if (!confirm) {
            BigButton("Borrar todos mis datos de RestApp", filled = false, onClick = { confirm = true })
        } else {
            RCard {
                Text("¿Seguro? Se borrarán tus ajustes, límites e historial.", color = Color.White)
                Spacer(Modifier.height(12.dp))
                BigButton("Sí, borrar todo", onClick = {
                    Dimmer.restore(ctx, prefs)
                    Scheduler.cancelAll(ctx)
                    act.stopMonitor()
                    prefs.clearAll()
                    onWiped()
                })
                Spacer(Modifier.height(8.dp))
                BigButton("Cancelar", filled = false, onClick = { confirm = false })
            }
        }
    }
}
