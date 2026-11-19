package com.example

import com.example.domain.AttendanceCalculator
import com.example.domain.AttendanceHealthStatus
import com.example.domain.DemoDataSeed
import com.example.domain.Holiday
import com.example.domain.IsoDate
import com.example.domain.Subject
import com.example.domain.TimetableSlot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AttendanceCalculatorTest {

    @Test
    fun `90 percent attendance calculates exact safe bunks and projections`() {
        // 27 attended out of 30 conducted = 90.0%, target 75%
        // floor(27 / 0.75) - 30 = 36 - 30 = 6 safe bunks
        val sub = Subject(id = "s1", name = "VLSI Design", code = "EC302", conducted = 30, attended = 27, requiredPercent = 75.0)
        val m = AttendanceCalculator.calculateBunkMetrics(sub)

        assertEquals(90.0, m.currentPercent, 0.01)
        assertEquals(6, m.safeBunks)
        assertEquals(0, m.classesToRecover)
        assertFalse(m.warnNextMissDropsBelow)
        assertEquals(AttendanceHealthStatus.SAFE, m.healthStatus)
        // Missing 5 classes -> 27 / 35 = 77.14% (still Safe)
        assertTrue(m.missedProjections[4].remainsSafe)
        assertEquals(77.14, m.missedProjections[4].projectedPercent, 0.02)
    }

    @Test
    fun `80 percent attendance calculates exact safe bunks at 75 and 80 thresholds`() {
        // 24 / 30 = 80.0%
        val sub75 = Subject(id = "s2", name = "Electromagnetic Theory", code = "EC304", conducted = 30, attended = 24, requiredPercent = 75.0)
        val m75 = AttendanceCalculator.calculateBunkMetrics(sub75)
        // floor(24 / 0.75) - 30 = 32 - 30 = 2 safe bunks
        assertEquals(80.0, m75.currentPercent, 0.01)
        assertEquals(2, m75.safeBunks)
        assertEquals(0, m75.classesToRecover)
        assertEquals(0, m75.classesToReach80)

        // Same 24/30 with 80% requirement -> 0 safe bunks, next miss drops below 80%
        val sub80 = sub75.copy(requiredPercent = 80.0)
        val m80 = AttendanceCalculator.calculateBunkMetrics(sub80)
        assertEquals(0, m80.safeBunks)
        assertEquals(0, m80.classesToRecover)
        assertTrue(m80.warnNextMissDropsBelow)
    }

    @Test
    fun `exactly 75 percent attendance returns zero safe bunks and warns on next miss`() {
        // 24 / 32 = 75.0%, target 75%
        val sub = Subject(id = "s3", name = "EDC", code = "EC201", conducted = 32, attended = 24, requiredPercent = 75.0)
        val m = AttendanceCalculator.calculateBunkMetrics(sub)

        assertEquals(75.0, m.currentPercent, 0.001)
        assertEquals(0, m.safeBunks)
        assertEquals(0, m.classesToRecover)
        assertTrue(m.warnNextMissDropsBelow)
        assertEquals(AttendanceHealthStatus.WARNING, m.healthStatus)
        assertFalse(m.missedProjections[0].remainsSafe)
        assertEquals(72.73, m.missedProjections[0].projectedPercent, 0.02)
    }

    @Test
    fun `below 75 percent attendance never returns negative safe bunks and computes exact recovery`() {
        // 21 / 30 = 70.0%, target 75%
        // (75*30 - 100*21) / (100 - 75) = (2250 - 2100) / 25 = 6 consecutive classes
        val sub = Subject(id = "s4", name = "VLSI System Design", code = "EC303", conducted = 30, attended = 21, requiredPercent = 75.0)
        val m = AttendanceCalculator.calculateBunkMetrics(sub)

        assertEquals(70.0, m.currentPercent, 0.01)
        assertEquals(0, m.safeBunks)
        assertTrue(m.safeBunks >= 0)
        assertEquals(6, m.classesToRecover)
        // Verify: (21+6)/(30+6) = 27/36 = 75.0%
        assertEquals(75.0, AttendanceCalculator.calculatePercentage(27, 36), 0.001)
        assertEquals(AttendanceHealthStatus.CRITICAL, m.healthStatus)
        assertTrue(m.warnNextMissDropsBelow)
    }

    @Test
    fun `future simulator increments attended and conducted properly without mutating source`() {
        val original = Subject(id = "s5", name = "Digital Electronics", code = "EC301", conducted = 40, attended = 34, requiredPercent = 75.0)
        val simMiss2 = AttendanceCalculator.simulateSubjectChange(original, addAttended = 0, addMissed = 2)
        assertEquals(34L, simMiss2.newAttended)
        assertEquals(42L, simMiss2.newConducted)
        assertEquals(80.95, simMiss2.afterPercent, 0.02)
        // Original unchanged
        assertEquals(40L, original.conducted)

        val simAttend5 = AttendanceCalculator.simulateSubjectChange(original, addAttended = 5, addMissed = 0)
        assertEquals(39L, simAttend5.newAttended)
        assertEquals(45L, simAttend5.newConducted)
        assertEquals(86.67, simAttend5.afterPercent, 0.02)
    }

    @Test
    fun `day impact treats holidays as zero conducted classes`() {
        val date = IsoDate(2026, 11, 18) // Wednesday
        val sub = Subject(id = "s1", name = "VLSI", code = "EC302", conducted = 30, attended = 24, requiredPercent = 75.0)
        val slot = TimetableSlot(id = "t1", subjectId = "s1", dayOfWeek = date.dayOfWeek().toLong(), periodIndex = 1, periodsCount = 2)
        val holiday = Holiday(id = date.toIsoString(), dateIso = date.toIsoString(), reason = "Tech Fest")

        val impact = AttendanceCalculator.calculateDayImpact(date, listOf(slot), listOf(sub), listOf(holiday))
        assertTrue(impact.isHoliday)
        assertEquals(0, impact.totalPeriods)
        assertTrue(impact.safeToMissEntireDay)
    }

    @Test
    fun `demo seed includes all 7 required subjects`() {
        val seed = DemoDataSeed.createSeedData("u1")
        assertEquals(7, seed.subjects.size)
        val names = seed.subjects.map { it.name }.toSet()
        assertTrue(names.contains("Digital Electronics"))
        assertTrue(names.contains("VLSI Design"))
        assertTrue(names.contains("VLSI System Design"))
        assertTrue(names.contains("Electromagnetic Theory"))
        assertTrue(names.contains("Electronic Devices and Circuits"))
        assertTrue(names.contains("Microwave Communication"))
        assertTrue(names.contains("RF Circuits"))
    }
}
