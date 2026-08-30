package com.nightpixel.sololeveling.ui.components

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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import com.nightpixel.sololeveling.data.gamification.RankTier
import com.nightpixel.sololeveling.ui.theme.SystemCyan
import com.nightpixel.sololeveling.ui.theme.accentGradient

/** Spec Section 5.3 - "sits next to the Stat radar chart on the Dashboard, but the two are
 * visually distinct" (Rank = life trajectory, radar = daily grind). Tapping it opens Life Goals,
 * matching spec Section 8's "Life Goals and Rank detail are reached by tapping the Rank badge."
 * The colored `Modifier.shadow` (ambient/spot tinted cyan) is what gives this its glow - a flat
 * `BorderStroke` alone read as a plain outlined circle (user feedback, 2026-08-30: "make it look
 * cooler, more modern, futuristic"), so the ring is now a gradient border instead, with a real
 * soft glow behind it. */
@Composable
fun RankBadge(rank: RankTier, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .size(72.dp)
            .shadow(
                elevation = 16.dp,
                shape = CircleShape,
                ambientColor = SystemCyan,
                spotColor = SystemCyan
            )
            .clickable(onClick = onClick),
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
