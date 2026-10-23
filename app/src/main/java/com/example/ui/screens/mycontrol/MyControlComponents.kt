package com.example.ui.screens.mycontrol

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GoalStatus
import com.example.data.model.MyControlConfigEntity
import com.example.data.model.MyControlDayEntity
import com.example.data.model.MyControlEventEntity
import com.example.data.model.MyControlEventType
import com.example.data.model.MyControlStats
import com.example.ui.components.TimePickerDialog12Hour
import com.example.ui.theme.CyanNeon
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
import com.example.util.TimeUtils
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Date

// =========================================================================
// 1. CALENDAR & EVENT HISTORY VIEW
// =========================================================================

@Composable
fun MyControlCalendarHistoryView(
  days: List<MyControlDayEntity>,
  events: List<MyControlEventEntity>,
  onDateSelectedForCheckIn: (String) -> Unit
) {
  var currentYearMonth by remember { mutableStateOf(YearMonth.now()) }
  val daysMap = remember(days) { days.associateBy { it.date } }

  var selectedDayDetail by remember { mutableStateOf<String?>(null) }

  Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
    // Month Navigator Header
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .background(ObsidianCard)
        .border(1.dp, ObsidianBorder, RoundedCornerShape(16.dp))
        .padding(14.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(onClick = { currentYearMonth = currentYearMonth.minusMonths(1) }) {
          Text("◀", color = CyanNeon, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Text(
          text = "${currentYearMonth.month.name} ${currentYearMonth.year}",
          color = WarmOffWhite,
          fontSize = 16.sp,
          fontWeight = FontWeight.Bold
        )

        IconButton(onClick = { currentYearMonth = currentYearMonth.plusMonths(1) }) {
          Text("▶", color = CyanNeon, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
      }
    }

    // Calendar Grid
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(ObsidianCard)
        .border(1.dp, ObsidianBorder, RoundedCornerShape(18.dp))
        .padding(14.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Weekday Headers
        Row(modifier = Modifier.fillMaxWidth()) {
          listOf("M", "T", "W", "T", "F", "S", "S").forEach { day ->
            Text(
              text = day,
              color = WarmMuted,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.weight(1f),
              textAlign = TextAlign.Center
            )
          }
        }

        // Days Grid
        val firstDayOfMonth = currentYearMonth.atDay(1)
        val daysInMonth = currentYearMonth.lengthOfMonth()
        val startOffset = (firstDayOfMonth.dayOfWeek.value - 1) // 0=Mon, 6=Sun

        val totalSlots = startOffset + daysInMonth
        val rows = (totalSlots + 6) / 7

        for (r in 0 until rows) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            for (c in 0 until 7) {
              val slotIndex = r * 7 + c
              val dayNum = slotIndex - startOffset + 1
              if (dayNum in 1..daysInMonth) {
                val dateStr = currentYearMonth.atDay(dayNum).format(DateTimeFormatter.ISO_LOCAL_DATE)
                val status = daysMap[dateStr]?.status ?: GoalStatus.PENDING

                Box(
                  modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .padding(2.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                      when (status) {
                        GoalStatus.COMPLETED -> SageGreen.copy(alpha = 0.25f)
                        GoalStatus.GOAL_NOT_MET -> Color(0xFFFFB74D).copy(alpha = 0.25f)
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
                      RoundedCornerShape(8.dp)
                    )
                    .clickable { selectedDayDetail = dateStr },
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = dayNum.toString(),
                    color = when (status) {
                      GoalStatus.COMPLETED -> SageGreen
                      GoalStatus.GOAL_NOT_MET -> Color(0xFFFFB74D)
                      GoalStatus.PENDING -> WarmMuted
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                  )
                }
              } else {
                Spacer(modifier = Modifier.weight(1f))
              }
            }
          }
        }
      }
    }

    // Legend
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically
    ) {
      LegendDot(color = SageGreen, label = "लक्ष्य पूरा हुआ (Completed)")
      Spacer(modifier = Modifier.width(16.dp))
      LegendDot(color = Color(0xFFFFB74D), label = "लक्ष्य अधूरा (Not Met)")
      Spacer(modifier = Modifier.width(16.dp))
      LegendDot(color = WarmMuted, label = "प्रतीक्षारत (Pending)")
    }

    // Append-Only Event Audit Timeline
    Text(
      text = "इवेंट एवं ऑडिट इतिहास (Timestamped Audit Log)",
      color = WarmOffWhite,
      fontSize = 15.sp,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.padding(top = 8.dp)
    )

    if (events.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(16.dp))
          .background(ObsidianCard)
          .padding(20.dp),
        contentAlignment = Alignment.Center
      ) {
        Text("अभी कोई रिकॉर्ड नहीं है। चेक-इन करने पर इतिहास बनेगा।", color = WarmMuted, fontSize = 12.sp)
      }
    } else {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        events.take(30).forEach { event ->
          EventLogCard(event = event)
        }
      }
    }
  }

  // Day Detail Dialog on Click
  if (selectedDayDetail != null) {
    val date = selectedDayDetail!!
    val dayRecord = daysMap[date]
    val dayEvents = events.filter { it.date == date }

    AlertDialog(
      onDismissRequest = { selectedDayDetail = null },
      containerColor = ObsidianCard,
      title = {
        Text("तारीख: $date", color = WarmOffWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "स्थिति: ${dayRecord?.status?.labelHi ?: "प्रतीक्षारत (Pending)"}",
            color = when (dayRecord?.status) {
              GoalStatus.COMPLETED -> SageGreen
              GoalStatus.GOAL_NOT_MET -> Color(0xFFFFB74D)
              else -> WarmMuted
            },
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
          )
          if (!dayRecord?.note.isNullOrBlank()) {
            Text("नोट: ${dayRecord?.note}", color = WarmOffWhite, fontSize = 12.sp)
          }
          if (dayEvents.isNotEmpty()) {
            Text("इस दिन के इवेंट्स (${dayEvents.size}):", color = WarmParchment, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            dayEvents.forEach { ev ->
              Text(
                text = "• ${ev.eventType.titleHi}: ${ev.status?.labelHi ?: ""} ${ev.urgeTrigger ?: ""} (${TimeUtils.formatDate(ev.timestampEpochMs, "hh:mm a")})",
                color = WarmMuted,
                fontSize = 11.sp
              )
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val d = selectedDayDetail!!
            selectedDayDetail = null
            onDateSelectedForCheckIn(d)
          },
          colors = ButtonDefaults.buttonColors(containerColor = CyanNeon)
        ) {
          Text("संपादित करें / चेक-इन", color = Color.Black, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { selectedDayDetail = null }) {
          Text("बंद करें", color = WarmMuted)
        }
      }
    )
  }
}

@Composable
private fun LegendDot(color: Color, label: String) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Box(
      modifier = Modifier
        .size(8.dp)
        .clip(CircleShape)
        .background(color)
    )
    Spacer(modifier = Modifier.width(5.dp))
    Text(label, color = WarmMuted, fontSize = 10.sp)
  }
}

