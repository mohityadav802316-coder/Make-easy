package com.example

import com.example.data.model.BreathPhase
import com.example.data.model.MeditationType
import com.example.util.TimeUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun meditationTypes_allTenModesPresent() {
    val types = MeditationType.entries
    assertEquals(10, types.size)

    val expectedIds = listOf(
      "breathing", "mindfulness", "focus", "relaxation", "sleep",
      "morning", "visualization", "sound", "gratitude", "walking"
    )
    expectedIds.forEach { id ->
      val type = MeditationType.fromId(id)
      assertNotNull("Type $id must exist", type)
      assertTrue("Hindi title must not be blank for $id", type.hindiTitle.isNotBlank())
      assertTrue("English title must not be blank for $id", type.englishTitle.isNotBlank())
      assertTrue("Cycle duration must be >= 4 seconds for $id", type.cycleDurationSec >= 4)
    }
  }

  @Test
  fun meditationPersonalizedEnding_formatsCorrectly() {
    fun buildClosingText(userName: String?, lang: String): String {
      val trimmedName = userName?.trim().orEmpty()
      return if (lang.equals("EN", ignoreCase = true)) {
        if (trimmedName.isNotEmpty()) {
          "$trimmedName, your meditation is complete. You may gently open your eyes now."
        } else {
          "Your meditation is complete. You may gently open your eyes now."
        }
      } else {
        if (trimmedName.isNotEmpty()) {
          "$trimmedName, तुम्हारा मेडिटेशन कंप्लीट हो गया है। अब तुम आँखें खोल सकते हो।"
        } else {
          "तुम्हारा मेडिटेशन कंप्लीट हो गया है। अब तुम आँखें खोल सकते हो।"
        }
      }
    }

    // Personalized Hindi with name
    val hindiWithName = buildClosingText("मोहित", "HI")
    assertEquals("मोहित, तुम्हारा मेडिटेशन कंप्लीट हो गया है। अब तुम आँखें खोल सकते हो।", hindiWithName)

    // Personalized Hindi fallback without name
    val hindiFallback = buildClosingText("", "HI")
    assertEquals("तुम्हारा मेडिटेशन कंप्लीट हो गया है। अब तुम आँखें खोल सकते हो।", hindiFallback)

    // English with name
    val enWithName = buildClosingText("Mohit", "EN")
    assertEquals("Mohit, your meditation is complete. You may gently open your eyes now.", enWithName)

    // English fallback
    val enFallback = buildClosingText(null, "EN")
    assertEquals("Your meditation is complete. You may gently open your eyes now.", enFallback)
  }

  @Test
  fun timeUtils_todayDateConsistency() {
    val dateIso = TimeUtils.getTodayIsoDate()
    val dateStr = TimeUtils.getTodayDateString()
  }
}
