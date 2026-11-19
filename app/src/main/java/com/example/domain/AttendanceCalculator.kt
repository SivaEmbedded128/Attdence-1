package com.example.domain

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.util.Locale
import kotlin.math.max

data class MissedProjection(
    val classesMissed: Int,
    val projectedConducted: Long,
    val projectedAttended: Long,
    val projectedPercent: Double,
    val remainsSafe: Boolean
)

data class BunkMetrics(
    val subjectId: String,
    val subjectName: String,
    val subjectCode: String,
    val attended: Long,
    val conducted: Long,
    val absent: Long,
    val requiredPercent: Double,
    val currentPercent: Double,
    val safeBunks: Int,
    val classesToRecover: Int,
    val classesToReach80: Int,
    val warnNextMissDropsBelow: Boolean,
    val healthStatus: AttendanceHealthStatus,
    val missedProjections: List<MissedProjection>,
    val formulaExplanation: String
)

data class SimulationResult(
    val subject: Subject,
    val addAttended: Int,
    val addMissed: Int,
    val beforePercent: Double,
    val afterPercent: Double,
    val newAttended: Long,
    val newConducted: Long,
    val newSafeBunks: Int,
    val newClassesToRecover: Int,
    val newStatus: AttendanceHealthStatus
)

data class DashboardSummary(
    val overallPercent: Double,
    val totalConducted: Long,
    val totalAttended: Long,
    val totalAbsent: Long,
    val subjectCount: Int,
    val totalSafeBunksAcrossSubjects: Int,
    val subjectsBelowRequired: Int,
    val overallStatus: AttendanceHealthStatus,
    val averageRequiredPercent: Double
)

data class SubjectDayImpact(
    val subject: Subject,
    val periodsMissed: Int,
    val beforePercent: Double,
    val afterPercent: Double,
    val dropsBelowRequired: Boolean
)

data class DayImpactResult(
    val date: IsoDate,
    val isHoliday: Boolean,
    val holidayReason: String?,
    val totalPeriods: Int,
    val overallBeforePercent: Double,
    val overallAfterPercent: Double,
    val subjectImpacts: List<SubjectDayImpact>,
    val safeToMissEntireDay: Boolean
)

data class SmartInsights(
    val lowestSubject: BunkMetrics?,
    val highestSubject: BunkMetrics?,
    val attentionSubjects: List<BunkMetrics>,
    val totalSafeBunks: Int,
    val classesToReach80Overall: Int,
    val tomorrowImpact: DayImpactResult,
    val weekMissOverallBefore: Double,
    val weekMissOverallAfter: Double,
    val weekMissPeriods: Int,
    val weekSubjectsDroppingBelow: List<String>,
    val recommendations: List<String>
)

/**
 * Pure, deterministic mathematical engine for AttendX.
 * Uses BigDecimal exact arithmetic to prevent IEEE-754 floating-point boundary bugs
 * (e.g., 30 / 0.75 returning 39.99999999999999 instead of 40.0).
 */
object AttendanceCalculator {

    private val MC = MathContext(16, RoundingMode.HALF_UP)
    private val HUNDRED = BigDecimal(100)

    fun calculatePercentage(attended: Long, conducted: Long): Double {
        if (conducted <= 0L) return 0.0
        val pct = (attended.toDouble() / conducted.toDouble()) * 100.0
        return pct.coerceIn(0.0, 100.0)
    }

    fun formatPercent(percent: Double): String =
        String.format(Locale.US, "%.2f%%", percent)

    fun formatPercentShort(percent: Double): String =
        String.format(Locale.US, "%.1f%%", percent)

    /**
     * Exact Maximum Safe Bunks:
     * floor((attended / (requiredPercent / 100)) - conducted)
     * Clamped to >= 0 (never returns negative bunks).
     */
    fun calculateSafeBunks(attended: Long, conducted: Long, requiredPercent: Double): Int {
        if (conducted <= 0L || attended <= 0L || requiredPercent <= 0.0) return 0
        val req = BigDecimal.valueOf(requiredPercent)
        val att = BigDecimal.valueOf(attended)
        val cond = BigDecimal.valueOf(conducted)
        // maxConductedAllowed = floor(attended * 100 / requiredPercent)
        val maxConductedAllowed = att.multiply(HUNDRED).divide(req, 0, RoundingMode.FLOOR)
        val diff = maxConductedAllowed.subtract(cond).toInt()
        return max(0, diff)
    }

