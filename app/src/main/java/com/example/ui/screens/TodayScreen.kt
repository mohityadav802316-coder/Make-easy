package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DayTaskEntity
import com.example.data.model.TaskStatus
import com.example.ui.components.ChronoTimeBlockDialog
import com.example.ui.components.DashboardBuilderBottomSheet
import com.example.ui.components.FullDayTimelineBottomSheet
import com.example.ui.components.MoreHubBottomSheet
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.GoldBrass
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianBorderSubtle
import com.example.ui.theme.ObsidianCharcoal
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.SageGreen
import com.example.ui.theme.WarmMuted
import com.example.ui.theme.WarmOffWhite
import com.example.ui.theme.WarmParchment
import com.example.ui.viewmodel.LifeTrackerViewModel
import com.example.ui.viewmodel.MainTab
import com.example.util.TimeUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
  viewModel: LifeTrackerViewModel,
  modifier: Modifier = Modifier
) {
  val selectedDate by viewModel.selectedDate.collectAsState()
  val tasks by viewModel.tasksForSelectedDate.collectAsState()
  val streakInfo by viewModel.streakInfo.collectAsState()
  val userName by viewModel.userName.collectAsState()
  val isAlarmEnabled by viewModel.isAlarmEnabled.collectAsState()
  val nowMinutes by viewModel.currentTimeMinutes.collectAsState()
  val dailyEnergyMode by viewModel.dailyEnergyMode.collectAsState()
  val userSettings by viewModel.userSettings.collectAsState()
  val todayTotalSpend by viewModel.todayTotalSpend.collectAsState()
  val monthlyBudget by viewModel.monthlyBudget.collectAsState()
  val currentMonthTotalSpend by viewModel.currentMonthTotalSpend.collectAsState()
  val currencySymbol by viewModel.currencySymbol.collectAsState()

  val todayRemainingBudget = remember(todayTotalSpend, monthlyBudget, currentMonthTotalSpend) {
    val cal = java.util.Calendar.getInstance()
    val curDay = cal.get(java.util.Calendar.DAY_OF_MONTH)
    val maxDays = cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
    val daysLeft = (maxDays - curDay + 1).coerceAtLeast(1)
    val remMonthly = (monthlyBudget - currentMonthTotalSpend).coerceAtLeast(0.0)
    val dailyQuota = remMonthly / daysLeft
    (dailyQuota - todayTotalSpend).coerceAtLeast(0.0)
  }

  val isToday = selectedDate == viewModel.todayDate

  // Sorted routine tasks (with safety deduplication so UI never shows doubles)
  val sortedRoutineTasks = remember(tasks) {
    tasks.filter { !it.isExtra }
      .groupBy { if (it.templateId != null) "t_${it.templateId}" else "n_${it.name.trim().lowercase()}_${it.timeMinutes}" }
      .values
      .map { group ->
        group.sortedWith(
          compareByDescending<DayTaskEntity> { it.status.score }
            .thenBy { it.id }
        ).first()
      }
      .sortedBy { it.timeMinutes }
  }

  // 1. Identify CURRENT running activity or NEXT upcoming activity
  val currentRunningTask = remember(sortedRoutineTasks, isToday, nowMinutes) {
    if (!isToday || sortedRoutineTasks.isEmpty()) null
    else {
      sortedRoutineTasks.firstOrNull { task ->
        val nextTask = sortedRoutineTasks.firstOrNull { it.timeMinutes > task.timeMinutes }
        val taskEndTime = nextTask?.timeMinutes ?: (task.timeMinutes + 60)
        nowMinutes in task.timeMinutes until taskEndTime && task.status != TaskStatus.COMPLETE
      }
    }
  }

  val heroTask = remember(sortedRoutineTasks, currentRunningTask, isToday, nowMinutes) {
    if (currentRunningTask != null) {
      currentRunningTask
    } else if (isToday) {
      // Find next upcoming uncompleted task
      sortedRoutineTasks.firstOrNull { it.timeMinutes >= nowMinutes && it.status != TaskStatus.COMPLETE }
        ?: sortedRoutineTasks.firstOrNull { it.status != TaskStatus.COMPLETE }
    } else {
      sortedRoutineTasks.firstOrNull { it.status != TaskStatus.COMPLETE }
        ?: sortedRoutineTasks.firstOrNull()
    }
  }

  val isCurrentRunning = currentRunningTask != null && heroTask?.id == currentRunningTask.id

  // 2. Next 3 upcoming activities for the minimal "आगे" list
  val upcomingThree = remember(sortedRoutineTasks, heroTask, nowMinutes, isToday) {
    if (heroTask == null) {
      emptyList()
    } else {
      sortedRoutineTasks
        .filter { it.id != heroTask.id && (it.timeMinutes > heroTask.timeMinutes || it.status != TaskStatus.COMPLETE) }
        .take(3)
    }
  }

  // 3. Day Progress Calculation (count ONLY really completed tasks)
  val completedTasksCount = remember(tasks) {
    tasks.count { it.status == TaskStatus.COMPLETE }
  }
  val totalTasksCount = remember(tasks) { tasks.size }
  val progressFraction = remember(completedTasksCount, totalTasksCount) {
    if (totalTasksCount > 0) completedTasksCount.toFloat() / totalTasksCount else 0f
  }
  val animatedProgress by animateFloatAsState(
    targetValue = progressFraction,
    animationSpec = tween(durationMillis = 600),
    label = "day_progress_anim"
  )

  // Sheet and Dialog states
  var showFullDayTimelineSheet by remember { mutableStateOf(false) }
  var showMoreHubSheet by remember { mutableStateOf(false) }
  var editingTask by remember { mutableStateOf<DayTaskEntity?>(null) }
  var isAddingTask by remember { mutableStateOf(false) }
  var showDashboardBuilderSheet by remember { mutableStateOf(false) }

  var isDoneAnimating by remember { mutableStateOf(false) }
  val coroutineScope = rememberCoroutineScope()

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(DarkBackground)
      .statusBarsPadding()
  ) {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .testTag("today_screen_root"),
      contentPadding = PaddingValues(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 32.dp),
      verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
      // =========================================================================
      // 1. HEADER: Date line, Greeting, and Quiet Top Actions
      // =========================================================================
      item(key = "today_header") {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f, fill = false)
          ) {
            // In-App Profile / Logo Image with subtle brass border
            Image(
              painter = painterResource(id = R.drawable.ic_profile_logo),
              contentDescription = "my mohit profile",
              contentScale = ContentScale.Crop,
              modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .border(1.5.dp, GoldBrass, CircleShape)
                .clickable { viewModel.setTab(MainTab.SETTINGS) }
                .testTag("in_app_profile_logo")
            )

            Column {
              // Exact lowercase in-app branding title
              Text(
                text = "my mohit",
                color = GoldBrass,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                modifier = Modifier.testTag("in_app_brand_title")
              )
              Spacer(modifier = Modifier.height(1.dp))
              val greeting = TimeUtils.getHindiGreeting(nowMinutes)
              val greetingText = if (userName.isNotBlank()) "$greeting, $userName" else greeting
              Text(
                text = greetingText,
                color = WarmOffWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
              )
              Text(
                text = TimeUtils.getHindiDateDisplay(selectedDate),
                color = WarmMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal
              )
            }
          }

          // Top right actions: Alarm bell and More hub
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            IconButton(
              onClick = { viewModel.setTab(MainTab.ALARM_CENTER) },
              modifier = Modifier
                .size(36.dp)
                .testTag("header_alarm_btn")
            ) {
              Icon(
                imageVector = if (isAlarmEnabled) Icons.Filled.Alarm else Icons.Outlined.Alarm,
                contentDescription = "Alarm Center",
                tint = if (isAlarmEnabled) GoldBrass else WarmMuted,
                modifier = Modifier.size(20.dp)
              )
            }

            IconButton(
              onClick = { showMoreHubSheet = true },
              modifier = Modifier
                .size(36.dp)
                .testTag("header_more_hub_btn")
            ) {
              Icon(
                imageVector = Icons.Default.MoreHoriz,
                contentDescription = "More Hub",
                tint = WarmParchment,
                modifier = Modifier.size(22.dp)
              )
            }
          }
        }
      }


      if (totalTasksCount > 0) {
        // =========================================================================
        // 2. DAY PROGRESS: "आज की प्रगति", "4 / 12" and 4dp rounded gold bar
        // =========================================================================
        item(key = "today_progress") {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .testTag("today_progress_section"),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "आज की प्रगति",
                color = WarmParchment,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
              )
              Text(
                text = "$completedTasksCount / $totalTasksCount",
                color = WarmOffWhite,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
              )
            }

            LinearProgressIndicator(
              progress = { animatedProgress },
              modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
              color = GoldBrass,
              trackColor = ObsidianBorderSubtle
            )
          }
        }
      }

      // =========================================================================
      // 3. "अभी" HERO CARD: 20dp radius, soft gold tint, title, progress & button
      // =========================================================================
      item(key = "today_hero_card") {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(ObsidianElevated)
            .border(1.dp, GoldBrass.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .padding(20.dp)
            .testTag("hero_activity_card")
        ) {
          // Subtle soft gold tint background
          Box(
            modifier = Modifier
              .matchParentSize()
              .background(GoldBrass.copy(alpha = 0.05f))
          )

          Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            if (sortedRoutineTasks.isEmpty()) {
              // Empty State when user has not added any routines yet
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 12.dp)
                  .testTag("today_empty_routine_view"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                Box(
                  modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(GoldBrass.copy(alpha = 0.12f)),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = GoldBrass,
                    modifier = Modifier.size(28.dp)
                  )
                }

                Text(
                  text = "अभी कोई routine नहीं है",
                  color = WarmOffWhite,
                  fontSize = 18.sp,
                  fontWeight = FontWeight.SemiBold,
                  textAlign = TextAlign.Center
                )

                Text(
                  text = "Settings → Routine & Schedule में जाकर अपना routine बनाएं।",
                  color = WarmMuted,
                  fontSize = 13.sp,
                  textAlign = TextAlign.Center,
                  lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                  onClick = { viewModel.setTab(MainTab.SETTINGS) },
                  colors = ButtonDefaults.buttonColors(
                    containerColor = GoldBrass,
                    contentColor = ObsidianCharcoal
                  ),
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier
                    .fillMaxWidth()
                    .testTag("create_routine_settings_btn")
                ) {
                  Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "Routine बनाएं (Go to Settings)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                  )
                }
              }
            } else if (heroTask != null) {
              // Top line: Label ("अभी" or "अगला") and Time Range
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = if (isCurrentRunning) "अभी" else "अगला",
                  color = GoldBrass,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 1.sp
                )

                val nextTask = sortedRoutineTasks.firstOrNull { it.timeMinutes > heroTask.timeMinutes }
                val taskEndTime = nextTask?.timeMinutes ?: (heroTask.timeMinutes + 60)
                Text(
                  text = "${TimeUtils.minutesTo12Hour(heroTask.timeMinutes)} – ${TimeUtils.minutesTo12Hour(taskEndTime)}",
                  color = WarmMuted,
                  fontSize = 13.sp
                )
              }

              // Activity Title (20sp medium)
              Text(
                text = heroTask.name,
                color = WarmOffWhite,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
              )

              // Thin progress line showing elapsed portion of block
              if (isCurrentRunning) {
                val nextTask = sortedRoutineTasks.firstOrNull { it.timeMinutes > heroTask.timeMinutes }
                val taskEndTime = nextTask?.timeMinutes ?: (heroTask.timeMinutes + 60)
                val duration = (taskEndTime - heroTask.timeMinutes).coerceAtLeast(15)
                val elapsed = (nowMinutes - heroTask.timeMinutes).coerceAtLeast(0)
                val fraction = (elapsed.toFloat() / duration).coerceIn(0f, 1f)

                LinearProgressIndicator(
                  progress = { fraction },
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(1.5.dp)),
                  color = GoldBrass,
                  trackColor = ObsidianBorderSubtle
                )
              }

              // ONE full-width primary button "हो गया"
              Button(
                onClick = {
                  if (!isDoneAnimating) {
                    isDoneAnimating = true
                    coroutineScope.launch {
                      delay(220)
                      viewModel.updateTaskStatus(heroTask, TaskStatus.COMPLETE)
                      isDoneAnimating = false
                    }
                  }
                },
                modifier = Modifier
                  .fillMaxWidth()
                  .height(48.dp)
                  .testTag("hero_task_done_btn"),
                colors = ButtonDefaults.buttonColors(
                  containerColor = if (isDoneAnimating) SageGreen else GoldBrass,
                  contentColor = ObsidianCharcoal
                ),
                shape = RoundedCornerShape(14.dp)
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.Center
                ) {
                  Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = if (isDoneAnimating) "संपन्न! ✓" else "हो गया",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                  )
                }
              }
            } else {
              // All tasks completed for the day
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Text("✨", fontSize = 28.sp)
                Text(
                  text = "आज के सभी कार्य संपन्न!",
                  color = WarmOffWhite,
                  fontSize = 18.sp,
                  fontWeight = FontWeight.Medium
                )
                Text(
                  text = "शानदार दिनचर्या! विश्राम करें या पूरा दिन की समीक्षा करें।",
                  color = WarmMuted,
                  fontSize = 13.sp
                )
              }
            }
          }
        }
      }

      if (sortedRoutineTasks.isNotEmpty()) {
        // =========================================================================
        // 4. "आगे" SECTION: Next 3 activities + quiet "पूरा दिन देखें" link
        // =========================================================================
        item(key = "today_upcoming_section") {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .testTag("upcoming_activities_section"),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "आगे",
              color = WarmParchment,
              fontSize = 15.sp,
              fontWeight = FontWeight.SemiBold
            )

            TextButton(
              onClick = { showFullDayTimelineSheet = true },
              contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
              modifier = Modifier.testTag("view_full_day_link")
            ) {
              Text(
                text = "पूरा दिन देखें →",
                color = GoldBrass,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
              )
            }
          }

          if (upcomingThree.isNotEmpty()) {
            Column(
              verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
              upcomingThree.forEach { task ->
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showFullDayTimelineSheet = true }
                    .padding(vertical = 4.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  // Small grey dot
                  Box(
                    modifier = Modifier
                      .size(6.dp)
                      .clip(CircleShape)
                      .background(WarmMuted)
                  )

                  Spacer(modifier = Modifier.width(14.dp))

                  // Time in muted text
                  Text(
                    text = TimeUtils.minutesTo12Hour(task.timeMinutes),
                    color = WarmMuted,
                    fontSize = 13.sp,
                    modifier = Modifier.width(68.dp)
                  )

                  // Title
                  Text(
                    text = task.name,
                    color = WarmOffWhite,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                  )
                }
              }
            }
          } else {
            Text(
              text = "आगे कोई लंबित कार्य नहीं है",
              color = WarmMuted,
              fontSize = 13.sp
            )
          }
        }
      }
      }

      // Quiet link to open More hub
      item(key = "today_more_hub_quiet_link") {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 8.dp),
          horizontalArrangement = Arrangement.Center
        ) {
          TextButton(
            onClick = { showMoreHubSheet = true },
            modifier = Modifier.testTag("quiet_more_hub_link")
          ) {
            Text(
              text = "अधिक सुविधाएं व टूल्स •••",
              color = WarmMuted,
              fontSize = 13.sp,
              fontWeight = FontWeight.Normal
            )
          }
        }
      }
    }
  }

  // =========================================================================
  // BOTTOM SHEETS & DIALOGS
  // =========================================================================

  // 1. Full-Day Timeline Bottom Sheet (Grouped by morning, afternoon, evening, night)
  if (showFullDayTimelineSheet) {
    FullDayTimelineBottomSheet(
      tasks = tasks,
      nowMinutes = nowMinutes,
      isToday = isToday,
      selectedDate = selectedDate,
      onDismiss = { showFullDayTimelineSheet = false },
      onStatusChange = { task, status -> viewModel.updateTaskStatus(task, status) },
      onEditTask = { task -> editingTask = task },
      onDeleteTask = { task -> viewModel.deleteTask(task.id) },
      onAddTask = { isAddingTask = true }
    )
  }

  // 2. "More" Hub Bottom Sheet (Notebook, Meditation, Music, Streak, Energy Mode, etc.)
  if (showMoreHubSheet) {
    MoreHubBottomSheet(
      streakInfo = streakInfo,
      dailyEnergyMode = dailyEnergyMode,
      onEnergyModeChange = { viewModel.setDailyEnergyMode(it) },
      onDismiss = { showMoreHubSheet = false },
      onNavigateTab = { tab ->
        showMoreHubSheet = false
        viewModel.setTab(tab)
      },
      onOpenCustomize = {
        showMoreHubSheet = false
        showDashboardBuilderSheet = true
      }
    )
  }

  // 3. Precision Time Block Add / Edit Dialog
  if (isAddingTask || editingTask != null) {
    ChronoTimeBlockDialog(
      initialTask = editingTask,
      initialTimeMinutes = nowMinutes,
      onDismiss = {
        editingTask = null
        isAddingTask = false
      },
      onSave = { name, timeMins, cat, notes ->
        viewModel.saveTask(
          id = editingTask?.id ?: 0L,
          name = name,
          timeMinutes = timeMins,
          category = cat,
          notes = notes,
          isExtra = false
        )
        editingTask = null
        isAddingTask = false
      }
    )
  }

  // 8. Personal Dashboard Builder Sheet
  if (showDashboardBuilderSheet) {
    val currentPresetKey = userSettings?.dashboardPreset ?: "CUSTOM"
    val currentOrderKeys = userSettings?.dashboardOrder ?: ""
    val currentDisabledKeys = userSettings?.dashboardDisabled ?: ""
    DashboardBuilderBottomSheet(
      sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
      currentPresetKey = currentPresetKey,
      currentOrderKeys = currentOrderKeys,
      currentDisabledKeys = currentDisabledKeys,
      onApply = { preset, ordered, disabled ->
        viewModel.updateDashboardConfiguration(preset, ordered, disabled)
        showDashboardBuilderSheet = false
      },
      onResetDefault = {
        viewModel.resetDashboardToDefault()
        showDashboardBuilderSheet = false
      },
      onDismiss = { showDashboardBuilderSheet = false }
    )
  }
}
