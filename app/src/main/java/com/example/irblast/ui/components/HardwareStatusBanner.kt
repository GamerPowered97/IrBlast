package com.example.irblast.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.SensorsOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.irblast.theme.IrError
import com.example.irblast.theme.IrSuccess
import com.example.irblast.theme.IrWarning

@Composable
fun HardwareStatusBanner(
    isHardwareAvailable: Boolean,
    isBlasting: Boolean = false,
    modifier: Modifier = Modifier
) {
    val statusColor = if (isHardwareAvailable) IrSuccess else IrWarning
    val shape = RoundedCornerShape(14.dp)
    val blastingColor = if (isHardwareAvailable) IrError else IrWarning
    val blastingLabel = if (isHardwareAvailable) "TRANSMITTING" else "SIMULATING"

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                if (isBlasting) {
                    Brush.horizontalGradient(
                        listOf(
                            blastingColor.copy(alpha = 0.25f),
                            blastingColor.copy(alpha = 0.35f)
                        )
                    )
                } else {
                    Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                        )
                    )
                }
            )
            .border(
                width = 1.dp,
                color = if (isBlasting) blastingColor.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = shape
            )
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Status glowing dot
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .shadow(4.dp, CircleShape, spotColor = statusColor)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = if (isHardwareAvailable) Icons.Default.Sensors else Icons.Default.SensorsOff,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isHardwareAvailable) "IR Blaster Ready" else "Simulation Mode",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            AnimatedVisibility(
                visible = isBlasting,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .shadow(4.dp, CircleShape, spotColor = blastingColor)
                            .clip(CircleShape)
                            .background(blastingColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = blastingLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = blastingColor,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}
