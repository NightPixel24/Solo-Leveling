package com.nightpixel.sololeveling.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

data class LineSeries(val label: String, val color: Color, val values: List<Float>)

/** A from-scratch Canvas multi-series line chart (Compose has no built-in chart API, same
 * reasoning as `RadarChart`) - used for the Analytics tab's stat XP trend (spec Section 6). All
 * series share one x axis (equal-length value lists, one point per day) and one y scale (0 to the
 * highest value across every series), so the lines are directly comparable. Labels/legend are left
 * to the caller (a plain Compose Row of colored dots reads more simply than baking text into the
 * canvas for a handful of short tags). */
@Composable
fun LineChart(series: List<LineSeries>, modifier: Modifier = Modifier) {
    val gridColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
    val maxValue = (series.flatMap { it.values }.maxOrNull() ?: 0f).coerceAtLeast(1f)
    val pointCount = series.maxOfOrNull { it.values.size } ?: 0

    Canvas(modifier = modifier.fillMaxWidth().height(180.dp)) {
        drawLine(gridColor, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 1.dp.toPx())

        if (pointCount < 2) return@Canvas
        val stepX = size.width / (pointCount - 1)

        series.forEach { line ->
            if (line.values.size < 2) return@forEach
            val path = Path()
            line.values.forEachIndexed { index, value ->
                val x = index * stepX
                val y = size.height - (value / maxValue) * size.height
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, color = line.color, style = Stroke(width = 2.dp.toPx()))
        }
    }
}
