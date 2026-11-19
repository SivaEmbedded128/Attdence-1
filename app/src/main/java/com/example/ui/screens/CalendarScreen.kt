package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BeachAccess
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.AttendanceRecord
import com.example.domain.AttendanceStatus
import com.example.domain.IsoDate
import com.example.ui.AttendXComputedState
import com.example.ui.components.AttendXCard
import com.example.ui.theme.CancelledSlate
import com.example.ui.theme.CriticalCoral
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalAttendXColors
import com.example.ui.theme.SafeEmerald
import com.example.ui.theme.UpcomingYellow
import com.example.ui.theme.VioletPrimary

@Composable
fun CalendarScreen(
    state: AttendXComputedState,
    onMarkAttendance: (subjectId: String, dateIso: String, periodIndex: Long, periodsCount: Long, status: AttendanceStatus) -> Unit,
    onDeleteRecord: (String) -> Unit,
    onToggleHoliday: (dateIso: String, reason: String) -> Unit
) {
    val colors = LocalAttendXColors.current
    val today = IsoDate.today()
    var displayedMonth by remember { mutableStateOf(IsoDate(today.year, today.month, 1)) }
    var selectedDate by remember { mutableStateOf(today) }

    val subjectMap = state.snapshot.subjects.associateBy { it.id }
    val holidaySet = state.snapshot.holidays.associateBy { it.dateIso }
    val recordsByDate = remember(state.snapshot.attendanceRecords) {
        state.snapshot.attendanceRecords.groupBy { it.dateIso }
    }
    val slotsByDow = remember(state.snapshot.timetableSlots) {
        state.snapshot.timetableSlots.groupBy { it.dayOfWeek.toInt() }
    }

    val firstDow = displayedMonth.dayOfWeek() // 1..7 (Mon..Sun)
    val daysCount = displayedMonth.daysInMonth()
    val leadingBlanks = firstDow - 1
    val totalCells = leadingBlanks + daysCount
    val rowsCount = (totalCells + 6) / 7

    val selectedIso = selectedDate.toIsoString()
    val selectedHoliday = holidaySet[selectedIso]
    val selectedDayRecords = recordsByDate[selectedIso].orEmpty()
    val selectedDaySlots = slotsByDow[selectedDate.dayOfWeek()].orEmpty().sortedBy { it.periodIndex }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("calendar_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Legend
        item {
            AttendXCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendDot(SafeEmerald, "Present")
                    LegendDot(CriticalCoral, "Absent")
                    LegendDot(CancelledSlate, "Cancelled/Holiday")
                    LegendDot(UpcomingYellow, "Upcoming")
                }
            }
        }

        // Monthly Calendar Grid
        item {
            AttendXCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { displayedMonth = displayedMonth.plusMonths(-1) }) {
                        Icon(Icons.Rounded.ChevronLeft, contentDescription = "Previous Month", tint = colors.textPrimary)
                    }
                    Text(
                        text = "${displayedMonth.monthName()} ${displayedMonth.year}",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary
                    )
                    IconButton(onClick = { displayedMonth = displayedMonth.plusMonths(1) }) {
                        Icon(Icons.Rounded.ChevronRight, contentDescription = "Next Month", tint = colors.textPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf("M", "T", "W", "T", "F", "S", "S").forEach { d ->
                        Text(
                            text = d,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textMuted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                for (r in 0 until rowsCount) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (c in 0 until 7) {
                            val cellIndex = r * 7 + c
                            val dayNum = cellIndex - leadingBlanks + 1
                            if (dayNum < 1 || dayNum > daysCount) {
                                Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                            } else {
                                val cellDate = IsoDate(displayedMonth.year, displayedMonth.month, dayNum)
                                val cellIso = cellDate.toIsoString()
                                val dayRecs = recordsByDate[cellIso].orEmpty()
                                val isHoliday = holidaySet.containsKey(cellIso)
                                val hasUpcoming = cellDate >= today && !isHoliday &&
                                    !slotsByDow[cellDate.dayOfWeek()].isNullOrEmpty() && dayRecs.isEmpty()

                                val dotColor = when {
                                    isHoliday -> CancelledSlate
                                    dayRecs.any { it.statusEnum == AttendanceStatus.ABSENT } -> CriticalCoral
                                    dayRecs.any { it.statusEnum == AttendanceStatus.PRESENT } -> SafeEmerald
                                    dayRecs.any { it.statusEnum == AttendanceStatus.CANCELLED } -> CancelledSlate
                                    hasUpcoming -> UpcomingYellow
                                    else -> null
                                }
                                val isSelected = cellDate == selectedDate
                                val isToday = cellDate == today

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .padding(2.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            when {
                                                isSelected -> VioletPrimary.copy(alpha = 0.28f)
                                                isToday -> colors.cardElevated
                                                else -> Color.Transparent
                                            }
                                        )
                                        .border(
                                            width = if (isSelected || isToday) 1.dp else 0.dp,
                                            color = if (isSelected) VioletPrimary else if (isToday) colors.accentCyan else Color.Transparent,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable { selectedDate = cellDate },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = dayNum.toString(),
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                            color = colors.textPrimary
                                        )
                                        if (dotColor != null) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(dotColor)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Selected Date Classes & Attendance Status
        item {
            AttendXCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${selectedDate.dayOfWeekFull()}, ${selectedDate.monthShort()} ${selectedDate.day}",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary
                        )
                        Text(
                            text = selectedHoliday?.let { "Holiday: ${it.reason}" }
                                ?: if (selectedDaySlots.isEmpty()) "No classes scheduled" else "${selectedDaySlots.size} scheduled slot(s)",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textMuted
                        )
                    }
                    OutlinedButton(
                        onClick = { onToggleHoliday(selectedIso, selectedHoliday?.reason ?: "Holiday") },
                        border = BorderStroke(1.dp, colors.border)
                    ) {
                        Icon(Icons.Rounded.BeachAccess, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.size(4.dp))
                        Text(if (selectedHoliday != null) "Unmark" else "Holiday")
                    }
                }
            }
        }

        // Scheduled slots for selected date
        if (selectedHoliday == null) {
            items(selectedDaySlots, key = { "cal_slot_${it.id}" }) { slot ->
                val sub = subjectMap[slot.subjectId] ?: return@items
                val recId = AttendanceRecord.buildId(selectedIso, slot.subjectId, slot.periodIndex)
                val rec = selectedDayRecords.firstOrNull { it.id == recId }
                val statusLabel = when {
                    rec != null -> rec.statusEnum.name
                    selectedDate >= today -> "UPCOMING"
                    else -> "UNMARKED"
                }
                val statusColor = when (rec?.statusEnum) {
                    AttendanceStatus.PRESENT -> SafeEmerald
                    AttendanceStatus.ABSENT -> CriticalCoral
                    AttendanceStatus.CANCELLED -> CancelledSlate
                    null -> if (selectedDate >= today) UpcomingYellow else colors.textMuted
                }

                AttendXCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Period ${slot.periodIndex} • ${slot.timeSlot}",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.accentCyan
                            )
                            Text(
                                text = "${sub.code} • ${sub.name}",
                                style = MaterialTheme.typography.titleSmall,
                                color = colors.textPrimary
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = statusColor.copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, statusColor.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = statusLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = statusColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            AttendanceStatus.PRESENT to SafeEmerald,
                            AttendanceStatus.ABSENT to CriticalCoral,
                            AttendanceStatus.CANCELLED to CancelledSlate
                        ).forEach { (st, clr) ->
                            val isSelected = rec?.statusEnum == st
                            OutlinedButton(
                                onClick = {
                                    onMarkAttendance(slot.subjectId, selectedIso, slot.periodIndex, slot.periodsCount, st)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, if (isSelected) clr else colors.border),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSelected) clr.copy(alpha = 0.2f) else Color.Transparent,
                                    contentColor = if (isSelected) clr else colors.textSecondary
                                ),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(
                                    text = st.name.lowercase().replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        if (rec != null) {
                            IconButton(
                                onClick = { onDeleteRecord(rec.id) },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(Icons.Rounded.Delete, contentDescription = "Clear Record", tint = CriticalCoral)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    val colors = LocalAttendXColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
    }
}