    /**
     * Minimum consecutive classes required to attend to reach >= targetPercent:
     * Solve: (attended + x) / (conducted + x) >= targetPercent / 100
     * => (100 - targetPercent) * x >= targetPercent * conducted - 100 * attended
     * => x = ceil((targetPercent * conducted - 100 * attended) / (100 - targetPercent))
     */
    fun calculateConsecutiveClassesNeeded(
        attended: Long,
        conducted: Long,
        targetPercent: Double
    ): Int {
        if (targetPercent <= 0.0) return 0
        if (conducted == 0L) return 1
        val current = calculatePercentage(attended, conducted)
        if (current + 1e-9 >= targetPercent) {
            // Double check exact rational inequality to avoid rounding artifacts
            val req = BigDecimal.valueOf(targetPercent)
            val lhs = BigDecimal.valueOf(attended).multiply(HUNDRED)
            val rhs = req.multiply(BigDecimal.valueOf(conducted))
            if (lhs >= rhs) return 0
        }
        if (targetPercent >= 100.0) {
            // Only possible if attended == conducted
            return if (attended == conducted) 0 else Int.MAX_VALUE
        }
        val req = BigDecimal.valueOf(targetPercent)
        val numerator = req.multiply(BigDecimal.valueOf(conducted))
            .subtract(BigDecimal.valueOf(attended).multiply(HUNDRED))
        if (numerator <= BigDecimal.ZERO) return 0
        val denominator = HUNDRED.subtract(req)
        return numerator.divide(denominator, 0, RoundingMode.CEILING).toInt().coerceAtLeast(0)
    }

    fun evaluateHealthStatus(
        currentPercent: Double,
        requiredPercent: Double,
        conducted: Long
    ): AttendanceHealthStatus {
        if (conducted <= 0L) return AttendanceHealthStatus.SAFE
        return when {
            currentPercent + 1e-9 < requiredPercent -> AttendanceHealthStatus.CRITICAL
            currentPercent < requiredPercent + 5.0 -> AttendanceHealthStatus.WARNING
            else -> AttendanceHealthStatus.SAFE
        }
    }

    fun calculateBunkMetrics(subject: Subject): BunkMetrics {
        val currentPct = calculatePercentage(subject.attended, subject.conducted)
        val safeBunks = calculateSafeBunks(subject.attended, subject.conducted, subject.requiredPercent)
        val toRecover = calculateConsecutiveClassesNeeded(
            subject.attended,
            subject.conducted,
            subject.requiredPercent
        )
        val toReach80 = calculateConsecutiveClassesNeeded(subject.attended, subject.conducted, 80.0)
        val status = evaluateHealthStatus(currentPct, subject.requiredPercent, subject.conducted)

        val projections = (1..5).map { miss ->
            val newConducted = subject.conducted + miss
            val newPct = calculatePercentage(subject.attended, newConducted)
            MissedProjection(
                classesMissed = miss,
                projectedConducted = newConducted,
                projectedAttended = subject.attended,
                projectedPercent = newPct,
                remainsSafe = newPct + 1e-9 >= subject.requiredPercent
            )
        }

        val nextMissPct = projections.first().projectedPercent
        val warnNextMiss = subject.conducted > 0L && (
            currentPct + 1e-9 < subject.requiredPercent ||
                nextMissPct + 1e-9 < subject.requiredPercent
            )

        val reqStr = if (subject.requiredPercent % 1.0 == 0.0) {
            subject.requiredPercent.toInt().toString()
        } else {
            String.format(Locale.US, "%.1f", subject.requiredPercent)
        }

        val explanation = buildString {
            append("Current = (${subject.attended} / ${subject.conducted}) × 100 = ${formatPercent(currentPct)}. ")
            if (subject.conducted == 0L) {
                append("No classes conducted yet.")
            } else if (toRecover > 0) {
                append("Below $reqStr% threshold. Attend $toRecover consecutive class")
                if (toRecover > 1) append("es")
                append(" to reach ≥$reqStr% (${subject.attended + toRecover}/${subject.conducted + toRecover}).")
            } else {
                append("Max Safe Bunks = floor((${subject.attended} / ${reqStr}%) - ${subject.conducted}) = $safeBunks.")
            }
        }

        return BunkMetrics(
            subjectId = subject.id,
            subjectName = subject.name,
            subjectCode = subject.code,
            attended = subject.attended,
            conducted = subject.conducted,
            absent = subject.absent,
            requiredPercent = subject.requiredPercent,
            currentPercent = currentPct,
            safeBunks = safeBunks,
            classesToRecover = toRecover,
            classesToReach80 = toReach80,
            warnNextMissDropsBelow = warnNextMiss,
            healthStatus = status,
            missedProjections = projections,
            formulaExplanation = explanation
        )
    }

