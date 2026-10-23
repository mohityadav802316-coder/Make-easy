package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.alarm.AlarmScheduler
import com.example.data.db.LifeTrackerDatabase
import com.example.data.model.DailyNoteEntity
import com.example.data.model.EnergyMode
import com.example.data.model.DashboardPreset
import com.example.data.model.DashboardSectionId
import com.example.widget.LifeTrackerWidgetProvider
import com.example.data.model.DaySummary
import com.example.data.model.DayTaskEntity
import com.example.data.model.JournalistCategory
import com.example.data.model.JournalistEntryEntity
import com.example.data.model.JournalistPersonEntity
import com.example.data.model.MeditationSessionEntity
import com.example.data.model.MeditationStats
import com.example.data.model.MeditationType
import com.example.data.model.ReflectionCategory
import com.example.data.model.ReflectionEntity
import com.example.data.model.RoutineTemplateEntity
import com.example.data.model.StreakInfo
import com.example.data.model.TaskStatus
import com.example.data.repository.LifeTrackerRepository
import com.example.meditation.MeditationManager
import com.example.meditation.MeditationState
import com.example.util.RoutineUtils
import com.example.util.TimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.data.model.TimelineDayRecord
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

enum class MainTab {
  TODAY,
  WEEK,
  CALENDAR,
  REFLECTION,
  SETTINGS,
  MEDITATION,
  TIMELINE,
  ALARM_CENTER,
  EXPENSE_DIARY,
  MY_CONTROL
}

class LifeTrackerViewModel(application: Application) : AndroidViewModel(application) {
  val database = LifeTrackerDatabase.getDatabase(application)
  val repository = LifeTrackerRepository(database)

  val todayDate: String
    get() = TimeUtils.getTodayDateString()

  private val _currentTimeMinutes = MutableStateFlow(TimeUtils.getCurrentMinutes())
  val currentTimeMinutes: StateFlow<Int> = _currentTimeMinutes.asStateFlow()

  private val _currentLiveTime = MutableStateFlow(TimeUtils.getNowTimeShort())
  val currentLiveTime: StateFlow<String> = _currentLiveTime.asStateFlow()

  private val _currentTab = MutableStateFlow(MainTab.TODAY)
  val currentTab: StateFlow<MainTab> = _currentTab.asStateFlow()

  private val _selectedDate = MutableStateFlow(todayDate)
  val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

  val userSettings = repository.getUserSettings().stateIn(
    viewModelScope,
    SharingStarted.Eagerly,
    null
  )

  val anchorDate: StateFlow<String> = userSettings.map {
    it?.anchorDate ?: todayDate
  }.stateIn(viewModelScope, SharingStarted.Eagerly, todayDate)

  val wakeUpMinutes: StateFlow<Int> = userSettings.map {
    it?.wakeUpMinutes ?: 300 // Default 5:00 AM
  }.stateIn(viewModelScope, SharingStarted.Eagerly, 300)

  val isAlarmEnabled: StateFlow<Boolean> = userSettings.map {
    it?.isAlarmEnabled ?: false
  }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

  val snoozeMinutes: StateFlow<Int> = userSettings.map {
    it?.snoozeMinutes ?: 10
  }.stateIn(viewModelScope, SharingStarted.Eagerly, 10)

  val alarmSoundType: StateFlow<String> = userSettings.map {
    it?.alarmSoundType ?: "DEFAULT"
  }.stateIn(viewModelScope, SharingStarted.Eagerly, "DEFAULT")

  val customSoundUri: StateFlow<String?> = userSettings.map {
    it?.customSoundUri
  }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

  val customSoundTitle: StateFlow<String> = userSettings.map {
    it?.customSoundTitle ?: "High-Tone Digital Alarm"
  }.stateIn(viewModelScope, SharingStarted.Eagerly, "High-Tone Digital Alarm")

  val alarmVolume: StateFlow<Float> = userSettings.map {
    it?.alarmVolume ?: 1.0f
  }.stateIn(viewModelScope, SharingStarted.Eagerly, 1.0f)

  val isVibrationEnabled: StateFlow<Boolean> = userSettings.map {
    it?.isVibrationEnabled ?: true
  }.stateIn(viewModelScope, SharingStarted.Eagerly, true)

  val alarmRepeatMode: StateFlow<String> = userSettings.map {
    it?.alarmRepeatMode ?: "DAILY"
  }.stateIn(viewModelScope, SharingStarted.Eagerly, "DAILY")

  val alarmCustomDaysMask: StateFlow<Int> = userSettings.map {
    it?.alarmCustomDaysMask ?: 127
  }.stateIn(viewModelScope, SharingStarted.Eagerly, 127)

  // Feature 2: Smart Pre-Reminder (0 = off, 5, 10, 15, 30 min before)
  val smartReminderMinutes: StateFlow<Int> = userSettings.map {
    it?.smartReminderMinutes ?: 0
  }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

  val userName: StateFlow<String> = userSettings.map {
    val name = it?.userName?.trim() ?: "मोहित"
    if (name.isBlank()) "मोहित" else name
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "मोहित")

