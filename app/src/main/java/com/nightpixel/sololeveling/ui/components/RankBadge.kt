package com.nightpixel.sololeveling.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.nightpixel.sololeveling.data.gamification.RankTier
import com.nightpixel.sololeveling.ui.theme.SystemCyan
import com.nightpixel.sololeveling.ui.theme.accentGradient

/** Spec Section 5.3 - "sits next to the Stat radar chart on the Dashboard, but the two are
 * visually distinct" (Rank = life trajectory, radar = daily grind). Tapping it opens Life Goals,
 * matching spec Section 8's "Life Goals and Rank detail are reached by tapping the Rank badge."
 * The glow is a hand-drawn radial gradient behind the badge, not a platform `Modifier.shadow` -
 * a real elevation shadow was tried first (user feedback, 2026-08-30: "make it look cooler, more
 * modern, futuristic") but its outline-based shadow renderer approximated the circle as a
 * low-poly hull on real hardware, showing a stray dark hexagon bleeding out from one edge of the
 * badge (user feedback, same day: "theres a hexagon in the logo that looks off") - a radial
 * gradient drawn directly in Canvas is a true circle with no such artifact, on any device. */
@Composable
fun RankBadge(rank: RankTier, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(88.dp).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(SystemCyan.copy(alpha = 0.5f), SystemCyan.copy(alpha = 0f))
                ),
                radius = size.minDimension / 2
            )
        }
        Surface(
            modifier = Modifier.size(72.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        ) {
            Box(
                modifier = Modifier.fillMaxSize().border(2.dp, accentGradient(), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    rank.label,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