    fun simulateSubjectChange(
        subject: Subject,
        addAttended: Int,
        addMissed: Int
    ): SimulationResult {
        val safeAttend = max(0, addAttended)
        val safeMiss = max(0, addMissed)
        val newAttended = subject.attended + safeAttend
        val newConducted = subject.conducted + safeAttend + safeMiss
        val beforePct = calculatePercentage(subject.attended, subject.conducted)
        val afterPct = calculatePercentage(newAttended, newConducted)
        val newSafeBunks = calculateSafeBunks(newAttended, newConducted, subject.requiredPercent)
        val newRecover = calculateConsecutiveClassesNeeded(
            newAttended,
            newConducted,
            subject.requiredPercent
        )
        val newStatus = evaluateHealthStatus(afterPct, subject.requiredPercent, newConducted)
        return SimulationResult(
            subject = subject,
            addAttended = safeAttend,
            addMissed = safeMiss,
            beforePercent = beforePct,
            afterPercent = afterPct,
            newAttended = newAttended,
            newConducted = newConducted,
            newSafeBunks = newSafeBunks,
            newClassesToRecover = newRecover,
            newStatus = newStatus
        )
    }

    fun calculateDashboardSummary(
        subjects: List<Subject>,
        defaultRequiredPercent: Double = 75.0
    ): DashboardSummary {
        val totalConducted = subjects.sumOf { it.conducted }
        val totalAttended = subjects.sumOf { it.attended }
        val totalAbsent = (totalConducted - totalAttended).coerceAtLeast(0L)
        val overallPct = calculatePercentage(totalAttended, totalConducted)
        val metrics = subjects.map { calculateBunkMetrics(it) }
        val totalSafeBunks = metrics.sumOf { it.safeBunks }
        val belowRequired = metrics.count { it.conducted > 0L && it.currentPercent + 1e-9 < it.requiredPercent }
        val avgReq = if (subjects.isEmpty()) defaultRequiredPercent else subjects.map { it.requiredPercent }.average()
        val overallStatus = if (subjects.isEmpty()) {
            AttendanceHealthStatus.SAFE
        } else {
            evaluateHealthStatus(overallPct, defaultRequiredPercent, totalConducted)
        }
        return DashboardSummary(
            overallPercent = overallPct,
            totalConducted = totalConducted,
            totalAttended = totalAttended,
            totalAbsent = totalAbsent,
            subjectCount = subjects.size,
            totalSafeBunksAcrossSubjects = totalSafeBunks,
            subjectsBelowRequired = belowRequired,
            overallStatus = overallStatus,
            averageRequiredPercent = avgReq
        )
    }

    fun calculateDayImpact(
        date: IsoDate,
        slots: List<TimetableSlot>,
        subjects: List<Subject>,
        holidays: List<Holiday>
    ): DayImpactResult {
        val dateIso = date.toIsoString()
        val holiday = holidays.firstOrNull { it.dateIso == dateIso }
        val totalAttended = subjects.sumOf { it.attended }
        val totalConducted = subjects.sumOf { it.conducted }
        val overallBefore = calculatePercentage(totalAttended, totalConducted)

        if (holiday != null || date.dayOfWeek() == 7) {
            return DayImpactResult(
                date = date,
                isHoliday = holiday != null || date.dayOfWeek() == 7,
                holidayReason = holiday?.reason ?: if (date.dayOfWeek() == 7) "Sunday" else null,
                totalPeriods = 0,
                overallBeforePercent = overallBefore,
                overallAfterPercent = overallBefore,
                subjectImpacts = emptyList(),
                safeToMissEntireDay = true
            )
        }

        val daySlots = slots.filter { it.dayOfWeek.toInt() == date.dayOfWeek() }
        val periodsBySubject = daySlots.groupBy { it.subjectId }
            .mapValues { (_, list) -> list.sumOf { it.periodsCount.toInt() } }

        val subjectMap = subjects.associateBy { it.id }
        val impacts = periodsBySubject.mapNotNull { (subjectId, periods) ->
            val sub = subjectMap[subjectId] ?: return@mapNotNull null
            val before = calculatePercentage(sub.attended, sub.conducted)
            val after = calculatePercentage(sub.attended, sub.conducted + periods)
            SubjectDayImpact(
                subject = sub,
                periodsMissed = periods,
                beforePercent = before,
                afterPercent = after,
                dropsBelowRequired = after + 1e-9 < sub.requiredPercent
            )
        }.sortedBy { it.afterPercent }

        val totalPeriods = impacts.sumOf { it.periodsMissed }
        val overallAfter = calculatePercentage(totalAttended, totalConducted + totalPeriods)
        val safeAll = impacts.none { it.dropsBelowRequired }

        return DayImpactResult(
            date = date,
            isHoliday = false,
            holidayReason = null,
            totalPeriods = totalPeriods,
            overallBeforePercent = overallBefore,
            overallAfterPercent = overallAfter,
            subjectImpacts = impacts,
            safeToMissEntireDay = safeAll
        )
    }

