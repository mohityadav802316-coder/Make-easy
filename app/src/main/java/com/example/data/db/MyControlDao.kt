package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.model.GoalStatus
import com.example.data.model.MyControlConfigEntity
import com.example.data.model.MyControlDayEntity
import com.example.data.model.MyControlEventEntity
import com.example.data.model.MyControlEventType
import kotlinx.coroutines.flow.Flow

@Dao
interface MyControlDao {

  // --- Daily Goal Status ---

  @Query("SELECT * FROM my_control_days ORDER BY date DESC")
  fun getAllDaysFlow(): Flow<List<MyControlDayEntity>>

  @Query("SELECT * FROM my_control_days ORDER BY date DESC")
  suspend fun getAllDaysSync(): List<MyControlDayEntity>

  @Query("SELECT * FROM my_control_days WHERE date = :date LIMIT 1")
  fun getDayFlow(date: String): Flow<MyControlDayEntity?>

  @Query("SELECT * FROM my_control_days WHERE date = :date LIMIT 1")
  suspend fun getDaySync(date: String): MyControlDayEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertDay(day: MyControlDayEntity)

  @Query("DELETE FROM my_control_days WHERE date = :date")
  suspend fun deleteDay(date: String)

  // --- Append-Only Event History ---

  @Query("SELECT * FROM my_control_events ORDER BY timestampEpochMs DESC")
  fun getAllEventsFlow(): Flow<List<MyControlEventEntity>>

  @Query("SELECT * FROM my_control_events ORDER BY timestampEpochMs DESC")
  suspend fun getAllEventsSync(): List<MyControlEventEntity>

  @Query("SELECT * FROM my_control_events WHERE date = :date ORDER BY timestampEpochMs DESC")
  fun getEventsForDateFlow(date: String): Flow<List<MyControlEventEntity>>

  @Query("SELECT * FROM my_control_events WHERE date = :date ORDER BY timestampEpochMs DESC")
  suspend fun getEventsForDateSync(date: String): List<MyControlEventEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertEvent(event: MyControlEventEntity): Long

  // --- Atomic Check-in Transaction ---

  @Transaction
  suspend fun recordDailyCheckIn(
    date: String,
    status: GoalStatus,
    note: String?,
    isRetrospective: Boolean,
    timezoneId: String
  ) {
    val existing = getDaySync(date)
    val now = System.currentTimeMillis()

    // 1. Upsert logical daily status
    upsertDay(
      MyControlDayEntity(
        date = date,
        status = status,
        note = note,
        updatedAtEpochMs = now,
        isRetrospective = isRetrospective,
        timezoneId = timezoneId
      )
    )

    // 2. Append immutable audit event
    val eventType = if (existing != null && existing.status != GoalStatus.PENDING) {
      MyControlEventType.STATUS_CORRECTION
    } else {
      MyControlEventType.DAILY_CHECKIN
    }

    insertEvent(
      MyControlEventEntity(
        eventType = eventType,
        date = date,
        timestampEpochMs = now,
        status = status,
        previousStatus = existing?.status,
        note = note,
        correctionReason = if (eventType == MyControlEventType.STATUS_CORRECTION) "User updated prior check-in" else null,
        timezoneId = timezoneId
      )
    )
  }

  // --- Urge Log Transaction ---

  @Transaction
  suspend fun recordUrgeCheckIn(
    date: String,
    intensity: Int,
    trigger: String?,
    strategy: String?,
    note: String?,
    timezoneId: String
  ): Long {
    val now = System.currentTimeMillis()
    return insertEvent(
      MyControlEventEntity(
        eventType = MyControlEventType.URGE_LOG,
        date = date,
        timestampEpochMs = now,
        note = note,
        urgeIntensity = intensity,
        urgeTrigger = trigger,
        urgeStrategy = strategy,
        timezoneId = timezoneId
      )
    )
  }

  // --- Configuration & Settings ---

  @Query("SELECT * FROM my_control_config WHERE id = 1 LIMIT 1")
  fun getConfigFlow(): Flow<MyControlConfigEntity?>

  @Query("SELECT * FROM my_control_config WHERE id = 1 LIMIT 1")
  suspend fun getConfigSync(): MyControlConfigEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertConfig(config: MyControlConfigEntity)

  // --- Reset / Purge ---

  @Query("DELETE FROM my_control_events")
  suspend fun clearAllEvents()

  @Query("DELETE FROM my_control_days")
  suspend fun clearAllDays()
}
