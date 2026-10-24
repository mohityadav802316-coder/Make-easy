package com.example.blackscreen

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Data model for Floating Black Screen Button customization & snap behavior.
 */
data class FloatingButtonConfig(
  val emoji: String = "⚫",
  val colorHex: String = "#00E5FF",
  val sizeDp: Int = 42,
  val opacity: Float = 0.55f,
  val preferredEdge: String = "RIGHT", // "LEFT" or "RIGHT"
  val edgeInsetDp: Int = 10,
  val verticalRatio: Float = 0.35f, // Proportional Y position (0.0..1.0)
  val lastX: Int = 80,
  val lastY: Int = 260
) {
  val parsedColor: Int
    get() = try {
      Color.parseColor(colorHex)
    } catch (_: Exception) {
      Color.parseColor("#00E5FF")
    }

  val isClassicDot: Boolean
    get() = emoji.isBlank() || emoji == "⚫"
}

/**
 * Manages persistent storage and reactive state for the Floating Black Screen Button.
 * Uses Android SharedPreferences for zero-migration compatibility and instant sync.
 */
object FloatingButtonPreferences {

  private const val PREFS_NAME = "floating_black_screen_button_prefs"

  private const val KEY_EMOJI = "btn_emoji"
  private const val KEY_COLOR_HEX = "btn_color_hex"
  private const val KEY_SIZE_DP = "btn_size_dp"
  private const val KEY_OPACITY = "btn_opacity"
  private const val KEY_PREFERRED_EDGE = "btn_preferred_edge"
  private const val KEY_EDGE_INSET_DP = "btn_edge_inset_dp"
  private const val KEY_VERTICAL_RATIO = "btn_vertical_ratio"
  private const val KEY_LAST_X = "btn_last_x"
  private const val KEY_LAST_Y = "btn_last_y"

  val DEFAULT_CONFIG = FloatingButtonConfig(
    emoji = "⚫",
    colorHex = "#00E5FF",
    sizeDp = 42,
    opacity = 0.55f,
    preferredEdge = "RIGHT",
    edgeInsetDp = 10,
    verticalRatio = 0.35f,
    lastX = 80,
    lastY = 260
  )

  // 10 Curated Presets for discreet, focus, and utility controls
  val PRESET_EMOJIS = listOf(
    "⚫" to "Classic Dot",
    "🌙" to "Night / Sleep",
    "⚡" to "Quick Power",
    "👁️" to "Stealth",
    "✨" to "Focus",
    "🧘" to "Meditation",
    "🔒" to "Lock",
    "🛡️" to "Shield",
    "🖤" to "Black Heart",
    "🔘" to "Pill Dot"
  )

  // Curated color palette matching Material Design 3 and Obsidian Dark theme
  val PRESET_COLORS = listOf(
    "#00E5FF" to "Cyan Neon",
    "#C6A664" to "Obsidian Gold",
    "#9D4EDD" to "Violet Glow",
    "#4EBA87" to "Sage Green",
    "#FF5E7E" to "Coral Rose",
    "#FFAA00" to "Amber Sun",
    "#4361EE" to "Deep Blue",
    "#1A1D24" to "Stealth Dark"
  )

  val SIZES = listOf(
    32 to "Small (32dp)",
    42 to "Medium (42dp)",
    52 to "Large (52dp)"
  )

  val INSETS = listOf(
    6 to "Compact (6dp)",
    10 to "Standard (10dp)",
    16 to "Relaxed (16dp)"
  )

  private val _configFlow = MutableStateFlow(DEFAULT_CONFIG)
  val configFlow: StateFlow<FloatingButtonConfig> = _configFlow.asStateFlow()

  private var isInitialized = false

  fun init(context: Context) {
    if (!isInitialized) {
      _configFlow.value = loadConfig(context)
      isInitialized = true
    }
  }

  fun getConfig(context: Context): FloatingButtonConfig {
    if (!isInitialized) {
      init(context)
    }
    return _configFlow.value
  }

