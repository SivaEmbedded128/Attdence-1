package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.AttendanceCalculator
import com.example.domain.AttendanceHealthStatus
import com.example.domain.AttendanceRecord
import com.example.domain.AttendanceStatus
import com.example.domain.IsoDate
import com.example.ui.AttendXComputedState
import com.example.ui.components.AttendXCard
import com.example.ui.components.AttendanceRingChart
import com.example.ui.components.GradientHeroCard
import com.example.ui.components.MetricStatTile
import com.example.ui.components.StatusPill
import com.example.ui.components.StorageModeBanner
import com.example.ui.components.SubjectBarChart
import com.example.ui.theme.BlueElectric
import com.example.ui.theme.CancelledSlate
import com.example.ui.theme.CriticalCoral
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalAttendXColors
import com.example.ui.theme.SafeEmerald
import com.example.ui.theme.VioletPrimary
import com.example.ui.theme.WarningAmber

@Composable
fun DashboardScreen(
    state: AttendXComputedState,
    onNavigateToCalculator: (String?) -> Unit,
    onNavigateToSubjects: () -> Unit,
    onNavigateToTimetable: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onMarkAttendance: (subjectId: String, dateIso: String, periodIndex: Long, periodsCount: Long, status: AttendanceStatus) -> Unit
) {
    val colors = LocalAttendXColors.current
    val summary = state.summary
    val today = IsoDate.today()
    val todayIso = today.toIsoString()
    val todayHoliday = state.snapshot.holidays.firstOrNull { it.dateIso == todayIso }
    val todaySlots = if (today.dayOfWeek() in 1..6 && todayHoliday == null) {
        state.snapshot.timetableSlots.filter { it.dayOfWeek.toInt() == today.dayOfWeek() }
    } else emptyList()

    val subjectMap = state.snapshot.subjects.associateBy { it.id }
    val recordMap = state.snapshot.attendanceRecords.associateBy { it.id }
    val metricsList = state.snapshot.subjects.mapNotNull { state.metricsBySubjectId[it.id] }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            StorageModeBanner(
                isCloudSynced = state.isCloudSynced,
                onSwitchToCloudClick = onNavigateToSettings
            )
        }

        // Hero Overall Attendance Card
        item {
            GradientHeroCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    AttendanceRingChart(
                        percent = summary.overallPercent,
                        status = summary.overallStatus,
                        size = 118.dp,
                        strokeWidth = 11.dp
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Overall",
                                style = MaterialTheme.typography.titleLarge,
                                color = colors.textPrimary
                            )
                            StatusPill(status = summary.overallStatus)
                        }
                        Text(
                            text = "${summary.totalAttended} attended of ${summary.totalConducted} conducted",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary
                        )
                        Text(
                            text = "Absent: ${summary.totalAbsent}  •  Target: ${state.snapshot.profile.defaultRequiredPercent.toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textMuted
                        )
                        Button(
                            onClick = { onNavigateToCalculator(null) },
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .testTag("dashboard_open_calculator_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text("Open Bunk Calculator", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }

        // KPI Grid (2 rows x 3 tiles)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricStatTile(
                        label = "Conducted",
                        value = summary.totalConducted.toString(),
                        accentColor = colors.accentCyan,
                        modifier = Modifier.weight(1f)
                    )
                    MetricStatTile(
                        label = "Attended",
                        value = summary.totalAttended.toString(),
                        accentColor = SafeEmerald,
                        modifier = Modifier.weight(1f)
                    )
                    MetricStatTile(
                        label = "Absent",
                        value = summary.totalAbsent.toString(),
                        accentColor = CriticalCoral,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricStatTile(
                        label = "Subjects",
                        value = summary.subjectCount.toString(),
                        accentColor = colors.accentViolet,
                        modifier = Modifier.weight(1f)
                    )
                    MetricStatTile(
                        label = "Safe Bunks",
                        value = summary.totalSafeBunksAcrossSubjects.toString(),
                        accentColor = SafeEmerald,
                        subtitle = "across subjects",
                        modifier = Modifier.weight(1f)
                    )
                    MetricStatTile(
                        label = "Below Target",
                        value = summary.subjectsBelowRequired.toString(),
                        accentColor = if (summary.subjectsBelowRequired > 0) CriticalCoral else SafeEmerald,
                        subtitle = "need attention",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Today's Classes Quick Marking
        item {
            AttendXCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Today's Schedule (${today.dayOfWeekShort()}, ${today.monthShort()} ${today.day})",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary
                        )
                        Text(
                            text = todayHoliday?.let { "Holiday: ${it.reason}" }
                                ?: if (todaySlots.isEmpty()) "No classes scheduled" else "Tap to mark Present, Absent, or Cancelled",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textMuted
                        )
                    }
                    TextButton(onClick = onNavigateToTimetable) {
                        Text("Timetable", color = colors.accentCyan)
                    }
                }

                if (todaySlots.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(todaySlots, key = { it.id }) { slot ->
                            val sub = subjectMap[slot.subjectId] ?: return@items
                            val recId = AttendanceRecord.buildId(todayIso, slot.subjectId, slot.periodIndex)
                            val existing = recordMap[recId]
                            Surface(
                                modifier = Modifier.width(235.dp),
                                shape = RoundedCornerShape(16.dp),
                                color = colors.cardElevated,
                                border = BorderStroke(1.dp, colors.border)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = sub.code,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = colors.accentViolet,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = slot.timeSlot,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colors.textMuted
                                        )
                                    }
                                    Text(
                                        text = sub.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = colors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        QuickStatusButton(
                                            label = "P",
                                            selected = existing?.statusEnum == AttendanceStatus.PRESENT,
                                            color = SafeEmerald,
                                            onClick = {
                                                onMarkAttendance(
                                                    slot.subjectId,
                                                    todayIso,
                                                    slot.periodIndex,
                                                    slot.periodsCount,
                                                    AttendanceStatus.PRESENT
                                                )
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                        QuickStatusButton(
                                            label = "A",
                                            selected = existing?.statusEnum == AttendanceStatus.ABSENT,
                                            color = CriticalCoral,
                                            onClick = {
                                                onMarkAttendance(
                                                    slot.subjectId,
                                                    todayIso,
                                                    slot.periodIndex,
                                                    slot.periodsCount,
                                                    AttendanceStatus.ABSENT
                                                )
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                        QuickStatusButton(
                                            label = "C",
                                            selected = existing?.statusEnum == AttendanceStatus.CANCELLED,
                                            color = CancelledSlate,
                                            onClick = {
                                                onMarkAttendance(
                                                    slot.subjectId,
                                                    todayIso,
                                                    slot.periodIndex,
                                                    slot.periodsCount,
                                                    AttendanceStatus.CANCELLED
                                                )
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Visual Attendance Progress Chart
        item {
            AttendXCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Subject Attendance Progress",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary
                )
                Text(
                    text = "Dashed amber line marks ${state.snapshot.profile.defaultRequiredPercent.toInt()}% default target",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted
                )
                Spacer(modifier = Modifier.height(14.dp))
                SubjectBarChart(
                    metrics = metricsList,
                    thresholdPercent = state.snapshot.profile.defaultRequiredPercent
                )
            }
        }

        // Smart Attendance Insights
        item {
            val ins = state.insights
            AttendXCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = VioletPrimary.copy(alpha = 0.45f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Insights,
                        contentDescription = "Smart Insights",
                        tint = colors.accentCyan
                    )
                    Text(
                        text = "Smart Attendance Insights",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ins.lowestSubject?.let { low ->
                        InsightRow(
                            icon = Icons.AutoMirrored.Rounded.TrendingDown,
                            tint = if (low.healthStatus == AttendanceHealthStatus.CRITICAL) CriticalCoral else WarningAmber,
                            title = "Lowest Attendance",
                            detail = "${low.subjectName} (${AttendanceCalculator.formatPercentShort(low.currentPercent)})"
                        )
                    }
                    ins.highestSubject?.let { high ->
                        InsightRow(
                            icon = Icons.AutoMirrored.Rounded.TrendingUp,
                            tint = SafeEmerald,
                            title = "Highest Attendance",
                            detail = "${high.subjectName} (${AttendanceCalculator.formatPercentShort(high.currentPercent)}) — ${high.safeBunks} safe bunks"
                        )
                    }
                    InsightRow(
                        icon = Icons.Rounded.AutoAwesome,
                        tint = colors.accentViolet,
                        title = "Reach 80% Overall",
                        detail = if (ins.classesToReach80Overall == 0) {
                            "Already at or above 80% overall!"
                        } else {
                            "Attend ${ins.classesToReach80Overall} consecutive classes across subjects to reach 80%."
                        }
                    )
                    val tmrw = ins.tomorrowImpact
                    InsightRow(
                        icon = if (tmrw.safeToMissEntireDay) Icons.Rounded.CheckCircle else Icons.Rounded.WarningAmber,
                        tint = if (tmrw.safeToMissEntireDay) SafeEmerald else CriticalCoral,
                        title = "Can I take leave tomorrow (${tmrw.date.dayOfWeekShort()})?",
                        detail = when {
                            tmrw.isHoliday -> "Yes — ${tmrw.holidayReason ?: "No classes scheduled"}."
                            tmrw.totalPeriods == 0 -> "Yes — no classes scheduled tomorrow."
                            tmrw.safeToMissEntireDay -> "Yes — missing ${tmrw.totalPeriods} periods keeps all subjects above target (overall ${AttendanceCalculator.formatPercentShort(tmrw.overallAfterPercent)})."
                            else -> {
                                val risky = tmrw.subjectImpacts.filter { it.dropsBelowRequired }
                                    .joinToString { "${it.subject.code}→${AttendanceCalculator.formatPercentShort(it.afterPercent)}" }
                                "Not safe — missing ${tmrw.totalPeriods} periods drops: $risky."
                            }
                        }
                    )
                    InsightRow(
                        icon = Icons.Rounded.DoNotDisturbOn,
                        tint = BlueElectric,
                        title = "What if I miss an entire week (${ins.weekMissPeriods} periods)?",
                        detail = "Overall drops from ${AttendanceCalculator.formatPercentShort(ins.weekMissOverallBefore)} to ${AttendanceCalculator.formatPercentShort(ins.weekMissOverallAfter)}" +
                            if (ins.weekSubjectsDroppingBelow.isNotEmpty()) {
                                " (${ins.weekSubjectsDroppingBelow.size} subjects drop below target)."
                            } else {
                                " (all subjects remain above target)."
                            }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Calculated Recommendations",
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.accentCyan
                )
                Spacer(modifier = Modifier.height(8.dp))
                ins.recommendations.forEach { rec ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = colors.cardElevated,
                        border = BorderStroke(1.dp, colors.borderSubtle)
                    ) {
                        Text(
                            text = rec,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textPrimary,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }

        // Subject Quick Summary List
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Subject Breakdown",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary
                )
                TextButton(onClick = onNavigateToSubjects) {
                    Text("Manage All", color = colors.accentCyan)
                }
            }
        }

        items(metricsList, key = { it.subjectId }) { m ->
            val progressColor = when (m.healthStatus) {
                AttendanceHealthStatus.SAFE -> SafeEmerald
                AttendanceHealthStatus.WARNING -> WarningAmber
                AttendanceHealthStatus.CRITICAL -> CriticalCoral
            }
            AttendXCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onNavigateToCalculator(m.subjectId) }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${m.subjectCode} • ${m.subjectName}",
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (m.classesToRecover > 0) {
                                "Attend ${m.classesToRecover} consecutive classes to hit ${m.requiredPercent.toInt()}%"
                            } else {
                                "${m.safeBunks} safe bunk${if (m.safeBunks == 1) "" else "s"} • Target ${m.requiredPercent.toInt()}%"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (m.classesToRecover > 0) CriticalCoral else colors.textMuted
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = AttendanceCalculator.formatPercentShort(m.currentPercent),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = progressColor
                        )
                        Text(
                            text = "${m.attended}/${m.conducted}",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            color = colors.textMuted
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { (m.currentPercent.toFloat() / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(999.dp)),
                    color = progressColor,
                    trackColor = colors.borderSubtle
                )
            }
        }
    }
}

@Composable
private fun QuickStatusButton(
    label: String,
    selected: Boolean,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAttendXColors.current
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(38.dp),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (selected) color else colors.border),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) color.copy(alpha = 0.22f) else Color.Transparent,
            contentColor = if (selected) color else colors.textSecondary
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(
            text = label,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
private fun InsightRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    title: String,
    detail: String
) {
    val colors = LocalAttendXColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(tint.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(17.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.labelLarge, color = colors.textPrimary)
            Text(text = detail, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        }
    }
}