@Composable
private fun EventLogCard(event: MyControlEventEntity) {
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
      verticalAlignment = Alignment.Top
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = event.eventType.titleHi,
            color = when (event.eventType) {
              MyControlEventType.DAILY_CHECKIN -> CyanNeon
              MyControlEventType.STATUS_CORRECTION -> GoldHighlight
              MyControlEventType.URGE_LOG -> Color(0xFFFF8A65)
              MyControlEventType.NOTE_ENTRY -> WarmParchment
            },
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "• ${event.date}",
            color = WarmMuted,
            fontSize = 11.sp
          )
        }

        if (event.status != null) {
          Text(
            text = "स्थिति: ${event.status.labelHi} (${event.status.labelEn})" +
              if (event.previousStatus != null) " [पूर्व स्थिति: ${event.previousStatus.labelHi}]" else "",
            color = WarmOffWhite,
            fontSize = 11.sp,
            lineHeight = 15.sp
          )
        }

        if (event.urgeIntensity != null) {
          Text(
            text = "तीव्रता: ${event.urgeIntensity}/5 | ट्रिगर: ${event.urgeTrigger ?: "अज्ञात"} | उपाय: ${event.urgeStrategy ?: "कोई नहीं"}",
            color = WarmParchment,
            fontSize = 11.sp
          )
        }

        if (!event.note.isNullOrBlank()) {
          Text(
            text = "\"${event.note}\"",
            color = WarmMuted,
            fontSize = 11.sp,
            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
          )
        }
      }

      Text(
        text = TimeUtils.formatDate(event.timestampEpochMs, "hh:mm a"),
        color = WarmMuted,
        fontSize = 10.sp
      )
    }
  }
}

