package com.example.irblast.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
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

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class ButtonStyleVariant {
    STANDARD,
    POWER,
    ACCENT,
    CIRCLE
}

fun isRepeatableAction(label: String, icon: String = ""): Boolean {
    val key = (icon.ifBlank { label }).trim().uppercase()
    val lbl = label.trim().uppercase()
    return key in listOf("UP", "DOWN", "LEFT", "RIGHT", "VOL_UP", "VOL_DOWN", "CH_UP", "CH_DOWN") ||
            lbl in listOf("UP", "DOWN", "LEFT", "RIGHT", "VOL +", "VOL -", "CH +", "CH -", "VOL+", "VOL-", "CH+", "CH-", "VOL UP", "VOL DOWN", "CH UP", "CH DOWN") ||
            lbl.startsWith("TEMP +") || lbl.startsWith("TEMP -") || lbl.startsWith("TEMP+") || lbl.startsWith("TEMP-") ||
            lbl.contains("VOLUME +") || lbl.contains("VOLUME -") ||
            lbl.contains("CHANNEL +") || lbl.contains("CHANNEL -") ||
            lbl.contains("SPEED +") || lbl.contains("SPEED -") ||
            lbl.contains("FAN +") || lbl.contains("FAN -") ||
            key.contains("TEMP") || key.contains("SPEED")
}

