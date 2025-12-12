package com.giftfest.game.ui.screen

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.giftfest.game.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun WheelScreen(
    gems: Int,
    freeSpins: Int,
    isSpinning: Boolean,
    onSpin: () -> Unit,
    onBack: () -> Unit
) {
    val wheelRotation = remember { Animatable(0f) }

    LaunchedEffect(isSpinning) {
        if (isSpinning) {
            wheelRotation.animateTo(
                targetValue = wheelRotation.value + 1800f + (0..360).random(),
                animationSpec = tween(
                    durationMillis = 3000,
                    easing = FastOutSlowInEasing
                )
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(BackgroundDark, Color(0xFF051208))
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        WheelHeader(gems = gems, onBack = onBack)

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "🎰 Колесо удачи",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = AccentGold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Крутите и выигрывайте призы!",
            fontSize = 14.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.weight(0.3f))

        Box(
            modifier = Modifier.size(300.dp),
            contentAlignment = Alignment.Center
        ) {
            LuckyWheel(
                rotation = wheelRotation.value,
                modifier = Modifier.fillMaxSize()
            )

            WheelPointer(modifier = Modifier.align(Alignment.TopCenter))
        }

        Spacer(modifier = Modifier.weight(0.3f))

        SpinInfo(freeSpins = freeSpins)

        Spacer(modifier = Modifier.height(16.dp))

        SpinButton(
            enabled = !isSpinning && (freeSpins > 0 || gems >= 10),
            isFree = freeSpins > 0,
            onClick = onSpin
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun WheelHeader(gems: Int, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(40.dp)
                .background(BackgroundCard, CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Назад",
                tint = TextPrimary
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier
                .background(BackgroundCard, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "💎", fontSize = 16.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "$gems",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = AccentBlue
            )
        }
    }
}

@Composable
private fun LuckyWheel(
    rotation: Float,
    modifier: Modifier = Modifier
) {
    val prizes = listOf(
        WheelPrizeDisplay("💰", "100", AccentGold),
        WheelPrizeDisplay("💎", "5", AccentBlue),
        WheelPrizeDisplay("⚡", "20", EnergyBar),
        WheelPrizeDisplay("🎁", "Особый", AccentPurple),
        WheelPrizeDisplay("💰", "500", AccentOrange),
        WheelPrizeDisplay("💎", "15", Color(0xFF00BCD4)),
        WheelPrizeDisplay("⚡", "50", Color(0xFF8BC34A)),
        WheelPrizeDisplay("🔥", "Фурия", AccentRed)
    )

    Canvas(
        modifier = modifier.rotate(rotation)
    ) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 2 - 10f
        val sweepAngle = 360f / prizes.size

        prizes.forEachIndexed { index, prize ->
            val startAngle = index * sweepAngle - 90f

            rotate(startAngle + sweepAngle / 2, center) {
                drawArc(
                    color = if (index % 2 == 0) prize.color.copy(alpha = 0.8f)
                           else prize.color.copy(alpha = 0.6f),
                    startAngle = -sweepAngle / 2,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2)
                )
            }

            val angle = Math.toRadians((startAngle + sweepAngle / 2).toDouble())
            val textRadius = radius * 0.65f
            val textX = center.x + textRadius * cos(angle).toFloat()
            val textY = center.y + textRadius * sin(angle).toFloat()

            drawContext.canvas.nativeCanvas.apply {
                drawText(
                    prize.emoji,
                    textX,
                    textY + 10f,
                    android.graphics.Paint().apply {
                        textSize = 36f
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                )
            }
        }

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(BackgroundCard, BackgroundDark)
            ),
            radius = radius * 0.2f,
            center = center
        )
    }
}

@Composable
private fun WheelPointer(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "pointer")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = modifier
            .offset(y = (-8).dp)
            .scale(scale)
    ) {
        Text(
            text = "▼",
            fontSize = 40.sp,
            color = AccentGold
        )
    }
}

@Composable
private fun SpinInfo(freeSpins: Int) {
    Card(
        modifier = Modifier.padding(horizontal = 32.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$freeSpins",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = ExpBar
                )
                Text(
                    text = "Бесплатно",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(40.dp)
                    .background(TextSecondary.copy(alpha = 0.3f))
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "💎", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "10",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentBlue
                    )
                }
                Text(
                    text = "за вращение",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun SpinButton(
    enabled: Boolean,
    isFree: Boolean,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "spin")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .padding(horizontal = 32.dp)
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isFree) ExpBar else AccentPurple,
            disabledContainerColor = ButtonDisabled
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = if (isFree) "🎰 КРУТИТЬ БЕСПЛАТНО!" else "🎰 КРУТИТЬ (10💎)",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private data class WheelPrizeDisplay(
    val emoji: String,
    val value: String,
    val color: Color
)
