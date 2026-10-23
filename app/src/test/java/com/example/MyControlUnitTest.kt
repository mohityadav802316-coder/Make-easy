package com.example

import com.example.data.model.GoalStatus
import com.example.data.model.MyControlDayEntity
import com.example.data.model.MyControlEventType
import com.example.util.MyControlStreakCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MyControlUnitTest {

  private val today = "2026-04-15"

  @Test
  fun testStreakCalculation_ConsecutiveCompletedDays() {
    val days = listOf(
      MyControlDayEntity(date = "2026-04-14", status = GoalStatus.COMPLETED),
      MyControlDayEntity(date = "2026-04-13", status = GoalStatus.COMPLETED),
      MyControlDayEntity(date = "2026-04-12", status = GoalStatus.COMPLETED)
    )

    val stats = MyControlStreakCalculator.calculateStats(days, today)

    // Today is untouched (PENDING), so current active streak is 3 (from yesterday backwards)
    assertEquals(3, stats.currentStreak)
    assertEquals(3, stats.longestStreak)
    assertEquals(3, stats.totalCheckIns)
    assertEquals(GoalStatus.PENDING, stats.todayStatus)
  }

  @Test
  fun testStreakCalculation_TodayCompletedIncreasesStreak() {
    val days = listOf(
      MyControlDayEntity(date = "2026-04-15", status = GoalStatus.COMPLETED),
      MyControlDayEntity(date = "2026-04-14", status = GoalStatus.COMPLETED),
      MyControlDayEntity(date = "2026-04-13", status = GoalStatus.COMPLETED)
    )

    val stats = MyControlStreakCalculator.calculateStats(days, today)

    assertEquals(3, stats.currentStreak)
    assertEquals(3, stats.longestStreak)
    assertEquals(3, stats.totalCheckIns)
    assertEquals(GoalStatus.COMPLETED, stats.todayStatus)
  }

  @Test
  fun testStreakCalculation_GoalNotMetBreaksActiveStreak() {
    val days = listOf(
      MyControlDayEntity(date = "2026-04-15", status = GoalStatus.PENDING),
      MyControlDayEntity(date = "2026-04-14", status = GoalStatus.GOAL_NOT_MET),
      MyControlDayEntity(date = "2026-04-13", status = GoalStatus.COMPLETED),
      MyControlDayEntity(date = "2026-04-12", status = GoalStatus.COMPLETED),
      MyControlDayEntity(date = "2026-04-11", status = GoalStatus.COMPLETED)
    )

    val stats = MyControlStreakCalculator.calculateStats(days, today)

    // Yesterday was GOAL_NOT_MET, so active streak is 0
    assertEquals(0, stats.currentStreak)
    // Longest streak prior to the break is 3
    assertEquals(3, stats.longestStreak)
    assertEquals(4, stats.totalCheckIns)
    assertEquals(GoalStatus.PENDING, stats.todayStatus)
  }

  @Test
  fun testStreakCalculation_PastPendingDayBreaksStreak() {
    val days = listOf(
      MyControlDayEntity(date = "2026-04-14", status = GoalStatus.COMPLETED),
      // 2026-04-13 is MISSING / PENDING (gap)
      MyControlDayEntity(date = "2026-04-12", status = GoalStatus.COMPLETED)
    )

    val stats = MyControlStreakCalculator.calculateStats(days, today)

    // Only yesterday is connected to today
    assertEquals(1, stats.currentStreak)
    assertEquals(1, stats.longestStreak)
    assertEquals(2, stats.totalCheckIns)
  }

  @Test
  fun testStreakCalculation_CorrectionUpdatesStatsAccurately() {
    // Suppose yesterday was recorded as GOAL_NOT_MET, then corrected to COMPLETED
    val correctedDays = listOf(
      MyControlDayEntity(date = "2026-04-14", status = GoalStatus.COMPLETED, note = "Corrected entry"),
      MyControlDayEntity(date = "2026-04-13", status = GoalStatus.COMPLETED),
      MyControlDayEntity(date = "2026-04-12", status = GoalStatus.COMPLETED)
    )

    val stats = MyControlStreakCalculator.calculateStats(correctedDays, today)

    assertEquals(3, stats.currentStreak)
    assertEquals(3, stats.longestStreak)
  }

  @Test
  fun testNeutralCopyAndNonShameLanguage() {
    // Verify that GoalStatus descriptions are respectful and non-punitive
    val notMetStatus = GoalStatus.GOAL_NOT_MET
    assertFalse("Must not use shame words like failure", notMetStatus.labelEn.contains("Failed", ignoreCase = true))
    assertFalse("Must not use sin/guilt words", notMetStatus.labelHi.contains("पाप", ignoreCase = true))
    assertEquals("Goal Not Met", notMetStatus.labelEn)

    // Verify Event Types
    val correctionEvent = MyControlEventType.STATUS_CORRECTION
    assertEquals("Status Correction", correctionEvent.titleEn)
  }
}
