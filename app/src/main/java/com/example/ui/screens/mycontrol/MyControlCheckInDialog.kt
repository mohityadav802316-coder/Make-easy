package com.example.ui.screens.mycontrol

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.window.Dialog
import com.example.data.model.GoalStatus
import com.example.data.model.MyControlDayEntity
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.GoldBrass
import com.example.ui.theme.GoldHighlight
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.SageGreen
import com.example.ui.theme.WarmMuted
import com.example.ui.theme.WarmOffWhite
import com.example.ui.theme.WarmParchment
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun MyControlCheckInDialog(
  initialDate: String,
  existingDays: List<MyControlDayEntity>,
  onDismiss: () -> Unit,
  onSave: (date: String, status: GoalStatus, note: String?, isRetrospective: Boolean) -> Unit
) {
  val todayStr = remember { LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) }
  val yesterdayStr = remember { LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE) }

  var selectedDate by remember { mutableStateOf(initialDate) }
  val existingRecord = remember(selectedDate, existingDays) {
    existingDays.firstOrNull { it.date == selectedDate }
  }

  var selectedStatus by remember(existingRecord) {
    mutableStateOf(existingRecord?.status ?: GoalStatus.COMPLETED)
  }
  var noteText by remember(existingRecord) {
    mutableStateOf(existingRecord?.note ?: "")
  }

  var showConfirmStep by remember { mutableStateOf(false) }

  val isToday = selectedDate == todayStr
  val isRetrospective = !isToday

  Dialog(onDismissRequest = onDismiss) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(24.dp))
        .background(ObsidianCard)
        .border(1.dp, ObsidianBorder, RoundedCornerShape(24.dp))
        .padding(22.dp)
    ) {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        // 1. Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = "दैनिक चेक-इन (Daily Check-in)",
              color = WarmOffWhite,
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "व्यक्तिगत लक्ष्य एवं आत्म-नियंत्रण प्रगति",
              color = WarmMuted,
              fontSize = 12.sp
            )
          }
          IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = WarmMuted)
          }
        }

        // 2. Date Quick Selector (Today vs Yesterday)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          DatePill(
            label = "आज (Today)",
            date = todayStr,
            isSelected = selectedDate == todayStr,
            onClick = { selectedDate = todayStr }
          )
          DatePill(
            label = "कल (Yesterday)",
            date = yesterdayStr,
            isSelected = selectedDate == yesterdayStr,
            onClick = { selectedDate = yesterdayStr }
          )
        }

        if (existingRecord != null && existingRecord.status != GoalStatus.PENDING) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(GoldBrass.copy(alpha = 0.1f))
              .border(1.dp, GoldBrass.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
              .padding(10.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(Icons.Default.Info, contentDescription = null, tint = GoldHighlight, modifier = Modifier.size(18.dp))
              Text(
                text = "इस दिन पहले से '${existingRecord.status.labelHi}' दर्ज है। नया इनपुट सुधार (Correction) के रूप में रिकॉर्ड होगा।",
                color = WarmOffWhite,
                fontSize = 12.sp,
                lineHeight = 16.sp
              )
            }
          }
        }

        // 3. Status Selection Cards
        Text(
          text = "आज का परिणाम चुनें (Select Status)",
          color = WarmParchment,
          fontSize = 13.sp,
          fontWeight = FontWeight.SemiBold
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          // Completed
          StatusOptionCard(
            title = "लक्ष्य पूरा हुआ",
            subtitle = "Goal Completed",
            color = SageGreen,
            isSelected = selectedStatus == GoalStatus.COMPLETED,
            modifier = Modifier.weight(1f),
            onClick = { selectedStatus = GoalStatus.COMPLETED }
          )

          // Goal Not Met
          StatusOptionCard(
            title = "लक्ष्य अधूरा रहा",
            subtitle = "Goal Not Met",
            color = Color(0xFFFFB74D),
            isSelected = selectedStatus == GoalStatus.GOAL_NOT_MET,
            modifier = Modifier.weight(1f),
            onClick = { selectedStatus = GoalStatus.GOAL_NOT_MET }
          )
        }

        if (selectedStatus == GoalStatus.GOAL_NOT_MET) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(ObsidianElevated)
              .padding(10.dp)
          ) {
            Text(
              text = "कोई बात नहीं। यह आत्म-अनुशासन की एक निरंतर यात्रा है। हर नया दिन एक नई शुरुआत है।",
              color = WarmMuted,
              fontSize = 11.sp,
              lineHeight = 15.sp
            )
          }
        }

        // 4. Optional Reflection Note
        OutlinedTextField(
          value = noteText,
          onValueChange = { noteText = it },
          label = { Text("विचार या नोट (वैकल्पिक / Optional Note)") },
          placeholder = { Text("आज की भावनाएं, ट्रिगर या सफलता का कारण...") },
          modifier = Modifier.fillMaxWidth(),
          maxLines = 3,
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = CyanNeon,
            unfocusedBorderColor = ObsidianBorder,
            focusedTextColor = WarmOffWhite,
            unfocusedTextColor = WarmOffWhite,
            focusedContainerColor = ObsidianElevated,
            unfocusedContainerColor = ObsidianElevated
          ),
          shape = RoundedCornerShape(12.dp)
        )

        // 5. Submit / Confirmation
        if (showConfirmStep) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(14.dp))
              .background(Color(0xFF2C2416))
              .border(1.dp, GoldBrass, RoundedCornerShape(14.dp))
              .padding(12.dp)
          ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Text(
                text = "पुष्टि करें (Confirm Submission)",
                color = GoldHighlight,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "तारीख: $selectedDate\nस्थिति: ${selectedStatus.labelHi} (${selectedStatus.labelEn})" +
                  if (noteText.isNotBlank()) "\nनोट: ${noteText.trim()}" else "",
                color = WarmOffWhite,
                fontSize = 12.sp,
                lineHeight = 17.sp
              )
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
              ) {
                TextButton(onClick = { showConfirmStep = false }) {
                  Text("बदलें", color = WarmMuted)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                  onClick = {
                    onSave(selectedDate, selectedStatus, noteText.ifBlank { null }, isRetrospective)
                    onDismiss()
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = GoldBrass),
                  shape = RoundedCornerShape(10.dp)
                ) {
                  Text("हाँ, सुरक्षित करें", color = Color.Black, fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        } else {
          Button(
            onClick = { showConfirmStep = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
              containerColor = if (selectedStatus == GoalStatus.COMPLETED) SageGreen else GoldBrass
            ),
            shape = RoundedCornerShape(14.dp)
          ) {
            Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "दर्ज करें (Record Status)",
              color = Color.Black,
              fontWeight = FontWeight.Bold,
              fontSize = 14.sp
            )
          }
        }
      }
    }
  }
}

