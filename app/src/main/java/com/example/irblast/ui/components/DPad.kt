package com.example.irblast.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
            .size(200.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // Up button
        Box(
            modifier = Modifier.align(Alignment.TopCenter),
            contentAlignment = Alignment.Center
        ) {
            IrButton(
                label = "",
                icon = "up",
                variant = ButtonStyleVariant.CIRCLE,
                hapticsEnabled = hapticsEnabled,
                modifier = Modifier.size(54.dp),
                onClick = onUp
            )
        }

        // Down button
        Box(
            modifier = Modifier.align(Alignment.BottomCenter),
            contentAlignment = Alignment.Center
        ) {
            IrButton(
                label = "",
                icon = "down",
                variant = ButtonStyleVariant.CIRCLE,
                hapticsEnabled = hapticsEnabled,
                modifier = Modifier.size(54.dp),
                onClick = onDown
            )
        }

        // Left button
        Box(
            modifier = Modifier.align(Alignment.CenterStart),
            contentAlignment = Alignment.Center
        ) {
            IrButton(
                label = "",
                icon = "left",
                variant = ButtonStyleVariant.CIRCLE,
                hapticsEnabled = hapticsEnabled,
                modifier = Modifier.size(54.dp),
                onClick = onLeft
            )
        }

        // Right button
        Box(
            modifier = Modifier.align(Alignment.CenterEnd),
            contentAlignment = Alignment.Center
        ) {
            IrButton(
                label = "",
                icon = "right",
                variant = ButtonStyleVariant.CIRCLE,
                hapticsEnabled = hapticsEnabled,
                modifier = Modifier.size(54.dp),
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
                modifier = Modifier.size(64.dp),
                shape = CircleShape,
                onClick = onOk
            )
        }
    }
}
