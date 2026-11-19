package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.AttendanceCalculator
import com.example.domain.AttendanceHealthStatus
import com.example.domain.Subject
import com.example.ui.AttendXComputedState
import com.example.ui.components.AttendXCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.CriticalCoral
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalAttendXColors
import com.example.ui.theme.SafeEmerald
import com.example.ui.theme.SubjectAccentColors
import com.example.ui.theme.VioletPrimary
import com.example.ui.theme.WarningAmber

@Composable
fun SubjectsScreen(
    state: AttendXComputedState,
    onUpsertSubject: (
        id: String?,
        name: String,
        code: String,
        conducted: Long,
        attended: Long,
        requiredPercent: Double,
        colorIndex: Long
    ) -> Unit,
    onDeleteSubject: (String) -> Unit,
    onOpenCalculatorForSubject: (String) -> Unit
) {
    val colors = LocalAttendXColors.current
    var searchQuery by remember { mutableStateOf("") }
    var filterStatus by remember { mutableStateOf<AttendanceHealthStatus?>(null) }
    var editingSubject by remember { mutableStateOf<Subject?>(null) }
    var showEditorDialog by remember { mutableStateOf(false) }
    var deletingSubject by remember { mutableStateOf<Subject?>(null) }

    val filteredSubjects = remember(state.snapshot.subjects, state.metricsBySubjectId, searchQuery, filterStatus) {
        state.snapshot.subjects.filter { sub ->
            val metrics = state.metricsBySubjectId[sub.id]
            val matchesQuery = searchQuery.isBlank() ||
                sub.name.contains(searchQuery, ignoreCase = true) ||
                sub.code.contains(searchQuery, ignoreCase = true)
            val matchesFilter = filterStatus == null || metrics?.healthStatus == filterStatus
            matchesQuery && matchesFilter
        }
    }

    Scaffold(
        containerColor = colors.baseBg,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingSubject = null
                    showEditorDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_subject_fab")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add Subject")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("subjects_screen"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("subject_search_input"),
                    placeholder = { Text("Search subject name or code") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = filterStatus == null,
                            onClick = { filterStatus = null },
                            label = { Text("All (${state.snapshot.subjects.size})") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                                selectedLabelColor = colors.textPrimary
                            )
                        )
                    }
                    AttendanceHealthStatus.entries.forEach { st ->
                        val count = state.metricsBySubjectId.values.count { it.healthStatus == st }
                        item {
                            FilterChip(
                                selected = filterStatus == st,
                                onClick = { filterStatus = if (filterStatus == st) null else st },
                                label = { Text("${st.label} ($count)") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                                    selectedLabelColor = colors.textPrimary
                                )
                            )
                        }
                    }
                }
            }

            if (filteredSubjects.isEmpty()) {
                item {
                    AttendXCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("No matching subjects", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                            Text("Tap + to add a new subject or clear your filter.", style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
                        }
                    }
                }
            }

            items(filteredSubjects, key = { it.id }) { sub ->
                val m = state.metricsBySubjectId[sub.id] ?: AttendanceCalculator.calculateBunkMetrics(sub)
                val accent = SubjectAccentColors[(sub.colorIndex.toInt()).coerceIn(0, SubjectAccentColors.lastIndex)]
                val barColor = when (m.healthStatus) {
                    AttendanceHealthStatus.SAFE -> SafeEmerald
                    AttendanceHealthStatus.WARNING -> WarningAmber
                    AttendanceHealthStatus.CRITICAL -> CriticalCoral
                }
                AttendXCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("subject_card_${sub.id}"),
                    onClick = { onOpenCalculatorForSubject(sub.id) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(accent)
                            )
                            Column {
                                Text(
                                    text = sub.code,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.accentCyan,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = sub.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = colors.textPrimary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        StatusPill(status = m.healthStatus)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Attended: ${sub.attended}  •  Absent: ${sub.absent}  •  Conducted: ${sub.conducted}",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                            Text(
                                text = "Required: ${sub.requiredPercent.toInt()}%  •  " +
                                    if (m.classesToRecover > 0) "Need ${m.classesToRecover} classes" else "${m.safeBunks} safe bunks",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (m.classesToRecover > 0) CriticalCoral else SafeEmerald
                            )
                        }
                        Text(
                            text = AttendanceCalculator.formatPercentShort(m.currentPercent),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = barColor
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { (m.currentPercent.toFloat() / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(999.dp)),
                        color = barColor,
                        trackColor = colors.borderSubtle
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(onClick = { onOpenCalculatorForSubject(sub.id) }) {
                            Icon(
                                imageVector = Icons.Rounded.Calculate,
                                contentDescription = "Calculate bunks for ${sub.name}",
                                tint = colors.accentCyan
                            )
                        }
                        IconButton(onClick = {
                            editingSubject = sub
                            showEditorDialog = true
                        }) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = "Edit ${sub.name}",
                                tint = colors.textSecondary
                            )
                        }
                        IconButton(onClick = { deletingSubject = sub }) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = "Delete ${sub.name}",
                                tint = CriticalCoral
                            )
                        }
                    }
                }
            }
        }
    }

    if (showEditorDialog) {
        SubjectEditorDialog(
            initial = editingSubject,
            defaultRequiredPercent = state.snapshot.profile.defaultRequiredPercent,
            nextColorIndex = (state.snapshot.subjects.size % SubjectAccentColors.size).toLong(),
            onDismiss = { showEditorDialog = false },
            onSave = { name, code, conducted, attended, reqPct, colorIdx ->
                onUpsertSubject(editingSubject?.id, name, code, conducted, attended, reqPct, colorIdx)
                showEditorDialog = false
            }
        )
    }

    deletingSubject?.let { sub ->
        AlertDialog(
            onDismissRequest = { deletingSubject = null },
            containerColor = colors.cardBg,
            title = { Text("Delete ${sub.name}?", color = colors.textPrimary) },
            text = {
                Text(
                    "This also removes timetable slots and attendance logs linked to ${sub.code}.",
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteSubject(sub.id)
                        deletingSubject = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CriticalCoral)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingSubject = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SubjectEditorDialog(
    initial: Subject?,
    defaultRequiredPercent: Double,
    nextColorIndex: Long,
    onDismiss: () -> Unit,
    onSave: (name: String, code: String, conducted: Long, attended: Long, requiredPercent: Double, colorIndex: Long) -> Unit
) {
    val colors = LocalAttendXColors.current
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var code by remember { mutableStateOf(initial?.code ?: "") }
    var attendedStr by remember { mutableStateOf((initial?.attended ?: 0L).toString()) }
    var absentStr by remember { mutableStateOf((initial?.absent ?: 0L).toString()) }
    var conductedStr by remember { mutableStateOf((initial?.conducted ?: 0L).toString()) }
    var reqStr by remember {
        mutableStateOf((initial?.requiredPercent ?: defaultRequiredPercent).toInt().toString())
    }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.cardBg,
        title = {
            Text(
                text = if (initial == null) "Add Subject" else "Edit ${initial.code}",
                color = colors.textPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Subject Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("subject_name_input")
                )
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Subject Code (e.g. EC301)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("subject_code_input")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = attendedStr,
                        onValueChange = { v ->
                            attendedStr = v.filter { it.isDigit() }
                            val att = attendedStr.toLongOrNull() ?: 0L
                            val abs = absentStr.toLongOrNull() ?: 0L
                            conductedStr = (att + abs).toString()
                        },
                        label = { Text("Attended") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("subject_attended_input")
                    )
                    OutlinedTextField(
                        value = absentStr,
                        onValueChange = { v ->
                            absentStr = v.filter { it.isDigit() }
                            val att = attendedStr.toLongOrNull() ?: 0L
                            val abs = absentStr.toLongOrNull() ?: 0L
                            conductedStr = (att + abs).toString()
                        },
                        label = { Text("Absent") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("subject_absent_input")
                    )
                }
                OutlinedTextField(
                    value = conductedStr,
                    onValueChange = { v ->
                        conductedStr = v.filter { it.isDigit() }
                        val cond = conductedStr.toLongOrNull() ?: 0L
                        val att = attendedStr.toLongOrNull() ?: 0L
                        if (cond >= att) {
                            absentStr = (cond - att).toString()
                        }
                    },
                    label = { Text("Total Conducted") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("subject_conducted_input")
                )
                OutlinedTextField(
                    value = reqStr,
                    onValueChange = { reqStr = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Required Attendance % (Default ${defaultRequiredPercent.toInt()}%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("subject_required_input")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(75, 80, 85).forEach { preset ->
                        FilterChip(
                            selected = reqStr == preset.toString(),
                            onClick = { reqStr = preset.toString() },
                            label = { Text("$preset%") }
                        )
                    }
                }
                if (errorText != null) {
                    Text(errorText!!, color = CriticalCoral, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanName = name.trim()
                    val cleanCode = code.trim()
                    val att = attendedStr.toLongOrNull() ?: 0L
                    val cond = conductedStr.toLongOrNull() ?: 0L
                    val req = reqStr.toDoubleOrNull() ?: defaultRequiredPercent
                    when {
                        cleanName.isBlank() -> errorText = "Enter a subject name."
                        cleanCode.isBlank() -> errorText = "Enter a subject code."
                        att > cond -> errorText = "Attended classes ($att) cannot exceed conducted ($cond)."
                        req < 1.0 || req > 100.0 -> errorText = "Required % must be between 1 and 100."
                        else -> onSave(
                            cleanName,
                            cleanCode,
                            cond,
                            att,
                            req,
                            initial?.colorIndex ?: nextColorIndex
                        )
                    }
                },
                modifier = Modifier.testTag("save_subject_button"),
                colors = ButtonDefaults.buttonColors(containerColor = VioletPrimary)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
