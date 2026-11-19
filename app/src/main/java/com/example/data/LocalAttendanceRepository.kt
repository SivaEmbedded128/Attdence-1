package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.AttendXDataSnapshot
import com.example.domain.AttendanceRecord
import com.example.domain.AttendanceStatus
import com.example.domain.DemoDataSeed
import com.example.domain.Holiday
import com.example.domain.Subject
import com.example.domain.TimetableSlot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.attendXLocalStore: DataStore<Preferences> by preferencesDataStore(name = "attendx_local_store")

/**
 * Device-only local persistence repository backed by Jetpack DataStore.
 * Used when the student selects Local Device Mode or when Firebase is unavailable.
 */
class LocalAttendanceRepository(
    private val context: Context
) : AttendanceRepository {

    override val isCloudSynced: Boolean = false

    private val dataKey = stringPreferencesKey("attendx_snapshot_json")
    private val localUid = "local_device_user"

    override fun observeData(): Flow<AttendXDataSnapshot> =
        context.attendXLocalStore.data.map { prefs ->
            val raw = prefs[dataKey]
            if (raw.isNullOrBlank()) {
                DemoDataSeed.createSeedData(localUid)
            } else {
                try {
                    BackupJsonCodec.decode(raw, localUid)
                } catch (_: Exception) {
                    DemoDataSeed.createSeedData(localUid)
                }
            }
        }

    override suspend fun ensureInitialized(displayName: String, email: String): Result<Unit> {
        return try {
            val prefs = context.attendXLocalStore.data.first()
            if (prefs[dataKey].isNullOrBlank()) {
                val seed = DemoDataSeed.createSeedData(localUid)
                saveSnapshot(seed)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun getCurrentSnapshot(): AttendXDataSnapshot {
        val prefs = context.attendXLocalStore.data.first()
        val raw = prefs[dataKey]
        return if (raw.isNullOrBlank()) {
            DemoDataSeed.createSeedData(localUid).also { saveSnapshot(it) }
        } else {
            try {
                BackupJsonCodec.decode(raw, localUid)
            } catch (_: Exception) {
                DemoDataSeed.createSeedData(localUid)
            }
        }
    }

    private suspend fun saveSnapshot(snapshot: AttendXDataSnapshot) {
        context.attendXLocalStore.edit { prefs ->
            prefs[dataKey] = BackupJsonCodec.encode(snapshot)
        }
    }

    override suspend fun upsertSubject(
        id: String?,
        name: String,
        code: String,
        conducted: Long,
        attended: Long,
        requiredPercent: Double,
        colorIndex: Long
    ): Result<Unit> = runCatching {
        val cleanName = name.trim().take(80)
        val cleanCode = code.trim().take(24)
        require(cleanName.isNotBlank()) { "Subject name cannot be empty." }
        require(cleanCode.isNotBlank()) { "Subject code cannot be empty." }
        val safeCond = conducted.coerceIn(0L, 10000L)
        val safeAtt = attended.coerceIn(0L, safeCond)
        val safeReq = requiredPercent.coerceIn(1.0, 100.0)

        val current = getCurrentSnapshot()
        val targetId = id?.takeIf { it.isNotBlank() } ?: "sub_${UUID.randomUUID().toString().take(8)}"
        val updatedList = current.subjects.toMutableList()
        val idx = updatedList.indexOfFirst { it.id == targetId }
        val newSub = Subject(
            id = targetId,
            userId = localUid,
            name = cleanName,
            code = cleanCode,
            conducted = safeCond,
            attended = safeAtt,
            requiredPercent = safeReq,
            colorIndex = colorIndex.coerceIn(0L, 20L)
        )
        if (idx >= 0) updatedList[idx] = newSub else updatedList.add(newSub)
        saveSnapshot(current.copy(subjects = updatedList.sortedBy { it.name.lowercase() }))
    }

    override suspend fun deleteSubject(subjectId: String): Result<Unit> = runCatching {
        val current = getCurrentSnapshot()
        saveSnapshot(
            current.copy(
                subjects = current.subjects.filterNot { it.id == subjectId },
                timetableSlots = current.timetableSlots.filterNot { it.subjectId == subjectId },
                attendanceRecords = current.attendanceRecords.filterNot { it.subjectId == subjectId }
            )
        )
    }

    override suspend fun upsertTimetableSlot(
        id: String?,
        subjectId: String,
        dayOfWeek: Long,
        periodIndex: Long,
        timeSlot: String,
        periodsCount: Long
    ): Result<Unit> = runCatching {
        require(subjectId.isNotBlank()) { "Please select a subject." }
        val current = getCurrentSnapshot()
        val targetId = id?.takeIf { it.isNotBlank() } ?: "slot_${dayOfWeek}_${UUID.randomUUID().toString().take(8)}"
        val updated = current.timetableSlots.toMutableList()
        val idx = updated.indexOfFirst { it.id == targetId }
        val slot = TimetableSlot(
            id = targetId,
            userId = localUid,
            subjectId = subjectId,
            dayOfWeek = dayOfWeek.coerceIn(1L, 6L),
            periodIndex = periodIndex.coerceIn(1L, 12L),
            timeSlot = timeSlot.trim().take(40).ifBlank { "09:00 - 09:50" },
            periodsCount = periodsCount.coerceIn(1L, 6L)
        )
        if (idx >= 0) updated[idx] = slot else updated.add(slot)
        saveSnapshot(current.copy(timetableSlots = updated.sortedWith(compareBy({ it.dayOfWeek }, { it.periodIndex }))))
    }

    override suspend fun deleteTimetableSlot(slotId: String): Result<Unit> = runCatching {
        val current = getCurrentSnapshot()
        saveSnapshot(current.copy(timetableSlots = current.timetableSlots.filterNot { it.id == slotId }))
    }

    override suspend fun markAttendance(
        subjectId: String,
        dateIso: String,
        periodIndex: Long,
        periodsCount: Long,
        status: AttendanceStatus,
        note: String
    ): Result<Unit> = runCatching {
        val current = getCurrentSnapshot()
        val recordId = AttendanceRecord.buildId(dateIso, subjectId, periodIndex)
        val safeCount = periodsCount.coerceIn(1L, 6L)
        val cleanNote = note.trim().take(160)

        val records = current.attendanceRecords.toMutableList()
        val existingIdx = records.indexOfFirst { it.id == recordId }

        var deltaCond = 0L
        var deltaAtt = 0L

        if (existingIdx >= 0) {
            val old = records[existingIdx]
            if (old.statusEnum == status && old.periodsCount == safeCount) {
                records[existingIdx] = old.copy(note = cleanNote)
                saveSnapshot(current.copy(attendanceRecords = records))
                return@runCatching
            }
            when (old.statusEnum) {
                AttendanceStatus.PRESENT -> {
                    deltaCond -= old.periodsCount
                    deltaAtt -= old.periodsCount
                }
                AttendanceStatus.ABSENT -> {
                    deltaCond -= old.periodsCount
                }
                AttendanceStatus.CANCELLED -> Unit
            }
            when (status) {
                AttendanceStatus.PRESENT -> {
                    deltaCond += safeCount
                    deltaAtt += safeCount
                }
                AttendanceStatus.ABSENT -> {
                    deltaCond += safeCount
                }
                AttendanceStatus.CANCELLED -> Unit
            }
            records[existingIdx] = old.copy(
                status = status.name,
                periodsCount = safeCount,
                note = cleanNote
            )
        } else {
            when (status) {
                AttendanceStatus.PRESENT -> {
                    deltaCond += safeCount
                    deltaAtt += safeCount
                }
                AttendanceStatus.ABSENT -> {
                    deltaCond += safeCount
                }
                AttendanceStatus.CANCELLED -> Unit
            }
            records.add(
                0,
                AttendanceRecord(
                    id = recordId,
                    userId = localUid,
                    subjectId = subjectId,
                    dateIso = dateIso,
                    periodIndex = periodIndex.coerceIn(1L, 12L),
                    periodsCount = safeCount,
                    status = status.name,
                    note = cleanNote
                )
            )
        }

        val subjects = current.subjects.map { sub ->
            if (sub.id == subjectId) {
                val newCond = (sub.conducted + deltaCond).coerceIn(0L, 10000L)
                val newAtt = (sub.attended + deltaAtt).coerceIn(0L, newCond)
                sub.copy(conducted = newCond, attended = newAtt)
            } else sub
        }

        saveSnapshot(
            current.copy(
                subjects = subjects,
                attendanceRecords = records.sortedWith(compareByDescending<AttendanceRecord> { it.dateIso }.thenBy { it.periodIndex })
            )
        )
    }

    override suspend fun deleteAttendanceRecord(recordId: String): Result<Unit> = runCatching {
        val current = getCurrentSnapshot()
        val target = current.attendanceRecords.firstOrNull { it.id == recordId } ?: return@runCatching
        val subjects = current.subjects.map { sub ->
            if (sub.id == target.subjectId && target.statusEnum != AttendanceStatus.CANCELLED) {
                val newCond = (sub.conducted - target.periodsCount).coerceAtLeast(0L)
                val newAtt = if (target.statusEnum == AttendanceStatus.PRESENT) {
                    (sub.attended - target.periodsCount).coerceIn(0L, newCond)
                } else {
                    sub.attended.coerceIn(0L, newCond)
                }
                sub.copy(conducted = newCond, attended = newAtt)
            } else sub
        }
        saveSnapshot(
            current.copy(
                subjects = subjects,
                attendanceRecords = current.attendanceRecords.filterNot { it.id == recordId }
            )
        )
    }

    override suspend fun toggleHoliday(dateIso: String, reason: String): Result<Unit> = runCatching {
        val current = getCurrentSnapshot()
        val exists = current.holidays.any { it.dateIso == dateIso }
        val updated = if (exists) {
            current.holidays.filterNot { it.dateIso == dateIso }
        } else {
            current.holidays + Holiday(
                id = dateIso,
                userId = localUid,
                dateIso = dateIso,
                reason = reason.trim().take(80).ifBlank { "College Holiday" }
            )
        }
        saveSnapshot(current.copy(holidays = updated.sortedBy { it.dateIso }))
    }

    override suspend fun updateDefaultRequiredPercent(percent: Double): Result<Unit> = runCatching {
        val current = getCurrentSnapshot()
        val safe = percent.coerceIn(1.0, 100.0)
        saveSnapshot(current.copy(profile = current.profile.copy(defaultRequiredPercent = safe)))
    }

    override suspend fun applySimulatedDelta(
        subjectId: String,
        addAttended: Int,
        addMissed: Int
    ): Result<Unit> = runCatching {
        val current = getCurrentSnapshot()
        val subjects = current.subjects.map { sub ->
            if (sub.id == subjectId) {
                val newCond = (sub.conducted + addAttended + addMissed).coerceIn(0L, 10000L)
                val newAtt = (sub.attended + addAttended).coerceIn(0L, newCond)
                sub.copy(conducted = newCond, attended = newAtt)
            } else sub
        }
        saveSnapshot(current.copy(subjects = subjects))
    }

    override suspend fun resetToDemoData(): Result<Unit> = runCatching {
        saveSnapshot(DemoDataSeed.createSeedData(localUid))
    }

    override suspend fun importBackupJson(jsonString: String): Result<Unit> = runCatching {
        val decoded = BackupJsonCodec.decode(jsonString, localUid)
        saveSnapshot(decoded)
    }

    override fun exportBackupJson(snapshot: AttendXDataSnapshot): String =
        BackupJsonCodec.encode(snapshot)
}
