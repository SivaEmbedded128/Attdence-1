package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Functions
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.AttendanceCalculator
import com.example.domain.IsoDate
import com.example.ui.AttendXComputedState
import com.example.ui.SimulationDelta
import com.example.ui.components.AttendXCard
import com.example.ui.components.AttendanceRingChart
import com.example.ui.components.GradientHeroCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.CriticalCoral
import com.example.ui.theme.CriticalCoralBg
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalAttendXColors
import com.example.ui.theme.SafeEmerald
import com.example.ui.theme.SafeEmeraldBg
import com.example.ui.theme.VioletPrimary
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberBg

@Composable
fun BunkCalculatorScreen(
    state: AttendXComputedState,
    selectedSubjectId: String?,
    simulationDeltas: Map<String, SimulationDelta>,
    onSelectSubject: (String) -> Unit,
    onUpdateSimulation: (subjectId: String, addAttended: Int, addMissed: Int) -> Unit,
    onSetAllSimulation: (Map<String, SimulationDelta>) -> Unit,
    onClearSimulation: () -> Unit,
    onApplySimulation: () -> Unit
) {
    val colors = LocalAttendXColors.current
    var activeTab by remember { mutableIntStateOf(0) }
    val subjects = state.snapshot.subjects
    val activeSubject = subjects.firstOrNull { it.id == selectedSubjectId } ?: subjects.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("bunk_calculator_screen")
    ) {
        TabRow(
            selectedTabIndex = activeTab,
            containerColor = colors.cardBg,
            contentColor = colors.accentCyan
        ) {
            Tab(
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                text = { Text("Bunk Calculator") },
                modifier = Modifier.testTag("tab_bunk_calculator")
            )
            Tab(
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                text = { Text("Future Simulator") },
                modifier = Modifier.testTag("tab_future_simulator")
            )
        }

        if (subjects.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Add subjects first to calculate safe bunks.", color = colors.textMuted)
            }
            return
        }

        if (activeTab == 0 && activeSubject != null) {
            SubjectBunkCalculatorView(
                state = state,
                activeSubjectId = activeSubject.id,
                onSelectSubject = onSelectSubject
            )
        } else {
            FutureSimulatorView(
                state = state,
                simulationDeltas = simulationDeltas,
                onUpdateSimulation = onUpdateSimulation,
                onSetAllSimulation = onSetAllSimulation,
                onClearSimulation = onClearSimulation,
                onApplySimulation = onApplySimulation
            )
        }
    }
}

