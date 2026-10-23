package com.example.util

import com.example.data.model.JournalistCategory
import com.example.data.model.JournalistEntryEntity
import com.example.data.model.JournalistPersonEntity
import org.json.JSONArray
import org.json.JSONObject

object ExportImportHelper {

  fun exportPersonAsTxt(person: JournalistPersonEntity, entries: List<JournalistEntryEntity>): String {
    val sb = StringBuilder()
    sb.append("═══════════════════════════════════════════════════\n")
    sb.append("       JOURNALIST DOSSIER: ${person.emoji} ${person.name}\n")
    sb.append("═══════════════════════════════════════════════════\n\n")

    val goodCount = entries.count { it.category == JournalistCategory.GOOD }
    val badCount = entries.count { it.category == JournalistCategory.BAD }
    val obsCount = entries.count { it.category == JournalistCategory.OBSERVATION }

    sb.append("SUMMARY:\n")
    sb.append("• कुल Entries: ${entries.size}\n")
    sb.append("• अच्छाई (Good): $goodCount\n")
    sb.append("• बुराई (Bad): $badCount\n")
    sb.append("• Observation: $obsCount\n\n")
    sb.append("───────────────────────────────────────────────────\n")
    sb.append("TIMELINE OF OBSERVATIONS:\n")
    sb.append("───────────────────────────────────────────────────\n\n")

    if (entries.isEmpty()) {
      sb.append("कोई entry नहीं मिली।\n")
    } else {
      for ((idx, entry) in entries.withIndex()) {
        val catHeader = when (entry.category) {
          JournalistCategory.GOOD -> "🟢 अच्छाई"
          JournalistCategory.BAD -> "🔴 बुराई"
          JournalistCategory.OBSERVATION -> "⚪ Observation"
        }
        val timeDisplay = TimeUtils.timeStringToHindi(entry.time)
        val dateDisplay = TimeUtils.formatDateDisplay(entry.date)

        sb.append("${idx + 1}. [$catHeader] $dateDisplay • $timeDisplay\n")
        sb.append("   विवरण: ${entry.text}\n")
        if (entry.context.isNotBlank()) {
          sb.append("   संदर्भ (Context): ${entry.context}\n")
        }
        if (entry.intensity.isNotBlank()) {
          sb.append("   तीव्रता (Intensity): ${entry.intensity}\n")
        }
        sb.append("\n")
      }
    }
    sb.append("═══════════════════════════════════════════════════\n")
    return sb.toString()
  }

  fun exportPersonAsJson(person: JournalistPersonEntity, entries: List<JournalistEntryEntity>): String {
    val root = JSONObject()
    val personObj = JSONObject()
    personObj.put("id", person.id)
    personObj.put("name", person.name)
    personObj.put("emoji", person.emoji)
    personObj.put("createdAt", person.createdAt)
    root.put("person", personObj)

    val entriesArray = JSONArray()
    for (e in entries) {
      val item = JSONObject()
      item.put("id", e.id)
      item.put("category", e.category.name)
      item.put("categoryLabel", e.category.label)
      item.put("text", e.text)
      item.put("date", e.date)
      item.put("time", e.time)
      item.put("timeHindi", TimeUtils.timeStringToHindi(e.time))
      item.put("context", e.context)
      item.put("intensity", e.intensity)
      item.put("timestamp", e.timestamp)
      item.put("createdAt", e.createdAt)
      entriesArray.put(item)
    }
    root.put("entries", entriesArray)
    return root.toString(2)
  }
}
