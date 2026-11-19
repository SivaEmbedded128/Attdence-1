package com.example.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BeachAccess
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.AttendanceCalculator
import com.example.domain.AttendanceRecord
import com.example.domain.AttendanceStatus
import com.example.domain.IsoDate
import com.example.domain.Subject
import com.example.domain.TimetableSlot
import com.example.ui.AttendXComputedState
import com.example.ui.components.AttendXCard
import com.example.ui.theme.CancelledSlate
import com.example.ui.theme.CriticalCoral
import com.example.ui.theme.CriticalCoralBg
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalAttendXColors
import com.example.ui.theme.SafeEmerald
import com.example.ui.theme.SafeEmeraldBg
import com.example.ui.theme.VioletPrimary
import com.example.ui.theme.WarningAmber

private val DAY_LABELS = listOf(
    1 to "Mon",
    2 to "Tue",
    3 to "Wed",
    4 to "Thu",
    5 to "Fri",
    6 to "Sat"
)

@Composable
fun TimetableScreen(
    state: AttendXComputedState,
    onUpsertSlot: (
        id: String?,
        subjectId: String,
        dayOfWeek: Long,
        periodIndex: Long,
        timeSlot: String,
        periodsCount: Long
    ) -> Unit,
    onDeleteSlot: (String) -> Unit,
    onMarkAttendance: (subjectId: String, dateIso: String, periodIndex: Long, periodsCount: Long, status: AttendanceStatus) -> Unit,
    onToggleHoliday: (dateIso: String, reason: String) -> Unit
) {
    val colors = LocalAttendXColors.current
    val today = IsoDate.today()
    var selectedDate by remember { mutableStateOf(today) }
    var selectedDayOfWeek by remember {
        mutableIntStateOf(today.dayOfWeek().coerceIn(1, 6))
    }
    var showSlotDialog by remember { mutableStateOf(false) }
    var editingSlot by remember { mutableStateOf<TimetableSlot?>(null) }
    var showHolidayDialog by remember { mutableStateOf(false) }

    val subjectMap = state.snapshot.subjects.associateBy { it.id }
    val recordMap = state.snapshot.attendanceRecords.associateBy { it.id }
    val selectedDateIso = selectedDate.toIsoString()
    val holiday = state.snapshot.holidays.firstOrNull { it.dateIso == selectedDateIso }

    val daySlots = remember(state.snapshot.timetableSlots, selectedDayOfWeek) {
        state.snapshot.timetableSlots
            .filter { it.dayOfWeek.toInt() == selectedDayOfWeek }
            .sortedBy { it.periodIndex }
    }

    val dayImpact = remember(selectedDate, state.snapshot.timetableSlots, state.snapshot.subjects, state.snapshot.holidays) {
        AttendanceCalculator.calculateDayImpact(
            date = selectedDate,
            slots = state.snapshot.timetableSlots,
            subjects = state.snapshot.subjects,
            holidays = state.snapshot.holidays
        )
    }

    Scaffold(
        containerColor = colors.baseBg,
        floatingActionButton = {
            if (state.snapshot.subjects.isNotEmpty()) {
                FloatingActionButton(
                    onClick = {
                        editingSlot = null
                        showSlotDialog = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_timetable_slot_fab")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add Timetable Slot")
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("timetable_screen"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Selected Date + Holiday Switcher
            item {
                AttendXCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = {
                            val prev = selectedDate.plusDays(-1)
                            selectedDate = prev
                            if (prev.dayOfWeek() in 1..6) selectedDayOfWeek = prev.dayOfWeek()
                        }) {
                            Icon(Icons.Rounded.ChevronLeft, contentDescription = "Previous Day", tint = colors.textPrimary)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${selectedDate.dayOfWeekFull()}, ${selectedDate.monthShort()} ${selectedDate.day}, ${selectedDate.year}",
                                style = MaterialTheme.typography.titleSmall,
                                color = colors.textPrimary
                            )
                            Text(
                                text = if (selectedDate == today) "Today" else selectedDateIso,
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.accentCyan
                            )
                        }
                        IconButton(onClick = {
                            val next = selectedDate.plusDays(1)
                            selectedDate = next
                            if (next.dayOfWeek() in 1..6) selectedDayOfWeek = next.dayOfWeek()
                        }) {
                            Icon(Icons.Rounded.ChevronRight, contentDescription = "Next Day", tint = colors.textPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (holiday != null) {
                                    onToggleHoliday(selectedDateIso, holiday.reason)
                                } else {
                                    showHolidayDialog = true
                                }
                            },
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, if (holiday != null) WarningAmber else colors.border)
                        ) {
                            Icon(Icons.Rounded.BeachAccess, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(if (holiday != null) "Remove Holiday" else "Mark Holiday")
                        }
                        if (selectedDate != today) {
                            OutlinedButton(
                                onClick = {
                                    selectedDate = today
                                    selectedDayOfWeek = today.dayOfWeek().coerceIn(1, 6)
                                },
                                border = BorderStroke(1.dp, colors.border)
                            ) {
                                Text("Today")
                            }
                        }
                    }
                }
            }

            // Monday to Saturday tabs
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(DAY_LABELS) { (dow, label) ->
                        FilterChip(
                            selected = selectedDayOfWeek == dow,
                            onClick = { selectedDayOfWeek = dow },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                selectedLabelColor = colors.textPrimary
                            ),
                            modifier = Modifier.testTag("timetable_day_$dow")
                        )
                    }
                }
            }

            // Impact of missing the entire selected day
            item {
                val safe = dayImpact.safeToMissEntireDay
                AttendXCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = if (safe) SafeEmerald.copy(alpha = 0.45f) else CriticalCoral.copy(alpha = 0.45f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.WarningAmber,
                            contentDescription = null,
                            tint = if (safe) SafeEmerald else CriticalCoral
                        )
                        Text(
                            text = "Day Bunk Impact (${selectedDate.dayOfWeekShort()})",
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.textPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    if (dayImpact.isHoliday) {
                        Text(
                            text = "Holiday (${dayImpact.holidayReason ?: "Off day"}) — 0 classes conducted.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                    } else if (dayImpact.totalPeriods == 0) {
                        Text(
                            text = "No classes scheduled on ${selectedDate.dayOfWeekFull()}.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                    } else {
                        Text(
                            text = "Missing all ${dayImpact.totalPeriods} periods shifts overall attendance from ${AttendanceCalculator.formatPercentShort(dayImpact.overallBeforePercent)} to ${AttendanceCalculator.formatPercentShort(dayImpact.overallAfterPercent)}.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        dayImpact.subjectImpacts.forEach { imp ->
                            Text(
                                text = "• ${imp.subject.code} (-${imp.periodsMissed}p): ${AttendanceCalculator.formatPercentShort(imp.beforePercent)} → ${AttendanceCalculator.formatPercentShort(imp.afterPercent)}" +
                                    if (imp.dropsBelowRequired) " (Drops below ${imp.subject.requiredPercent.toInt()}%!)" else "",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                color = if (imp.dropsBelowRequired) CriticalCoral else SafeEmerald
                            )
                        }
                    }
                }
            }

            // Scheduled slots for selected day
            if (daySlots.isEmpty()) {
                item {
                    AttendXCard(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No periods scheduled for ${DAY_LABELS.first { it.first == selectedDayOfWeek }.second}. Tap + to add one.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textMuted
                            )
                        }
                    }
                }
            }

            items(daySlots, key = { it.id }) { slot ->
                val sub = subjectMap[slot.subjectId]
                val recId = AttendanceRecord.buildId(selectedDateIso, slot.subjectId, slot.periodIndex)
                val existing = recordMap[recId]
                val matchesDateWeekday = selectedDate.dayOfWeek() == slot.dayOfWeek.toInt()

                AttendXCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Period ${slot.periodIndex}  •  ${slot.timeSlot}  •  ${slot.periodsCount} hr${if (slot.periodsCount > 1) "s" else ""}",
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.accentCyan
                            )
                            Text(
                                text = sub?.let { "${it.code} • ${it.name}" } ?: "Unknown Subject",
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.textPrimary
                            )
                        }
                        Row {
                            IconButton(onClick = {
                                editingSlot = slot
                                showSlotDialog = true
                            }) {
                                Icon(Icons.Rounded.Edit, contentDescription = "Edit Slot", tint = colors.textSecondary)
                            }
                            IconButton(onClick = { onDeleteSlot(slot.id) }) {
                                Icon(Icons.Rounded.Delete, contentDescription = "Delete Slot", tint = CriticalCoral)
                            }
                        }
                    }

                    if (matchesDateWeekday && holiday == null && sub != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MarkButton(
                                label = "Present",
                                selected = existing?.statusEnum == AttendanceStatus.PRESENT,
                                color = SafeEmerald,
                                onClick = {
                                    onMarkAttendance(slot.subjectId, selectedDateIso, slot.periodIndex, slot.periodsCount, AttendanceStatus.PRESENT)
                                },
                                modifier = Modifier.weight(1f)
                            )
                            MarkButton(
                                label = "Absent",
                                selected = existing?.statusEnum == AttendanceStatus.ABSENT,
                                color = CriticalCoral,
                                onClick = {
                                    onMarkAttendance(slot.subjectId, selectedDateIso, slot.periodIndex, slot.periodsCount, AttendanceStatus.ABSENT)
                                },
                                modifier = Modifier.weight(1f)
                            )
                            MarkButton(
                                label = "Cancelled",
                                selected = existing?.statusEnum == AttendanceStatus.CANCELLED,
                                color = CancelledSlate,
                                onClick = {
                                    onMarkAttendance(slot.subjectId, selectedDateIso, slot.periodIndex, slot.periodsCount, AttendanceStatus.CANCELLED)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSlotDialog && state.snapshot.subjects.isNotEmpty()) {
        TimetableSlotDialog(
            initial = editingSlot,
            defaultDayOfWeek = selectedDayOfWeek.toLong(),
            subjects = state.snapshot.subjects,
            onDismiss = { showSlotDialog = false },
            onSave = { subId, dow, periodIdx, timeStr, count ->
                onUpsertSlot(editingSlot?.id, subId, dow, periodIdx, timeStr, count)
                showSlotDialog = false
            }
        )
    }

    if (showHolidayDialog) {
        var reason by remember { mutableStateOf("College Holiday") }
        AlertDialog(
            onDismissRequest = { showHolidayDialog = false },
            containerColor = colors.cardBg,
            title = { Text("Mark Holiday ($selectedDateIso)", color = colors.textPrimary) },
            text = {
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onToggleHoliday(selectedDateIso, reason)
                        showHolidayDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VioletPrimary)
                ) {
                    Text("Save Holiday")
                }
            },
            dismissButton = {
                TextButton(onClick = { showHolidayDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun MarkButton(
    label: String,
    selected: Boolean,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAttendXColors.current
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (selected) color else colors.border),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) color.copy(alpha = 0.22f) else Color.Transparent,
            contentColor = if (selected) color else colors.textSecondary
        ),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimetableSlotDialog(
    initial: TimetableSlot?,
    defaultDayOfWeek: Long,
    subjects: List<Subject>,
    onDismiss: () -> Unit,
    onSave: (subjectId: String, dayOfWeek: Long, periodIndex: Long, timeSlot: String, periodsCount: Long) -> Unit
) {
    val colors = LocalAttendXColors.current
    var selectedSubject by remember {
        mutableStateOf(subjects.firstOrNull { it.id == initial?.subjectId } ?: subjects.first())
    }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var dayOfWeek by remember { mutableStateOf((initial?.dayOfWeek ?: defaultDayOfWeek).toInt()) }
    var periodIndex by remember { mutableStateOf((initial?.periodIndex ?: 1L).toString()) }
    var periodsCount by remember { mutableStateOf((initial?.periodsCount ?: 1L).toString()) }
    var timeSlot by remember { mutableStateOf(initial?.timeSlot ?: "09:00 - 09:50") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.cardBg,
        title = {
            Text(
                text = if (initial == null) "Add Timetable Slot" else "Edit Slot",
                color = colors.textPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ExposedDropdownMenuBox(
                    expanded = dropdownExpanded,
                    onExpandedChange = { dropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = "${selectedSubject.code} • ${selectedSubject.name}",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Subject") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false }
                    ) {
                        subjects.forEach { sub ->
                            DropdownMenuItem(
                                text = { Text("${sub.code} • ${sub.name}") },
                                onClick = {
                                    selectedSubject = sub
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(DAY_LABELS) { (dow, label) ->
                        FilterChip(
                            selected = dayOfWeek == dow,
                            onClick = { dayOfWeek = dow },
                            label = { Text(label) }
                        )
                    }
                }

                OutlinedTextField(
                    value = timeSlot,
                    onValueChange = { timeSlot = it },
                    label = { Text("Time Slot (e.g. 09:00 - 09:50)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = periodIndex,
                        onValueChange = { periodIndex = it.filter { c -> c.isDigit() } },
                        label = { Text("Period #") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = periodsCount,
                        onValueChange = { periodsCount = it.filter { c -> c.isDigit() } },
                        label = { Text("Periods Count") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val pIdx = (periodIndex.toLongOrNull() ?: 1L).coerceIn(1L, 12L)
                    val pCnt = (periodsCount.toLongOrNull() ?: 1L).coerceIn(1L, 6L)
                    onSave(selectedSubject.id, dayOfWeek.toLong(), pIdx, timeSlot, pCnt)
                },
                colors = ButtonDefaults.buttonColors(containerColor = VioletPrimary)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
