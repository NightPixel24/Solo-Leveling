package com.nightpixel.sololeveling.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nightpixel.sololeveling.ui.theme.accentGradient

/** A thin gradient line - the "HUD accent" touch dropped under a screen's `TabRow`/`TopAppBar`
 * instead of Material3's default flat divider (user feedback, 2026-08-30: "make it look cooler,
 * more modern, futuristic"). */
@Composable
fun GradientDivider(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().height(2.dp).background(accentGradient()))
}
