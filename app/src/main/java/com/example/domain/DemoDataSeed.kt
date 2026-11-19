package com.example.domain

/**
 * Generates realistic sample subjects, Mon-Sat timetable, recent attendance history,
 * and a sample holiday so the dashboard, bunk calculator, calendar, and analytics
 * can be tested immediately with 90%, 80%, exactly 75%, and <75% scenarios.
 */
object DemoDataSeed {

    fun createSeedData(userId: String, today: IsoDate = IsoDate.today()): AttendXDataSnapshot {
        val subjects = listOf(
            Subject(
                id = "sub_de",
                userId = userId,
                name = "Digital Electronics",
                code = "EC301",
                conducted = 40L,
                attended = 34L, // 85.0% -> Safe (5 safe bunks)
                requiredPercent = 75.0,
                colorIndex = 0L
            ),
            Subject(
                id = "sub_vlsi",
                userId = userId,
                name = "VLSI Design",
                code = "EC302",
                conducted = 30L,
                attended = 27L, // 90.0% -> Safe (6 safe bunks)
                requiredPercent = 75.0,
                colorIndex = 1L
            ),
            Subject(
                id = "sub_vlsi_sys",
                userId = userId,
                name = "VLSI System Design",
                code = "EC303",
                conducted = 30L,
                attended = 21L, // 70.0% -> Critical (needs 6 consecutive to recover to 75%)
                requiredPercent = 75.0,
                colorIndex = 2L
            ),
            Subject(
                id = "sub_emt",
                userId = userId,
                name = "Electromagnetic Theory",
                code = "EC304",
                conducted = 30L,
                attended = 24L, // 80.0% -> Safe (2 safe bunks at 75%)
                requiredPercent = 75.0,
                colorIndex = 3L
            ),
            Subject(
                id = "sub_edc",
                userId = userId,
                name = "Electronic Devices and Circuits",
                code = "EC201",
                conducted = 32L,
                attended = 24L, // 75.0% -> Exactly 75% Warning (0 safe bunks, next miss drops below!)
                requiredPercent = 75.0,
                colorIndex = 4L
            ),
            Subject(
                id = "sub_mw",
                userId = userId,
                name = "Microwave Communication",
                code = "EC401",
                conducted = 34L,
                attended = 26L, // 76.47% -> Warning (0 safe bunks, next miss drops to 74.29%)
                requiredPercent = 75.0,
                colorIndex = 5L
            ),
            Subject(
                id = "sub_rf",
                userId = userId,
                name = "RF Circuits",
                code = "EC402",
                conducted = 25L,
                attended = 17L, // 68.0% -> Critical (below 75%, needs 7 consecutive)
                requiredPercent = 75.0,
                colorIndex = 6L
            )
        )

        // Monday (1) to Saturday (6)
        val timetable = listOf(
            // Monday
            TimetableSlot("slot_1_1", userId, "sub_de", 1L, 1L, "09:00 - 09:50", 1L),
            TimetableSlot("slot_1_2", userId, "sub_vlsi", 1L, 2L, "10:00 - 10:50", 1L),
            TimetableSlot("slot_1_3", userId, "sub_emt", 1L, 3L, "11:10 - 12:00", 1L),
            TimetableSlot("slot_1_4", userId, "sub_edc", 1L, 4L, "14:00 - 15:40", 2L),
            // Tuesday
            TimetableSlot("slot_2_1", userId, "sub_vlsi_sys", 2L, 1L, "09:00 - 09:50", 1L),
            TimetableSlot("slot_2_2", userId, "sub_mw", 2L, 2L, "10:00 - 10:50", 1L),
            TimetableSlot("slot_2_3", userId, "sub_rf", 2L, 3L, "11:10 - 12:00", 1L),
            TimetableSlot("slot_2_4", userId, "sub_de", 2L, 4L, "13:30 - 14:20", 1L),
            // Wednesday
            TimetableSlot("slot_3_1", userId, "sub_edc", 3L, 1L, "09:00 - 09:50", 1L),
            TimetableSlot("slot_3_2", userId, "sub_vlsi", 3L, 2L, "10:00 - 11:40", 2L),
            TimetableSlot("slot_3_3", userId, "sub_vlsi_sys", 3L, 3L, "13:30 - 14:20", 1L),
            // Thursday
            TimetableSlot("slot_4_1", userId, "sub_emt", 4L, 1L, "09:00 - 09:50", 1L),
            TimetableSlot("slot_4_2", userId, "sub_mw", 4L, 2L, "10:00 - 10:50", 1L),
            TimetableSlot("slot_4_3", userId, "sub_rf", 4L, 3L, "11:10 - 12:50", 2L),
            // Friday
            TimetableSlot("slot_5_1", userId, "sub_de", 5L, 1L, "09:00 - 09:50", 1L),
            TimetableSlot("slot_5_2", userId, "sub_vlsi_sys", 5L, 2L, "10:00 - 10:50", 1L),
            TimetableSlot("slot_5_3", userId, "sub_edc", 5L, 3L, "11:10 - 12:00", 1L),
            TimetableSlot("slot_5_4", userId, "sub_mw", 5L, 4L, "14:00 - 14:50", 1L),
            // Saturday
            TimetableSlot("slot_6_1", userId, "sub_vlsi", 6L, 1L, "09:30 - 10:20", 1L),
            TimetableSlot("slot_6_2", userId, "sub_rf", 6L, 2L, "10:30 - 11:20", 1L),
            TimetableSlot("slot_6_3", userId, "sub_emt", 6L, 3L, "11:30 - 12:20", 1L)
        )

        // Generate 21 days of realistic attendance history ending yesterday
        val records = mutableListOf<AttendanceRecord>()
        val holidayDate = today.plusDays(-5)
        val holidays = listOf(
            Holiday(
                id = holidayDate.toIsoString(),
                userId = userId,
                dateIso = holidayDate.toIsoString(),
                reason = "Annual Tech Symposium"
            )
        )

        for (offset in 21 downTo 1) {
            val d = today.plusDays(-offset)
            val dow = d.dayOfWeek()
            if (dow == 7 || d == holidayDate) continue
            val daySlots = timetable.filter { it.dayOfWeek.toInt() == dow }
            daySlots.forEachIndexed { idx, slot ->
                val status = when {
                    (offset + idx) % 11 == 0 -> AttendanceStatus.CANCELLED
                    slot.subjectId in listOf("sub_vlsi_sys", "sub_rf") && (offset + idx) % 3 == 0 -> AttendanceStatus.ABSENT
                    (offset + idx) % 5 == 0 -> AttendanceStatus.ABSENT
                    else -> AttendanceStatus.PRESENT
                }
                val dateStr = d.toIsoString()
                records.add(
                    AttendanceRecord(
                        id = AttendanceRecord.buildId(dateStr, slot.subjectId, slot.periodIndex),
                        userId = userId,
                        subjectId = slot.subjectId,
                        dateIso = dateStr,
                        periodIndex = slot.periodIndex,
                        periodsCount = slot.periodsCount,
                        status = status.name,
                        note = if (status == AttendanceStatus.CANCELLED) "Faculty away" else ""
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
                defaultRequiredPercent = 75.0,
                seededDemoData = true
            ),
            subjects = subjects,
            timetableSlots = timetable,
            attendanceRecords = records,
            holidays = holidays
        )
    }
}
