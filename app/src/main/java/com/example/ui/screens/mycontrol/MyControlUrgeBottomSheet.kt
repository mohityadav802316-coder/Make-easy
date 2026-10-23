package com.example.ui.screens.mycontrol

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.GoldBrass
import com.example.ui.theme.GoldHighlight
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianBorderSubtle
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianCharcoal
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.SageGreen
import com.example.ui.theme.WarmMuted
import com.example.ui.theme.WarmOffWhite
import com.example.ui.theme.WarmParchment
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyControlUrgeBottomSheet(
  sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
  onDismiss: () -> Unit,
  onSaveUrgeLog: (intensity: Int, trigger: String?, strategy: String?, note: String?) -> Unit
) {
  var urgeIntensity by remember { mutableIntStateOf(3) }
  var selectedTrigger by remember { mutableStateOf<String?>("तनाव / Stress") }
  var selectedStrategy by remember { mutableStateOf<String?>("गहरी सांस / Deep Breathing") }
  var urgeNote by remember { mutableStateOf("") }

  // Quick 60s Breathing Timer state
  var isBreathingActive by remember { mutableStateOf(false) }
  var breathingSecondsRemaining by remember { mutableIntStateOf(60) }
  var breathPhase by remember { mutableStateOf("सांस अंदर लें (Inhale)") }

  LaunchedEffect(isBreathingActive) {
    if (isBreathingActive) {
      breathingSecondsRemaining = 60
      while (breathingSecondsRemaining > 0 && isBreathingActive) {
        val cycleSec = (60 - breathingSecondsRemaining) % 16
        breathPhase = when (cycleSec) {
          in 0..3 -> "सांस अंदर लें (Inhale... 4s)"
          in 4..7 -> "सांस रोकें (Hold... 4s)"
          in 8..11 -> "सांस बाहर छोड़ें (Exhale... 4s)"
          else -> "विश्राम (Relax... 4s)"
        }
        delay(1000)
        breathingSecondsRemaining--
      }
      isBreathingActive = false
      breathPhase = "सत्र समाप्त • शांति महसूस करें"
    }
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = ObsidianCharcoal,
    scrimColor = Color.Black.copy(alpha = 0.75f),
    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .navigationBarsPadding()
        .padding(horizontal = 20.dp)
        .padding(bottom = 28.dp)
        .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // 1. Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(36.dp)
              .clip(CircleShape)
              .background(CyanNeon.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(Icons.Default.Security, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(20.dp))
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = "इच्छा नियंत्रण सहायक (Urge SOS)",
              color = WarmOffWhite,
              fontSize = 17.sp,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "क्षणिक आवेग को समझें और स्वस्थ तरीके से संभालें",
              color = WarmMuted,
              fontSize = 11.sp
            )
          }
        }
        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
          Icon(Icons.Default.Close, contentDescription = "Close", tint = WarmMuted)
        }
      }

      // 2. Interactive 60-Second Box Breathing Tool
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(18.dp))
          .background(ObsidianElevated)
          .border(1.dp, if (isBreathingActive) CyanNeon else ObsidianBorder, RoundedCornerShape(18.dp))
          .padding(16.dp)
      ) {
        Column(
          modifier = Modifier.fillMaxWidth(),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text(
            text = "60-सेकंड श्वास विश्राम (Emergency Calm)",
            color = WarmOffWhite,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
          )
          Spacer(modifier = Modifier.height(10.dp))

          Box(
            modifier = Modifier
              .size(80.dp)
              .clip(CircleShape)
              .background(if (isBreathingActive) CyanNeon.copy(alpha = 0.2f) else ObsidianBorder.copy(alpha = 0.4f))
              .border(2.dp, if (isBreathingActive) CyanNeon else ObsidianBorder, CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = if (isBreathingActive) "${breathingSecondsRemaining}s" else "60s",
              color = if (isBreathingActive) CyanNeon else WarmMuted,
              fontSize = 20.sp,
              fontWeight = FontWeight.Bold
            )
          }

          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = breathPhase,
            color = if (isBreathingActive) GoldHighlight else WarmParchment,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
          )

          Spacer(modifier = Modifier.height(10.dp))
          Button(
            onClick = { isBreathingActive = !isBreathingActive },
            colors = ButtonDefaults.buttonColors(
              containerColor = if (isBreathingActive) Color(0xFFC62828) else CyanNeon
            ),
            shape = RoundedCornerShape(10.dp)
          ) {
            Icon(
              imageVector = if (isBreathingActive) Icons.Default.Stop else Icons.Default.PlayArrow,
              contentDescription = null,
              tint = Color.Black,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (isBreathingActive) "रोकें (Stop)" else "श्वास व्यायाम शुरू करें",
              color = Color.Black,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }

      // 3. Urge Intensity Slider (1 to 5)
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("इच्छा की तीव्रता (Urge Intensity):", color = WarmOffWhite, fontSize = 13.sp)
          Text(
            text = when (urgeIntensity) {
              1 -> "1 / 5 (हल्की / Mild)"
              2 -> "2 / 5 (सामान्य / Moderate)"
              3 -> "3 / 5 (मजबूत / Strong)"
              4 -> "4 / 5 (तीव्र / Very Strong)"
              else -> "5 / 5 (चरम / Extreme Peak)"
            },
            color = GoldHighlight,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
          )
        }
        Slider(
          value = urgeIntensity.toFloat(),
          onValueChange = { urgeIntensity = it.toInt() },
          valueRange = 1f..5f,
          steps = 3,
          colors = SliderDefaults.colors(
            thumbColor = GoldBrass,
            activeTrackColor = CyanNeon,
            inactiveTrackColor = ObsidianBorder
          )
        )
      }

      // 4. Trigger Tags
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("ट्रिगर या कारण (What Triggered It?):", color = WarmOffWhite, fontSize = 13.sp)
        val triggers = listOf(
          "तनाव / Stress",
          "अकेलापन / Boredom",
          "देर रात / Late Night",
          "थकान / Exhaustion",
          "फोन ब्राउजिंग / Phone Browsing",
          "अन्य / Other"
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          triggers.take(3).forEach { t ->
            UrgeChip(
              label = t,
              isSelected = selectedTrigger == t,
              onClick = { selectedTrigger = t },
              modifier = Modifier.weight(1f)
            )
          }
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          triggers.drop(3).forEach { t ->
            UrgeChip(
              label = t,
              isSelected = selectedTrigger == t,
              onClick = { selectedTrigger = t },
              modifier = Modifier.weight(1f)
            )
          }
        }
      }

      // 5. Coping Strategy Tags
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("आपने क्या कदम उठाया? (Coping Action):", color = WarmOffWhite, fontSize = 13.sp)
        val strategies = listOf(
          "गहरी सांस / Deep Breathing",
          "ठंडा पानी / Cold Water",
          "टहलना / Walk Outside",
          "कसरत / Push-ups",
          "फोन दूर रखना / Put Phone Away",
          "डायरी / Journaling"
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          strategies.take(3).forEach { s ->
            UrgeChip(
              label = s,
              isSelected = selectedStrategy == s,
              onClick = { selectedStrategy = s },
              modifier = Modifier.weight(1f)
            )
          }
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          strategies.drop(3).forEach { s ->
            UrgeChip(
              label = s,
              isSelected = selectedStrategy == s,
              onClick = { selectedStrategy = s },
              modifier = Modifier.weight(1f)
            )
          }
        }
      }

      // 6. Optional Note
      OutlinedTextField(
        value = urgeNote,
        onValueChange = { urgeNote = it },
        label = { Text("निजी नोट (वैकल्पिक / Optional Note)") },
        placeholder = { Text("क्या महसूस हुआ, कैसे शांत हुए...") },
        modifier = Modifier.fillMaxWidth(),
        maxLines = 2,
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

      // 7. Save Log Button
      Button(
        onClick = {
          onSaveUrgeLog(
            urgeIntensity,
            selectedTrigger,
            selectedStrategy,
            urgeNote.ifBlank { null }
          )
          onDismiss()
        },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = CyanNeon),
        shape = RoundedCornerShape(14.dp)
      ) {
        Text(
          text = "इच्छा रिकॉर्ड सुरक्षित करें (Log Urge Management)",
          color = Color.Black,
          fontWeight = FontWeight.Bold,
          fontSize = 13.sp
        )
      }
    }
  }
}

@Composable
private fun UrgeChip(
  label: String,
  isSelected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(10.dp))
      .background(if (isSelected) CyanNeon.copy(alpha = 0.2f) else ObsidianElevated)
      .border(1.dp, if (isSelected) CyanNeon else ObsidianBorderSubtle, RoundedCornerShape(10.dp))
      .clickable(onClick = onClick)
      .padding(vertical = 8.dp, horizontal = 4.dp),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = label.split("/").first().trim(),
      color = if (isSelected) CyanNeon else WarmMuted,
      fontSize = 11.sp,
      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
      maxLines = 1
    )
  }
}
