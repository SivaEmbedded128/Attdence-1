package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ReportExporter
import com.example.domain.AttendanceCalculator
import com.example.domain.AttendanceHealthStatus
import com.example.domain.AttendanceStatus
import com.example.domain.IsoDate
import com.example.ui.AttendXComputedState
import com.example.ui.components.AttendXCard
import com.example.ui.components.AttendanceTrendChart
import com.example.ui.components.SubjectBarChart
import com.example.ui.components.TrendPoint
import com.example.ui.theme.BlueElectric
import com.example.ui.theme.CancelledSlate
import com.example.ui.theme.CriticalCoral
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalAttendXColors
import com.example.ui.theme.SafeEmerald
import com.example.ui.theme.SubjectAccentColors
import com.example.ui.theme.VioletPrimary
import com.example.ui.theme.WarningAmber

@Composable
fun AnalyticsScreen(
    state: AttendXComputedState,
    onMarkAttendance: (subjectId: String, dateIso: String, periodIndex: Long, periodsCount: Long, status: AttendanceStatus) -> Unit,
    onDeleteRecord: (String) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val colors = LocalAttendXColors.current
    var historySubjectFilter by remember { mutableStateOf<String?>(null) }

    val csvSaveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(ReportExporter.generateCsvContent(state.snapshot).toByteArray())
                }
                onShowMessage("CSV report saved")
            } catch (e: Exception) {
                onShowMessage("Failed to save CSV: ${e.localizedMessage}")
            }
        }
    }

    val metricsList = remember(state.snapshot.subjects, state.metricsBySubjectId) {
        state.snapshot.subjects.mapNotNull { state.metricsBySubjectId[it.id] }
    }

    val weeklyTrends = remember(state.snapshot.attendanceRecords) {
        state.snapshot.attendanceRecords
            .filter { it.statusEnum != AttendanceStatus.CANCELLED }
            .groupBy { IsoDate.parse(it.dateIso)?.weekKey() ?: it.dateIso.take(7) }
            .toSortedMap()
            .entries
            .toList()
            .takeLast(6)
            .map { (week, recs) ->
                val total = recs.sumOf { it.periodsCount.toInt() }
                val present = recs.filter { it.statusEnum == AttendanceStatus.PRESENT }.sumOf { it.periodsCount.toInt() }
                TrendPoint(
                    label = week.substringAfter("-"),
                    percent = AttendanceCalculator.calculatePercentage(present.toLong(), total.toLong()),
                    totalClasses = total
                )
            }
    }

    val monthlyTrends = remember(state.snapshot.attendanceRecords) {
        state.snapshot.attendanceRecords
            .filter { it.statusEnum != AttendanceStatus.CANCELLED }
            .groupBy { IsoDate.parse(it.dateIso)?.monthKey() ?: it.dateIso.take(7) }
            .toSortedMap()
            .entries
            .toList()
            .takeLast(6)
            .map { (monthKey, recs) ->
                val total = recs.sumOf { it.periodsCount.toInt() }
                val present = recs.filter { it.statusEnum == AttendanceStatus.PRESENT }.sumOf { it.periodsCount.toInt() }
                val parsed = IsoDate.parse("$monthKey-01")
                TrendPoint(
                    label = parsed?.monthShort() ?: monthKey,
                    percent = AttendanceCalculator.calculatePercentage(present.toLong(), total.toLong()),
                    totalClasses = total
                )
            }
    }

    val subjectMap = state.snapshot.subjects.associateBy { it.id }
    val filteredHistory = remember(state.snapshot.attendanceRecords, historySubjectFilter) {
        state.snapshot.attendanceRecords.filter { rec ->
            historySubjectFilter == null || rec.subjectId == historySubjectFilter
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("analytics_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Export CSV & Printable PDF Report
        item {
            AttendXCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Export & Reports",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary
                )
                Text(
                    text = "Download attendance data as CSV or export a formatted PDF report.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            csvSaveLauncher.launch("AttendX_Report_${IsoDate.today().toIsoString()}.csv")
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("export_csv_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = VioletPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Save CSV")
                    }
                    OutlinedButton(
                        onClick = { ReportExporter.writeCsvToCacheAndShare(context, state.snapshot) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, colors.border)
                    ) {
                        Text("Share CSV")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { ReportExporter.printPdfReport(context, state.snapshot) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("export_pdf_print_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = BlueElectric),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Print / Save PDF")
                    }
                    OutlinedButton(
                        onClick = {
                            val pdf = ReportExporter.generatePdfFile(context, state.snapshot)
                            ReportExporter.shareFile(context, pdf, "application/pdf", "Share PDF Report")
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, colors.border)
                    ) {
                        Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Share PDF")
                    }
                }
            }
        }

        // Subject-Wise Attendance Bar Chart + Comparative Bars
        item {
            AttendXCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.BarChart, contentDescription = null, tint = colors.accentCyan)
                    Text(
                        text = "Subject-Wise Attendance Bar Chart",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary
                    )
                }
                Text(
                    text = "Compare attendance percentages across all enrolled courses (amber line = ${state.snapshot.profile.defaultRequiredPercent.toInt()}%)",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted
                )
                Spacer(modifier = Modifier.height(12.dp))
                SubjectBarChart(
                    metrics = metricsList,
                    thresholdPercent = state.snapshot.profile.defaultRequiredPercent
                )
                Spacer(modifier = Modifier.height(14.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    metricsList.sortedByDescending { it.currentPercent }.forEachIndexed { idx, m ->
                        val barClr = when (m.healthStatus) {
                            AttendanceHealthStatus.SAFE -> SubjectAccentColors[idx % SubjectAccentColors.size]
                            AttendanceHealthStatus.WARNING -> WarningAmber
                            AttendanceHealthStatus.CRITICAL -> CriticalCoral
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${m.subjectCode} • ${m.subjectName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "${AttendanceCalculator.formatPercentShort(m.currentPercent)} (${m.attended}/${m.conducted})",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = barClr
                                )
                            }
                            LinearProgressIndicator(
                                progress = { (m.currentPercent.toFloat() / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(999.dp)),
                                color = barClr,
                                trackColor = colors.borderSubtle
                            )
                        }
                    }
                }
            }
        }

        // Weekly Attendance Trends
        item {
            AttendXCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Weekly Attendance Trend",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))
                AttendanceTrendChart(points = weeklyTrends, lineColor = colors.accentCyan)
            }
        }

        // Monthly Attendance Trends
        item {
            AttendXCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Monthly Attendance Trend",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))
                AttendanceTrendChart(points = monthlyTrends, lineColor = colors.accentViolet)
            }
        }

        // Attendance History with Correction Actions
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Attendance History (${filteredHistory.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = historySubjectFilter == null,
                            onClick = { historySubjectFilter = null },
                            label = { Text("All") }
                        )
                    }
                    items(state.snapshot.subjects, key = { "hist_filter_${it.id}" }) { sub ->
                        FilterChip(
                            selected = historySubjectFilter == sub.id,
                            onClick = {
                                historySubjectFilter = if (historySubjectFilter == sub.id) null else sub.id
                            },
                            label = { Text(sub.code) }
                        )
                    }
                }
            }
        }

        items(filteredHistory.take(40), key = { it.id }) { rec ->
            val sub = subjectMap[rec.subjectId]
            val stColor = when (rec.statusEnum) {
                AttendanceStatus.PRESENT -> SafeEmerald
                AttendanceStatus.ABSENT -> CriticalCoral
                AttendanceStatus.CANCELLED -> CancelledSlate
            }
            AttendXCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${rec.dateIso} • Period ${rec.periodIndex} (${rec.periodsCount} hr)",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.accentCyan
                        )
                        Text(
                            text = sub?.let { "${it.code} • ${it.name}" } ?: rec.subjectId,
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.textPrimary
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = stColor.copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, stColor.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = rec.statusEnum.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = stColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        IconButton(onClick = { onDeleteRecord(rec.id) }, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Delete record", tint = CriticalCoral)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        AttendanceStatus.PRESENT to SafeEmerald,
                        AttendanceStatus.ABSENT to CriticalCoral,
                        AttendanceStatus.CANCELLED to CancelledSlate
                    ).forEach { (st, clr) ->
                        val active = rec.statusEnum == st
                        OutlinedButton(
                            onClick = {
                                onMarkAttendance(rec.subjectId, rec.dateIso, rec.periodIndex, rec.periodsCount, st)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (active) clr else colors.border),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (active) clr.copy(alpha = 0.2f) else Color.Transparent,
                                contentColor = if (active) clr else colors.textSecondary
                            ),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = st.name.lowercase().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
    }
}
