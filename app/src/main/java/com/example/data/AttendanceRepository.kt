package com.example.data

import com.example.domain.AttendXDataSnapshot
import com.example.domain.AttendanceRecord
import com.example.domain.AttendanceStatus
import com.example.domain.Holiday
import com.example.domain.Subject
import com.example.domain.TimetableSlot
import com.example.domain.UserProfile
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

interface AttendanceRepository {
    val isCloudSynced: Boolean

    fun observeData(): Flow<AttendXDataSnapshot>

    suspend fun ensureInitialized(displayName: String = "Student", email: String = ""): Result<Unit>

    suspend fun upsertSubject(
        id: String?,
        name: String,
        code: String,
        conducted: Long,
        attended: Long,
        requiredPercent: Double,
        colorIndex: Long
    ): Result<Unit>

    suspend fun deleteSubject(subjectId: String): Result<Unit>

    suspend fun upsertTimetableSlot(
        id: String?,
        subjectId: String,
        dayOfWeek: Long,
        periodIndex: Long,
        timeSlot: String,
        periodsCount: Long
    ): Result<Unit>

    suspend fun deleteTimetableSlot(slotId: String): Result<Unit>

    suspend fun markAttendance(
        subjectId: String,
        dateIso: String,
        periodIndex: Long,
        periodsCount: Long,
        status: AttendanceStatus,
        note: String = ""
    ): Result<Unit>

    suspend fun deleteAttendanceRecord(recordId: String): Result<Unit>

    suspend fun toggleHoliday(dateIso: String, reason: String): Result<Unit>

    suspend fun updateDefaultRequiredPercent(percent: Double): Result<Unit>

    suspend fun applySimulatedDelta(subjectId: String, addAttended: Int, addMissed: Int): Result<Unit>

    suspend fun resetToDemoData(): Result<Unit>

    suspend fun importBackupJson(jsonString: String): Result<Unit>

    fun exportBackupJson(snapshot: AttendXDataSnapshot): String
}

object BackupJsonCodec {
    fun encode(snapshot: AttendXDataSnapshot): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("defaultRequiredPercent", snapshot.profile.defaultRequiredPercent)

        val subjectsArr = JSONArray()
        snapshot.subjects.forEach { s ->
            subjectsArr.put(
                JSONObject().apply {
                    put("id", s.id)
                    put("name", s.name)
                    put("code", s.code)
                    put("conducted", s.conducted)
                    put("attended", s.attended)
                    put("requiredPercent", s.requiredPercent)
                    put("colorIndex", s.colorIndex)
                }
            )
        }
        root.put("subjects", subjectsArr)

        val slotsArr = JSONArray()
        snapshot.timetableSlots.forEach { t ->
            slotsArr.put(
                JSONObject().apply {
                    put("id", t.id)
                    put("subjectId", t.subjectId)
                    put("dayOfWeek", t.dayOfWeek)
                    put("periodIndex", t.periodIndex)
                    put("timeSlot", t.timeSlot)
                    put("periodsCount", t.periodsCount)
                }
            )
        }
        root.put("timetableSlots", slotsArr)

        val recordsArr = JSONArray()
        snapshot.attendanceRecords.forEach { r ->
            recordsArr.put(
                JSONObject().apply {
                    put("id", r.id)
                    put("subjectId", r.subjectId)
                    put("dateIso", r.dateIso)
                    put("periodIndex", r.periodIndex)
                    put("periodsCount", r.periodsCount)
                    put("status", r.status)
                    put("note", r.note)
                }
            )
        }
        root.put("attendanceRecords", recordsArr)

