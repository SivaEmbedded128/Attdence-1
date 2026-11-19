package com.example.data

import android.content.Context
import com.example.R
import com.example.domain.AttendXDataSnapshot
import com.example.domain.AttendanceRecord
import com.example.domain.AttendanceStatus
import com.example.domain.DemoDataSeed
import com.example.domain.Holiday
import com.example.domain.Subject
import com.example.domain.TimetableSlot
import com.example.domain.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirestoreAttendanceRepository(
    private val db: FirebaseFirestore,
    private val currentUserId: String,
    private val auth: FirebaseAuth = Firebase.auth
) : AttendanceRepository {

    constructor(context: Context, currentUserId: String) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        currentUserId = currentUserId
    )

    override val isCloudSynced: Boolean = true

    private fun requireUserId(): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
        check(uid == currentUserId) { "Signed-in user changed; refusing cross-user operation." }
        return uid
    }

    private fun userDoc() = db.collection("users").document(currentUserId)
    private fun subjectsCol() = userDoc().collection("subjects")
    private fun timetableCol() = userDoc().collection("timetable")
    private fun attendanceCol() = userDoc().collection("attendance")
    private fun holidaysCol() = userDoc().collection("holidays")

    override fun observeData(): Flow<AttendXDataSnapshot> = flow {
        val uid = requireUserId()

        val profileFlow = userDoc().snapshots()
            .map { snap ->
                snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: UserProfile(id = uid, userId = uid)
            }
            .catch { error -> logAndRethrow(error, OperationType.GET, "users/$uid") }

        val subjectsFlow = subjectsCol().snapshots()
            .map { snap ->
                snap.toObjects(Subject::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.name.lowercase() }
            }
            .catch { error -> logAndRethrow(error, OperationType.LIST, "users/$uid/subjects") }

        val timetableFlow = timetableCol().snapshots()
            .map { snap ->
                snap.toObjects(TimetableSlot::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedWith(compareBy({ it.dayOfWeek }, { it.periodIndex }))
            }
            .catch { error -> logAndRethrow(error, OperationType.LIST, "users/$uid/timetable") }

        val attendanceFlow = attendanceCol().snapshots()
            .map { snap ->
                snap.toObjects(AttendanceRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedWith(compareByDescending<AttendanceRecord> { it.dateIso }.thenBy { it.periodIndex })
            }
            .catch { error -> logAndRethrow(error, OperationType.LIST, "users/$uid/attendance") }

        val holidaysFlow = holidaysCol().snapshots()
            .map { snap ->
                snap.toObjects(Holiday::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.dateIso }
            }
            .catch { error -> logAndRethrow(error, OperationType.LIST, "users/$uid/holidays") }

        emitAll(
            combine(
                profileFlow,
                subjectsFlow,
                timetableFlow,
                attendanceFlow,
                holidaysFlow
            ) { profile, subjects, timetable, attendance, holidays ->
                AttendXDataSnapshot(
                    profile = profile,
                    subjects = subjects,
                    timetableSlots = timetable,
                    attendanceRecords = attendance,
                    holidays = holidays
                )
            }
        )
    }

    private fun logAndRethrow(error: Throwable, op: OperationType, path: String): Nothing {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == currentUserId) {
            handleFirestoreError(exceptionToLog, op, path)
        }
        throw error
    }

    override suspend fun ensureInitialized(displayName: String, email: String): Result<Unit> {
        val path = "users/$currentUserId"
        return try {
            val uid = requireUserId()
            val snap = userDoc().get().await()
            if (!snap.exists()) {
                val seed = DemoDataSeed.createSeedData(uid)
                writeFullSnapshot(
                    seed.copy(
                        profile = seed.profile.copy(
                            displayName = displayName.ifBlank { "Student" }.take(100),
                            email = email.take(160)
                        )
                    )
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    private suspend fun writeFullSnapshot(snapshot: AttendXDataSnapshot) {
        val uid = requireUserId()
        val batch = db.batch()

        batch.set(
            userDoc(),
            mapOf(
                "userId" to uid,
                "displayName" to snapshot.profile.displayName.take(100),
                "email" to snapshot.profile.email.take(160),
                "defaultRequiredPercent" to snapshot.profile.defaultRequiredPercent.coerceIn(1.0, 100.0),
                "seededDemoData" to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )

        snapshot.subjects.forEach { s ->
            val cond = s.conducted.coerceIn(0L, 10000L)
            val att = s.attended.coerceIn(0L, cond)
            batch.set(
                subjectsCol().document(sanitizeId(s.id)),
                mapOf(
                    "userId" to uid,
                    "name" to s.name.trim().take(80).ifBlank { "Subject" },
                    "code" to s.code.trim().take(24).ifBlank { "CODE" },
                    "conducted" to cond,
                    "attended" to att,
                    "requiredPercent" to s.requiredPercent.coerceIn(1.0, 100.0),
                    "colorIndex" to s.colorIndex.coerceIn(0L, 20L),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }

        snapshot.timetableSlots.forEach { t ->
            batch.set(
                timetableCol().document(sanitizeId(t.id)),
                mapOf(
                    "userId" to uid,
                    "subjectId" to sanitizeId(t.subjectId),
                    "dayOfWeek" to t.dayOfWeek.coerceIn(1L, 6L),
                    "periodIndex" to t.periodIndex.coerceIn(1L, 12L),
                    "timeSlot" to t.timeSlot.trim().take(40).ifBlank { "09:00 - 09:50" },
                    "periodsCount" to t.periodsCount.coerceIn(1L, 6L),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }

        snapshot.attendanceRecords.forEach { r ->
            val recId = sanitizeId(r.id.ifBlank { AttendanceRecord.buildId(r.dateIso, r.subjectId, r.periodIndex) })
            batch.set(
                attendanceCol().document(recId),
                mapOf(
                    "userId" to uid,
                    "subjectId" to sanitizeId(r.subjectId),
                    "dateIso" to r.dateIso,
                    "periodIndex" to r.periodIndex.coerceIn(1L, 12L),
                    "periodsCount" to r.periodsCount.coerceIn(1L, 6L),
                    "status" to r.statusEnum.name,
                    "note" to r.note.take(160),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }

        snapshot.holidays.forEach { h ->
            batch.set(
                holidaysCol().document(h.dateIso),
                mapOf(
                    "userId" to uid,
                    "dateIso" to h.dateIso,
                    "reason" to h.reason.trim().take(80).ifBlank { "Holiday" },
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }

        batch.commit().await()
    }

    override suspend fun upsertSubject(
        id: String?,
        name: String,
        code: String,
        conducted: Long,
        attended: Long,
        requiredPercent: Double,
        colorIndex: Long
    ): Result<Unit> {
        val docId = sanitizeId(id?.takeIf { it.isNotBlank() } ?: "sub_${UUID.randomUUID().toString().take(8)}")
        val docPath = "users/$currentUserId/subjects/$docId"
        return try {
            val uid = requireUserId()
            val cleanName = name.trim().take(80)
            val cleanCode = code.trim().take(24)
            require(cleanName.isNotBlank()) { "Subject name cannot be empty." }
            require(cleanCode.isNotBlank()) { "Subject code cannot be empty." }
            val safeConducted = conducted.coerceIn(0L, 10000L)
            val safeAttended = attended.coerceIn(0L, safeConducted)
            val safeReq = requiredPercent.coerceIn(1.0, 100.0)
            val safeColor = colorIndex.coerceIn(0L, 20L)

            val docRef = subjectsCol().document(docId)
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "name" to cleanName,
                        "code" to cleanCode,
                        "conducted" to safeConducted,
                        "attended" to safeAttended,
                        "requiredPercent" to safeReq,
                        "colorIndex" to safeColor,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "name" to cleanName,
                        "code" to cleanCode,
                        "conducted" to safeConducted,
                        "attended" to safeAttended,
                        "requiredPercent" to safeReq,
                        "colorIndex" to safeColor,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    override suspend fun deleteSubject(subjectId: String): Result<Unit> {
        val docPath = "users/$currentUserId/subjects/$subjectId"
        return try {
            requireUserId()
            require(subjectId.isNotBlank())
            val batch = db.batch()
            batch.delete(subjectsCol().document(subjectId))
            val relatedSlots = timetableCol().whereEqualTo("subjectId", subjectId).get().await()
            relatedSlots.documents.forEach { batch.delete(it.reference) }
            val relatedRecords = attendanceCol().whereEqualTo("subjectId", subjectId).get().await()
            relatedRecords.documents.forEach { batch.delete(it.reference) }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    override suspend fun upsertTimetableSlot(
        id: String?,
        subjectId: String,
        dayOfWeek: Long,
        periodIndex: Long,
        timeSlot: String,
        periodsCount: Long
    ): Result<Unit> {
        val docId = sanitizeId(id?.takeIf { it.isNotBlank() } ?: "slot_${dayOfWeek}_${UUID.randomUUID().toString().take(8)}")
        val docPath = "users/$currentUserId/timetable/$docId"
        return try {
            val uid = requireUserId()
            require(subjectId.isNotBlank()) { "Please choose a subject." }
            val safeDay = dayOfWeek.coerceIn(1L, 6L)
            val safePeriod = periodIndex.coerceIn(1L, 12L)
            val safeCount = periodsCount.coerceIn(1L, 6L)
            val cleanTime = timeSlot.trim().take(40).ifBlank { "09:00 - 09:50" }

            val docRef = timetableCol().document(docId)
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "subjectId" to sanitizeId(subjectId),
                        "dayOfWeek" to safeDay,
                        "periodIndex" to safePeriod,
                        "timeSlot" to cleanTime,
                        "periodsCount" to safeCount,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "subjectId" to sanitizeId(subjectId),
                        "dayOfWeek" to safeDay,
                        "periodIndex" to safePeriod,
                        "timeSlot" to cleanTime,
                        "periodsCount" to safeCount,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    override suspend fun deleteTimetableSlot(slotId: String): Result<Unit> {
        val docPath = "users/$currentUserId/timetable/$slotId"
        return try {
            requireUserId()
            timetableCol().document(slotId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    override suspend fun markAttendance(
        subjectId: String,
        dateIso: String,
        periodIndex: Long,
        periodsCount: Long,
        status: AttendanceStatus,
        note: String
    ): Result<Unit> {
        val recordId = sanitizeId(AttendanceRecord.buildId(dateIso, subjectId, periodIndex))
        val docPath = "users/$currentUserId/attendance/$recordId"
        return try {
            val uid = requireUserId()
            val safePeriod = periodIndex.coerceIn(1L, 12L)
            val safeCount = periodsCount.coerceIn(1L, 6L)
            val cleanNote = note.trim().take(160)

            val recordRef = attendanceCol().document(recordId)
            val subjectRef = subjectsCol().document(sanitizeId(subjectId))

            val existingRecordSnap = recordRef.get().await()
            val subjectSnap = subjectRef.get().await()
            val currentCond = subjectSnap.getLong("conducted") ?: 0L
            val currentAtt = subjectSnap.getLong("attended") ?: 0L

            var deltaCond = 0L
            var deltaAtt = 0L

            if (existingRecordSnap.exists()) {
                val oldStatus = AttendanceStatus.fromStringSafe(existingRecordSnap.getString("status"))
                val oldCount = existingRecordSnap.getLong("periodsCount") ?: safeCount
                if (oldStatus == status && oldCount == safeCount) {
                    if ((existingRecordSnap.getString("note") ?: "") != cleanNote) {
                        recordRef.update(
                            mapOf("note" to cleanNote, "updatedAt" to FieldValue.serverTimestamp())
                        ).await()
                    }
                    return Result.success(Unit)
                }
                // Subtract old status contribution
                when (oldStatus) {
                    AttendanceStatus.PRESENT -> {
                        deltaCond -= oldCount
                        deltaAtt -= oldCount
                    }
                    AttendanceStatus.ABSENT -> {
                        deltaCond -= oldCount
                    }
                    AttendanceStatus.CANCELLED -> Unit
                }
                // Add new status contribution
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
            }

            val newCond = (currentCond + deltaCond).coerceIn(0L, 10000L)
            val newAtt = (currentAtt + deltaAtt).coerceIn(0L, newCond)

            val batch = db.batch()
            if (existingRecordSnap.exists()) {
                batch.update(
                    recordRef,
                    mapOf(
                        "status" to status.name,
                        "periodsCount" to safeCount,
                        "note" to cleanNote,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                batch.set(
                    recordRef,
                    mapOf(
                        "userId" to uid,
                        "subjectId" to sanitizeId(subjectId),
                        "dateIso" to dateIso,
                        "periodIndex" to safePeriod,
                        "periodsCount" to safeCount,
                        "status" to status.name,
                        "note" to cleanNote,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            if (subjectSnap.exists()) {
                batch.update(
                    subjectRef,
                    mapOf(
                        "conducted" to newCond,
                        "attended" to newAtt,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    override suspend fun deleteAttendanceRecord(recordId: String): Result<Unit> {
        val docPath = "users/$currentUserId/attendance/$recordId"
        return try {
            requireUserId()
            val recordRef = attendanceCol().document(recordId)
            val snap = recordRef.get().await()
            if (!snap.exists()) return Result.success(Unit)

            val subjectId = snap.getString("subjectId").orEmpty()
            val status = AttendanceStatus.fromStringSafe(snap.getString("status"))
            val count = snap.getLong("periodsCount") ?: 1L

            val batch = db.batch()
            batch.delete(recordRef)

            if (subjectId.isNotBlank() && status != AttendanceStatus.CANCELLED) {
                val subjectRef = subjectsCol().document(subjectId)
                val subjectSnap = subjectRef.get().await()
                if (subjectSnap.exists()) {
                    val cond = subjectSnap.getLong("conducted") ?: 0L
                    val att = subjectSnap.getLong("attended") ?: 0L
                    val newCond = (cond - count).coerceAtLeast(0L)
                    val newAtt = if (status == AttendanceStatus.PRESENT) {
                        (att - count).coerceIn(0L, newCond)
                    } else {
                        att.coerceIn(0L, newCond)
                    }
                    batch.update(
                        subjectRef,
                        mapOf(
                            "conducted" to newCond,
                            "attended" to newAtt,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    override suspend fun toggleHoliday(dateIso: String, reason: String): Result<Unit> {
        val docPath = "users/$currentUserId/holidays/$dateIso"
        return try {
            val uid = requireUserId()
            val docRef = holidaysCol().document(dateIso)
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.delete().await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "dateIso" to dateIso,
                        "reason" to reason.trim().take(80).ifBlank { "College Holiday" },
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    override suspend fun updateDefaultRequiredPercent(percent: Double): Result<Unit> {
        val docPath = "users/$currentUserId"
        return try {
            requireUserId()
            val safe = percent.coerceIn(1.0, 100.0)
            userDoc().update(
                mapOf(
                    "defaultRequiredPercent" to safe,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    override suspend fun applySimulatedDelta(
        subjectId: String,
        addAttended: Int,
        addMissed: Int
    ): Result<Unit> {
        val docPath = "users/$currentUserId/subjects/$subjectId"
        return try {
            requireUserId()
            val ref = subjectsCol().document(sanitizeId(subjectId))
            val snap = ref.get().await()
            if (!snap.exists()) return Result.failure(IllegalArgumentException("Subject not found"))
            val cond = snap.getLong("conducted") ?: 0L
            val att = snap.getLong("attended") ?: 0L
            val newCond = (cond + addAttended + addMissed).coerceIn(0L, 10000L)
            val newAtt = (att + addAttended).coerceIn(0L, newCond)
            ref.update(
                mapOf(
                    "conducted" to newCond,
                    "attended" to newAtt,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    override suspend fun resetToDemoData(): Result<Unit> {
        val docPath = "users/$currentUserId"
        return try {
            val uid = requireUserId()
            clearAllUserSubcollections()
            val profileSnap = userDoc().get().await()
            val displayName = profileSnap.getString("displayName") ?: "Student"
            val email = profileSnap.getString("email") ?: ""
            val seed = DemoDataSeed.createSeedData(uid)
            writeFullSnapshot(
                seed.copy(
                    profile = seed.profile.copy(displayName = displayName, email = email)
                )
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    override suspend fun importBackupJson(jsonString: String): Result<Unit> {
        val docPath = "users/$currentUserId"
        return try {
            val uid = requireUserId()
            val decoded = BackupJsonCodec.decode(jsonString, uid)
            clearAllUserSubcollections()
            val profileSnap = userDoc().get().await()
            val displayName = profileSnap.getString("displayName") ?: "Student"
            val email = profileSnap.getString("email") ?: ""
            writeFullSnapshot(
                decoded.copy(
                    profile = decoded.profile.copy(displayName = displayName, email = email)
                )
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    private suspend fun clearAllUserSubcollections() {
        val batch = db.batch()
        subjectsCol().get().await().documents.forEach { batch.delete(it.reference) }
        timetableCol().get().await().documents.forEach { batch.delete(it.reference) }
        attendanceCol().get().await().documents.forEach { batch.delete(it.reference) }
        holidaysCol().get().await().documents.forEach { batch.delete(it.reference) }
        batch.commit().await()
    }

    override fun exportBackupJson(snapshot: AttendXDataSnapshot): String =
        BackupJsonCodec.encode(snapshot)

    private fun sanitizeId(raw: String): String =
        raw.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(120).ifBlank { "id_${UUID.randomUUID().toString().take(8)}" }
}
