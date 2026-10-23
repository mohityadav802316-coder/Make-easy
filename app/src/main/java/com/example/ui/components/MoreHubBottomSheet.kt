package com.example.ui.components

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.EnergyMode
import com.example.data.model.StreakInfo
import com.example.ui.theme.GoldBrass
import com.example.ui.theme.GoldHighlight
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianBorderSubtle
import com.example.ui.theme.ObsidianCharcoal
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.WarmMuted
import com.example.ui.theme.WarmOffWhite
import com.example.ui.theme.WarmParchment
import com.example.ui.viewmodel.MainTab

data class HubFeatureItem(
  val title: String,
  val subtitle: String,
  val icon: ImageVector,
  val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreHubBottomSheet(
  sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
  streakInfo: StreakInfo?,
  dailyEnergyMode: EnergyMode,
  onEnergyModeChange: (EnergyMode) -> Unit,
  onDismiss: () -> Unit,
  onNavigateTab: (MainTab) -> Unit,
  onOpenCustomize: () -> Unit
) {
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = ObsidianCharcoal,
    scrimColor = Color.Black.copy(alpha = 0.7f),
    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .navigationBarsPadding()
        .padding(horizontal = 20.dp)
        .padding(bottom = 28.dp)
        .testTag("more_hub_sheet")
    ) {
      // 1. Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Image(
            painter = painterResource(id = R.drawable.ic_profile_logo),
            contentDescription = "my mohit profile",
            contentScale = ContentScale.Crop,
            modifier = Modifier
              .size(42.dp)
              .clip(CircleShape)
              .border(1.5.dp, GoldBrass, CircleShape)
          )
          Column {
            Text(
              text = "my mohit",
              color = GoldBrass,
              fontSize = 17.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.5.sp
            )
            Text(
              text = "सुविधाएं व हब • दैनिक टूल्स",
              color = WarmMuted,
              fontSize = 12.sp
            )
          }
        }
        IconButton(onClick = onDismiss) {
          Icon(Icons.Default.Close, contentDescription = "बंद करें", tint = WarmMuted)
        }
      }

      Spacer(modifier = Modifier.height(18.dp))

      // 2. Streak and Energy Mode Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // Streak Card
        Box(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(ObsidianElevated)
            .border(1.dp, ObsidianBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
        ) {
          Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.LocalFireDepartment,
                contentDescription = null,
                tint = GoldHighlight,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text("स्ट्रीक", color = WarmParchment, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "${streakInfo?.currentStreak ?: 0} दिन",
              color = WarmOffWhite,
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }

        // Energy Mode Selector
        Box(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(ObsidianElevated)
            .border(1.dp, ObsidianBorder, RoundedCornerShape(16.dp))
            .clickable {
              val next = when (dailyEnergyMode) {
                EnergyMode.NORMAL -> EnergyMode.HIGH
                EnergyMode.HIGH -> EnergyMode.LOW
                EnergyMode.LOW -> EnergyMode.NORMAL
              }
              onEnergyModeChange(next)
            }
            .padding(14.dp)
        ) {
          Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(dailyEnergyMode.emoji, fontSize = 13.sp)
              Spacer(modifier = Modifier.width(4.dp))
              Text("ऊर्जा स्तर", color = WarmParchment, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = dailyEnergyMode.label,
              color = GoldBrass,
              fontSize = 16.sp,
              fontWeight = FontWeight.SemiBold
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(18.dp))

      // 3. Grid of features
      val items = listOf(
        HubFeatureItem("Notebook", "डायरी व आत्मनिरीक्षण", Icons.Filled.Spa) {
          onDismiss()
          onNavigateTab(MainTab.REFLECTION)
        },
        HubFeatureItem("ध्यान (Meditation)", "माइंडफुलनेस सेशन्स", Icons.Filled.SelfImprovement) {
          onDismiss()
          onNavigateTab(MainTab.MEDITATION)
        },
        HubFeatureItem("टाइमलाइन (Timeline)", "दैनिक इतिहास", Icons.Filled.History) {
          onDismiss()
          onNavigateTab(MainTab.TIMELINE)
        },
        HubFeatureItem("My Control", "आदत एवं आत्म-नियंत्रण", Icons.Filled.Security) {
          onDismiss()
          onNavigateTab(MainTab.MY_CONTROL)
        },
        HubFeatureItem("खर्च डायरी", "दैनिक खर्च व बजट", Icons.Filled.ReceiptLong) {
          onDismiss()
          onNavigateTab(MainTab.EXPENSE_DIARY)
        },
        HubFeatureItem("अलार्म सेंटर", "जागरण व गतिविधि अलार्म", Icons.Filled.Alarm) {
          onDismiss()
          onNavigateTab(MainTab.ALARM_CENTER)
        },
        HubFeatureItem("डैशबोर्ड सेटिंग्स", "होम स्क्रीन कस्टमाइज़", Icons.Filled.DashboardCustomize) {
          onDismiss()
          onOpenCustomize()
        }
      )

      LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        items(items) { item ->
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(14.dp))
              .background(ObsidianElevated)
              .border(1.dp, ObsidianBorderSubtle, RoundedCornerShape(14.dp))
              .clickable { item.onClick() }
              .padding(12.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.fillMaxWidth()
            ) {
              Box(
                modifier = Modifier
                  .size(34.dp)
                  .clip(CircleShape)
                  .background(GoldBrass.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = item.icon,
                  contentDescription = item.title,
                  tint = GoldBrass,
                  modifier = Modifier.size(18.dp)
                )
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = item.title,
                  color = WarmOffWhite,
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Medium
                )
                Text(
                  text = item.subtitle,
                  color = WarmMuted,
                  fontSize = 11.sp
                )
              }
            }
          }
        }
      }
    }
  }
}
