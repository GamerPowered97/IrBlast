package com.example.irblast.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.irblast.theme.IrError

enum class ButtonStyleVariant {
    STANDARD,
    POWER,
    ACCENT,
    CIRCLE
}

@Composable
fun IrButton(
    label: String,
    modifier: Modifier = Modifier,
    icon: String = "",
    variant: ButtonStyleVariant = ButtonStyleVariant.STANDARD,
    hapticsEnabled: Boolean = true,
    enabled: Boolean = true,
    shape: Shape = if (variant == ButtonStyleVariant.CIRCLE) CircleShape else RoundedCornerShape(16.dp),
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1.0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 420f),
        label = "button_scale"
    )

    val shadowElevation = if (isPressed) 1.dp else when (variant) {
        ButtonStyleVariant.POWER -> 6.dp
        ButtonStyleVariant.ACCENT -> 5.dp
        ButtonStyleVariant.CIRCLE -> 3.dp
        ButtonStyleVariant.STANDARD -> 3.dp
    }

    val brush = when (variant) {
        ButtonStyleVariant.POWER -> Brush.verticalGradient(
            colors = if (isPressed) listOf(IrError.copy(alpha = 0.85f), IrError.copy(alpha = 0.70f))
            else listOf(Color(0xFFFF5E62), Color(0xFFFF2548))
        )
        ButtonStyleVariant.ACCENT -> Brush.verticalGradient(
            colors = if (isPressed) listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.85f), MaterialTheme.colorScheme.primary.copy(alpha = 0.70f))
            else listOf(Color(0xFF8075FF), MaterialTheme.colorScheme.primary)
        )
        ButtonStyleVariant.CIRCLE -> Brush.verticalGradient(
            colors = if (isPressed) listOf(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f), MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            else listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surface)
        )
        ButtonStyleVariant.STANDARD -> Brush.verticalGradient(
            colors = if (isPressed) listOf(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f), MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            else listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surface)
        )
    }

    val contentColor = when (variant) {
        ButtonStyleVariant.POWER -> Color.White
        ButtonStyleVariant.ACCENT -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurface
    }

    val borderColor = when (variant) {
        ButtonStyleVariant.POWER -> if (isPressed) Color.White.copy(alpha = 0.6f) else Color(0xFFFF8A9E).copy(alpha = 0.5f)
        ButtonStyleVariant.ACCENT -> if (isPressed) Color.White.copy(alpha = 0.6f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        else -> if (isPressed) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    }

    val imageVector = remember(icon, label) { getIconVector(icon, label) }

    Box(
        modifier = modifier
            .scale(scale)
            .shadow(elevation = shadowElevation, shape = shape, spotColor = if (variant == ButtonStyleVariant.POWER) IrError else MaterialTheme.colorScheme.primary)
            .clip(shape)
            .background(brush)
            .border(
                width = if (variant == ButtonStyleVariant.POWER || variant == ButtonStyleVariant.ACCENT) 1.5.dp else 1.dp,
                color = borderColor,
                shape = shape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled
            ) {
                if (hapticsEnabled) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                onClick()
            }
            .padding(horizontal = 8.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (imageVector != null) {
                Icon(
                    imageVector = imageVector,
                    contentDescription = label,
                    tint = contentColor,
                    modifier = Modifier.size(24.dp)
                )
                if (label.isNotBlank() && variant != ButtonStyleVariant.CIRCLE) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = label,
                        color = contentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Text(
                    text = label,
                    color = contentColor,
                    fontSize = if (label.length > 5) 12.sp else 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private fun getIconVector(icon: String, label: String): ImageVector? {
    val key = (icon.ifBlank { label }).trim().lowercase()
    return when {
        "power" in key || "standby" in key -> Icons.Filled.PowerSettingsNew
        "vol_up" in key || "vol +" in key || "volume +" in key -> Icons.AutoMirrored.Filled.VolumeUp
        "vol_down" in key || "vol -" in key || "volume -" in key -> Icons.AutoMirrored.Filled.VolumeDown
        "mute" in key -> Icons.AutoMirrored.Filled.VolumeMute
        key == "up" || key.endsWith(" up") || key.startsWith("up ") || key == "arrow_up" -> Icons.Filled.KeyboardArrowUp
        key == "down" || key.endsWith(" down") || key.startsWith("down ") || key == "arrow_down" -> Icons.Filled.KeyboardArrowDown
        key == "left" || key.endsWith(" left") || key.startsWith("left ") || key == "arrow_left" -> Icons.Filled.KeyboardArrowLeft
        key == "right" || key.endsWith(" right") || key.startsWith("right ") || key == "arrow_right" -> Icons.Filled.KeyboardArrowRight
        key == "check" || key == "tick" -> Icons.Filled.Check
        "home" in key -> Icons.Filled.Home
        "menu" in key -> Icons.Filled.Menu
        "back" in key || "return" in key || "exit" in key -> Icons.AutoMirrored.Filled.ArrowBack
        "input" in key || "source" in key -> Icons.Filled.Tv
        "play" in key -> Icons.Filled.PlayArrow
        "pause" in key -> Icons.Filled.Pause
        "stop" in key -> Icons.Filled.Stop
        "forward" in key || "ff" in key -> Icons.Filled.FastForward
        "rewind" in key || "rew" in key -> Icons.Filled.FastRewind
        else -> null
    }
}
