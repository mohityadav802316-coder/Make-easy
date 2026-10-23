package com.example.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueryBuilder
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Available sections that can be shown, hidden, and reordered on the Home Dashboard.
 */
enum class DashboardSectionId(
  val key: String,
  val titleEn: String,
  val titleHi: String,
  val description: String,
  val defaultEnabled: Boolean
) {
  NEXT_ACTIVITY(
    key = "NEXT_ACTIVITY",
    titleEn = "Next Activity",
    titleHi = "अगली गतिविधि (Next Task)",
    description = "वर्तमान या आगामी कार्य का लाइव कार्ड",
    defaultEnabled = true
  ),
  PRIORITY_TASK(
    key = "PRIORITY_TASK",
    titleEn = "Priority Task",
    titleHi = "प्राथमिकता कार्य (Priority Focus)",
    description = "दिन का सबसे महत्वपूर्ण लंबित कार्य",
    defaultEnabled = true
  ),
  TODAYS_ROUTINE(
    key = "TODAYS_ROUTINE",
    titleEn = "Today's Routine",
    titleHi = "आज की दिनचर्या (Routine Timeline)",
    description = "7-दिवसीय चक्र का संपूर्ण दैनिक शेड्यूल",
    defaultEnabled = true
  ),
  DAILY_PROGRESS(
    key = "DAILY_PROGRESS",
    titleEn = "Daily Progress",
    titleHi = "दैनिक प्रगति (Progress Bar)",
    description = "आज के कार्यों की पूर्ति प्रतिशत",
    defaultEnabled = true
  ),
  STREAK(
    key = "STREAK",
    titleEn = "Streak & Momentum",
    titleHi = "निरंतरता स्ट्रीक (Daily Streak)",
    description = "दिनचर्या पालन का सातत्य एवं स्कोर",
    defaultEnabled = true
  ),
  ENERGY_MODE(
    key = "ENERGY_MODE",
    titleEn = "Energy Mode",
    titleHi = "ऊर्जा स्तर मोड (Energy Mode)",
    description = "कम, सामान्य व उच्च ऊर्जा के अनुसार दिनचर्या समायोजन",
    defaultEnabled = true
  ),
  MEDITATION(
    key = "MEDITATION",
    titleEn = "Meditation Sanctuary",
    titleHi = "ध्यान साधना (Meditation Quick Card)",
    description = "10 मिनट प्राणायाम व ध्यान का सीधा शॉर्टकट",
    defaultEnabled = true
  ),
  NEXT_ALARM(
    key = "NEXT_ALARM",
    titleEn = "Next Wake-Up Alarm",
    titleHi = "अलार्म स्थिति (Wake-Up Alarm)",
    description = "सुबह जागने के अलार्म का समय व स्थिति",
    defaultEnabled = true
  ),
  RECOVERY_DAY(
    key = "RECOVERY_DAY",
    titleEn = "Recovery Day",
    titleHi = "रिकवरी दिवस (Recovery Mode Banner)",
    description = "रूटीन छूटने पर हल्का शेड्यूल बैनर",
    defaultEnabled = true
  ),
  WEEKLY_SUMMARY(
    key = "WEEKLY_SUMMARY",
    titleEn = "Weekly Summary",
    titleHi = "साप्ताहिक सारांश (Weekly Overview)",
    description = "7-दिवसीय चक्र का संक्षेप में प्रदर्शन",
    defaultEnabled = true
  );

  fun getIcon(): ImageVector {
    return when (this) {
      NEXT_ACTIVITY -> Icons.Default.PlayArrow
      PRIORITY_TASK -> Icons.Default.Star
      TODAYS_ROUTINE -> Icons.Default.Timeline
      DAILY_PROGRESS -> Icons.Default.CheckCircle
      STREAK -> Icons.Default.LocalFireDepartment
      ENERGY_MODE -> Icons.Default.Bolt
      MEDITATION -> Icons.Default.SelfImprovement
      NEXT_ALARM -> Icons.Default.Alarm
      RECOVERY_DAY -> Icons.Default.Healing
      WEEKLY_SUMMARY -> Icons.Default.QueryBuilder
    }
  }

  companion object {
    fun fromKey(key: String): DashboardSectionId? {
      return entries.firstOrNull { it.key.equals(key, ignoreCase = true) }
    }

    val defaultOrderedList: List<DashboardSectionId> = listOf(
      NEXT_ACTIVITY,
      PRIORITY_TASK,
      TODAYS_ROUTINE,
      DAILY_PROGRESS,
      STREAK,
      ENERGY_MODE,
      MEDITATION,
      NEXT_ALARM,
      RECOVERY_DAY,
      WEEKLY_SUMMARY
    )
  }
}

/**
 * Dashboard Presets available for quick one-tap setup.
 */
enum class DashboardPreset(
  val key: String,
  val titleEn: String,
  val titleHi: String,
  val description: String,
  val sections: List<DashboardSectionId>
) {
  MINIMAL(
    key = "MINIMAL",
    titleEn = "Minimal",
    titleHi = "न्यूनतम (Minimal)",
    description = "केवल अत्यंत आवश्यक कार्य व स्ट्रीक",
    sections = listOf(
      DashboardSectionId.NEXT_ACTIVITY,
      DashboardSectionId.PRIORITY_TASK,
      DashboardSectionId.STREAK
    )
  ),
  PRODUCTIVITY(
    key = "PRODUCTIVITY",
    titleEn = "Productivity",
    titleHi = "उत्पादकता (Productivity)",
    description = "कार्य, दिनचर्या, दैनिक प्रगति एवं कार्य सूची",
    sections = listOf(
      DashboardSectionId.NEXT_ACTIVITY,
      DashboardSectionId.PRIORITY_TASK,
      DashboardSectionId.TODAYS_ROUTINE,
      DashboardSectionId.DAILY_PROGRESS,
      DashboardSectionId.STREAK
    )
  ),
  ROUTINE(
    key = "ROUTINE",
    titleEn = "Routine",
    titleHi = "दिनचर्या (Routine)",
    description = "पूर्ण दिनचर्या, आगामी कार्य, अलार्म व रिकवरी",
    sections = listOf(
      DashboardSectionId.TODAYS_ROUTINE,
      DashboardSectionId.NEXT_ACTIVITY,
      DashboardSectionId.NEXT_ALARM,
      DashboardSectionId.RECOVERY_DAY,
      DashboardSectionId.DAILY_PROGRESS
    )
  ),
  CUSTOM(
    key = "CUSTOM",
    titleEn = "Custom",
    titleHi = "कस्टम (Custom)",
    description = "आपकी अपनी व्यक्तिगत पसंद",
    sections = DashboardSectionId.defaultOrderedList
  );

  companion object {
    fun fromKey(key: String?): DashboardPreset =
      entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: CUSTOM
  }
}