        val holidaysArr = JSONArray()
        snapshot.holidays.forEach { h ->
            holidaysArr.put(
                JSONObject().apply {
                    put("dateIso", h.dateIso)
                    put("reason", h.reason)
                }
            )
        }
        root.put("holidays", holidaysArr)
        return root.toString(2)
    }

    fun decode(jsonString: String, userId: String): AttendXDataSnapshot {
        val root = JSONObject(jsonString)
        val defaultPct = root.optDouble("defaultRequiredPercent", 75.0).coerceIn(1.0, 100.0)

        val subjects = mutableListOf<Subject>()
        val subjectsArr = root.optJSONArray("subjects") ?: JSONArray()
        for (i in 0 until subjectsArr.length()) {
            val obj = subjectsArr.getJSONObject(i)
            val cond = obj.optLong("conducted", 0L).coerceAtLeast(0L)
            val att = obj.optLong("attended", 0L).coerceIn(0L, cond)
            subjects.add(
                Subject(
                    id = obj.optString("id").ifBlank { "sub_$i" },
                    userId = userId,
                    name = obj.optString("name", "Subject"),
                    code = obj.optString("code", "SUB$i"),
                    conducted = cond,
                    attended = att,
                    requiredPercent = obj.optDouble("requiredPercent", defaultPct).coerceIn(1.0, 100.0),
                    colorIndex = obj.optLong("colorIndex", (i % 8).toLong())
                )
            )
        }

        val slots = mutableListOf<TimetableSlot>()
        val slotsArr = root.optJSONArray("timetableSlots") ?: JSONArray()
        for (i in 0 until slotsArr.length()) {
            val obj = slotsArr.getJSONObject(i)
            slots.add(
                TimetableSlot(
                    id = obj.optString("id").ifBlank { "slot_$i" },
                    userId = userId,
                    subjectId = obj.optString("subjectId"),
                    dayOfWeek = obj.optLong("dayOfWeek", 1L).coerceIn(1L, 6L),
                    periodIndex = obj.optLong("periodIndex", 1L).coerceIn(1L, 12L),
                    timeSlot = obj.optString("timeSlot", "09:00 - 09:50"),
                    periodsCount = obj.optLong("periodsCount", 1L).coerceIn(1L, 6L)
                )
            )
        }

        val records = mutableListOf<AttendanceRecord>()
        val recordsArr = root.optJSONArray("attendanceRecords") ?: JSONArray()
        for (i in 0 until recordsArr.length()) {
            val obj = recordsArr.getJSONObject(i)
            val dateIso = obj.optString("dateIso")
            val subjectId = obj.optString("subjectId")
            val periodIdx = obj.optLong("periodIndex", 1L).coerceIn(1L, 12L)
            val id = obj.optString("id").ifBlank { AttendanceRecord.buildId(dateIso, subjectId, periodIdx) }
            records.add(
                AttendanceRecord(
                    id = id,
                    userId = userId,
                    subjectId = subjectId,
                    dateIso = dateIso,
                    periodIndex = periodIdx,
                    periodsCount = obj.optLong("periodsCount", 1L).coerceIn(1L, 6L),
                    status = AttendanceStatus.fromStringSafe(obj.optString("status")).name,
                    note = obj.optString("note", "")
                )
            )
        }

        val holidays = mutableListOf<Holiday>()
        val holidaysArr = root.optJSONArray("holidays") ?: JSONArray()
        for (i in 0 until holidaysArr.length()) {
            val obj = holidaysArr.getJSONObject(i)
            val dateIso = obj.optString("dateIso")
            if (dateIso.isNotBlank()) {
                holidays.add(
                    Holiday(
                        id = dateIso,
                        userId = userId,
                        dateIso = dateIso,
                        reason = obj.optString("reason", "Holiday")
                    )
                )
            }
        }

        return AttendXDataSnapshot(
            profile = UserProfile(
                id = userId,
                userId = userId,
                displayName = "Student",
                email = "",
                defaultRequiredPercent = defaultPct,
                seededDemoData = true
            ),
            subjects = subjects,
            timetableSlots = slots,
            attendanceRecords = records,
            holidays = holidays
        )
    }
}