// =========================================================================
// 2. PROGRESS & ANALYTICS VIEW
// =========================================================================

@Composable
fun MyControlAnalyticsView(
  stats: MyControlStats,
  days: List<MyControlDayEntity>,
  events: List<MyControlEventEntity>
) {
  val urgeEvents = remember(events) { events.filter { it.eventType == MyControlEventType.URGE_LOG } }
  val triggerCounts = remember(urgeEvents) {
    urgeEvents.mapNotNull { it.urgeTrigger }.groupingBy { it }.eachCount()
  }

  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    // 1. Success Rate Progress Card
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(ObsidianCard)
        .border(1.dp, ObsidianBorder, RoundedCornerShape(18.dp))
        .padding(16.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("सफलता दर (Overall Success Rate)", color = WarmOffWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
          Text("${stats.successRatePercent.toInt()}%", color = SageGreen, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        LinearProgressIndicator(
          progress = { (stats.successRatePercent / 100f).coerceIn(0f, 1f) },
          modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp)),
          color = SageGreen,
          trackColor = ObsidianElevated
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text("सफल दिन: ${stats.totalCompleted}", color = SageGreen, fontSize = 12.sp)
          Text("अधूरे लक्ष्य: ${stats.totalGoalNotMet}", color = Color(0xFFFFB74D), fontSize = 12.sp)
          Text("कुल चेक-इन: ${stats.totalCheckIns}", color = WarmMuted, fontSize = 12.sp)
        }
      }
    }

    // 2. Streak Highlights
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      AnalyticsStatCard(
        title = "वर्तमान स्ट्रीक",
        value = "${stats.currentStreak} दिन",
        color = CyanNeon,
        modifier = Modifier.weight(1f)
      )
      AnalyticsStatCard(
        title = "सर्वश्रेष्ठ स्ट्रीक",
        value = "${stats.longestStreak} दिन",
        color = GoldHighlight,
        modifier = Modifier.weight(1f)
      )
    }

    // 3. Urge Management Breakdown
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(ObsidianCard)
        .border(1.dp, ObsidianBorder, RoundedCornerShape(18.dp))
        .padding(16.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("इच्छा प्रबंधन एवं ट्रिगर विश्लेषण (Urge Triggers)", color = WarmOffWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text("कुल संभाली गई इच्छाएं: ${urgeEvents.size}", color = WarmMuted, fontSize = 12.sp)

        if (triggerCounts.isEmpty()) {
          Text("अभी कोई इच्छा रिकॉर्ड नहीं है।", color = WarmMuted, fontSize = 11.sp)
        } else {
          triggerCounts.forEach { (trigger, count) ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(trigger, color = WarmParchment, fontSize = 12.sp)
              Text("$count बार", color = CyanNeon, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }

    // 4. Scientific Neutral Guidance
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(ObsidianElevated)
        .padding(16.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("आत्म-नियंत्रण का विज्ञान (Self-Control Mindset)", color = WarmOffWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text(
          "यह ट्रैकर व्यक्तिगत अनुशासन और ऊर्जा फोकस के लिए है। किसी भी प्रकार की नकारात्मक आत्म-आलोचना या अपराधबोध से बचें। निरंतरता एक आदत है जिसे अभ्यास द्वारा मजबूत किया जाता है।",
          color = WarmMuted,
          fontSize = 11.sp,
          lineHeight = 16.sp
        )
      }
    }
  }
}

@Composable
private fun AnalyticsStatCard(
  title: String,
  value: String,
  color: Color,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(16.dp))
      .background(ObsidianCard)
      .border(1.dp, ObsidianBorder, RoundedCornerShape(16.dp))
      .padding(14.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(title, color = WarmMuted, fontSize = 11.sp)
      Spacer(modifier = Modifier.height(4.dp))
      Text(value, color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
  }
}

// =========================================================================
// 3. SETTINGS & PRIVACY VIEW
// =========================================================================

@Composable
fun MyControlSettingsView(
  config: MyControlConfigEntity,
  onUpdateReminder: (Boolean, Int) -> Unit,
  onUpdatePin: (Boolean, String) -> Unit,
  onClearData: () -> Unit,
  onExportJson: suspend () -> String
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  var reminderEnabled by remember(config) { mutableStateOf(config.reminderEnabled) }
  var reminderTimeMinutes by remember(config) { mutableIntStateOf(config.reminderTimeMinutes) }
  var showTimePicker by remember { mutableStateOf(false) }

  var pinLockEnabled by remember(config) { mutableStateOf(config.privacyPinEnabled) }
  var showPinDialog by remember { mutableStateOf(false) }
  var newPinInput by remember { mutableStateOf("") }

  var showResetConfirmDialog by remember { mutableStateOf(false) }

  Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
    // 1. Daily Neutral Reminder
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
          Column(modifier = Modifier.weight(1f)) {
            Text("दैनिक चेक-इन रिमाइंडर (Daily Reminder)", color = WarmOffWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(
              "गोपनीय तटस्थ सूचना: 'Your My Control daily check-in is ready.'",
              color = WarmMuted,
              fontSize = 11.sp,
              lineHeight = 15.sp
            )
          }
          Switch(
            checked = reminderEnabled,
            onCheckedChange = {
              reminderEnabled = it
              onUpdateReminder(it, reminderTimeMinutes)
            },
            colors = SwitchDefaults.colors(
              checkedThumbColor = Color.Black,
              checkedTrackColor = CyanNeon,
              uncheckedThumbColor = WarmMuted,
              uncheckedTrackColor = ObsidianElevated
            )
          )
        }

        if (reminderEnabled) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(ObsidianElevated)
              .clickable { showTimePicker = true }
              .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("रिमाइंडर समय (Time):", color = WarmParchment, fontSize = 12.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = TimeUtils.minutesTo12Hour(reminderTimeMinutes),
                color = CyanNeon,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
              )
              Spacer(modifier = Modifier.width(6.dp))
              Icon(Icons.Default.Edit, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(14.dp))
            }
          }
        }
      }
    }

    // 2. Privacy PIN Lock
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
          Column(modifier = Modifier.weight(1f)) {
            Text("गोपनीयता पिन लॉक (Privacy PIN Lock)", color = WarmOffWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(
              "My Control अनुभाग खोलने के लिए 4-अंकीय पिन आवश्यक करें",
              color = WarmMuted,
              fontSize = 11.sp
            )
          }
          Switch(
            checked = pinLockEnabled,
            onCheckedChange = {
              if (it) {
                showPinDialog = true
              } else {
                pinLockEnabled = false
                onUpdatePin(false, "")
              }
            },
            colors = SwitchDefaults.colors(
              checkedThumbColor = Color.Black,
              checkedTrackColor = CyanNeon,
              uncheckedThumbColor = WarmMuted,
              uncheckedTrackColor = ObsidianElevated
            )
          )
        }

        if (pinLockEnabled && config.privacyPin.isNotBlank()) {
          TextButton(onClick = { showPinDialog = true }) {
            Text("पिन बदलें (Change PIN)", color = CyanNeon, fontSize = 12.sp)
          }
        }
      }
    }

    // 3. Export Data
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(ObsidianCard)
        .border(1.dp, ObsidianBorder, RoundedCornerShape(18.dp))
        .padding(16.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("डेटा निर्यात (Export Records)", color = WarmOffWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(
          "अपना संपूर्ण इतिहास JSON प्रारूप में निर्यात करें। यह फाइल स्थानीय रूप से साझा की जाएगी।",
          color = WarmMuted,
          fontSize = 11.sp
        )

        Button(
          onClick = {
            scope.launch {
              val json = onExportJson()
              val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, json)
                type = "text/plain"
              }
              context.startActivity(Intent.createChooser(sendIntent, "Share My Control Data"))
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = ObsidianElevated),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(Icons.Default.Share, contentDescription = null, tint = WarmOffWhite, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("डेटा शेयर / बैकअप करें", color = WarmOffWhite, fontSize = 12.sp)
        }
      }
    }

    // 4. Clear Data
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(ObsidianCard)
        .border(1.dp, ObsidianBorder, RoundedCornerShape(18.dp))
        .padding(16.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("डेटा रीसेट (Clear Data)", color = Color(0xFFE57373), fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(
          "My Control का सभी इतिहास व स्ट्रीक डेटा स्थायी रूप से मिटाएं।",
          color = WarmMuted,
          fontSize = 11.sp
        )

        Button(
          onClick = { showResetConfirmDialog = true },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C).copy(alpha = 0.2f)),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFE57373), modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("सभी रिकॉर्ड हटाएं (Delete All)", color = Color(0xFFE57373), fontSize = 12.sp)
        }
      }
    }
  }

  // Time Picker Dialog
  if (showTimePicker) {
    TimePickerDialog12Hour(
      initialMinutes = reminderTimeMinutes,
      onConfirm = { mins ->
        reminderTimeMinutes = mins
        showTimePicker = false
        onUpdateReminder(reminderEnabled, mins)
      },
      onDismiss = { showTimePicker = false }
    )
  }

  // Set PIN Dialog
  if (showPinDialog) {
    AlertDialog(
      onDismissRequest = { showPinDialog = false },
      containerColor = ObsidianCard,
      title = { Text("4-अंकीय पिन दर्ज करें (Set 4-Digit PIN)", color = WarmOffWhite, fontSize = 15.sp) },
      text = {
        OutlinedTextField(
          value = newPinInput,
          onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) newPinInput = it },
          label = { Text("4 Digits PIN") },
          singleLine = true,
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = CyanNeon,
            unfocusedBorderColor = ObsidianBorder,
            focusedTextColor = WarmOffWhite,
            unfocusedTextColor = WarmOffWhite
          )
        )
      },
      confirmButton = {
        Button(
          onClick = {
            if (newPinInput.length == 4) {
              pinLockEnabled = true
              onUpdatePin(true, newPinInput)
              showPinDialog = false
              newPinInput = ""
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = CyanNeon),
          enabled = newPinInput.length == 4
        ) {
          Text("सेव करें", color = Color.Black, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showPinDialog = false }) {
          Text("रद्द करें", color = WarmMuted)
        }
      }
    )
  }

  // Reset Confirm Dialog
  if (showResetConfirmDialog) {
    AlertDialog(
      onDismissRequest = { showResetConfirmDialog = false },
      containerColor = ObsidianCard,
      title = { Text("क्या आप निश्चित हैं?", color = Color(0xFFE57373), fontSize = 16.sp, fontWeight = FontWeight.Bold) },
      text = {
        Text(
          "My Control का संपूर्ण चेक-इन इतिहास और स्ट्रीक हमेशा के लिए हट जाएगी। इस क्रिया को वापस नहीं लिया जा सकता।",
          color = WarmOffWhite,
          fontSize = 13.sp
        )
      },
      confirmButton = {
        Button(
          onClick = {
            onClearData()
            showResetConfirmDialog = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
        ) {
          Text("हाँ, सब मिटाएं", color = Color.White, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showResetConfirmDialog = false }) {
          Text("रद्द करें", color = WarmMuted)
        }
      }
    )
  }
}