  val meditationChimeEnabled: StateFlow<Boolean> = userSettings.map {
    it?.meditationChimeEnabled ?: true
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

  val meditationVoiceLanguage: StateFlow<String> = userSettings.map {
    it?.meditationVoiceLanguage ?: "HI"
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "HI")

  // Optional Feature Toggles
  val enableQuickAdd: StateFlow<Boolean> = userSettings.map {
    it?.enableQuickAdd ?: true
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

  val enableMorningBrief: StateFlow<Boolean> = userSettings.map {
    it?.enableMorningBrief ?: true
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

  val enableLifeTimeline: StateFlow<Boolean> = userSettings.map {
    it?.enableLifeTimeline ?: true
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

  // Meditation System
  val meditationManager = MeditationManager.getInstance(application)
  val meditationState: StateFlow<MeditationState> = meditationManager.state

  val allMeditationSessions: StateFlow<List<MeditationSessionEntity>> = repository.getAllMeditationSessions().stateIn(
    viewModelScope,
    SharingStarted.WhileSubscribed(5000),
    emptyList()
  )

  val meditationStats: StateFlow<MeditationStats> = allMeditationSessions.map { sessions ->
    val completed = sessions.filter { it.isCompleted }
    val totalMins = completed.sumOf { it.completedSeconds } / 60
    val topType = if (sessions.isNotEmpty()) {
      sessions.groupBy { it.type }.maxByOrNull { it.value.size }?.key ?: "BREATHING"
    } else "BREATHING"
    MeditationStats(
      totalSessions = sessions.size,
      totalMinutes = totalMins,
      completedSessions = completed.size,
      mostPracticedType = topType
    )
  }.stateIn(
    viewModelScope,
    SharingStarted.WhileSubscribed(5000),
    MeditationStats()
  )

  // Energy Mode Flow (resets to NORMAL if date does not match today)
  val dailyEnergyMode: StateFlow<EnergyMode> = userSettings.map { s ->
    if (s != null && s.dailyEnergyModeDate == todayDate) {
      EnergyMode.fromString(s.dailyEnergyMode)
    } else {
      EnergyMode.NORMAL
    }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), EnergyMode.NORMAL)

  // Expense Diary State & Flows
  val allExpenses: StateFlow<List<com.example.data.model.ExpenseEntity>> =
    repository.getAllExpenses().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val todayExpenses: StateFlow<List<com.example.data.model.ExpenseEntity>> =
    repository.getTodayExpenses(todayDate).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val todayTotalSpend: StateFlow<Double> =
    repository.getTodayTotalSpend(todayDate).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

  val expenseQuickChips: StateFlow<List<com.example.data.model.ExpenseQuickChipEntity>> =
    repository.getAllQuickChips().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val expenseCategories: StateFlow<List<com.example.data.model.ExpenseCategoryEntity>> =
    repository.getAllExpenseCategories().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  // Expense Diary budget & undo state
  private val expensePrefs = getApplication<Application>().getSharedPreferences("expense_diary_prefs", Context.MODE_PRIVATE)
  private val _monthlyBudget = MutableStateFlow(expensePrefs.getFloat("monthly_budget", 15000f).toDouble())
  val monthlyBudget: StateFlow<Double> = _monthlyBudget.asStateFlow()

  var lastDeletedExpense: com.example.data.model.ExpenseEntity? = null
    private set

  fun setMonthlyBudget(amount: Double) {
    _monthlyBudget.value = amount
    expensePrefs.edit().putFloat("monthly_budget", amount.toFloat()).apply()
  }

  fun addExpense(
    amount: Double,
    note: String,
    category: String,
    categoryEmoji: String = "🍔",
    categoryColorHex: String = "#FF9800",
    date: String = com.example.util.TimeUtils.getTodayDateString(),
    time: String = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date()),
    mood: String? = null
  ) {
    viewModelScope.launch {
      val entity = com.example.data.model.ExpenseEntity(
        amount = amount,
        note = note.trim(),
        category = category,
        categoryEmoji = categoryEmoji,
        categoryColorHex = categoryColorHex,
        date = date,
        time = time,
        timestamp = System.currentTimeMillis(),
        mood = mood
      )
      repository.addExpense(entity)
    }
  }

  fun updateExpense(expense: com.example.data.model.ExpenseEntity) {
    viewModelScope.launch {
      repository.updateExpense(expense)
    }
  }

  fun deleteExpense(expense: com.example.data.model.ExpenseEntity) {
    viewModelScope.launch {
      lastDeletedExpense = expense
      repository.deleteExpense(expense)
    }
  }

  fun undoDeleteExpense() {
    viewModelScope.launch {
      lastDeletedExpense?.let {
        repository.addExpense(it)
        lastDeletedExpense = null
      }
    }
  }

  fun addCustomCategory(
    nameHi: String,
    nameEn: String,
    emoji: String,
    colorHex: String,
    keywords: String
  ) {
    viewModelScope.launch {
      val id = "cat_" + System.currentTimeMillis()
      val cat = com.example.data.model.ExpenseCategoryEntity(
        id = id,
        nameHi = nameHi.trim(),
        nameEn = nameEn.trim(),
        emoji = emoji.trim(),
        colorHex = colorHex,
        keywords = keywords.trim(),
        orderIndex = 100
      )
      repository.expenseDao.insertCategory(cat)
    }
  }

  fun addQuickChip(
    label: String,
    amount: Double,
    note: String,
    category: String,
    emoji: String
  ) {
    viewModelScope.launch {
      val chip = com.example.data.model.ExpenseQuickChipEntity(
        label = label.trim(),
        amount = amount,
        note = note.trim(),
        category = category,
        emoji = emoji,
        usageCount = 1
      )
      repository.expenseDao.insertQuickChip(chip)
    }
  }

  fun deleteQuickChip(id: Long) {
    viewModelScope.launch {
      repository.expenseDao.deleteQuickChipById(id)
    }
  }

  fun updateQuickChip(chip: com.example.data.model.ExpenseQuickChipEntity) {
    viewModelScope.launch {
      repository.updateQuickChip(chip)
    }
  }

  // --- Recurring Expenses State & Operations ---
  val recurringExpenses: StateFlow<List<com.example.data.model.RecurringExpenseEntity>> =
    repository.getAllRecurringExpenses().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  fun addRecurringExpense(
    title: String,
    amount: Double,
    category: String,
    categoryEmoji: String = "🏠",
    categoryColorHex: String = "#8B5CF6",
    dayOfMonth: Int = 1
  ) {
    viewModelScope.launch {
      val rec = com.example.data.model.RecurringExpenseEntity(
        title = title.trim(),
        amount = amount,
        category = category,
        categoryEmoji = categoryEmoji,
        categoryColorHex = categoryColorHex,
        dayOfMonth = dayOfMonth.coerceIn(1, 31),
        isEnabled = true
      )
      repository.addRecurringExpense(rec)
    }
  }

  fun updateRecurringExpense(rec: com.example.data.model.RecurringExpenseEntity) {
    viewModelScope.launch {
      repository.updateRecurringExpense(rec)
    }
  }

  fun deleteRecurringExpense(rec: com.example.data.model.RecurringExpenseEntity) {
    viewModelScope.launch {
      repository.deleteRecurringExpense(rec)
    }
  }

  fun checkRecurringExpenses() {
    viewModelScope.launch {
      repository.checkAndTriggerRecurringExpenses(todayDate)
    }
  }

  fun updateExpenseCategory(cat: com.example.data.model.ExpenseCategoryEntity) {
    viewModelScope.launch {
      repository.updateExpenseCategory(cat)
    }
  }

  fun deleteExpenseCategory(cat: com.example.data.model.ExpenseCategoryEntity) {
    viewModelScope.launch {
      repository.deleteExpenseCategory(cat)
    }
  }

  // --- Expense Preferences & Security ---
  private val _currencySymbol = MutableStateFlow(expensePrefs.getString("currency_symbol", "₹") ?: "₹")
  val currencySymbol: StateFlow<String> = _currencySymbol.asStateFlow()

  fun setCurrencySymbol(sym: String) {
    _currencySymbol.value = sym
    expensePrefs.edit().putString("currency_symbol", sym).apply()
  }

  private val _firstDayOfWeek = MutableStateFlow(expensePrefs.getString("first_day_of_week", "MONDAY") ?: "MONDAY")
  val firstDayOfWeek: StateFlow<String> = _firstDayOfWeek.asStateFlow()

  fun setFirstDayOfWeek(day: String) {
    _firstDayOfWeek.value = day
    expensePrefs.edit().putString("first_day_of_week", day).apply()
  }

  private val _reportColorTheme = MutableStateFlow(expensePrefs.getString("report_color_theme", "CLASSIC") ?: "CLASSIC")
  val reportColorTheme: StateFlow<String> = _reportColorTheme.asStateFlow()

  fun setReportColorTheme(theme: String) {
    _reportColorTheme.value = theme
    expensePrefs.edit().putString("report_color_theme", theme).apply()
  }

  private val _isAppLockEnabled = MutableStateFlow(expensePrefs.getBoolean("app_lock_enabled", false))
  val isAppLockEnabled: StateFlow<Boolean> = _isAppLockEnabled.asStateFlow()

  private val _appLockPin = MutableStateFlow(expensePrefs.getString("app_lock_pin", "") ?: "")
  val appLockPin: StateFlow<String> = _appLockPin.asStateFlow()

  fun setAppLock(enabled: Boolean, pin: String) {
    _isAppLockEnabled.value = enabled
    _appLockPin.value = pin
    expensePrefs.edit().putBoolean("app_lock_enabled", enabled).putString("app_lock_pin", pin).apply()
  }

  fun restoreExpenseBackup(jsonStr: String, onResult: (Boolean, String) -> Unit) {
    viewModelScope.launch {
      try {
        val data = com.example.expense.ExpenseBackupHelper.parseJsonBackup(jsonStr)
        setMonthlyBudget(data.monthlyBudget)
        for (cat in data.categories) {
          repository.expenseDao.insertCategory(cat)
        }
        for (chip in data.quickChips) {
          repository.expenseDao.insertQuickChip(chip)
        }
        for (exp in data.expenses) {
          repository.expenseDao.insertExpense(exp)
        }
        for (rec in data.recurring) {
          repository.expenseDao.insertRecurringExpense(rec)
        }
        onResult(true, "बैकअप रीस्टोर सफल! (${data.expenses.size} खर्च)")
      } catch (e: Exception) {
        onResult(false, e.localizedMessage ?: "अमान्य बैकअप JSON")
      }
    }
  }

  // Feature 6: Priority filtering
  private val _priorityFilter = MutableStateFlow<String?>("ALL")
  val priorityFilter: StateFlow<String?> = _priorityFilter.asStateFlow()

  val currentCycleDay: StateFlow<Int> = combine(anchorDate, selectedDate) { anchor, selected ->
    TimeUtils.calculateCycleDay(anchor, selected)
  }.stateIn(viewModelScope, SharingStarted.Eagerly, 1)

  @OptIn(ExperimentalCoroutinesApi::class)
  val tasksForSelectedDate: StateFlow<List<DayTaskEntity>> = _selectedDate.flatMapLatest { date ->
    repository.getTasksForDate(date)
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val isRecoveryDayActive: StateFlow<Boolean> = tasksForSelectedDate.map { tasks ->
    tasks.any { it.category == "Recovery" || it.name.contains("Recovery", ignoreCase = true) }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

  val filteredTasksForSelectedDate: StateFlow<List<DayTaskEntity>> = combine(tasksForSelectedDate, _priorityFilter) { tasks, filter ->
    if (filter.isNullOrBlank() || filter == "ALL") {
      tasks
    } else {
      tasks.filter { it.priority.equals(filter, ignoreCase = true) }
    }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val allTasks: StateFlow<List<DayTaskEntity>> = repository.getAllTasks().stateIn(
    viewModelScope,
    SharingStarted.WhileSubscribed(5000),
    emptyList()
  )

  val totalCompletedTasksCount: StateFlow<Int> = allTasks.map { tasks ->
    tasks.count { it.status == TaskStatus.COMPLETE }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

  val currentMonthTotalSpend: StateFlow<Double> = allExpenses.map { list ->
    val cal = java.util.Calendar.getInstance()
    val curMonthPrefix = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.US).format(cal.time)
    list.filter { it.date.startsWith(curMonthPrefix) }.sumOf { it.amount }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

  // Feature 1: Streak System calculated from actual task completion
  val streakInfo: StateFlow<StreakInfo> = allTasks.map { tasksList ->
    calculateStreak(tasksList)
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StreakInfo())

  // Feature 3: Daily Reflection (Note + Mood)
  @OptIn(ExperimentalCoroutinesApi::class)
  val dailyNoteForSelectedDate: StateFlow<DailyNoteEntity?> = _selectedDate.flatMapLatest { date ->
    repository.getDailyNote(date)
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

  val allDailyNotes: StateFlow<List<DailyNoteEntity>> = repository.getAllDailyNotes().stateIn(
    viewModelScope,
    SharingStarted.WhileSubscribed(5000),
    emptyList()
  )

  val summaryForSelectedDate: StateFlow<DaySummary> = combine(tasksForSelectedDate, currentCycleDay, selectedDate) { tasks, cycleDay, date ->
    val total = tasks.size
    val completed = tasks.count { it.status == TaskStatus.COMPLETE }
    val partial = tasks.count { it.status == TaskStatus.PARTIAL }
    val missed = tasks.count { it.status == TaskStatus.MISSED }
    val totalScore = (completed * 1.0f) + (partial * 0.5f)
    val percentage = if (total > 0) ((totalScore / total.toFloat()) * 100f).roundToInt() else 0

    DaySummary(
      date = date,
      dayOfCycle = cycleDay,
      totalTasks = total,
      completedCount = completed,
      partialCount = partial,
      missedCount = missed,
      totalScore = totalScore,
      completionPercentage = percentage
    )
  }.stateIn(
    viewModelScope,
    SharingStarted.WhileSubscribed(5000),
    DaySummary(todayDate, 1, 0, 0, 0, 0, 0f, 0)
  )

  // 7-day Cycle Dates and Summaries
  val currentCycleDates: StateFlow<List<String>> = combine(anchorDate, selectedDate) { anchor, selected ->
    TimeUtils.getCycleDates(anchor, selected)
  }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  val cycleSummaries: StateFlow<List<DaySummary>> = combine(currentCycleDates, allTasks, anchorDate) { cycleDates, tasksList, anchor ->
    val tasksByDate = tasksList.groupBy { it.date }
    cycleDates.map { date ->
      val cycleDay = TimeUtils.calculateCycleDay(anchor, date)
      val tasks = tasksByDate[date] ?: emptyList()
      val total = tasks.size
      val completed = tasks.count { it.status == TaskStatus.COMPLETE }
      val partial = tasks.count { it.status == TaskStatus.PARTIAL }
      val missed = tasks.count { it.status == TaskStatus.MISSED }
      val totalScore = (completed * 1.0f) + (partial * 0.5f)
      val percentage = if (total > 0) ((totalScore / total.toFloat()) * 100f).roundToInt() else 0

      DaySummary(
        date = date,
        dayOfCycle = cycleDay,
        totalTasks = total,
        completedCount = completed,
        partialCount = partial,
        missedCount = missed,
        totalScore = totalScore,
        completionPercentage = percentage
      )
    }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  // Routine Templates
  val routineTemplates: StateFlow<List<RoutineTemplateEntity>> = repository.getAllRoutineTemplates().stateIn(
    viewModelScope,
    SharingStarted.WhileSubscribed(5000),
    emptyList()
  )

  // Reflections
  val allReflections: StateFlow<List<ReflectionEntity>> = repository.getAllReflections().stateIn(
    viewModelScope,
    SharingStarted.WhileSubscribed(5000),
    emptyList()
  )

  private val _reflectionCategoryFilter = MutableStateFlow<ReflectionCategory?>(null)
  val reflectionCategoryFilter: StateFlow<ReflectionCategory?> = _reflectionCategoryFilter.asStateFlow()

  private val _reflectionSearchQuery = MutableStateFlow("")
  val reflectionSearchQuery: StateFlow<String> = _reflectionSearchQuery.asStateFlow()

  private val _reflectionTagFilter = MutableStateFlow<String?>(null)
  val reflectionTagFilter: StateFlow<String?> = _reflectionTagFilter.asStateFlow()

  val filteredReflections: StateFlow<List<ReflectionEntity>> = combine(
    allReflections,
    _reflectionCategoryFilter,
    _reflectionSearchQuery,
    _reflectionTagFilter
  ) { list, cat, query, tag ->
    list.filter { item ->
      val matchesCat = cat == null || item.category == cat
      val matchesQuery = query.isBlank() ||
        item.title.contains(query, ignoreCase = true) ||
        item.description.contains(query, ignoreCase = true) ||
        item.tags.contains(query, ignoreCase = true)
      val matchesTag = tag.isNullOrBlank() || item.tags.contains(tag, ignoreCase = true)
      matchesCat && matchesQuery && matchesTag
    }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  // Journalist State
  val allJournalistPersons: StateFlow<List<JournalistPersonEntity>> = repository.getAllJournalistPersons().stateIn(
    viewModelScope,
    SharingStarted.WhileSubscribed(5000),
    emptyList()
  )

  val allJournalistEntries: StateFlow<List<JournalistEntryEntity>> = repository.getAllJournalistEntries().stateIn(
    viewModelScope,
    SharingStarted.WhileSubscribed(5000),
    emptyList()
  )

  private val _selectedPersonId = MutableStateFlow<String?>(null)
  val selectedPersonId: StateFlow<String?> = _selectedPersonId.asStateFlow()

  private val _journalistCategoryFilter = MutableStateFlow<JournalistCategory?>(null)
  val journalistCategoryFilter: StateFlow<JournalistCategory?> = _journalistCategoryFilter.asStateFlow()

  private val _journalistSearchQuery = MutableStateFlow("")
  val journalistSearchQuery: StateFlow<String> = _journalistSearchQuery.asStateFlow()

  // Filtered entries for the currently selected person profile
  val profileEntries: StateFlow<List<JournalistEntryEntity>> = combine(
    allJournalistEntries,
    _selectedPersonId,
    _journalistCategoryFilter,
    _journalistSearchQuery
  ) { entries, personId, cat, query ->
    if (personId == null) {
      emptyList()
    } else {
      entries.filter { item ->
        val matchesPerson = item.personId == personId
        val matchesCategory = cat == null || item.category == cat
        val matchesQuery = query.isBlank() ||
          item.text.contains(query, ignoreCase = true) ||
          item.context.contains(query, ignoreCase = true) ||
          item.intensity.contains(query, ignoreCase = true) ||
          item.date.contains(query, ignoreCase = true)
        matchesPerson && matchesCategory && matchesQuery
      }
    }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val categoryAggregates: StateFlow<List<CategoryAggregate>> = allTasks.map { tasks ->
    tasks.groupBy { it.category }.map { (cat, list) ->
      CategoryAggregate(
        category = cat,
        totalCount = list.size,
        completedCount = list.count { it.status == TaskStatus.COMPLETE },
        partialCount = list.count { it.status == TaskStatus.PARTIAL }
      )
    }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  // Life Timeline Records
  val timelineRecords: StateFlow<List<TimelineDayRecord>> = combine(
    allTasks,
    allDailyNotes,
    allMeditationSessions,
    anchorDate
  ) { tasks, notes, meditations, anchor ->
    val tasksByDate = tasks.groupBy { it.date }
    val notesByDate = notes.associateBy { it.date }
    val medsByDate = meditations.groupBy { it.date }

    val allDates = (tasksByDate.keys + notesByDate.keys + medsByDate.keys).distinct().sortedDescending()

    allDates.map { date ->
      val cycleDay = TimeUtils.calculateCycleDay(anchor, date)
      val dayTasks = tasksByDate[date] ?: emptyList()
      val total = dayTasks.size
      val completed = dayTasks.count { it.status == TaskStatus.COMPLETE }
      val partial = dayTasks.count { it.status == TaskStatus.PARTIAL }
      val missed = dayTasks.count { it.status == TaskStatus.MISSED }
      val score = completed + (partial * 0.5f)
      val percentage = if (total > 0) ((score / total) * 100).roundToInt() else 0

      val dailyNote = notesByDate[date]
      val dayMeds = medsByDate[date] ?: emptyList()
      val medMins = dayMeds.filter { it.isCompleted }.sumOf { it.completedSeconds } / 60

      val priorityCompleted = dayTasks
        .filter { it.priority != "NORMAL" && it.status == TaskStatus.COMPLETE }
        .map { it.name }

      val priorityMissed = dayTasks
        .filter { it.priority != "NORMAL" && it.status == TaskStatus.MISSED }
        .map { it.name }

      val achievements = mutableListOf<String>()
      if (percentage == 100 && total > 0) achievements.add("🎯 100% Perfect Routine")
      else if (percentage >= 85) achievements.add("⭐ High Discipline (>85%)")
      if (dayMeds.any { it.isCompleted }) achievements.add("🧘 Mindful Practice Done")
      if (priorityCompleted.isNotEmpty() && priorityMissed.isEmpty()) achievements.add("⚡ All Priorities Crushed")

      TimelineDayRecord(
        date = date,
        dayOfCycle = cycleDay,
        completionPercentage = percentage,
        totalTasks = total,
        completedTasks = completed,
        missedTasks = missed,
        mood = dailyNote?.mood,
        note = dailyNote?.note?.ifBlank { null },
        meditationMinutes = medMins,
        meditationCount = dayMeds.size,
        priorityTasksCompleted = priorityCompleted,
        priorityTasksMissed = priorityMissed,
        achievements = achievements
      )
    }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  init {
    viewModelScope.launch {
      repository.initializeDefaultsIfNeeded(todayDate)
      AlarmScheduler.createNotificationChannels(getApplication())
      AlarmScheduler.rescheduleAllTimetableAlarms(getApplication())
      // Push widget update
      LifeTrackerWidgetProvider.updateAllWidgets(getApplication())
      // Initialize Expense Diary defaults
      repository.initializeExpenseDefaultsIfNeeded()
      repository.checkAndTriggerRecurringExpenses(todayDate)
    }
    viewModelScope.launch {
      userSettings.collect { settings ->
        if (settings != null) {
          meditationManager.updateSettings(
            userName = settings.userName.ifBlank { "मोहित" },
            chimeEnabled = settings.meditationChimeEnabled,
            language = settings.meditationVoiceLanguage
          )
        }
      }
    }
    viewModelScope.launch {
      while (true) {
        val mins = TimeUtils.getCurrentMinutes()
        if (_currentTimeMinutes.value != mins) {
          _currentTimeMinutes.value = mins
        }
        val liveStr = TimeUtils.getNowTimeShort()
        if (_currentLiveTime.value != liveStr) {
          _currentLiveTime.value = liveStr
        }
        delay(1000L)
      }
    }
  }

  fun onAppResume() {
    val currentToday = todayDate
    viewModelScope.launch {
      val cycleDay = TimeUtils.calculateCycleDay(anchorDate.value, currentToday)
      repository.ensureDayInitialized(currentToday, cycleDay)
      if (_currentTab.value == MainTab.TODAY && _selectedDate.value != currentToday) {
        _selectedDate.value = currentToday
      }
    }
  }

  fun onSystemDateTick() {
    onAppResume()
  }

  fun onAppPause() {
  }

  private val _navStack = mutableListOf<MainTab>(MainTab.TODAY)

  fun navigateTo(tab: MainTab) {
    if (_currentTab.value != tab) {
      _navStack.add(tab)
      _currentTab.value = tab
    }
  }

  fun navigateBack(): Boolean {
    if (_navStack.size > 1) {
      _navStack.removeAt(_navStack.size - 1)
      _currentTab.value = _navStack.lastOrNull() ?: MainTab.TODAY
      return true
    } else if (_currentTab.value != MainTab.TODAY) {
      _navStack.clear()
      _navStack.add(MainTab.TODAY)
      _currentTab.value = MainTab.TODAY
      return true
    }
    return false
  }

  fun setTab(tab: MainTab) {
    navigateTo(tab)
  }

  fun selectDate(date: String) {
    _selectedDate.value = date
    viewModelScope.launch {
      val cycleDay = TimeUtils.calculateCycleDay(anchorDate.value, date)
      repository.ensureDayInitialized(date, cycleDay)
    }
  }

  fun navigateDay(delta: Int) {
    val newDate = TimeUtils.shiftDate(_selectedDate.value, delta)
    // Only allow today and past days ("agr aaj day 1 hai to sirf aaj hi click ho agle din ka nhi")
    if (delta > 0 && newDate > todayDate) {
      return
    }
    selectDate(newDate)
  }

  fun navigateCycle(deltaWeeks: Int) {
    val newDate = TimeUtils.shiftDate(_selectedDate.value, deltaWeeks * 7)
    // Prevent navigating into future cycles
    if (deltaWeeks > 0) {
      val cycleDates = TimeUtils.getCycleDates(anchorDate.value, newDate)
      val firstDate = cycleDates.firstOrNull()
      if (firstDate != null && firstDate > todayDate) {
        return
      }
    }
    selectDate(newDate)
  }

  fun setToday() {
    selectDate(todayDate)
  }

  fun updateTaskStatus(task: DayTaskEntity, newStatus: TaskStatus) {
    viewModelScope.launch {
      repository.updateTaskStatus(task, newStatus)
      try {
        LifeTrackerWidgetProvider.updateAllWidgets(getApplication())
      } catch (_: Exception) {}
    }
  }

  fun saveTask(
    id: Long = 0,
    name: String,
    timeMinutes: Int,
    category: String,
    notes: String,
    isExtra: Boolean,
    priority: String = "NORMAL"
  ) {
    viewModelScope.launch {
      if (id == 0L) {
        val newTask = DayTaskEntity(
          date = _selectedDate.value,
          templateId = if (isExtra) null else -1L,
          name = name,
          timeMinutes = timeMinutes,
          category = category,
          status = TaskStatus.MISSED,
          notes = notes,
          isExtra = isExtra,
          orderIndex = tasksForSelectedDate.value.size,
          priority = priority
        )
        repository.insertTask(newTask)
      } else {
        val existing = tasksForSelectedDate.value.firstOrNull { it.id == id }
        if (existing != null) {
          repository.updateTask(
            existing.copy(
              name = name,
              timeMinutes = timeMinutes,
              category = category,
              notes = notes,
              isExtra = isExtra,
              priority = priority
            )
          )
        }
      }
    }
  }

  fun deleteTask(taskId: Long) {
    val target = tasksForSelectedDate.value.firstOrNull { it.id == taskId }
    val date = target?.date ?: _selectedDate.value
    viewModelScope.launch {
      repository.deleteTask(taskId, date)
    }
  }

  // Routine templates management
  fun saveRoutineTemplate(
    context: Context,
    id: Long = 0,
    name: String,
    timeMinutes: Int,
    category: String,
    notes: String,
    daysMask: Int,
    isActive: Boolean,
    priority: String = "NORMAL",
    onSuccess: (() -> Unit)? = null
  ) {
    viewModelScope.launch {
      val existingById = if (id > 0L) repository.getRoutineTemplateById(id) else null
      val targetKey = RoutineUtils.resolveActivityKey(name, existingById?.activityKey)

      // Look up existing template by targetKey or by id
      val existingByKey = repository.getRoutineTemplateByActivityKey(targetKey)
      val existing = existingById ?: existingByKey

      val targetId = if (existing != null) {
        // UPDATE existing row to prevent duplicate active routine rows
        val updated = existing.copy(
          activityKey = targetKey,
          name = name.trim(),
          timeMinutes = timeMinutes,
          category = category.trim(),
          notes = notes.trim(),
          daysMask = daysMask,
          isActive = isActive,
          priority = priority
        )
        repository.updateRoutineTemplate(updated)
        existing.id
      } else {
        // INSERT new unique routine row
        val item = RoutineTemplateEntity(
          activityKey = targetKey,
          name = name.trim(),
          timeMinutes = timeMinutes,
          category = category.trim(),
          notes = notes.trim(),
          daysMask = daysMask,
          isActive = isActive,
          orderIndex = routineTemplates.value.size,
          priority = priority
        )
        repository.insertRoutineTemplate(item)
      }

      // 1. Immediately synchronize today's tasks so Home Screen & Daily Context reflect changes instantly
      repository.syncTodayTasksWithTemplates(todayDate)

      // 2. Cancel previous alarm and reschedule new alarm for the updated time
      AlarmScheduler.cancelTimetableAlarm(context, targetId, targetKey)
      if (isActive) {
        val saved = repository.getRoutineTemplateById(targetId)
        if (saved != null) {
          val settings = userSettings.value
          val smartReminder = settings?.smartReminderMinutes ?: 0
          AlarmScheduler.scheduleTimetableAlarm(context, saved, smartReminder)
        }
      }

      onSuccess?.invoke()
    }
  }

  fun saveRoutineTemplate(
    id: Long = 0,
    name: String,
    timeMinutes: Int,
    category: String,
    notes: String,
    daysMask: Int,
    isActive: Boolean
  ) {
    saveRoutineTemplate(
      context = getApplication(),
      id = id,
      name = name,
      timeMinutes = timeMinutes,
      category = category,
      notes = notes,
      daysMask = daysMask,
      isActive = isActive
    )
  }

  fun deleteRoutineTemplate(
    context: Context,
    id: Long,
    onSuccess: (() -> Unit)? = null
  ) {
    viewModelScope.launch {
      // 1. Cancel scheduled alarm
      AlarmScheduler.cancelTimetableAlarm(context, id)
      // 2. Delete template from database
      repository.deleteRoutineTemplate(id)
      // 3. Sync today's tasks to remove deleted template from today
      repository.syncTodayTasksWithTemplates(todayDate)

      onSuccess?.invoke()
    }
  }

  fun deleteRoutineTemplate(id: Long) {
    deleteRoutineTemplate(context = getApplication(), id = id)
  }

  // Reflection management
  fun setReflectionCategoryFilter(category: ReflectionCategory?) {
    _reflectionCategoryFilter.value = category
  }

  fun setReflectionSearchQuery(query: String) {
    _reflectionSearchQuery.value = query
  }

  fun setReflectionTagFilter(tag: String?) {
    _reflectionTagFilter.value = tag
  }

  fun saveReflection(
    id: Long = 0,
    title: String,
    description: String,
    date: String,
    category: ReflectionCategory,
    tags: String
  ) {
    viewModelScope.launch {
      if (id == 0L) {
        val entity = ReflectionEntity(
          title = title,
          description = description,
          date = date,
          timestamp = System.currentTimeMillis(),
          category = category,
          tags = tags
        )
        repository.insertReflection(entity)
      } else {
        val existing = allReflections.value.firstOrNull { it.id == id }
        if (existing != null) {
          repository.updateReflection(
            existing.copy(
              title = title,
              description = description,
              date = date,
              category = category,
              tags = tags
            )
          )
        }
      }
    }
  }

  fun deleteReflection(id: Long) {
    viewModelScope.launch {
      repository.deleteReflection(id)
    }
  }

  // Journalist actions
  fun setSelectedPerson(personId: String?) {
    _selectedPersonId.value = personId
    _journalistCategoryFilter.value = null
    _journalistSearchQuery.value = ""
  }

  fun setJournalistCategoryFilter(cat: JournalistCategory?) {
    _journalistCategoryFilter.value = cat
  }

  fun setJournalistSearchQuery(query: String) {
    _journalistSearchQuery.value = query
  }

  fun addJournalistPerson(name: String, emoji: String = "👤") {
    viewModelScope.launch {
      val trimmed = name.trim()
      if (trimmed.isEmpty()) return@launch
      val id = java.util.UUID.randomUUID().toString()
      val person = JournalistPersonEntity(
        id = id,
        name = trimmed,
        emoji = emoji.ifBlank { "👤" },
        createdAt = System.currentTimeMillis()
      )
      repository.insertJournalistPerson(person)
    }
  }

  fun deleteJournalistPerson(id: String) {
    viewModelScope.launch {
      repository.deleteJournalistPerson(id)
      if (_selectedPersonId.value == id) {
        _selectedPersonId.value = null
      }
    }
  }

  fun saveJournalistEntry(
    id: String? = null,
    personId: String,
    personName: String,
    category: JournalistCategory,
    text: String,
    date: String,
    time: String,
    context: String = "",
    intensity: String = ""
  ) {
    viewModelScope.launch {
      val trimmedText = text.trim()
      if (trimmedText.isEmpty()) return@launch

      val entryId = id ?: java.util.UUID.randomUUID().toString()
      val now = System.currentTimeMillis()
      val timestamp = "${date}T${time}:00"

      val entry = JournalistEntryEntity(
        id = entryId,
        personId = personId,
        personName = personName,
        category = category,
        text = trimmedText,
        timestamp = timestamp,
        date = date,
        time = time,
        context = context.trim(),
        intensity = intensity.trim(),
        createdAt = if (id == null) now else (allJournalistEntries.value.firstOrNull { it.id == id }?.createdAt ?: now),
        updatedAt = now
      )
      if (id == null) {
        repository.insertJournalistEntry(entry)
      } else {
        repository.updateJournalistEntry(entry)
      }
    }
  }

  fun deleteJournalistEntry(id: String) {
    viewModelScope.launch {
      repository.deleteJournalistEntry(id)
    }
  }

  fun updateAnchorDate(newDate: String) {
    viewModelScope.launch {
      repository.updateAnchorDate(newDate)
    }
  }

  fun updateWakeUpSchedule(
    context: android.content.Context,
    newWakeUpMinutes: Int,
    isAlarmEnabled: Boolean,
    snoozeMinutes: Int = 10,
    updateFutureMasterRoutine: Boolean = true
  ) {
    viewModelScope.launch {
      repository.updateWakeUpSchedule(newWakeUpMinutes, isAlarmEnabled, snoozeMinutes, updateFutureMasterRoutine)
      if (isAlarmEnabled) {
        com.example.alarm.AlarmScheduler.scheduleWakeUpAlarm(
          context,
          newWakeUpMinutes,
          alarmRepeatMode.value,
          alarmCustomDaysMask.value
        )
      } else {
        com.example.alarm.AlarmScheduler.cancelAlarm(context)
      }
    }
  }

  fun toggleAlarm(context: android.content.Context, isEnabled: Boolean) {
    viewModelScope.launch {
      repository.setAlarmEnabled(isEnabled)
      if (isEnabled) {
        com.example.alarm.AlarmScheduler.scheduleWakeUpAlarm(
          context,
          wakeUpMinutes.value,
          alarmRepeatMode.value,
          alarmCustomDaysMask.value
        )
      } else {
        com.example.alarm.AlarmScheduler.cancelAlarm(context)
      }
    }
  }

  fun updateAlarmSound(soundType: String, customUri: String?, soundTitle: String) {
    viewModelScope.launch {
      repository.updateAlarmSound(soundType, customUri, soundTitle)
    }
  }

  fun updateAlarmVolume(volume: Float) {
    viewModelScope.launch {
      repository.updateAlarmVolume(volume)
    }
  }

  fun updateVibrationEnabled(enabled: Boolean) {
    viewModelScope.launch {
      repository.updateVibrationEnabled(enabled)
    }
  }

  fun updateAlarmSnoozeMinutes(snoozeMinutes: Int) {
    viewModelScope.launch {
      repository.updateAlarmSnoozeMinutes(snoozeMinutes)
    }
  }

  fun updateAlarmRepeat(repeatMode: String, customDaysMask: Int = 127, context: android.content.Context? = null) {
    viewModelScope.launch {
      repository.updateAlarmRepeat(repeatMode, customDaysMask)
      if (isAlarmEnabled.value && context != null) {
        com.example.alarm.AlarmScheduler.scheduleWakeUpAlarm(
          context,
          wakeUpMinutes.value,
          repeatMode,
          customDaysMask
        )
      }
    }
  }

  // App Reset & User Data Verification
  suspend fun hasExistingUserData(): Boolean {
    return repository.hasExistingUserData(getApplication<Application>())
  }

  fun resetAppToFreshState(onResult: (Boolean, String) -> Unit) {
    viewModelScope.launch {
      val res = repository.resetAppToFreshState(getApplication<Application>(), _selectedDate.value)
      if (res.isSuccess) {
        selectDate(_selectedDate.value)
        onResult(true, "ऐप सफलतापूर्वक नए सिरे से रीसेट हो गया है!")
      } else {
        onResult(false, res.exceptionOrNull()?.localizedMessage ?: "रीसेट विफल")
      }
    }
  }

  // Feature 1: Streak calculation logic
  private fun calculateStreak(tasksList: List<DayTaskEntity>): StreakInfo {
    val tasksByDate = tasksList.groupBy { it.date }
    val today = TimeUtils.getTodayDateString()
    val todayTasks = tasksByDate[today] ?: emptyList()
    val todayCompleted = todayTasks.count { it.status == TaskStatus.COMPLETE }
    val todayPartial = todayTasks.count { it.status == TaskStatus.PARTIAL }
    val todayTotal = todayTasks.size
    val todayScore = (todayCompleted * 1.0f) + (todayPartial * 0.5f)
    val todayPct = if (todayTotal > 0) ((todayScore / todayTotal) * 100f).roundToInt() else 0

    val isTodayMaintained = todayTotal > 0 && todayPct >= 50
    val isTodayInProgress = todayTotal > 0 && (todayCompleted > 0 || todayPartial > 0) && todayPct < 50

    val todayStatus = when {
      isTodayMaintained -> "Maintained 🔥"
      isTodayInProgress -> "In Progress ⏳"
      todayTotal > 0 -> "Pending ⚠️"
      else -> "No tasks scheduled"
    }

    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val qualifyingDates = tasksByDate.filter { (_, tasks) ->
      val total = tasks.size
      val score = tasks.count { it.status == TaskStatus.COMPLETE } + tasks.count { it.status == TaskStatus.PARTIAL } * 0.5f
      total > 0 && (score / total) >= 0.5f
    }.keys.mapNotNull {
      try { (sdf.parse(it)?.time ?: 0L) to it } catch (_: Exception) { null }
    }.sortedBy { it.first }

    val qualifyingDateSet = qualifyingDates.map { it.second }.toSet()

    // Current streak: count backward from yesterday or today
    var currentStreak = 0
    val cal = Calendar.getInstance()
    if (isTodayMaintained) {
      currentStreak = 1
      cal.add(Calendar.DAY_OF_YEAR, -1)
    } else {
      cal.add(Calendar.DAY_OF_YEAR, -1)
    }

    while (true) {
      val dStr = sdf.format(cal.time)
      if (qualifyingDateSet.contains(dStr)) {
        currentStreak++
        cal.add(Calendar.DAY_OF_YEAR, -1)
      } else {
        break
      }
    }

    // Longest streak across all history
    var longestStreak = 0
    var tempStreak = 0
    var prevTime: Long? = null
    for ((time, _) in qualifyingDates) {
      if (prevTime == null) {
        tempStreak = 1
      } else {
        val diffDays = ((time - prevTime) / (1000 * 60 * 60 * 24)).toInt()
        if (diffDays == 1) {
          tempStreak++
        } else if (diffDays > 1) {
          tempStreak = 1
        }
      }
      prevTime = time
      if (tempStreak > longestStreak) {
        longestStreak = tempStreak
      }
    }
    if (currentStreak > longestStreak) {
      longestStreak = currentStreak
    }

    return StreakInfo(
      currentStreak = currentStreak,
      longestStreak = longestStreak,
      todayStatus = todayStatus,
      isMaintainedToday = isTodayMaintained
    )
  }

  // Feature 2: Smart Reminder update
  fun updateSmartReminderMinutes(minutes: Int) {
    viewModelScope.launch {
      repository.updateSmartReminderMinutes(minutes)
      AlarmScheduler.rescheduleAllTimetableAlarms(getApplication())
    }
  }

  // Feature 3: Daily Reflection (Note + Mood)
  fun saveDailyNote(note: String, mood: String) {
    viewModelScope.launch {
      repository.saveDailyNote(_selectedDate.value, note, mood)
    }
  }

  // Feature 6: Priority filtering & updating
  fun setPriorityFilter(priority: String?) {
    _priorityFilter.value = priority
  }

  fun updateTaskPriority(task: DayTaskEntity, priority: String) {
    viewModelScope.launch {
      repository.updateTask(task.copy(priority = priority))
    }
  }

  // Feature 7: Flexible Routine / "Today Only" customization
  fun updateTodayTaskOnly(task: DayTaskEntity) {
    viewModelScope.launch {
      repository.updateTask(task)
      val smartMins = userSettings.value?.smartReminderMinutes ?: 0
      AlarmScheduler.scheduleTodayTaskAlarm(getApplication(), task, smartMins)
    }
  }

  fun restoreTodayRoutineToDefault() {
    viewModelScope.launch {
      val date = TimeUtils.getTodayDateString()
      val cycleDay = currentCycleDay.value
      repository.restoreTodayTasksToDefault(date, cycleDay)
      val smartMins = userSettings.value?.smartReminderMinutes ?: 0
      AlarmScheduler.rescheduleAllTimetableAlarms(getApplication())
    }
  }

  // Meditation Actions
  fun startMeditation(
    type: MeditationType,
    durationMinutes: Int,
    chimeEnabled: Boolean? = null,
    voiceLanguage: String? = null
  ) {
    val settings = userSettings.value
    val name = settings?.userName?.trim()?.ifBlank { "मोहित" } ?: "मोहित"
    val chime = chimeEnabled ?: settings?.meditationChimeEnabled ?: true
    val lang = voiceLanguage ?: settings?.meditationVoiceLanguage ?: "HI"
    meditationManager.startSession(
      type = type,
      durationMinutes = durationMinutes,
      userName = name,
      chimeEnabled = chime,
      voiceLanguage = lang
    )
  }

  fun pauseMeditation() {
    meditationManager.pauseSession()
  }

  fun resumeMeditation() {
    meditationManager.resumeSession()
  }

  fun stopMeditation() {
    meditationManager.stopSession(savePartial = true)
  }

  fun dismissMeditationCompletion() {
    meditationManager.dismissCompletion()
  }

  fun updateUserName(name: String) {
    viewModelScope.launch {
      repository.updateUserName(name)
    }
  }

  fun updateMeditationSettings(chimeEnabled: Boolean, language: String) {
    viewModelScope.launch {
      repository.updateMeditationSettings(chimeEnabled, language)
    }
  }

  fun updateFeatureToggle(feature: String, enabled: Boolean) {
    viewModelScope.launch {
      repository.updateFeatureToggle(feature, enabled)
    }
  }

  fun applyEmergencyRecoveryMode(forTomorrow: Boolean) {
    viewModelScope.launch {
      val targetDate = if (forTomorrow) TimeUtils.shiftDate(todayDate, 1) else todayDate
      repository.applyEmergencyRecoveryTasks(targetDate)
    }
  }

  fun quickAddRoutineActivity(
    name: String,
    timeMinutes: Int,
    category: String,
    priority: String = "NORMAL"
  ) {
    viewModelScope.launch {
      val targetKey = RoutineUtils.resolveActivityKey(name)
      val existing = repository.getRoutineTemplateByActivityKey(targetKey)

      if (existing != null) {
        val updated = existing.copy(
          name = name.trim(),
          timeMinutes = timeMinutes,
          category = category.trim(),
          priority = priority,
          isActive = true
        )
        repository.updateRoutineTemplate(updated)
      } else {
        val maxOrder = routineTemplates.value.maxOfOrNull { it.orderIndex } ?: 0
        val template = RoutineTemplateEntity(
          activityKey = targetKey,
          name = name.trim(),
          timeMinutes = timeMinutes,
          category = category.trim(),
          priority = priority,
          orderIndex = maxOrder + 1,
          daysMask = 127,
          isActive = true
        )
        repository.insertRoutineTemplate(template)
      }
      repository.syncTodayTasksWithTemplates(todayDate)
      AlarmScheduler.rescheduleAllTimetableAlarms(getApplication())
    }
  }

  fun quickAddTodayTask(
    name: String,
    timeMinutes: Int,
    category: String,
    priority: String = "NORMAL"
  ) {
    viewModelScope.launch {
      val tasks = tasksForSelectedDate.value
      val maxOrder = tasks.maxOfOrNull { it.orderIndex } ?: 0
      val newTask = DayTaskEntity(
        date = _selectedDate.value,
        templateId = null,
        name = name.trim(),
        timeMinutes = timeMinutes,
        category = category.trim(),
        status = TaskStatus.MISSED,
        isExtra = true,
        orderIndex = maxOrder + 1,
        priority = priority
      )
      repository.insertTask(newTask)
      val smartMins = userSettings.value?.smartReminderMinutes ?: 0
      AlarmScheduler.scheduleTodayTaskAlarm(getApplication(), newTask, smartMins)
      LifeTrackerWidgetProvider.updateAllWidgets(getApplication())
    }
  }

  // Energy Mode Actions
  fun setDailyEnergyMode(mode: EnergyMode) {
    viewModelScope.launch {
      repository.updateDailyEnergyMode(mode.name, todayDate)
    }
  }

  // Recovery Day Actions
  fun activateRecoveryDay(forDate: String = _selectedDate.value) {
    viewModelScope.launch {
      com.example.recovery.RecoveryDayManager.activateRecoveryDay(
        getApplication(),
        forDate,
        _currentTimeMinutes.value
      )
      LifeTrackerWidgetProvider.updateAllWidgets(getApplication())
    }
  }

  fun restoreDefaultRoutine(forDate: String = _selectedDate.value) {
    viewModelScope.launch {
      val cycleDay = TimeUtils.calculateCycleDay(anchorDate.value, forDate)
      com.example.recovery.RecoveryDayManager.restoreDefaultRoutine(
        getApplication(),
        forDate,
        cycleDay
      )
      LifeTrackerWidgetProvider.updateAllWidgets(getApplication())
    }
  }

  // --- PERSONAL DASHBOARD BUILDER ACTIONS ---

  fun updateDashboardConfiguration(
    preset: DashboardPreset,
    orderedSections: List<DashboardSectionId>,
    disabledSections: Set<DashboardSectionId>
  ) {
    viewModelScope.launch {
      val orderStr = orderedSections.joinToString(",") { it.key }
      val disabledStr = disabledSections.joinToString(",") { it.key }
      repository.updateDashboardConfiguration(preset.key, orderStr, disabledStr)
    }
  }

  fun resetDashboardToDefault() {
    viewModelScope.launch {
      val defaultOrder = DashboardSectionId.defaultOrderedList.joinToString(",") { it.key }
      repository.updateDashboardConfiguration("CUSTOM", defaultOrder, "")
    }
  }


  // --- SYSTEM-WIDE OLED BLACK SCREEN MODE ACTIONS ---

  val isBlackScreenEnabled: StateFlow<Boolean> = userSettings.map {
    it?.isBlackScreenEnabled ?: false
  }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

  val isBlackScreenOverlayActive: StateFlow<Boolean> = userSettings.map {
    it?.isBlackScreenOverlayActive ?: false
  }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

  val isFloatingDotEnabled: StateFlow<Boolean> = userSettings.map {
    it?.isFloatingDotEnabled ?: true
  }.stateIn(viewModelScope, SharingStarted.Eagerly, true)

  fun setBlackScreenEnabled(enabled: Boolean) {
    viewModelScope.launch {
      repository.updateBlackScreenEnabled(enabled)
      if (!enabled) {
        repository.updateBlackScreenOverlayActive(false)
      }
      com.example.blackscreen.BlackScreenManager.syncServiceState(getApplication())
    }
  }

  fun setBlackScreenFloatingDotEnabled(enabled: Boolean) {
    viewModelScope.launch {
      repository.updateBlackScreenFloatingDotEnabled(enabled)
      com.example.blackscreen.BlackScreenManager.syncServiceState(getApplication())
    }
  }

  fun setBlackScreenDotPosition(x: Int, y: Int) {
    viewModelScope.launch {
      repository.updateBlackScreenDotPosition(x, y)
      com.example.blackscreen.BlackScreenManager.syncServiceState(getApplication())
    }
  }

  fun setBlackScreenDotSize(size: Int) {
    viewModelScope.launch {
      repository.updateBlackScreenDotSize(size)
      com.example.blackscreen.BlackScreenManager.syncServiceState(getApplication())
    }
  }

  fun setBlackScreenDotOpacity(opacity: Float) {
    viewModelScope.launch {
      repository.updateBlackScreenDotOpacity(opacity)
      com.example.blackscreen.BlackScreenManager.syncServiceState(getApplication())
    }
  }

  fun setBlackScreenActivationMethod(method: String) {
    viewModelScope.launch {
      repository.updateBlackScreenActivationMethod(method)
      com.example.blackscreen.BlackScreenManager.syncServiceState(getApplication())
    }
  }

  fun setBlackScreenExitGesture(gesture: String) {
    viewModelScope.launch {
      repository.updateBlackScreenExitGesture(gesture)
      com.example.blackscreen.BlackScreenManager.syncServiceState(getApplication())
    }
  }

  fun setBlackScreenGestureSensitivity(sensitivity: Float) {
    viewModelScope.launch {
      repository.updateBlackScreenGestureSensitivity(sensitivity)
      com.example.blackscreen.BlackScreenManager.syncServiceState(getApplication())
    }
  }

  fun setBlackScreenShowClock(show: Boolean) {
    viewModelScope.launch {
      repository.updateBlackScreenShowClock(show)
      com.example.blackscreen.BlackScreenManager.syncServiceState(getApplication())
    }
  }

  fun setBlackScreenClockFormat24(is24Hour: Boolean) {
    viewModelScope.launch {
      repository.updateBlackScreenClockFormat24(is24Hour)
      com.example.blackscreen.BlackScreenManager.syncServiceState(getApplication())
    }
  }

  fun setBlackScreenRestoreAfterUnlock(restore: Boolean) {
    viewModelScope.launch {
      repository.updateBlackScreenRestoreAfterUnlock(restore)
    }
  }

  fun setBlackScreenRestoreAfterReboot(restore: Boolean) {
    viewModelScope.launch {
      repository.updateBlackScreenRestoreAfterReboot(restore)
    }
  }

  fun activateBlackScreenOverlay() {
    viewModelScope.launch {
      repository.updateBlackScreenEnabled(true)
      repository.updateBlackScreenOverlayActive(true)
      com.example.blackscreen.BlackScreenManager.activateBlackOverlay(getApplication())
    }
  }

  fun deactivateBlackScreenOverlay() {
    viewModelScope.launch {
      repository.updateBlackScreenOverlayActive(false)
      com.example.blackscreen.BlackScreenManager.deactivateBlackOverlay(getApplication())
    }
  }

  // =========================================================================
  // MY CONTROL — PRIVATE HABIT & SELF-CONTROL TRACKER
  // =========================================================================

  val myControlDays = database.myControlDao().getAllDaysFlow().stateIn(
    viewModelScope,
    SharingStarted.WhileSubscribed(5000),
    emptyList()
  )

  val myControlEvents = database.myControlDao().getAllEventsFlow().stateIn(
    viewModelScope,
    SharingStarted.WhileSubscribed(5000),
    emptyList()
  )

  val myControlConfig: StateFlow<com.example.data.model.MyControlConfigEntity> = database.myControlDao().getConfigFlow()
    .map { it ?: com.example.data.model.MyControlConfigEntity() }
    .stateIn(
      viewModelScope,
      SharingStarted.WhileSubscribed(5000),
      com.example.data.model.MyControlConfigEntity()
    )

  val myControlStats: StateFlow<com.example.data.model.MyControlStats> = kotlinx.coroutines.flow.combine(
    myControlDays,
    _selectedDate
  ) { days, _ ->
    com.example.util.MyControlStreakCalculator.calculateStats(days, todayDate)
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.example.data.model.MyControlStats())

  fun recordMyControlCheckIn(
    date: String,
    status: com.example.data.model.GoalStatus,
    note: String?,
    isRetrospective: Boolean = false
  ) {
    viewModelScope.launch(Dispatchers.IO) {
      database.myControlDao().recordDailyCheckIn(
        date = date,
        status = status,
        note = note?.trim(),
        isRetrospective = isRetrospective,
        timezoneId = java.util.TimeZone.getDefault().id
      )
    }
  }

  fun recordUrgeCheckIn(
    date: String = todayDate,
    intensity: Int,
    trigger: String?,
    strategy: String?,
    note: String?
  ) {
    viewModelScope.launch(Dispatchers.IO) {
      database.myControlDao().recordUrgeCheckIn(
        date = date,
        intensity = intensity,
        trigger = trigger,
        strategy = strategy,
        note = note?.trim(),
        timezoneId = java.util.TimeZone.getDefault().id
      )
    }
  }

  fun updateMyControlReminder(enabled: Boolean, timeMinutes: Int) {
    viewModelScope.launch(Dispatchers.IO) {
      val current = database.myControlDao().getConfigSync() ?: com.example.data.model.MyControlConfigEntity()
      database.myControlDao().upsertConfig(
        current.copy(reminderEnabled = enabled, reminderTimeMinutes = timeMinutes)
      )
      if (enabled) {
        AlarmScheduler.scheduleMyControlReminder(getApplication(), timeMinutes)
      } else {
        AlarmScheduler.cancelMyControlReminder(getApplication())
      }
    }
  }

  fun updateMyControlPin(enabled: Boolean, pin: String) {
    viewModelScope.launch(Dispatchers.IO) {
      val current = database.myControlDao().getConfigSync() ?: com.example.data.model.MyControlConfigEntity()
      database.myControlDao().upsertConfig(
        current.copy(privacyPinEnabled = enabled, privacyPin = pin)
      )
    }
  }

  fun clearMyControlData() {
    viewModelScope.launch(Dispatchers.IO) {
      database.myControlDao().clearAllDays()
      database.myControlDao().clearAllEvents()
    }
  }

  suspend fun exportMyControlJson(): String {
    return kotlinx.coroutines.withContext(Dispatchers.IO) {
      val days = database.myControlDao().getAllDaysSync()
      val events = database.myControlDao().getAllEventsSync()
      val config = database.myControlDao().getConfigSync()

      val json = org.json.JSONObject()
      json.put("appName", "Life Tracker - My Control")
      json.put("exportedAtEpochMs", System.currentTimeMillis())
      json.put("goalTitle", config?.goalTitle ?: "Daily Self-Control")

      val daysArray = org.json.JSONArray()
      for (d in days) {
        val o = org.json.JSONObject()
        o.put("date", d.date)
        o.put("status", d.status.name)
        o.put("note", d.note ?: "")
        o.put("updatedAtEpochMs", d.updatedAtEpochMs)
        o.put("isRetrospective", d.isRetrospective)
        daysArray.put(o)
      }
      json.put("days", daysArray)

      val eventsArray = org.json.JSONArray()
      for (e in events) {
        val o = org.json.JSONObject()
        o.put("id", e.id)
        o.put("eventType", e.eventType.name)
        o.put("date", e.date)
        o.put("timestampEpochMs", e.timestampEpochMs)
        o.put("status", e.status?.name ?: "")
        o.put("previousStatus", e.previousStatus?.name ?: "")
        o.put("note", e.note ?: "")
        o.put("urgeIntensity", e.urgeIntensity ?: 0)
        o.put("urgeTrigger", e.urgeTrigger ?: "")
        o.put("urgeStrategy", e.urgeStrategy ?: "")
        eventsArray.put(o)
      }
      json.put("events", eventsArray)

      json.toString(2)
    }
  }
}

data class OverallStats(
  val totalDays: Int = 0,
  val totalTasks: Int = 0,
  val completedTasks: Int = 0,
  val partialTasks: Int = 0,
  val missedTasks: Int = 0,
  val totalScore: Float = 0f
)

data class CategoryAggregate(
  val category: String,
  val totalCount: Int,
  val completedCount: Int,
  val partialCount: Int
)

data class CycleDayAverage(
  val dayOfCycle: Int,
  val avgPercentage: Double,
  val dayCount: Int
)
