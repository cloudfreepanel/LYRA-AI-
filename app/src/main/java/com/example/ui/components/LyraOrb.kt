package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonViolet
import kotlin.math.cos
import kotlin.math.sin

enum class OrbState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING
}

@Composable
fun LyraOrb(
    state: OrbState,
    rmsLevel: Float = 0f,
    modifier: Modifier = Modifier,
    size: Dp = 150.dp,
    onClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_rotation")

    // Slow continuous rotation
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (state == OrbState.THINKING) 3000 else 10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Breathing pulse for idle & speaking
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (state == OrbState.SPEAKING) 800 else 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing"
    )

    // Audio reactive pulse
    val audioAnim = remember { Animatable(0f) }
    LaunchedEffect(rmsLevel) {
        audioAnim.animateTo(
            targetValue = rmsLevel.coerceIn(0f, 1f),
            animationSpec = tween(durationMillis = 100, easing = LinearEasing)
        )
    }

    Box(
        modifier = modifier
            .size(size)
            .testTag("lyra_orb")
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val baseRadius = this.size.minDimension / 2.8f

            val effectiveScale = when (state) {
                OrbState.IDLE -> breathScale
                OrbState.LISTENING -> 1.0f + (audioAnim.value * 0.45f)
                OrbState.THINKING -> breathScale * 1.05f
                OrbState.SPEAKING -> breathScale + (audioAnim.value * 0.2f)
            }

            val currentRadius = baseRadius * effectiveScale

            // Colors based on state
            val primaryOrbColor = when (state) {
                OrbState.IDLE -> NeonCyan
                OrbState.LISTENING -> NeonCyan
                OrbState.THINKING -> NeonViolet
                OrbState.SPEAKING -> NeonMagenta
            }

            val secondaryOrbColor = when (state) {
                OrbState.IDLE -> NeonViolet
                OrbState.LISTENING -> Color(0xFF38BDF8)
                OrbState.THINKING -> Color(0xFFE879F9)
                OrbState.SPEAKING -> NeonCyan
            }

            // 1. Outer Glow halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryOrbColor.copy(alpha = 0.35f),
                        secondaryOrbColor.copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = currentRadius * 1.7f
                ),
                radius = currentRadius * 1.7f,
                center = center
            )

            // 2. Outer Orbiting Energy Rings
            val numRings = if (state == OrbState.LISTENING || state == OrbState.SPEAKING) 3 else 2
            for (i in 0 until numRings) {
                val ringOffsetAngle = Math.toRadians((rotationAngle * (if (i % 2 == 0) 1 else -1) + (i * 60)).toDouble())
                val ringRadius = currentRadius * (1.18f + (i * 0.16f)) + (audioAnim.value * 12f)
                drawCircle(
                    color = (if (i % 2 == 0) primaryOrbColor else secondaryOrbColor).copy(alpha = 0.4f - (i * 0.1f)),
                    radius = ringRadius,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )

                // Orbiting glowing node
                val nodeX = center.x + (ringRadius * cos(ringOffsetAngle)).toFloat()
                val nodeY = center.y + (ringRadius * sin(ringOffsetAngle)).toFloat()
                drawCircle(
                    color = primaryOrbColor,
                    radius = 4.dp.toPx(),
                    center = Offset(nodeX, nodeY)
                )
            }

            // 3. Main Spherical Core with radial gradient
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.95f),
                        primaryOrbColor.copy(alpha = 0.85f),
                        secondaryOrbColor.copy(alpha = 0.6f),
                        Color(0xFF0F172A).copy(alpha = 0.9f)
                    ),
                    center = Offset(center.x - (currentRadius * 0.2f), center.y - (currentRadius * 0.25f)),
                    radius = currentRadius
                ),
                radius = currentRadius,
                center = center
            )

            // 4. Inner Cybernetic Highlight Ring
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        primaryOrbColor,
                        secondaryOrbColor,
                        primaryOrbColor
                    ),
                    center = center
                ),
                radius = currentRadius * 0.72f,
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )

            // 5. Center Core Pulse
            val corePulseRadius = currentRadius * 0.28f * (if (state == OrbState.LISTENING) 1.0f + audioAnim.value else 1.0f)
            drawCircle(
                color = Color.White.copy(alpha = 0.8f),
                radius = corePulseRadius,
                center = center
            )
        }
    }
}
