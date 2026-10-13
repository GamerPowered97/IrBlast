package com.example.irblast.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun DPad(
    modifier: Modifier = Modifier,
    hapticsEnabled: Boolean = true,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onLeft: () -> Unit,
    onRight: () -> Unit,
    onOk: () -> Unit
) {
    Box(
        modifier = modifier
            .size(220.dp)
            .shadow(elevation = 8.dp, shape = CircleShape, spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.colorScheme.surface,
                        Color(0xFF07080F)
                    )
                )
            )
            .border(
                width = 2.dp,
                brush = Brush.sweepGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                    )
                ),
                shape = CircleShape
            )
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        // Up button
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            IrButton(
                label = "",
                icon = "up",
                variant = ButtonStyleVariant.CIRCLE,
                hapticsEnabled = hapticsEnabled,
                modifier = Modifier.size(52.dp),
                onClick = onUp
            )
        }

        // Down button
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            IrButton(
                label = "",
                icon = "down",
                variant = ButtonStyleVariant.CIRCLE,
                hapticsEnabled = hapticsEnabled,
                modifier = Modifier.size(52.dp),
                onClick = onDown
            )
        }

        // Left button
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            IrButton(
                label = "",
                icon = "left",
                variant = ButtonStyleVariant.CIRCLE,
                hapticsEnabled = hapticsEnabled,
                modifier = Modifier.size(52.dp),
                onClick = onLeft
            )
        }

        // Right button
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            IrButton(
                label = "",
                icon = "right",
                variant = ButtonStyleVariant.CIRCLE,
                hapticsEnabled = hapticsEnabled,
                modifier = Modifier.size(52.dp),
                onClick = onRight
            )
        }

        // Center OK button
        Box(
            modifier = Modifier.align(Alignment.Center),
            contentAlignment = Alignment.Center
        ) {
            IrButton(
                label = "OK",
                variant = ButtonStyleVariant.ACCENT,
                hapticsEnabled = hapticsEnabled,
                modifier = Modifier.size(68.dp),
                shape = CircleShape,
                onClick = onOk
            )
        }
    }
}
