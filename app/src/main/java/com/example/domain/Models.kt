package com.example.domain

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class AttendanceStatus {
    PRESENT,
    ABSENT,
    CANCELLED;

    companion object {
        fun fromStringSafe(raw: String?): AttendanceStatus =
            entries.firstOrNull { it.name == raw } ?: CANCELLED
    }
}

enum class AttendanceHealthStatus(val label: String) {
    SAFE("Safe"),
    WARNING("Warning"),
    CRITICAL("Critical")
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val defaultRequiredPercent: Double = 75.0,
    val seededDemoData: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Subject(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val code: String = "",
    val conducted: Long = 0L,
    val attended: Long = 0L,
    val requiredPercent: Double = 75.0,
    val colorIndex: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val absent: Long
        get() = (conducted - attended).coerceAtLeast(0L)
}

@Keep
@IgnoreExtraProperties
data class TimetableSlot(
    @DocumentId val id: String = "",
    val userId: String = "",
    val subjectId: String = "",
    val dayOfWeek: Long = 1L, // 1=Monday .. 6=Saturday
    val periodIndex: Long = 1L,
    val timeSlot: String = "09:00 - 09:50",
    val periodsCount: Long = 1L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AttendanceRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val subjectId: String = "",
    val dateIso: String = "",
    val periodIndex: Long = 1L,
    val periodsCount: Long = 1L,
    val status: String = AttendanceStatus.PRESENT.name,
    val note: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val statusEnum: AttendanceStatus
        get() = AttendanceStatus.fromStringSafe(status)

    companion object {
        fun buildId(dateIso: String, subjectId: String, periodIndex: Long): String {
            val safeSub = subjectId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            return "${dateIso}_${safeSub}_${periodIndex}"
        }
    }
}

@Keep
@IgnoreExtraProperties
data class Holiday(
    @DocumentId val id: String = "",
    val userId: String = "",
    val dateIso: String = "",
    val reason: String = "Holiday",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class AttendXDataSnapshot(
    val profile: UserProfile = UserProfile(),
    val subjects: List<Subject> = emptyList(),
    val timetableSlots: List<TimetableSlot> = emptyList(),
    val attendanceRecords: List<AttendanceRecord> = emptyList(),
    val holidays: List<Holiday> = emptyList()
)