  private fun getPrefs(context: Context): SharedPreferences {
    return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
  }

  fun loadConfig(context: Context): FloatingButtonConfig {
    val prefs = getPrefs(context)
    return FloatingButtonConfig(
      emoji = prefs.getString(KEY_EMOJI, DEFAULT_CONFIG.emoji) ?: DEFAULT_CONFIG.emoji,
      colorHex = prefs.getString(KEY_COLOR_HEX, DEFAULT_CONFIG.colorHex) ?: DEFAULT_CONFIG.colorHex,
      sizeDp = prefs.getInt(KEY_SIZE_DP, DEFAULT_CONFIG.sizeDp),
      opacity = prefs.getFloat(KEY_OPACITY, DEFAULT_CONFIG.opacity),
      preferredEdge = prefs.getString(KEY_PREFERRED_EDGE, DEFAULT_CONFIG.preferredEdge) ?: DEFAULT_CONFIG.preferredEdge,
      edgeInsetDp = prefs.getInt(KEY_EDGE_INSET_DP, DEFAULT_CONFIG.edgeInsetDp),
      verticalRatio = prefs.getFloat(KEY_VERTICAL_RATIO, DEFAULT_CONFIG.verticalRatio),
      lastX = prefs.getInt(KEY_LAST_X, DEFAULT_CONFIG.lastX),
      lastY = prefs.getInt(KEY_LAST_Y, DEFAULT_CONFIG.lastY)
    )
  }

  fun updateEmoji(context: Context, emoji: String) {
    val clean = emoji.trim()
    val updated = getConfig(context).copy(emoji = clean)
    saveConfig(context, updated)
  }

  fun updateColor(context: Context, colorHex: String) {
    val updated = getConfig(context).copy(colorHex = colorHex)
    saveConfig(context, updated)
  }

  fun updateSize(context: Context, sizeDp: Int) {
    val updated = getConfig(context).copy(sizeDp = sizeDp.coerceIn(24, 72))
    saveConfig(context, updated)
  }

  fun updateOpacity(context: Context, opacity: Float) {
    val updated = getConfig(context).copy(opacity = opacity.coerceIn(0.15f, 1.0f))
    saveConfig(context, updated)
  }

  fun updateEdgeInset(context: Context, insetDp: Int) {
    val updated = getConfig(context).copy(edgeInsetDp = insetDp.coerceIn(4, 24))
    saveConfig(context, updated)
  }

  fun updateEdge(context: Context, edge: String) {
    val updated = getConfig(context).copy(preferredEdge = if (edge == "LEFT") "LEFT" else "RIGHT")
    saveConfig(context, updated)
  }

  fun savePosition(
    context: Context,
    x: Int,
    y: Int,
    preferredEdge: String,
    verticalRatio: Float
  ) {
    val current = getConfig(context)
    val updated = current.copy(
      lastX = x,
      lastY = y,
      preferredEdge = preferredEdge,
      verticalRatio = verticalRatio.coerceIn(0.0f, 1.0f)
    )
    saveConfig(context, updated)
  }

  fun resetToDefault(context: Context) {
    saveConfig(context, DEFAULT_CONFIG)
  }

  private fun saveConfig(context: Context, config: FloatingButtonConfig) {
    _configFlow.value = config
    getPrefs(context).edit().apply {
      putString(KEY_EMOJI, config.emoji)
      putString(KEY_COLOR_HEX, config.colorHex)
      putInt(KEY_SIZE_DP, config.sizeDp)
      putFloat(KEY_OPACITY, config.opacity)
      putString(KEY_PREFERRED_EDGE, config.preferredEdge)
      putInt(KEY_EDGE_INSET_DP, config.edgeInsetDp)
      putFloat(KEY_VERTICAL_RATIO, config.verticalRatio)
      putInt(KEY_LAST_X, config.lastX)
      putInt(KEY_LAST_Y, config.lastY)
      apply()
    }
    // Notify active service to re-render floating button immediately
    BlackScreenManager.notifyConfigChanged(context)
  }
}
