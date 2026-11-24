package com.restapp.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap

// Paleta: azul oscuro, morado y blanco
val Navy = Color(0xFF0B1030)
val NavySurface = Color(0xFF121A4A)
val NavyCard = Color(0xFF1B2461)
val Purple = Color(0xFF7C4DFF)
val Lilac = Color(0xFFB9A4FF)
val Muted = Color(0xFFB8BCE0)
val Warn = Color(0xFFFF8A9B)

private val Scheme = darkColorScheme(
    primary = Purple,
    onPrimary = Color.White,
    secondary = Lilac,
    background = Navy,
    onBackground = Color.White,
    surface = NavySurface,
    onSurface = Color.White,
    surfaceVariant = NavyCard,
    onSurfaceVariant = Muted
)

@Composable
fun RestAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, content = content)
}

@Composable
fun RCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = NavyCard)
    ) {
        Column(Modifier.padding(18.dp), content = content)
    }
}

@Composable
fun BigButton(
    text: String,
    modifier: Modifier = Modifier,
    filled: Boolean = true,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    if (filled) {
        Button(
            onClick = onClick,
            modifier = modifier.fillMaxWidth().height(56.dp),
            enabled = enabled,
            shape = shape,
            colors = ButtonDefaults.buttonColors(containerColor = Purple, contentColor = Color.White)
        ) { Text(text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.fillMaxWidth().height(56.dp),
            enabled = enabled,
            shape = shape,
            border = BorderStroke(1.5.dp, Lilac),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
        ) { Text(text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
fun SwitchRow(title: String, subtitle: String, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = Muted, fontSize = 13.sp)
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedTrackColor = Purple,
                checkedThumbColor = Color.White,
                uncheckedTrackColor = NavySurface,
                uncheckedThumbColor = Muted
            )
        )
    }
}

@Composable
fun ScreenScaffold(title: String, subtitle: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(title, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color.White)
        if (subtitle != null) Text(subtitle, color = Muted, fontSize = 15.sp)
        content()
    }
}

@Composable
fun AppIcon(pkg: String, size: Dp = 44.dp) {
    val ctx = LocalContext.current
    val bmp = remember(pkg) {
        try {
            ctx.packageManager.getApplicationIcon(pkg).toBitmap(96, 96).asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }
    val shape = RoundedCornerShape(12.dp)
    if (bmp != null) {
        Image(bitmap = bmp, contentDescription = null, modifier = Modifier.size(size).clip(shape))
    } else {
        Box(Modifier.size(size).clip(shape).background(Purple))
    }
}

@Composable
fun AppRow(pkg: String, label: String, ms: Long, maxMs: Long, social: Boolean) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        AppIcon(pkg)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1,
                    modifier = Modifier.weight(1f, fill = false))
                if (social) {
                    Spacer(Modifier.width(8.dp))
                    Text("red", color = Lilac, fontSize = 11.sp)
                }
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { if (maxMs <= 0L) 0f else (ms.toFloat() / maxMs).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(8.dp)),
                color = if (social) Purple else Lilac,
                trackColor = NavySurface
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(fmtDur(ms), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun Ring(progress: Float, diameter: Dp, content: @Composable () -> Unit) {
    Box(Modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 20.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            drawArc(
                color = NavySurface, startAngle = 0f, sweepAngle = 360f, useCenter = false,
                topLeft = Offset(inset, inset), size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
            drawArc(
                color = if (progress > 1f) Warn else Purple, startAngle = -90f,
                sweepAngle = 360f * progress.coerceIn(0f, 1f), useCenter = false,
                topLeft = Offset(inset, inset), size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
        }
        content()
    }
}

/** Barras: [values] es el total; [inner] (opcional) pinta encima la parte de redes sociales. */
@Composable
fun BarChart(
    values: List<Long>,
    inner: List<Long>?,
    labels: List<String>,
    highlight: Int,
    modifier: Modifier = Modifier
) {
    val maxV = (values.maxOrNull() ?: 0L).coerceAtLeast(1L)
    Column(modifier.fillMaxWidth().height(190.dp)) {
        Canvas(Modifier.fillMaxWidth().weight(1f)) {
            val n = values.size.coerceAtLeast(1)
            val slot = this.size.width / n
            val barW = slot * 0.58f
            values.forEachIndexed { i, v ->
                val h = (this.size.height * (v.toFloat() / maxV)).coerceAtLeast(3f)
                val x = i * slot + (slot - barW) / 2f
                drawRoundRect(
                    color = if (i == highlight) Color.White else Lilac.copy(alpha = 0.55f),
                    topLeft = Offset(x, this.size.height - h),
                    size = Size(barW, h),
                    cornerRadius = CornerRadius(barW / 3f, barW / 3f)
                )
                val iv = inner?.getOrNull(i) ?: 0L
                if (iv > 0L) {
                    val ih = (this.size.height * (iv.toFloat() / maxV)).coerceAtLeast(3f)
                    drawRoundRect(
                        color = Purple,
                        topLeft = Offset(x, this.size.height - ih),
                        size = Size(barW, ih),
                        cornerRadius = CornerRadius(barW / 3f, barW / 3f)
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth()) {
            labels.forEach { l ->
                Text(l, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, color = Muted, fontSize = 11.sp, maxLines = 1)
            }
        }
    }
}

@Composable
fun StatPill(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = NavyCard)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, color = Muted, fontSize = 13.sp)
            Spacer(Modifier.height(4.dp))
            Text(value, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
    }
}
