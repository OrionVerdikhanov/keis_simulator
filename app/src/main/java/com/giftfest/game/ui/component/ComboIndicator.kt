package com.giftfest.game.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.giftfest.game.domain.model.GameState
import com.giftfest.game.ui.theme.*

@Composable
fun ComboIndicator(
    combo: Int,
    multiplier: Float,
    timeLeft: Long,
    modifier: Modifier = Modifier
) {
    if (combo <= 0) return

    val infiniteTransition = rememberInfiniteTransition(label = "combo")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (combo >= 5) 1.1f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(300),
            repeatMode = RepeatMode.Reverse
        ),
        label = "combo_scale"
    )

    val comboColor = when {
        combo >= 8 -> Color(0xFFFF4500) // Red-orange
        combo >= 5 -> Color(0xFFFFD700) // Gold
        combo >= 3 -> Color(0xFF4CAF50) // Green
        else -> Color(0xFF2196F3) // Blue
    }

    val progress = (timeLeft.toFloat() / GameState.COMBO_TIMEOUT_MS).coerceIn(0f, 1f)

    Row(
        modifier = modifier
            .scale(scale)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(comboColor.copy(alpha = 0.3f), comboColor.copy(alpha = 0.1f))
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Combo count
        Text(
            text = "${combo}x",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = comboColor
        )

        // Multiplier
        Text(
            text = "×${String.format("%.1f", multiplier)}",
            fontSize = 12.sp,
            color = TextSecondary
        )

        // Timer bar
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .width(40.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = comboColor,
            trackColor = Color.Gray.copy(alpha = 0.3f)
        )
    }
}

@Composable
fun FeverIndicator(
    progress: Float,
    isActive: Boolean,
    timeLeft: Long,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "fever")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isActive) 1.05f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fever_pulse"
    )

    val rainbowOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rainbow"
    )

    val feverColors = listOf(
        Color(0xFFFF6B6B),
        Color(0xFFFFE66D),
        Color(0xFF4ECDC4),
        Color(0xFF45B7D1),
        Color(0xFFFF6B6B)
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .scale(pulseScale)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isActive) "🔥 FEVER MODE!" else "🔥 Fever",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isActive) Color(0xFFFF6B6B) else TextSecondary
            )

            if (isActive) {
                Text(
                    text = "${timeLeft / 1000}s",
                    fontSize = 12.sp,
                    color = Color(0xFFFF6B6B)
                )
            } else {
                Text(
                    text = "${(progress * 100).toInt()}%",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.Gray.copy(alpha = 0.3f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(if (isActive) 1f else progress)
                    .background(
                        brush = if (isActive) {
                            Brush.horizontalGradient(
                                colors = feverColors,
                                startX = -100f + (rainbowOffset * 500f),
                                endX = 400f + (rainbowOffset * 500f)
                            )
                        } else {
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFFFF6B6B), Color(0xFFFFE66D))
                            )
                        }
                    )
            )
        }
    }
}

@Composable
fun ActiveBoostersBar(
    boosters: Map<com.giftfest.game.domain.model.ActiveBooster, Long>,
    modifier: Modifier = Modifier
) {
    if (boosters.isEmpty()) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        boosters.forEach { (booster, timeLeft) ->
            BoosterBadge(
                emoji = booster.type.emoji,
                name = booster.type.displayName,
                timeLeft = timeLeft
            )
        }
    }
}

@Composable
private fun BoosterBadge(
    emoji: String,
    name: String,
    timeLeft: Long
) {
    val seconds = (timeLeft / 1000).toInt()
    val minutes = seconds / 60
    val secs = seconds % 60
    val timeText = if (minutes > 0) "${minutes}m" else "${secs}s"

    Row(
        modifier = Modifier
            .background(
                color = BackgroundCard,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = emoji, fontSize = 14.sp)
        Text(
            text = timeText,
            fontSize = 10.sp,
            color = AccentGold
        )
    }
}
