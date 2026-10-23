package com.example.data.model

/**
 * Energy Mode for the day: Low, Normal, High.
 * Essential routine activities remain intact.
 * Automatically resets to Normal the next day.
 */
enum class EnergyMode(
  val titleEn: String,
  val titleHi: String,
  val icon: String,
  val description: String
) {
  LOW("Low Energy", "कम ऊर्जा", "🔋", "हल्का दिन • सिर्फ आवश्यक कार्य और विश्राम"),
  NORMAL("Normal Energy", "सामान्य ऊर्जा", "⚡", "संतुलित दिनचर्या • सामान्य रूटीन"),
  HIGH("High Energy", "उच्च ऊर्जा", "🚀", "पीक फोकस • उच्च उत्पादकता और लक्ष्य");

  val emoji: String get() = icon
  val label: String get() = titleHi

  companion object {
    fun fromString(value: String?): EnergyMode =
      entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: NORMAL
  }
}
