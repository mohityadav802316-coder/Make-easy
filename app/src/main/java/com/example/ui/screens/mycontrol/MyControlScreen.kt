package com.example.ui.screens.mycontrol

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GoalStatus
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.GoldBrass
import com.example.ui.theme.GoldHighlight
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianBorderSubtle
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.SageGreen
import com.example.ui.theme.WarmMuted
import com.example.ui.theme.WarmOffWhite
import com.example.ui.theme.WarmParchment
import com.example.ui.viewmodel.LifeTrackerViewModel
import com.example.util.MyControlStreakCalculator
import com.example.util.TimeUtils
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private enum class MyControlSubTab(val titleHi: String, val titleEn: String) {
  DASHBOARD("डैशबोर्ड", "Dashboard"),
  CALENDAR("कैलेंडर व इतिहास", "Calendar"),
  ANALYTICS("प्रगति व विश्लेषण", "Analytics"),
  SETTINGS("सेटिंग्स", "Settings")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyControlScreen(
  viewModel: LifeTrackerViewModel,
  modifier: Modifier = Modifier
) {
  // Handle Android Back button
  BackHandler {
    viewModel.navigateBack()
  }

  val config by viewModel.myControlConfig.collectAsState()
  val days by viewModel.myControlDays.collectAsState()
  val events by viewModel.myControlEvents.collectAsState()
  val stats by viewModel.myControlStats.collectAsState()

  // PIN lock session handling
  var isUnlockedInSession by remember(config.privacyPinEnabled) {
    mutableStateOf(!config.privacyPinEnabled || config.privacyPin.isBlank())
  }

  // Modals state
  var showCheckInDialog by remember { mutableStateOf(false) }
  var checkInTargetDate by remember { mutableStateOf(viewModel.todayDate) }
  var showUrgeBottomSheet by remember { mutableStateOf(false) }

  // Sub tab navigation
  var selectedSubTab by remember { mutableStateOf(MyControlSubTab.DASHBOARD) }

  if (!isUnlockedInSession) {
    MyControlPinOverlay(
      correctPin = config.privacyPin,
      onUnlocked = { isUnlockedInSession = true },
      onCancel = { viewModel.navigateBack() }
    )
    return
  }

  Scaffold(
    modifier = modifier
      .fillMaxSize()
      .background(DarkBackground),
    containerColor = DarkBackground
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .statusBarsPadding()
        .padding(horizontal = 16.dp)
    ) {
      // 1. Top Header Bar
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(onClick = { viewModel.navigateBack() }, modifier = Modifier.size(36.dp)) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back",
              tint = WarmOffWhite
            )
          }
          Spacer(modifier = Modifier.width(6.dp))
          Column {
            Text(
              text = "MY CONTROL",
              color = CyanNeon,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.sp
            )
            Text(
              text = "निजी आदत एवं आत्म-नियंत्रण",
              color = WarmOffWhite,
              fontSize = 17.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          // Urge SOS Button in header
          Button(
            onClick = { showUrgeBottomSheet = true },
            colors = ButtonDefaults.buttonColors(containerColor = CyanNeon.copy(alpha = 0.2f)),
            shape = RoundedCornerShape(12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp)
          ) {
            Icon(Icons.Default.Security, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Urge SOS", color = CyanNeon, fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }
        }
      }

      // 2. Sub Tabs Row
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        MyControlSubTab.entries.forEach { tab ->
          val isSelected = selectedSubTab == tab
          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(10.dp))
              .background(if (isSelected) CyanNeon.copy(alpha = 0.2f) else ObsidianElevated)
              .border(1.dp, if (isSelected) CyanNeon else ObsidianBorderSubtle, RoundedCornerShape(10.dp))
              .clickable { selectedSubTab = tab }
              .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = tab.titleHi,
              color = if (isSelected) CyanNeon else WarmMuted,
              fontSize = 11.sp,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
              maxLines = 1
            )
          }
        }
      }

      // 3. Tab Content
      Crossfade(targetState = selectedSubTab, label = "my_control_tabs") { tab ->
        when (tab) {
          MyControlSubTab.DASHBOARD -> {
            MyControlDashboardView(
              stats = stats,
              days = days,
              events = events,
              onOpenCheckIn = { date ->
                checkInTargetDate = date
                showCheckInDialog = true
              },
              onOpenUrgeSheet = { showUrgeBottomSheet = true },
              onViewAllHistory = { selectedSubTab = MyControlSubTab.CALENDAR }
            )
          }
          MyControlSubTab.CALENDAR -> {
            MyControlCalendarHistoryView(
              days = days,
              events = events,
              onDateSelectedForCheckIn = { date ->
                checkInTargetDate = date
                showCheckInDialog = true
              }
            )
          }
          MyControlSubTab.ANALYTICS -> {
            MyControlAnalyticsView(
              stats = stats,
              days = days,
              events = events
            )
          }
          MyControlSubTab.SETTINGS -> {
            MyControlSettingsView(
              config = config,
              onUpdateReminder = { enabled, mins ->
                viewModel.updateMyControlReminder(enabled, mins)
              },
              onUpdatePin = { enabled, pin ->
                viewModel.updateMyControlPin(enabled, pin)
              },
              onClearData = {
                viewModel.clearMyControlData()
              },
              onExportJson = {
                viewModel.exportMyControlJson()
              }
            )
          }
        }
      }
    }
  }

  // Daily Check-in Modal
  if (showCheckInDialog) {
    MyControlCheckInDialog(
      initialDate = checkInTargetDate,
      existingDays = days,
      onDismiss = { showCheckInDialog = false },
      onSave = { date, status, note, isRetrospective ->
        viewModel.recordMyControlCheckIn(date, status, note, isRetrospective)
      }
    )
  }

  // Urge Coping Bottom Sheet
  if (showUrgeBottomSheet) {
    MyControlUrgeBottomSheet(
      onDismiss = { showUrgeBottomSheet = false },
      onSaveUrgeLog = { intensity, trigger, strategy, note ->
        viewModel.recordUrgeCheckIn(
          date = viewModel.todayDate,
          intensity = intensity,
          trigger = trigger,
          strategy = strategy,
          note = note
        )
      }
    )
  }
}

