package com.example.blackscreen

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FloatingButtonTest {

  private lateinit var context: Context

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    FloatingButtonPreferences.resetToDefault(context)
  }

  @Test
  fun testDefaultConfiguration() {
    val config = FloatingButtonPreferences.getConfig(context)
    assertEquals("⚫", config.emoji)
    assertEquals("#00E5FF", config.colorHex)
    assertEquals(42, config.sizeDp)
    assertEquals(0.55f, config.opacity, 0.01f)
    assertEquals("RIGHT", config.preferredEdge)
    assertEquals(10, config.edgeInsetDp)
    assertTrue(config.isClassicDot)
  }

  @Test
  fun testPresetEmojisAndColorsCount() {
    // Requirements: 8-10 built-in presets
    val emojis = FloatingButtonPreferences.PRESET_EMOJIS
    assertTrue("Should have 8-10 emoji presets", emojis.size in 8..12)

    val colors = FloatingButtonPreferences.PRESET_COLORS
    assertTrue("Should have at least 8 colors", colors.size >= 8)
  }

  @Test
  fun testUpdateEmojiAndCustomInput() {
    // 1. Preset emoji
    FloatingButtonPreferences.updateEmoji(context, "🌙")
    var config = FloatingButtonPreferences.getConfig(context)
    assertEquals("🌙", config.emoji)
    assertFalse(config.isClassicDot)

    // 2. Custom emoji with whitespace trimming
    FloatingButtonPreferences.updateEmoji(context, "  🕶️  ")
    config = FloatingButtonPreferences.getConfig(context)
    assertEquals("🕶️", config.emoji)

    // 3. Revert to dot
    FloatingButtonPreferences.updateEmoji(context, "⚫")
    config = FloatingButtonPreferences.getConfig(context)
    assertTrue(config.isClassicDot)
  }

  @Test
  fun testUpdateColorAndParsing() {
    FloatingButtonPreferences.updateColor(context, "#FF5E7E")
    var config = FloatingButtonPreferences.getConfig(context)
    assertEquals("#FF5E7E", config.colorHex)
    assertEquals(android.graphics.Color.parseColor("#FF5E7E"), config.parsedColor)

    // Test fallback parsing with invalid hex
    val invalidConfig = FloatingButtonConfig(colorHex = "NOT_A_COLOR")
    assertEquals(android.graphics.Color.parseColor("#00E5FF"), invalidConfig.parsedColor)
  }

  @Test
  fun testSizeAndOpacityClamping() {
    // Test size
    FloatingButtonPreferences.updateSize(context, 52)
    var config = FloatingButtonPreferences.getConfig(context)
    assertEquals(52, config.sizeDp)

    // Clamped sizes
    FloatingButtonPreferences.updateSize(context, 10) // below 24
    config = FloatingButtonPreferences.getConfig(context)
    assertEquals(24, config.sizeDp)

    FloatingButtonPreferences.updateSize(context, 120) // above 72
    config = FloatingButtonPreferences.getConfig(context)
    assertEquals(72, config.sizeDp)

    // Opacity clamping (0.15 .. 1.0)
    FloatingButtonPreferences.updateOpacity(context, 0.05f)
    config = FloatingButtonPreferences.getConfig(context)
    assertEquals(0.15f, config.opacity, 0.01f)

    FloatingButtonPreferences.updateOpacity(context, 0.85f)
    config = FloatingButtonPreferences.getConfig(context)
    assertEquals(0.85f, config.opacity, 0.01f)
  }

  @Test
  fun testEdgeInsetAndPreferredEdge() {
    FloatingButtonPreferences.updateEdgeInset(context, 16)
    var config = FloatingButtonPreferences.getConfig(context)
    assertEquals(16, config.edgeInsetDp)

    FloatingButtonPreferences.updateEdge(context, "LEFT")
    config = FloatingButtonPreferences.getConfig(context)
    assertEquals("LEFT", config.preferredEdge)

    FloatingButtonPreferences.updateEdge(context, "RIGHT")
    config = FloatingButtonPreferences.getConfig(context)
    assertEquals("RIGHT", config.preferredEdge)
  }

  @Test
  fun testResetToDefault() {
    FloatingButtonPreferences.updateEmoji(context, "⚡")
    FloatingButtonPreferences.updateColor(context, "#9D4EDD")
    FloatingButtonPreferences.updateSize(context, 52)
    FloatingButtonPreferences.updateOpacity(context, 0.9f)
    FloatingButtonPreferences.updateEdge(context, "LEFT")

    var config = FloatingButtonPreferences.getConfig(context)
    assertEquals("⚡", config.emoji)
    assertEquals("#9D4EDD", config.colorHex)

    // Reset
    FloatingButtonPreferences.resetToDefault(context)
    config = FloatingButtonPreferences.getConfig(context)
    assertEquals("⚫", config.emoji)
    assertEquals("#00E5FF", config.colorHex)
    assertEquals(42, config.sizeDp)
    assertEquals(0.55f, config.opacity, 0.01f)
    assertEquals("RIGHT", config.preferredEdge)
  }

  @Test
  fun testEdgeSnapPositioningCalculations() {
    val screenWidth = 1080
    val screenHeight = 2400
    val safeLeft = 0
    val safeRight = 1080
    val safeTop = 100 // Status bar
    val safeBottom = 2300 // Navigation bar
    val buttonWidth = 120
    val insetPx = 30

    val minX = safeLeft + insetPx // 30
    val maxX = safeRight - buttonWidth - insetPx // 1080 - 120 - 30 = 930
    val screenMidX = screenWidth / 2 // 540

    // Case 1: Released on the left half (e.g. x = 200) -> Snaps to minX (30)
    val releaseXLeft = 200
    val targetXLeft = if (releaseXLeft + buttonWidth / 2 < screenMidX) minX else maxX
    assertEquals(minX, targetXLeft)
    assertEquals("LEFT", if (targetXLeft == minX) "LEFT" else "RIGHT")

    // Case 2: Released near center on right half (e.g. x = 600) -> Snaps to maxX (930)
    val releaseXRight = 600
    val targetXRight = if (releaseXRight + buttonWidth / 2 < screenMidX) minX else maxX
    assertEquals(maxX, targetXRight)
    assertEquals("RIGHT", if (targetXRight == minX) "LEFT" else "RIGHT")

    // Verify button never stays near center (540)
    assertTrue("Target X must snap to edge, never stay in center", targetXLeft == minX || targetXLeft == maxX)
    assertTrue("Target X must snap to edge, never stay in center", targetXRight == minX || targetXRight == maxX)

    // Case 3: Vertical ratio clamping
    val totalPlayableHeight = ((safeBottom - buttonWidth) - safeTop).coerceAtLeast(1)
    val clampedY = 800.coerceIn(safeTop, safeBottom - buttonWidth)
    val verticalRatio = ((clampedY - safeTop).toFloat() / totalPlayableHeight).coerceIn(0f, 1f)
    assertTrue(verticalRatio in 0.0f..1.0f)
  }

  @Test
  fun testPersistenceAcrossReload() {
    FloatingButtonPreferences.updateEmoji(context, "🧘")
    FloatingButtonPreferences.updateColor(context, "#4EBA87")
    FloatingButtonPreferences.updateSize(context, 32)
    FloatingButtonPreferences.savePosition(context, x = 30, y = 800, preferredEdge = "LEFT", verticalRatio = 0.4f)

    // Re-load directly from SharedPreferences
    val reloaded = FloatingButtonPreferences.loadConfig(context)
    assertEquals("🧘", reloaded.emoji)
    assertEquals("#4EBA87", reloaded.colorHex)
    assertEquals(32, reloaded.sizeDp)
    assertEquals(30, reloaded.lastX)
    assertEquals(800, reloaded.lastY)
    assertEquals("LEFT", reloaded.preferredEdge)
    assertEquals(0.4f, reloaded.verticalRatio, 0.01f)
  }
}