@Composable
private fun DatePill(
  label: String,
  date: String,
  isSelected: Boolean,
  onClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(12.dp))
      .background(if (isSelected) CyanNeon.copy(alpha = 0.2f) else ObsidianElevated)
      .border(1.dp, if (isSelected) CyanNeon else ObsidianBorder, RoundedCornerShape(12.dp))
      .clickable(onClick = onClick)
      .padding(horizontal = 14.dp, vertical = 8.dp)
  ) {
    Text(
      text = label,
      color = if (isSelected) CyanNeon else WarmMuted,
      fontSize = 12.sp,
      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
    )
  }
}

@Composable
private fun StatusOptionCard(
  title: String,
  subtitle: String,
  color: Color,
  isSelected: Boolean,
  modifier: Modifier = Modifier,
  onClick: () -> Unit
) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(16.dp))
      .background(if (isSelected) color.copy(alpha = 0.2f) else ObsidianElevated)
      .border(1.5.dp, if (isSelected) color else ObsidianBorder, RoundedCornerShape(16.dp))
      .clickable(onClick = onClick)
      .padding(14.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Box(
        modifier = Modifier
          .size(32.dp)
          .clip(CircleShape)
          .background(if (isSelected) color else ObsidianBorder.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
      ) {
        if (isSelected) {
          Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
        }
      }
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = title,
        color = if (isSelected) WarmOffWhite else WarmMuted,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold
      )
      Text(
        text = subtitle,
        color = if (isSelected) color else WarmMuted.copy(alpha = 0.7f),
        fontSize = 11.sp
      )
    }
  }
}
