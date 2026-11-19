package com.example.domain

import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Locale

/**
 * API-24-safe lightweight ISO-8601 calendar date utility (yyyy-MM-dd)
 * with Monday=1 .. Sunday=7 day-of-week indexing.
 */
data class IsoDate(
    val year: Int,
    val month: Int, // 1..12
    val day: Int    // 1..31
) : Comparable<IsoDate> {

    override fun compareTo(other: IsoDate): Int {
        if (year != other.year) return year.compareTo(other.year)
        if (month != other.month) return month.compareTo(other.month)
        return day.compareTo(other.day)
    }

    fun toIsoString(): String = String.format(Locale.US, "%04d-%02d-%02d", year, month, day)

    /** Monday = 1, Tuesday = 2, ..., Saturday = 6, Sunday = 7 */
    fun dayOfWeek(): Int {
        val cal = GregorianCalendar(year, month - 1, day)
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            else -> 7
        }
    }

    fun dayOfWeekShort(): String = when (dayOfWeek()) {
        1 -> "Mon"
        2 -> "Tue"
        3 -> "Wed"
        4 -> "Thu"
        5 -> "Fri"
        6 -> "Sat"
        else -> "Sun"
    }

    fun dayOfWeekFull(): String = when (dayOfWeek()) {
        1 -> "Monday"
        2 -> "Tuesday"
        3 -> "Wednesday"
        4 -> "Thursday"
        5 -> "Friday"
        6 -> "Saturday"
        else -> "Sunday"
    }

    fun monthName(): String = when (month) {
        1 -> "January"
        2 -> "February"
        3 -> "March"
        4 -> "April"
        5 -> "May"
        6 -> "June"
        7 -> "July"
        8 -> "August"
        9 -> "September"
        10 -> "October"
        11 -> "November"
        else -> "December"
    }

    fun monthShort(): String = monthName().take(3)

    fun plusDays(days: Int): IsoDate {
        val cal = GregorianCalendar(year, month - 1, day)
        cal.add(Calendar.DAY_OF_MONTH, days)
        return IsoDate(
            year = cal.get(Calendar.YEAR),
            month = cal.get(Calendar.MONTH) + 1,
            day = cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun plusMonths(months: Int): IsoDate {
        val cal = GregorianCalendar(year, month - 1, 1)
        cal.add(Calendar.MONTH, months)
        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        return IsoDate(
            year = cal.get(Calendar.YEAR),
            month = cal.get(Calendar.MONTH) + 1,
            day = day.coerceAtMost(maxDay)
        )
    }

    fun daysInMonth(): Int {
        val cal = GregorianCalendar(year, month - 1, 1)
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    /** ISO-style week key e.g. "2026-W47" for weekly analytics grouping */
    fun weekKey(): String {
        val cal = GregorianCalendar(year, month - 1, day).apply {
            firstDayOfWeek = Calendar.MONDAY
            minimalDaysInFirstWeek = 4
        }
        val week = cal.get(Calendar.WEEK_OF_YEAR)
        val weekYear = if (month == 1 && week >= 52) year - 1
        else if (month == 12 && week == 1) year + 1
        else year
        return String.format(Locale.US, "%04d-W%02d", weekYear, week)
    }

    fun monthKey(): String = String.format(Locale.US, "%04d-%02d", year, month)

    companion object {
        fun today(): IsoDate {
            val cal = Calendar.getInstance()
            return IsoDate(
                year = cal.get(Calendar.YEAR),
                month = cal.get(Calendar.MONTH) + 1,
                day = cal.get(Calendar.DAY_OF_MONTH)
            )
        }

        fun parse(iso: String): IsoDate? {
            val parts = iso.trim().split("-")
            if (parts.size != 3) return null
            val y = parts[0].toIntOrNull() ?: return null
            val m = parts[1].toIntOrNull() ?: return null
            val d = parts[2].toIntOrNull() ?: return null
            if (m !in 1..12 || d !in 1..31) return null
            return IsoDate(y, m, d)
        }
    }
}
