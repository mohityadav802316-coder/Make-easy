package com.example.util

import com.example.data.model.GoalStatus
import com.example.data.model.MyControlDayEntity
import com.example.data.model.MyControlStats
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object MyControlStreakCalculator {

  private val DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE

  /**
   * Recalculates stats dynamically from persisted daily records.
   *
   * @param records List of recorded days from database (can be in any order)
   * @param todayDate Local calendar date for today (e.g., "2026-10-23")
   */
  fun calculateStats(
    records: List<MyControlDayEntity>,
    todayDate: String
  ): MyControlStats {
    if (records.isEmpty()) {
      return MyControlStats(
        currentStreak = 0,
        longestStreak = 0,
        totalCheckIns = 0,
        totalCompleted = 0,
        totalGoalNotMet = 0,
        successRatePercent = 0f,
        todayStatus = GoalStatus.PENDING,
        yesterdayStatus = GoalStatus.PENDING,
        isYesterdayPending = true
      )
    }

    val mapByDate = records.associateBy { it.date }

    val todayLocal = try {
      LocalDate.parse(todayDate, DATE_FORMATTER)
    } catch (_: Exception) {
      LocalDate.now()
    }
    val yesterdayLocal = todayLocal.minusDays(1)
    val yesterdayDate = yesterdayLocal.format(DATE_FORMATTER)

    val todayRecord = mapByDate[todayDate]
    val todayStatus = todayRecord?.status ?: GoalStatus.PENDING

    val yesterdayRecord = mapByDate[yesterdayDate]
    val yesterdayStatus = yesterdayRecord?.status ?: GoalStatus.PENDING
    val isYesterdayPending = yesterdayStatus == GoalStatus.PENDING

    // Non-pending check-in totals
    val nonPendingRecords = records.filter { it.status != GoalStatus.PENDING }
    val totalCheckIns = nonPendingRecords.size
    val totalCompleted = nonPendingRecords.count { it.status == GoalStatus.COMPLETED }
    val totalGoalNotMet = nonPendingRecords.count { it.status == GoalStatus.GOAL_NOT_MET }
    val successRate = if (totalCheckIns > 0) {
      (totalCompleted.toFloat() / totalCheckIns.toFloat()) * 100f
    } else 0f

    // Calculate current streak
    // Start scanning from today (if completed) or yesterday (if today is pending)
    var currentStreak = 0
    var scanDate = if (todayStatus == GoalStatus.COMPLETED) todayLocal else yesterdayLocal

    // If scanning starts at yesterday, check if yesterday was actually completed
    var keepScanning = true
    while (keepScanning) {
      val dateStr = scanDate.format(DATE_FORMATTER)
      val record = mapByDate[dateStr]
      if (record != null && record.status == GoalStatus.COMPLETED) {
        currentStreak++
        scanDate = scanDate.minusDays(1)
      } else {
        // Streak ends on GOAL_NOT_MET, PENDING, or missing record
        keepScanning = false
      }
    }

    // Calculate longest streak across all history
    val sortedCompletedDates = records
      .filter { it.status == GoalStatus.COMPLETED }
      .mapNotNull {
        try {
          LocalDate.parse(it.date, DATE_FORMATTER)
        } catch (_: Exception) {
          null
        }
      }
      .distinct()
      .sorted()

    var longestStreak = 0
    var tempStreak = 0
    var prevDate: LocalDate? = null

    for (d in sortedCompletedDates) {
      if (prevDate == null) {
        tempStreak = 1
      } else {
        val daysBetween = ChronoUnit.DAYS.between(prevDate, d)
        if (daysBetween == 1L) {
          tempStreak++
        } else {
          tempStreak = 1
        }
      }
      if (tempStreak > longestStreak) {
        longestStreak = tempStreak
      }
      prevDate = d
    }

    if (currentStreak > longestStreak) {
      longestStreak = currentStreak
    }

    return MyControlStats(
      currentStreak = currentStreak,
      longestStreak = longestStreak,
      totalCheckIns = totalCheckIns,
      totalCompleted = totalCompleted,
      totalGoalNotMet = totalGoalNotMet,
      successRatePercent = successRate,
      todayStatus = todayStatus,
      yesterdayStatus = yesterdayStatus,
      isYesterdayPending = isYesterdayPending
    )
  }

  /**
   * Weekly summary for the last 7 calendar days.
   */
  fun getWeeklySummary(
    records: List<MyControlDayEntity>,
    todayDate: String
  ): List<Pair<String, GoalStatus>> {
    val map = records.associateBy { it.date }
    val today = try {
      LocalDate.parse(todayDate, DATE_FORMATTER)
    } catch (_: Exception) {
      LocalDate.now()
    }

    return (6 downTo 0).map { offset ->
      val date = today.minusDays(offset.toLong()).format(DATE_FORMATTER)
      val status = map[date]?.status ?: GoalStatus.PENDING
      date to status
    }
  }

  /**
   * Monthly summary for the current month calendar days.
   */
  fun getMonthSummary(
    records: List<MyControlDayEntity>,
    yearMonthStr: String // e.g., "2026-10"
  ): Map<String, GoalStatus> {
    return records
      .filter { it.date.startsWith(yearMonthStr) }
      .associate { it.date to it.status }
  }
}
