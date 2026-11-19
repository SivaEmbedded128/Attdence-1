package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.AttendanceCalculator
import com.example.domain.AttendanceHealthStatus
import com.example.domain.BunkMetrics
import com.example.ui.theme.CriticalCoral
import com.example.ui.theme.CriticalCoralBg
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalAttendXColors
import com.example.ui.theme.SafeEmerald
import com.example.ui.theme.SafeEmeraldBg
import com.example.ui.theme.SubjectAccentColors
import com.example.ui.theme.VioletPrimary
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberBg

@Composable
fun AttendXCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    containerColor: Color = LocalAttendXColors.current.cardBg,
    borderColor: Color = LocalAttendXColors.current.borderSubtle,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardMod = if (onClick != null) {
        modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    } else {
        modifier
    }
    Card(
        modifier = cardMod,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
fun GradientHeroCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LocalAttendXColors.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(colors.heroGradient)
            .border(1.dp, colors.border, RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Column(content = content)
    }
}

@Composable
fun StatusPill(
    status: AttendanceHealthStatus,
    modifier: Modifier = Modifier
) {
    val (bg, fg) = when (status) {
        AttendanceHealthStatus.SAFE -> SafeEmeraldBg to SafeEmerald
        AttendanceHealthStatus.WARNING -> WarningAmberBg to WarningAmber
        AttendanceHealthStatus.CRITICAL -> CriticalCoralBg to CriticalCoral
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = bg,
        border = BorderStroke(1.dp, fg.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(fg)
            )
            Text(
                text = status.label,
                style = MaterialTheme.typography.labelMedium,
                color = fg,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun StorageModeBanner(
    isCloudSynced: Boolean,
    onSwitchToCloudClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAttendXColors.current
    if (isCloudSynced) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = SafeEmeraldBg,
            border = BorderStroke(1.dp, SafeEmerald.copy(alpha = 0.35f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.CloudDone,
                    contentDescription = "Cloud Sync Active",
                    tint = SafeEmerald,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Synced privately with your Google account via Cloud Firestore",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    } else {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = WarningAmberBg,
            border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier.padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.PhoneAndroid,
                    contentDescription = "Local Storage Mode",
                    tint = WarningAmber,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Local Device Mode — Data is stored only on this device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onSwitchToCloudClick) {
                    Text(
                        text = "Sync",
                        color = colors.accentCyan,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun AttendanceRingChart(
    percent: Double,
    status: AttendanceHealthStatus,
    size: Dp = 124.dp,
    strokeWidth: Dp = 12.dp,
    modifier: Modifier = Modifier
) {
    val colors = LocalAttendXColors.current
    val animatedPct by animateFloatAsState(
        targetValue = percent.toFloat().coerceIn(0f, 100f),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "ring_pct"
    )
    val sweepColors = when (status) {
        AttendanceHealthStatus.SAFE -> listOf(SafeEmerald, CyanGlow)
        AttendanceHealthStatus.WARNING -> listOf(WarningAmber, Color(0xFFFBBF24))
        AttendanceHealthStatus.CRITICAL -> listOf(CriticalCoral, Color(0xFFF97316))
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            val inset = strokePx / 2f
            val arcSize = Size(this.size.width - strokePx, this.size.height - strokePx)
            drawArc(
                color = colors.borderSubtle,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            drawArc(
                brush = Brush.sweepGradient(sweepColors),
                startAngle = -90f,
                sweepAngle = (animatedPct / 100f) * 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = AttendanceCalculator.formatPercentShort(percent),
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = colors.textPrimary
            )
            Text(
                text = status.label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = sweepColors.first(),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun MetricStatTile(
    label: String,
    value: String,
    accentColor: Color = LocalAttendXColors.current.accentViolet,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    val colors = LocalAttendXColors.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = colors.cardElevated,
        border = BorderStroke(1.dp, colors.borderSubtle)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = colors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = accentColor
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun SubjectBarChart(
    metrics: List<BunkMetrics>,
    thresholdPercent: Double,
    modifier: Modifier = Modifier
) {
    val colors = LocalAttendXColors.current
    if (metrics.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(160.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("No subjects added yet", color = colors.textMuted)
        }
        return
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
        ) {
            val count = metrics.size
            val barSlotWidth = size.width / count.toFloat()
            val barWidth = (barSlotWidth * 0.56f).coerceAtMost(44.dp.toPx())
            val chartHeight = size.height - 12.dp.toPx()

            listOf(0.5f, 1.0f).forEach { frac ->
                val y = chartHeight * (1f - frac)
                drawLine(
                    color = colors.borderSubtle,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            val thresholdY = chartHeight * (1f - (thresholdPercent.toFloat().coerceIn(0f, 100f) / 100f))
            drawLine(
                color = WarningAmber,
                start = Offset(0f, thresholdY),
                end = Offset(size.width, thresholdY),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
            )

            metrics.forEachIndexed { idx, m ->
                val pct = (m.currentPercent.toFloat().coerceIn(0f, 100f)) / 100f
                val barHeight = (chartHeight * pct).coerceAtLeast(4.dp.toPx())
                val x = idx * barSlotWidth + (barSlotWidth - barWidth) / 2f
                val y = chartHeight - barHeight
                val barColor = when (m.healthStatus) {
                    AttendanceHealthStatus.SAFE -> SubjectAccentColors[idx % SubjectAccentColors.size]
                    AttendanceHealthStatus.WARNING -> WarningAmber
                    AttendanceHealthStatus.CRITICAL -> CriticalCoral
                }
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(barColor, barColor.copy(alpha = 0.45f)),
                        startY = y,
                        endY = chartHeight
                    ),
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            metrics.forEach { m ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = m.subjectCode.take(6),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary,
                        maxLines = 1
                    )
                    Text(
                        text = "${m.currentPercent.toInt()}%",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        color = colors.textMuted
                    )
                }
            }
        }
    }
}

data class TrendPoint(
    val label: String,
    val percent: Double,
    val totalClasses: Int
)

@Composable
fun AttendanceTrendChart(
    points: List<TrendPoint>,
    lineColor: Color = LocalAttendXColors.current.accentCyan,
    modifier: Modifier = Modifier
) {
    val colors = LocalAttendXColors.current
    if (points.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(140.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Log attendance entries to view trends", color = colors.textMuted)
        }
        return
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            val h = size.height - 16.dp.toPx()
            val w = size.width
            val y75 = h * 0.25f
            drawLine(
                color = WarningAmber.copy(alpha = 0.6f),
                start = Offset(0f, y75),
                end = Offset(w, y75),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
            )

            val coords = points.mapIndexed { idx, p ->
                val x = if (points.size == 1) w / 2f else (idx.toFloat() / (points.size - 1)) * (w - 24.dp.toPx()) + 12.dp.toPx()
                val y = h * (1f - (p.percent.toFloat().coerceIn(0f, 100f) / 100f)) + 8.dp.toPx()
                Offset(x, y)
            }

            if (coords.size > 1) {
                val fillPath = Path().apply {
                    moveTo(coords.first().x, h + 8.dp.toPx())
                    coords.forEach { lineTo(it.x, it.y) }
                    lineTo(coords.last().x, h + 8.dp.toPx())
                    close()
                }
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(lineColor.copy(alpha = 0.35f), Color.Transparent)
                    )
                )

                val strokePath = Path().apply {
                    moveTo(coords.first().x, coords.first().y)
                    for (i in 1 until coords.size) {
                        lineTo(coords[i].x, coords[i].y)
                    }
                }
                drawPath(
                    path = strokePath,
                    color = lineColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            coords.forEach { pt ->
                drawCircle(color = VioletPrimary, radius = 5.dp.toPx(), center = pt)
                drawCircle(color = Color.White, radius = 2.5.dp.toPx(), center = pt)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            points.forEach { p ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = p.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textMuted
                    )
                    Text(
                        text = "${p.percent.toInt()}%",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        color = colors.textSecondary
                    )
                }
            }
        }
    }
}