@Composable
private fun SubjectBunkCalculatorView(
    state: AttendXComputedState,
    activeSubjectId: String,
    onSelectSubject: (String) -> Unit
) {
    val colors = LocalAttendXColors.current
    val subjects = state.snapshot.subjects
    val selectedSubject = subjects.firstOrNull { it.id == activeSubjectId } ?: subjects.first()
    val metrics = state.metricsBySubjectId[selectedSubject.id]
        ?: AttendanceCalculator.calculateBunkMetrics(selectedSubject)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(subjects, key = { it.id }) { sub ->
                    FilterChip(
                        selected = sub.id == selectedSubject.id,
                        onClick = { onSelectSubject(sub.id) },
                        label = { Text("${sub.code} • ${sub.name}") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f),
                            selectedLabelColor = colors.textPrimary
                        )
                    )
                }
            }
        }

        item {
            GradientHeroCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    AttendanceRingChart(
                        percent = metrics.currentPercent,
                        status = metrics.healthStatus,
                        size = 114.dp,
                        strokeWidth = 10.dp
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = metrics.subjectCode,
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.accentCyan
                        )
                        Text(
                            text = metrics.subjectName,
                            style = MaterialTheme.typography.titleLarge,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "${metrics.attended} / ${metrics.conducted} attended  •  Target ${metrics.requiredPercent.toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                        StatusPill(status = metrics.healthStatus)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                val highlightBg = if (metrics.classesToRecover > 0) CriticalCoralBg else SafeEmeraldBg
                val highlightBorder = if (metrics.classesToRecover > 0) CriticalCoral else SafeEmerald
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = highlightBg,
                    border = BorderStroke(1.dp, highlightBorder.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = if (metrics.classesToRecover > 0) Icons.Rounded.WarningAmber else Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = highlightBorder,
                            modifier = Modifier.size(30.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            if (metrics.classesToRecover > 0) {
                                Text(
                                    text = "0 Safe Bunks Available",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = CriticalCoral,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Attend ${metrics.classesToRecover} consecutive class${if (metrics.classesToRecover > 1) "es" else ""} to recover to ${metrics.requiredPercent.toInt()}%.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textPrimary
                                )
                            } else {
                                Text(
                                    text = "${metrics.safeBunks} Safe Bunk${if (metrics.safeBunks == 1) "" else "s"} Available",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = SafeEmerald,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "You can miss up to ${metrics.safeBunks} more class${if (metrics.safeBunks == 1) "" else "es"} while staying ≥${metrics.requiredPercent.toInt()}%.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textPrimary
                                )
                            }
                        }
                    }
                }

                if (metrics.warnNextMissDropsBelow && metrics.classesToRecover == 0) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = WarningAmberBg,
                        border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = WarningAmber)
                            Text(
                                text = "Threshold Warning: Missing even 1 more class drops attendance to ${AttendanceCalculator.formatPercent(metrics.missedProjections.first().projectedPercent)} (below ${metrics.requiredPercent.toInt()}%).",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textPrimary
                            )
                        }
                    }
                }
            }
        }

        item {
            AttendXCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Impact of Missing Next 1–5 Classes",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    metrics.missedProjections.forEach { proj ->
                        val color = if (proj.remainsSafe) SafeEmerald else CriticalCoral
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = colors.cardElevated,
                            border = BorderStroke(1.dp, colors.borderSubtle)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Miss ${proj.classesMissed} class${if (proj.classesMissed > 1) "es" else ""}",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = colors.textPrimary
                                    )
                                    Text(
                                        text = "${proj.projectedAttended} / ${proj.projectedConducted} conducted",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.textMuted
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = AttendanceCalculator.formatPercent(proj.projectedPercent),
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = color
                                    )
                                    Text(
                                        text = if (proj.remainsSafe) "Safe" else "Below ${metrics.requiredPercent.toInt()}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = color
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            AttendXCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.Functions, contentDescription = null, tint = colors.accentViolet)
                    Text(
                        text = "Exact Mathematical Breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = metrics.formulaExplanation,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (metrics.classesToReach80 == 0) {
                        "80% Milestone: Already at ≥80%."
                    } else {
                        "80% Milestone: Attend ${metrics.classesToReach80} consecutive classes to reach 80%."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.accentCyan
                )
            }
        }

        item {
            Text(
                text = "All Subjects Quick Bunk Overview",
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary
            )
        }

        items(subjects, key = { "calc_${it.id}" }) { sub ->
            val m = state.metricsBySubjectId[sub.id] ?: AttendanceCalculator.calculateBunkMetrics(sub)
            AttendXCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onSelectSubject(sub.id) },
                borderColor = if (sub.id == selectedSubject.id) VioletPrimary else colors.borderSubtle
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
                                "Recover: Attend ${m.classesToRecover} consecutive (${AttendanceCalculator.formatPercentShort(m.currentPercent)})"
                            } else {
                                "Safe Bunks: ${m.safeBunks}  •  ${AttendanceCalculator.formatPercentShort(m.currentPercent)}"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (m.classesToRecover > 0) CriticalCoral else SafeEmerald
                        )
                    }
                    StatusPill(status = m.healthStatus)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FutureSimulatorView(
    state: AttendXComputedState,
    simulationDeltas: Map<String, SimulationDelta>,
    onUpdateSimulation: (subjectId: String, addAttended: Int, addMissed: Int) -> Unit,
    onSetAllSimulation: (Map<String, SimulationDelta>) -> Unit,
    onClearSimulation: () -> Unit,
    onApplySimulation: () -> Unit
) {
    val colors = LocalAttendXColors.current
    val subjects = state.snapshot.subjects
    val simResults = remember(subjects, simulationDeltas) {
        subjects.map { sub ->
            val d = simulationDeltas[sub.id] ?: SimulationDelta()
            AttendanceCalculator.simulateSubjectChange(sub, d.addAttended, d.addMissed)
        }
    }

    val simTotalAtt = simResults.sumOf { it.newAttended }
    val simTotalCond = simResults.sumOf { it.newConducted }
    val simOverallPct = AttendanceCalculator.calculatePercentage(simTotalAtt, simTotalCond)
    val beforeOverallPct = state.summary.overallPercent
    val simStatus = AttendanceCalculator.evaluateHealthStatus(
        simOverallPct,
        state.snapshot.profile.defaultRequiredPercent,
        simTotalCond
    )
    val hasChanges = simulationDeltas.values.any { it.addAttended > 0 || it.addMissed > 0 }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            GradientHeroCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Rounded.AutoGraph, contentDescription = null, tint = colors.accentCyan)
                            Text(
                                text = "Simulated Overall Attendance",
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.textPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${AttendanceCalculator.formatPercent(beforeOverallPct)}  →  ${AttendanceCalculator.formatPercent(simOverallPct)}",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = if (simOverallPct >= beforeOverallPct) SafeEmerald else CriticalCoral
                        )
                        Text(
                            text = "Preview only — actual records remain untouched until you confirm.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textMuted
                        )
                    }
                    StatusPill(status = simStatus)
                }

                if (hasChanges) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = onClearSimulation,
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, colors.border)
                        ) {
                            Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.size(6.dp))
                            Text("Reset")
                        }
                        Button(
                            onClick = onApplySimulation,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("apply_simulation_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Confirm & Apply")
                        }
                    }
                }
            }
        }

        item {
            AttendXCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Quick What-If Scenarios",
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AssistChip(
                        onClick = {
                            val tomorrowDow = IsoDate.today().plusDays(1).dayOfWeek()
                            val map = mutableMapOf<String, SimulationDelta>()
                            state.snapshot.timetableSlots
                                .filter { it.dayOfWeek.toInt() == tomorrowDow }
                                .forEach { slot ->
                                    val prev = map[slot.subjectId] ?: SimulationDelta()
                                    map[slot.subjectId] = prev.copy(addMissed = prev.addMissed + slot.periodsCount.toInt())
                                }
                            onSetAllSimulation(map)
                        },
                        label = { Text("Miss tomorrow's classes") }
                    )
                    AssistChip(
                        onClick = {
                            val map = mutableMapOf<String, SimulationDelta>()
                            state.snapshot.timetableSlots.forEach { slot ->
                                val prev = map[slot.subjectId] ?: SimulationDelta()
                                map[slot.subjectId] = prev.copy(addAttended = prev.addAttended + slot.periodsCount.toInt())
                            }
                            onSetAllSimulation(map)
                        },
                        label = { Text("Attend all next week") }
                    )
                    AssistChip(
                        onClick = {
                            val target = subjects.firstOrNull {
                                it.name.contains("Digital Electronics", ignoreCase = true)
                            } ?: subjects.first()
                            onSetAllSimulation(mapOf(target.id to SimulationDelta(addMissed = 2)))
                        },
                        label = { Text("Miss 2 Digital Electronics") }
                    )
                    AssistChip(
                        onClick = {
                            val target = subjects.firstOrNull {
                                it.name.contains("VLSI", ignoreCase = true)
                            } ?: subjects.first()
                            onSetAllSimulation(mapOf(target.id to SimulationDelta(addAttended = 5)))
                        },
                        label = { Text("Attend 5 VLSI classes") }
                    )
                }
            }
        }

        items(simResults, key = { "sim_${it.subject.id}" }) { sim ->
            val delta = simulationDeltas[sim.subject.id] ?: SimulationDelta()
            AttendXCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${sim.subject.code} • ${sim.subject.name}",
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${AttendanceCalculator.formatPercentShort(sim.beforePercent)} → ${AttendanceCalculator.formatPercentShort(sim.afterPercent)} (${sim.newAttended}/${sim.newConducted})",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 12.sp,
                            color = if (sim.afterPercent + 1e-9 >= sim.subject.requiredPercent) SafeEmerald else CriticalCoral
                        )
                    }
                    StatusPill(status = sim.newStatus)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StepperBox(
                        label = "Attend +",
                        value = delta.addAttended,
                        accent = SafeEmerald,
                        onDecrement = {
                            onUpdateSimulation(sim.subject.id, (delta.addAttended - 1).coerceAtLeast(0), delta.addMissed)
                        },
                        onIncrement = {
                            onUpdateSimulation(sim.subject.id, delta.addAttended + 1, delta.addMissed)
                        },
                        modifier = Modifier.weight(1f)
                    )
                    StepperBox(
                        label = "Miss +",
                        value = delta.addMissed,
                        accent = CriticalCoral,
                        onDecrement = {
                            onUpdateSimulation(sim.subject.id, delta.addAttended, (delta.addMissed - 1).coerceAtLeast(0))
                        },
                        onIncrement = {
                            onUpdateSimulation(sim.subject.id, delta.addAttended, delta.addMissed + 1)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun StepperBox(
    label: String,
    value: Int,
    accent: Color,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAttendXColors.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = colors.cardElevated,
        border = BorderStroke(1.dp, colors.borderSubtle)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onDecrement, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Rounded.Remove, contentDescription = "Decrease $label", tint = colors.textSecondary)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = label, style = MaterialTheme.typography.labelSmall, color = colors.textMuted)
                Text(
                    text = value.toString(),
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = accent
                )
            }
            IconButton(onClick = onIncrement, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Rounded.Add, contentDescription = "Increase $label", tint = accent)
            }
        }
    }
}
