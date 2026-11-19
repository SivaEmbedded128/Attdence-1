package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AttendanceRepository
import com.example.domain.AttendXDataSnapshot
import com.example.domain.AttendanceCalculator
import com.example.domain.AttendanceStatus
import com.example.domain.BunkMetrics
import com.example.domain.DashboardSummary
import com.example.domain.IsoDate
import com.example.domain.SmartInsights
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class AttendXComputedState(
    val snapshot: AttendXDataSnapshot,
    val summary: DashboardSummary,
    val metricsBySubjectId: Map<String, BunkMetrics>,
    val insights: SmartInsights,
    val isCloudSynced: Boolean
)

data class SimulationDelta(
    val addAttended: Int = 0,
    val addMissed: Int = 0
)

class AttendXViewModel(
    private val repository: AttendanceRepository,
    private val displayName: String,
    private val email: String
) : ViewModel() {

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages = _messages.asSharedFlow()

    private val _simulationDeltas = MutableStateFlow<Map<String, SimulationDelta>>(emptyMap())
    val simulationDeltas: StateFlow<Map<String, SimulationDelta>> = _simulationDeltas.asStateFlow()

    val uiState: StateFlow<UiState<AttendXComputedState>> = repository.observeData()
        .map<AttendXDataSnapshot, UiState<AttendXComputedState>> { snapshot ->
            val summary = AttendanceCalculator.calculateDashboardSummary(
                snapshot.subjects,
                snapshot.profile.defaultRequiredPercent
            )
            val metricsMap = snapshot.subjects.associate { sub ->
                sub.id to AttendanceCalculator.calculateBunkMetrics(sub)
            }
            val insights = AttendanceCalculator.generateSmartInsights(
                subjects = snapshot.subjects,
                slots = snapshot.timetableSlots,
                holidays = snapshot.holidays,
                today = IsoDate.today()
            )
            UiState.Success(
                AttendXComputedState(
                    snapshot = snapshot,
                    summary = summary,
                    metricsBySubjectId = metricsMap,
                    insights = insights,
                    isCloudSynced = repository.isCloudSynced
                )
            )
        }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing AttendX data", rootError)
            emit(UiState.Error(rootError.localizedMessage ?: "Failed to load attendance data"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    init {
        viewModelScope.launch {
            repository.ensureInitialized(displayName, email).onFailure { err ->
                _messages.tryEmit(err.localizedMessage ?: "Initialization warning")
            }
        }
    }

    fun upsertSubject(
        id: String?,
        name: String,
        code: String,
        conducted: Long,
        attended: Long,
        requiredPercent: Double,
        colorIndex: Long
    ) {
        viewModelScope.launch {
            repository.upsertSubject(id, name, code, conducted, attended, requiredPercent, colorIndex)
                .onSuccess { _messages.tryEmit(if (id == null) "Subject added" else "Subject updated") }
                .onFailure { _messages.tryEmit(it.localizedMessage ?: "Failed to save subject") }
        }
    }

    fun deleteSubject(subjectId: String) {
        viewModelScope.launch {
            repository.deleteSubject(subjectId)
                .onSuccess { _messages.tryEmit("Subject deleted") }
                .onFailure { _messages.tryEmit(it.localizedMessage ?: "Failed to delete subject") }
        }
    }

    fun upsertTimetableSlot(
        id: String?,
        subjectId: String,
        dayOfWeek: Long,
        periodIndex: Long,
        timeSlot: String,
        periodsCount: Long
    ) {
        viewModelScope.launch {
            repository.upsertTimetableSlot(id, subjectId, dayOfWeek, periodIndex, timeSlot, periodsCount)
                .onSuccess { _messages.tryEmit(if (id == null) "Timetable slot added" else "Slot updated") }
                .onFailure { _messages.tryEmit(it.localizedMessage ?: "Failed to save slot") }
        }
    }

    fun deleteTimetableSlot(slotId: String) {
        viewModelScope.launch {
            repository.deleteTimetableSlot(slotId)
                .onSuccess { _messages.tryEmit("Timetable slot removed") }
                .onFailure { _messages.tryEmit(it.localizedMessage ?: "Failed to remove slot") }
        }
    }

    fun markAttendance(
        subjectId: String,
        dateIso: String,
        periodIndex: Long,
        periodsCount: Long,
        status: AttendanceStatus,
        note: String = ""
    ) {
        viewModelScope.launch {
            repository.markAttendance(subjectId, dateIso, periodIndex, periodsCount, status, note)
                .onSuccess { _messages.tryEmit("Marked ${status.name.lowercase().replaceFirstChar { it.uppercase() }}") }
                .onFailure { _messages.tryEmit(it.localizedMessage ?: "Failed to mark attendance") }
        }
    }

    fun deleteAttendanceRecord(recordId: String) {
        viewModelScope.launch {
            repository.deleteAttendanceRecord(recordId)
                .onSuccess { _messages.tryEmit("Attendance record removed and counts adjusted") }
                .onFailure { _messages.tryEmit(it.localizedMessage ?: "Failed to delete record") }
        }
    }

    fun toggleHoliday(dateIso: String, reason: String) {
        viewModelScope.launch {
            repository.toggleHoliday(dateIso, reason)
                .onSuccess { _messages.tryEmit("Holiday updated for $dateIso") }
                .onFailure { _messages.tryEmit(it.localizedMessage ?: "Failed to update holiday") }
        }
    }

    fun updateDefaultRequiredPercent(percent: Double) {
        viewModelScope.launch {
            repository.updateDefaultRequiredPercent(percent)
                .onSuccess { _messages.tryEmit("Default threshold set to ${percent.toInt()}%") }
                .onFailure { _messages.tryEmit(it.localizedMessage ?: "Failed to update threshold") }
        }
    }

    fun updateSimulation(subjectId: String, addAttended: Int, addMissed: Int) {
        val current = _simulationDeltas.value.toMutableMap()
        if (addAttended <= 0 && addMissed <= 0) {
            current.remove(subjectId)
        } else {
            current[subjectId] = SimulationDelta(
                addAttended = addAttended.coerceAtLeast(0),
                addMissed = addMissed.coerceAtLeast(0)
            )
        }
        _simulationDeltas.value = current
    }

    fun setAllSimulationDeltas(deltas: Map<String, SimulationDelta>) {
        _simulationDeltas.value = deltas.filterValues { it.addAttended > 0 || it.addMissed > 0 }
    }

    fun clearSimulation() {
        _simulationDeltas.value = emptyMap()
    }

    fun applySimulationToRecords() {
        val deltas = _simulationDeltas.value
        if (deltas.isEmpty()) return
        viewModelScope.launch {
            var allOk = true
            deltas.forEach { (subjectId, delta) ->
                val res = repository.applySimulatedDelta(subjectId, delta.addAttended, delta.addMissed)
                if (res.isFailure) allOk = false
            }
            if (allOk) {
                _simulationDeltas.value = emptyMap()
                _messages.tryEmit("Simulated changes applied to actual records")
            } else {
                _messages.tryEmit("Some simulated changes could not be saved")
            }
        }
    }

    fun resetToDemoData() {
        viewModelScope.launch {
            repository.resetToDemoData()
                .onSuccess {
                    _simulationDeltas.value = emptyMap()
                    _messages.tryEmit("Demo data restored")
                }
                .onFailure { _messages.tryEmit(it.localizedMessage ?: "Failed to restore demo data") }
        }
    }

    fun importBackupJson(json: String) {
        viewModelScope.launch {
            repository.importBackupJson(json)
                .onSuccess { _messages.tryEmit("Backup restored successfully") }
                .onFailure { _messages.tryEmit("Invalid backup file: ${it.localizedMessage ?: "Parse error"}") }
        }
    }

    fun exportBackupJson(snapshot: AttendXDataSnapshot): String =
        repository.exportBackupJson(snapshot)

    private companion object {
        const val TAG = "AttendXVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
