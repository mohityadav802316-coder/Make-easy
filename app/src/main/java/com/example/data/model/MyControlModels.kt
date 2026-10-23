package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Status of the daily self-control goal.
 * Values are strictly neutral and objective.
 */
enum class GoalStatus(val labelEn: String, val labelHi: String) {
  PENDING("Pending", "प्रतीक्षारत"),
  COMPLETED("Completed", "सफल"),
  GOAL_NOT_MET("Goal Not Met", "लक्ष्य अधूरा");

  companion object {
    fun fromString(value: String?): GoalStatus =
      entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: PENDING
  }
}

/**
 * Audit log event types for append-only timestamped event history.
 */
enum class MyControlEventType(val titleEn: String, val titleHi: String) {
  DAILY_CHECKIN("Daily Check-in", "दैनिक चेक-इन"),
  STATUS_CORRECTION("Status Correction", "स्थिति सुधार"),
  URGE_LOG("Urge Management Log", "इच्छा नियंत्रण रिकॉर्ड"),
  NOTE_ENTRY("Reflection Note", "निजी विचार")
}

/**
 * Daily Goal Status Record (one logical entry per calendar date).
 */
@Entity(
  tableName = "my_control_days",
  indices = [
    Index(value = ["date"], unique = true),
    Index(value = ["status"])
  ]
)
data class MyControlDayEntity(
  @PrimaryKey val date: String, // Format: YYYY-MM-DD
  val status: GoalStatus,
  val note: String? = null,
  val updatedAtEpochMs: Long = System.currentTimeMillis(),
  val isRetrospective: Boolean = false,
  val timezoneId: String = java.util.TimeZone.getDefault().id
)

/**
 * Append-only immutable event history record.
 */
@Entity(
  tableName = "my_control_events",
  indices = [
    Index(value = ["date"]),
    Index(value = ["timestampEpochMs"]),
    Index(value = ["eventType"])
  ]
)
data class MyControlEventEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val eventType: MyControlEventType,
  val date: String, // Target calendar date (YYYY-MM-DD)
  val timestampEpochMs: Long = System.currentTimeMillis(),
  val status: GoalStatus? = null,
  val previousStatus: GoalStatus? = null,
  val note: String? = null,
  val urgeIntensity: Int? = null, // 1 (mild) to 5 (extreme)
  val urgeTrigger: String? = null, // e.g., "तनाव", "अकेलापन", "देर रात", "फोन"
  val urgeStrategy: String? = null, // e.g., "गहरी सांस", "टहलना", "ठंडा पानी"
  val correctionReason: String? = null,
  val timezoneId: String = java.util.TimeZone.getDefault().id
)

/**
 * Local configuration and privacy settings for My Control.
 */
@Entity(tableName = "my_control_config")
data class MyControlConfigEntity(
  @PrimaryKey val id: Int = 1,
  val reminderEnabled: Boolean = false,
  val reminderTimeMinutes: Int = 1260, // 9:00 PM (21:00)
  val privacyPinEnabled: Boolean = false,
  val privacyPin: String = "",
  val goalTitle: String = "Complete a full calendar day without masturbation",
  val promptYesterdayPending: Boolean = true
)

/**
 * Calculated streak and progress statistics.
 */
data class MyControlStats(
  val currentStreak: Int = 0,
  val longestStreak: Int = 0,
  val totalCheckIns: Int = 0,
  val totalCompleted: Int = 0,
  val totalGoalNotMet: Int = 0,
  val successRatePercent: Float = 0f,
  val todayStatus: GoalStatus = GoalStatus.PENDING,
  val yesterdayStatus: GoalStatus = GoalStatus.PENDING,
  val isYesterdayPending: Boolean = false
)