    fun generateSmartInsights(
        subjects: List<Subject>,
        slots: List<TimetableSlot>,
        holidays: List<Holiday>,
        today: IsoDate = IsoDate.today()
    ): SmartInsights {
        val metrics = subjects.map { calculateBunkMetrics(it) }
        val activeMetrics = metrics.filter { it.conducted > 0L }
        val lowest = activeMetrics.minByOrNull { it.currentPercent }
        val highest = activeMetrics.maxByOrNull { it.currentPercent }
        val attention = metrics.filter {
            it.healthStatus == AttendanceHealthStatus.CRITICAL || it.warnNextMissDropsBelow
        }.sortedBy { it.currentPercent }

        val totalSafeBunks = metrics.sumOf { it.safeBunks }
        val totalAttended = subjects.sumOf { it.attended }
        val totalConducted = subjects.sumOf { it.conducted }
        val classesTo80Overall = calculateConsecutiveClassesNeeded(totalAttended, totalConducted, 80.0)

        val tomorrow = today.plusDays(1)
        val tomorrowImpact = calculateDayImpact(tomorrow, slots, subjects, holidays)

        // Full week miss impact (Mon..Sat)
        val weekPeriodsBySubject = slots.groupBy { it.subjectId }
            .mapValues { (_, list) -> list.sumOf { it.periodsCount.toInt() } }
        val weekPeriods = weekPeriodsBySubject.values.sum()
        val weekBefore = calculatePercentage(totalAttended, totalConducted)
        val weekAfter = calculatePercentage(totalAttended, totalConducted + weekPeriods)
        val subjectsDroppingInWeek = subjects.mapNotNull { sub ->
            val missCount = weekPeriodsBySubject[sub.id] ?: 0
            if (missCount == 0) return@mapNotNull null
            val after = calculatePercentage(sub.attended, sub.conducted + missCount)
            if (after + 1e-9 < sub.requiredPercent) {
                "${sub.name} (${formatPercentShort(after)})"
            } else null
        }

        // Two concrete recommendations based on actual math
        val recommendations = mutableListOf<String>()
        val critical = metrics.filter { it.classesToRecover > 0 }.sortedBy { it.currentPercent }
        val borderline = metrics.filter { it.classesToRecover == 0 && it.warnNextMissDropsBelow }
            .sortedBy { it.currentPercent }

        if (critical.isNotEmpty()) {
            val top = critical.first()
            recommendations.add(
                "Priority Recovery — ${top.subjectName} (${formatPercentShort(top.currentPercent)}): Attend the next ${top.classesToRecover} consecutive class${if (top.classesToRecover > 1) "es" else ""} without missing to reach ${top.requiredPercent.toInt()}%."
            )
        }
        if (critical.size > 1) {
            val second = critical[1]
            recommendations.add(
                "Second Risk — ${second.subjectName} (${formatPercentShort(second.currentPercent)}): Needs ${second.classesToRecover} consecutive present class${if (second.classesToRecover > 1) "es" else ""} to clear the ${second.requiredPercent.toInt()}% threshold."
            )
        } else if (borderline.isNotEmpty()) {
            val border = borderline.first()
            val afterOne = border.missedProjections.first().projectedPercent
            recommendations.add(
                "Zero Margin — ${border.subjectName} (${formatPercentShort(border.currentPercent)}): Do not miss the next class; 1 absence drops attendance to ${formatPercentShort(afterOne)}, below ${border.requiredPercent.toInt()}%."
            )
        }

        if (recommendations.size < 2 && highest != null && highest.safeBunks > 0) {
            recommendations.add(
                "Safe Buffer — ${highest.subjectName} (${formatPercentShort(highest.currentPercent)}): You have ${highest.safeBunks} safe bunk${if (highest.safeBunks > 1) "s" else ""} available while staying above ${highest.requiredPercent.toInt()}%."
            )
        }
        if (recommendations.isEmpty()) {
            recommendations.add("All subjects are comfortably above their targets. Keep attending regularly to build extra margin.")
        }

        return SmartInsights(
            lowestSubject = lowest,
            highestSubject = highest,
            attentionSubjects = attention,
            totalSafeBunks = totalSafeBunks,
            classesToReach80Overall = classesTo80Overall,
            tomorrowImpact = tomorrowImpact,
            weekMissOverallBefore = weekBefore,
            weekMissOverallAfter = weekAfter,
            weekMissPeriods = weekPeriods,
            weekSubjectsDroppingBelow = subjectsDroppingInWeek,
            recommendations = recommendations.take(2)
        )
    }
}
