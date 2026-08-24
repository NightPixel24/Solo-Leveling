package com.nightpixel.sololeveling.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Pentagon radar chart (spec Section 5.1) - one axis per stat, each plotted as a 0f..1f
 * fraction (normalized level) so a lopsided shape is visible at a glance. Generic over axis
 * count, though this app always calls it with the 5 stats. */
@Composable
fun RadarChart(
    values: List<Pair<String, Float>>,
    modifier: Modifier = Modifier,
    fillColor: Color = MaterialTheme.colorScheme.primary
) {
    val textMeasurer = rememberTextMeasurer()
    val gridColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
    val labelStyle = TextStyle(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
    val axisCount = values.size

    Canvas(modifier = modifier.fillMaxWidth().aspectRatio(1f)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2f * 0.65f
        val angleStep = (2 * PI / axisCount).toFloat()

        fun pointAt(index: Int, fraction: Float): Offset {
            val angle = -PI.toFloat() / 2f + index * angleStep
            return Offset(
                center.x + radius * fraction * cos(angle),
                center.y + radius * fraction * sin(angle)
            )
        }

        listOf(0.25f, 0.5f, 0.75f, 1f).forEach { fraction ->
            val ring = Path().apply {
                moveTo(pointAt(0, fraction).x, pointAt(0, fraction).y)
                for (i in 1 until axisCount) {
                    val p = pointAt(i, fraction)
                    lineTo(p.x, p.y)
                }
                close()
            }
            drawPath(ring, color = gridColor, style = Stroke(width = 1.dp.toPx()))
        }

        for (i in 0 until axisCount) {
            drawLine(gridColor, center, pointAt(i, 1f), strokeWidth = 1.dp.toPx())
        }

        val dataPath = Path().apply {
            val first = pointAt(0, values[0].second.coerceIn(0f, 1f))
            moveTo(first.x, first.y)
            for (i in 1 until axisCount) {
                val p = pointAt(i, values[i].second.coerceIn(0f, 1f))
                lineTo(p.x, p.y)
            }
            close()
        }
        drawPath(dataPath, color = fillColor.copy(alpha = 0.35f))
        drawPath(dataPath, color = fillColor, style = Stroke(width = 2.dp.toPx()))

        for (i in 0 until axisCount) {
            val labelPoint = pointAt(i, 1.22f)
            val measured = textMeasurer.measure(values[i].first, style = labelStyle)
            drawText(
                textMeasurer = textMeasurer,
                text = values[i].first,
                topLeft = Offset(
                    labelPoint.x - measured.size.width / 2f,
                    labelPoint.y - measured.size.height / 2f
                ),
                style = labelStyle
            )
        }
    }
}