@Composable
fun IrButton(
    label: String,
    modifier: Modifier = Modifier,
    icon: String = "",
    variant: ButtonStyleVariant = ButtonStyleVariant.STANDARD,
    hapticsEnabled: Boolean = true,
    enabled: Boolean = true,
    isEditMode: Boolean = false,
    isRepeatable: Boolean = isRepeatableAction(label, icon),
    shape: Shape = if (variant == ButtonStyleVariant.CIRCLE) CircleShape else RoundedCornerShape(16.dp),
    onLongClick: (() -> Unit)? = null,
    onPressDown: (() -> Unit)? = null,
    onPressUp: (() -> Unit)? = null,
    onClick: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    var isPressedManually by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressedManually) 0.90f else 1.0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 420f),
        label = "button_scale"
    )

    val shadowElevation = if (isPressedManually) 1.dp else when (variant) {
        ButtonStyleVariant.POWER -> 6.dp
        ButtonStyleVariant.ACCENT -> 5.dp
        ButtonStyleVariant.CIRCLE -> 3.dp
        ButtonStyleVariant.STANDARD -> 3.dp
    }

    val brush = when (variant) {
        ButtonStyleVariant.POWER -> Brush.verticalGradient(
            colors = if (isPressedManually) listOf(IrError.copy(alpha = 0.85f), IrError.copy(alpha = 0.70f))
            else listOf(Color(0xFFFF5E62), Color(0xFFFF2548))
        )
        ButtonStyleVariant.ACCENT -> Brush.verticalGradient(
            colors = if (isPressedManually) listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.85f), MaterialTheme.colorScheme.primary.copy(alpha = 0.70f))
            else listOf(Color(0xFF8075FF), MaterialTheme.colorScheme.primary)
        )
        ButtonStyleVariant.CIRCLE -> Brush.verticalGradient(
            colors = if (isPressedManually) listOf(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f), MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            else listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surface)
        )
        ButtonStyleVariant.STANDARD -> Brush.verticalGradient(
            colors = if (isPressedManually) listOf(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f), MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            else listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surface)
        )
    }

    val contentColor = when (variant) {
        ButtonStyleVariant.POWER -> Color.White
        ButtonStyleVariant.ACCENT -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurface
    }

    val borderColor = if (isEditMode) {
        Color(0xFFFFA502) // Amber indicator for edit mode
    } else when (variant) {
        ButtonStyleVariant.POWER -> if (isPressedManually) Color.White.copy(alpha = 0.6f) else Color(0xFFFF8A9E).copy(alpha = 0.5f)
        ButtonStyleVariant.ACCENT -> if (isPressedManually) Color.White.copy(alpha = 0.6f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        else -> if (isPressedManually) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    }

    val imageVector = remember(icon, label) { getIconVector(icon, label) }

    val hasIconAndLabel = imageVector != null && label.isNotBlank() && variant != ButtonStyleVariant.CIRCLE

    val touchModifier = if (!enabled) {
        Modifier
    } else if (isEditMode) {
        Modifier.pointerInput(Unit) {
            detectTapGestures(
                onTap = { onClick() },
                onLongPress = { onLongClick?.invoke() }
            )
        }
    } else if (isRepeatable && onPressDown != null) {
        Modifier.pointerInput(hapticsEnabled, onPressDown, onPressUp) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                down.consume()
                isPressedManually = true
                if (hapticsEnabled) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                onPressDown()
                waitForUpOrCancellation()
                isPressedManually = false
                onPressUp?.invoke()
            }
        }
    } else {
        Modifier.pointerInput(hapticsEnabled, onLongClick, onClick) {
            detectTapGestures(
                onPress = {
                    isPressedManually = true
                    tryAwaitRelease()
                    isPressedManually = false
                },
                onTap = {
                    if (hapticsEnabled) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    onClick()
                },
                onLongPress = if (onLongClick != null) {
                    {
                        if (hapticsEnabled) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        onLongClick()
                    }
                } else null
            )
        }
    }

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 44.dp, minHeight = 44.dp)
            .scale(scale)
            .shadow(elevation = shadowElevation, shape = shape, spotColor = if (variant == ButtonStyleVariant.POWER) IrError else MaterialTheme.colorScheme.primary)
            .clip(shape)
            .background(brush)
            .border(
                width = if (isEditMode) 2.dp else if (variant == ButtonStyleVariant.POWER || variant == ButtonStyleVariant.ACCENT) 1.5.dp else 1.dp,
                color = borderColor,
                shape = shape
            )
            .then(touchModifier)
            .padding(horizontal = 8.dp, vertical = if (hasIconAndLabel) 4.dp else 8.dp),
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
                    modifier = Modifier.size(if (hasIconAndLabel) 20.dp else 24.dp)
                )
                if (hasIconAndLabel) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = label,
                        color = contentColor,
                        fontSize = 10.sp,
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
        "ch_up" in key || "ch +" in key || "channel +" in key -> Icons.Filled.KeyboardArrowUp
        "ch_down" in key || "ch -" in key || "channel -" in key -> Icons.Filled.KeyboardArrowDown
        key == "up" || key.endsWith(" up") || key.startsWith("up ") || key == "arrow_up" || "temp +" in key || "temp up" in key || "speed +" in key -> Icons.Filled.KeyboardArrowUp
        key == "down" || key.endsWith(" down") || key.startsWith("down ") || key == "arrow_down" || "temp -" in key || "temp down" in key || "speed -" in key -> Icons.Filled.KeyboardArrowDown
        key == "left" || key.endsWith(" left") || key.startsWith("left ") || key == "arrow_left" -> Icons.AutoMirrored.Filled.KeyboardArrowLeft
        key == "right" || key.endsWith(" right") || key.startsWith("right ") || key == "arrow_right" -> Icons.AutoMirrored.Filled.KeyboardArrowRight
        key == "check" || key == "tick" -> Icons.Filled.Check
        "home" in key -> Icons.Filled.Home
        "menu" in key -> Icons.Filled.Menu
        "back" in key || "return" in key || "exit" in key -> Icons.AutoMirrored.Filled.ArrowBack
        "input" in key || "source" in key || "hdmi" in key || "keystone" in key -> Icons.Filled.Tv
        "play" in key -> Icons.Filled.PlayArrow
        "pause" in key -> Icons.Filled.Pause
        "stop" in key -> Icons.Filled.Stop
        "forward" in key || "ff" in key -> Icons.Filled.FastForward
        "rewind" in key || "rew" in key -> Icons.Filled.FastRewind
        else -> null
    }
}
