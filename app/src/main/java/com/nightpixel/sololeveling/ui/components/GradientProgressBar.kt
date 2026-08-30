package com.nightpixel.sololeveling.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/** A `LinearProgressIndicator` replacement with a gradient fill (`baseColor` fading into a
 * brighter tint of itself) and a soft glow at the filled tip, instead of one flat color -
 * user feedback, 2026-08-30: "make it look cooler, more modern, futuristic". Used for the
 * Dashboard's stat XP bars; the track background and fill both round off at the same radius as
 * the bar's own height so it reads as a smooth capsule, not a hard-edged rectangle. */
@Composable
fun GradientProgressBar(
    progress: Float,
    baseColor: Color,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 8.dp
) {
    val clamped = progress.coerceIn(0f, 1f)
    Canvas(modifier = modifier.fillMaxWidth().height(height)) {
        val radius = CornerRadius(size.height / 2, size.height / 2)
        drawRoundRect(
            color = baseColor.copy(alpha = 0.18f),
            cornerRadius = radius
        )
        if (clamped > 0f) {
            val fillWidth = size.width * clamped
            drawRoundRect(
                brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                    listOf(baseColor.copy(alpha = 0.85f), baseColor)
                ),
                size = size.copy(width = fillWidth),
                cornerRadius = radius
            )
            // A brighter glow ring right at the leading edge of the fill - the "energy bar tip"
            // touch that reads as a HUD element rather than a stock progress bar.
            drawCircle(
                color = baseColor,
                radius = size.height * 0.55f,
                center = androidx.compose.ui.geometry.Offset(fillWidth, size.height / 2),
                style = Stroke(width = size.height * 0.25f)
            )
        }
    }
}
