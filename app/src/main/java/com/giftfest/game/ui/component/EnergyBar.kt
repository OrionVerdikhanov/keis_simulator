package com.giftfest.game.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.giftfest.game.ui.theme.*

@Composable
fun EnergyBar(
    energy: Int,
    maxEnergy: Int,
    timeToNextEnergy: Long,
    modifier: Modifier = Modifier
) {
    val progress = (energy.toFloat() / maxEnergy.toFloat()).coerceIn(0f, 1f)
    val isLow = energy < maxEnergy * 0.2f

    val infiniteTransition = rememberInfiniteTransition(label = "energy")

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = if (isLow) 0.5f else 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isLow) 500 else 1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(BackgroundCard, BackgroundCard.copy(alpha = 0.8f))
                ),
                shape = RoundedCornerShape(14.dp)
            )
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        EnergyBar.copy(alpha = 0.3f * pulseAlpha),
                        EnergyBarGlow.copy(alpha = 0.5f * pulseAlpha),
                        EnergyBar.copy(alpha = 0.3f * pulseAlpha)
                    )
                ),
                shape = RoundedCornerShape(14.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "⚡",
            fontSize = 22.sp
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$energy",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isLow) AccentRed else EnergyBar
                    )
                    Text(
                        text = " / $maxEnergy",
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                }

                if (energy < maxEnergy && timeToNextEnergy > 0) {
                    Row(
                        modifier = Modifier
                            .background(
                                color = BackgroundCell,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "+1",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = EnergyBar
                        )
                        Text(
                            text = " ${formatTime(timeToNextEnergy)}",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                } else if (energy >= maxEnergy) {
                    Text(
                        text = "FULL",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentGreen,
                        modifier = Modifier
                            .background(
                                color = AccentGreen.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(BackgroundCell)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = if (isLow) {
                                    listOf(AccentRed, AccentOrange)
                                } else {
                                    listOf(EnergyBar, EnergyBarGlow, EnergyBar)
                                }
                            )
                        )
                )

                if (progress > 0.1f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress * 0.8f)
                            .offset(x = (shimmerOffset * 100).dp)
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.White.copy(alpha = 0.3f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }
            }
        }
    }
}

private fun formatTime(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
