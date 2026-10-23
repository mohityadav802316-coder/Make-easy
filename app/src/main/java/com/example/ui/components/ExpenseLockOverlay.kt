package com.example.ui.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.GoldBrass
import com.example.ui.theme.GoldHighlight
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.WarmMuted
import com.example.ui.theme.WarmOffWhite

@Composable
fun ExpenseLockOverlay(
  correctPin: String,
  onUnlocked: () -> Unit,
  onCancel: () -> Unit,
  modifier: Modifier = Modifier
) {
  var enteredPin by remember { mutableStateOf("") }
  var isError by remember { mutableStateOf(false) }

  fun handleKey(key: String) {
    if (enteredPin.length < 4) {
      val next = enteredPin + key
      enteredPin = next
      isError = false
      if (next.length == 4) {
        if (next == correctPin) {
          onUnlocked()
        } else {
          isError = true
          enteredPin = ""
        }
      }
    }
  }

  fun handleBackspace() {
    if (enteredPin.isNotEmpty()) {
      enteredPin = enteredPin.dropLast(1)
      isError = false
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(DarkBackground)
      .statusBarsPadding()
      .padding(24.dp)
  ) {
    IconButton(
      onClick = onCancel,
      modifier = Modifier.align(Alignment.TopEnd)
    ) {
      Icon(Icons.Default.Close, contentDescription = "Close", tint = WarmMuted)
    }

    Column(
      modifier = Modifier
        .fillMaxWidth()
        .align(Alignment.Center),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Box(
        modifier = Modifier
          .size(64.dp)
          .clip(CircleShape)
          .background(ObsidianElevated)
          .border(1.dp, GoldBrass.copy(alpha = 0.5f), CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.Lock,
          contentDescription = "Locked",
          tint = GoldHighlight,
          modifier = Modifier.size(32.dp)
        )
      }

      Spacer(modifier = Modifier.height(20.dp))

      Text(
        text = "Expense Diary Locked",
        color = WarmOffWhite,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold
      )

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = if (isError) "गलत पिन दर्ज किया गया, पुनः प्रयास करें" else "Enter 4-digit security PIN to unlock",
        color = if (isError) Color(0xFFE57373) else WarmMuted,
        fontSize = 13.sp
      )

      Spacer(modifier = Modifier.height(28.dp))

      // PIN Dots
      Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        for (i in 0 until 4) {
          val filled = i < enteredPin.length
          Box(
            modifier = Modifier
              .size(16.dp)
              .clip(CircleShape)
              .background(if (filled) GoldBrass else ObsidianElevated)
              .border(
                1.5.dp,
                if (isError) Color(0xFFE57373) else if (filled) GoldHighlight else ObsidianBorder,
                CircleShape
              )
          )
        }
      }

      Spacer(modifier = Modifier.height(40.dp))

      // Number Pad
      val padRows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", "DEL")
      )

      Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        padRows.forEach { row ->
          Row(
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            row.forEach { digit ->
              when (digit) {
                "" -> {
                  Spacer(modifier = Modifier.size(64.dp))
                }
                "DEL" -> {
                  Box(
                    modifier = Modifier
                      .size(64.dp)
                      .clip(CircleShape)
                      .background(ObsidianElevated)
                      .border(1.dp, ObsidianBorder, CircleShape)
                      .clickable { handleBackspace() },
                    contentAlignment = Alignment.Center
                  ) {
                    Icon(
                      imageVector = Icons.AutoMirrored.Filled.Backspace,
                      contentDescription = "Delete",
                      tint = WarmMuted,
                      modifier = Modifier.size(24.dp)
                    )
                  }
                }
                else -> {
                  Box(
                    modifier = Modifier
                      .size(64.dp)
                      .clip(CircleShape)
                      .background(ObsidianElevated)
                      .border(1.dp, ObsidianBorder, CircleShape)
                      .clickable { handleKey(digit) },
                    contentAlignment = Alignment.Center
                  ) {
                    Text(
                      text = digit,
                      color = WarmOffWhite,
                      fontSize = 22.sp,
                      fontWeight = FontWeight.SemiBold
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