@Composable
private fun MyControlDashboardView(
  stats: com.example.data.model.MyControlStats,
  days: List<com.example.data.model.MyControlDayEntity>,
  events: List<com.example.data.model.MyControlEventEntity>,
  onOpenCheckIn: (String) -> Unit,
  onOpenUrgeSheet: () -> Unit,
  onViewAllHistory: () -> Unit
) {
  val todayStr = remember { LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) }
  val yesterdayStr = remember { LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE) }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    // A. Streak Hero Card
    item {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(22.dp))
          .background(ObsidianCard)
          .border(1.5.dp, CyanNeon.copy(alpha = 0.5f), RoundedCornerShape(22.dp))
          .padding(20.dp)
      ) {
        Column(
          modifier = Modifier.fillMaxWidth(),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(24.dp)
                  .clip(CircleShape)
                  .background(CyanNeon.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
              ) {
                Icon(Icons.Default.Security, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(14.dp))
              }
              Spacer(modifier = Modifier.width(6.dp))
              Text("सक्रिय आत्म-नियंत्रण स्ट्रीक", color = WarmParchment, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Text("लक्ष्य: 100% व्यक्तिगत", color = WarmMuted, fontSize = 11.sp)
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Big Counter
          Row(verticalAlignment = Alignment.Bottom) {
            Text(
              text = "${stats.currentStreak}",
              color = CyanNeon,
              fontSize = 54.sp,
              fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "दिन (Days)",
              color = WarmOffWhite,
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(bottom = 10.dp)
            )
          }

          Text(
            text = "सफल पूर्ण किए गए स्थानीय कैलेंडर दिन",
            color = WarmMuted,
            fontSize = 12.sp
          )

          Spacer(modifier = Modifier.height(16.dp))

          // Secondary metrics row
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
          ) {
            MetricPill(label = "सर्वश्रेष्ठ", value = "${stats.longestStreak} दिन", color = GoldHighlight)
            MetricPill(label = "कुल चेक-इन", value = "${stats.totalCheckIns}", color = WarmOffWhite)
            MetricPill(label = "सफलता दर", value = "${stats.successRatePercent.toInt()}%", color = SageGreen)
          }
        }
      }
    }

    // B. Yesterday Pending Review Alert (If yesterday is pending)
    if (stats.isYesterdayPending) {
      item {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GoldBrass.copy(alpha = 0.15f))
            .border(1.dp, GoldBrass, RoundedCornerShape(16.dp))
            .padding(14.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text("कल का चेक-इन बाकी है", color = GoldHighlight, fontSize = 13.sp, fontWeight = FontWeight.Bold)
              Text("ईमानदार समीक्षा दर्ज करें ताकि स्ट्रीक अपडेट रहे", color = WarmOffWhite, fontSize = 11.sp)
            }
            Button(
              onClick = { onOpenCheckIn(yesterdayStr) },
              colors = ButtonDefaults.buttonColors(containerColor = GoldBrass),
              shape = RoundedCornerShape(10.dp)
            ) {
              Text("चेक-इन करें", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }

    // C. Today's Status Card
    item {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(18.dp))
          .background(ObsidianCard)
          .border(1.dp, ObsidianBorder, RoundedCornerShape(18.dp))
          .padding(16.dp)
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("आज की स्थिति (Today's Status)", color = WarmOffWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            StatusBadge(status = stats.todayStatus)
          }

          when (stats.todayStatus) {
            GoalStatus.PENDING -> {
              Text(
                text = "आज का दिन अभी प्रगति पर है। दिन के अंत में या जब आप चाहें, अपनी प्रगति दर्ज करें।",
                color = WarmMuted,
                fontSize = 12.sp,
                lineHeight = 16.sp
              )
              Button(
                onClick = { onOpenCheckIn(todayStr) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = CyanNeon),
                shape = RoundedCornerShape(12.dp)
              ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("आज का चेक-इन दर्ज करें (Check-in Today)", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
              }
            }
            GoalStatus.COMPLETED -> {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "शानदार! आज का लक्ष्य सफलतापूर्वक पूरा दर्ज किया गया है।",
                  color = SageGreen,
                  fontSize = 12.sp,
                  modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { onOpenCheckIn(todayStr) }) {
                  Text("संशोधित करें", color = CyanNeon, fontSize = 12.sp)
                }
              }
            }
            GoalStatus.GOAL_NOT_MET -> {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "कोई बात नहीं। हर नया दिन एक नई शुरुआत लाता है।",
                  color = Color(0xFFFFB74D),
                  fontSize = 12.sp,
                  modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { onOpenCheckIn(todayStr) }) {
                  Text("संशोधित करें", color = CyanNeon, fontSize = 12.sp)
                }
              }
            }
          }
        }
      }
    }

    // D. Quick Action Buttons
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Button(
          onClick = { onOpenCheckIn(todayStr) },
          colors = ButtonDefaults.buttonColors(containerColor = ObsidianElevated),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.weight(1f)
        ) {
          Icon(Icons.Default.Edit, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("दैनिक चेक-इन", color = WarmOffWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        Button(
          onClick = onOpenUrgeSheet,
          colors = ButtonDefaults.buttonColors(containerColor = CyanNeon.copy(alpha = 0.2f)),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.weight(1f)
        ) {
          Icon(Icons.Default.Security, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Urge SOS", color = CyanNeon, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
      }
    }

    // E. 7-Day Visual Pill Row
    item {
      val weeklySummary = remember(days) { MyControlStreakCalculator.getWeeklySummary(days, todayStr) }
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(16.dp))
          .background(ObsidianCard)
          .border(1.dp, ObsidianBorder, RoundedCornerShape(16.dp))
          .padding(14.dp)
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text("पिछले 7 दिन (Past 7 Days)", color = WarmOffWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            weeklySummary.forEach { (date, status) ->
              val dayLabel = try {
                LocalDate.parse(date).dayOfWeek.name.take(2)
              } catch (_: Exception) {
                ""
              }
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                Text(dayLabel, color = WarmMuted, fontSize = 10.sp)
                Box(
                  modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                      when (status) {
                        GoalStatus.COMPLETED -> SageGreen.copy(alpha = 0.3f)
                        GoalStatus.GOAL_NOT_MET -> Color(0xFFFFB74D).copy(alpha = 0.3f)
                        GoalStatus.PENDING -> ObsidianElevated
                      }
                    )
                    .border(
                      1.dp,
                      when (status) {
                        GoalStatus.COMPLETED -> SageGreen
                        GoalStatus.GOAL_NOT_MET -> Color(0xFFFFB74D)
                        GoalStatus.PENDING -> ObsidianBorderSubtle
                      },
                      CircleShape
                    ),
                  contentAlignment = Alignment.Center
                ) {
                  if (status == GoalStatus.COMPLETED) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = SageGreen, modifier = Modifier.size(14.dp))
                  } else if (status == GoalStatus.GOAL_NOT_MET) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFFFFB74D), modifier = Modifier.size(14.dp))
                  }
                }
              }
            }
          }
        }
      }
    }

    // F. Recent Activity Header & Button
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("हालिया गतिविधियां (Recent History)", color = WarmOffWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        TextButton(onClick = onViewAllHistory) {
          Text("सभी देखें", color = CyanNeon, fontSize = 12.sp)
        }
      }
    }

    val recentEvents = events.take(4)
    if (recentEvents.isEmpty()) {
      item {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ObsidianCard)
            .padding(16.dp),
          contentAlignment = Alignment.Center
        ) {
          Text("कोई हालिया गतिविधि नहीं। पहली प्रविष्टि दर्ज करें।", color = WarmMuted, fontSize = 12.sp)
        }
      }
    } else {
      items(recentEvents) { event ->
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ObsidianCard)
            .border(1.dp, ObsidianBorderSubtle, RoundedCornerShape(12.dp))
            .padding(12.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(event.eventType.titleHi, color = CyanNeon, fontSize = 12.sp, fontWeight = FontWeight.Bold)
              Text("तारीख: ${event.date} • ${event.status?.labelHi ?: event.urgeTrigger ?: ""}", color = WarmOffWhite, fontSize = 11.sp)
            }
            Text(TimeUtils.formatDate(event.timestampEpochMs, "hh:mm a"), color = WarmMuted, fontSize = 10.sp)
          }
        }
      }
    }

    item {
      Spacer(modifier = Modifier.height(20.dp))
    }
  }
}

@Composable
private fun MetricPill(label: String, value: String, color: Color) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(label, color = WarmMuted, fontSize = 10.sp)
    Text(value, color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
  }
}

@Composable
private fun StatusBadge(status: GoalStatus) {
  val (bgColor, textColor, label) = when (status) {
    GoalStatus.COMPLETED -> Triple(SageGreen.copy(alpha = 0.2f), SageGreen, "सफल (Completed)")
    GoalStatus.GOAL_NOT_MET -> Triple(Color(0xFFFFB74D).copy(alpha = 0.2f), Color(0xFFFFB74D), "अधूरा (Not Met)")
    GoalStatus.PENDING -> Triple(ObsidianElevated, WarmMuted, "प्रतीक्षारत (Pending)")
  }

  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(8.dp))
      .background(bgColor)
      .border(1.dp, textColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
      .padding(horizontal = 8.dp, vertical = 4.dp)
  ) {
    Text(label, color = textColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
  }
}
