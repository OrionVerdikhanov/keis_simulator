package com.giftfest.game.ui.component

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.giftfest.game.ui.theme.*

/**
 * Tutorial steps for new players
 */
enum class TutorialStep(
    val emoji: String,
    val title: String,
    val description: String
) {
    WELCOME(
        "🎉",
        "Добро пожаловать в Gift Fest!",
        "Это игра про соединение подарков. Соединяйте одинаковые подарки, чтобы получить более ценные!"
    ),
    SPAWN_GIFT(
        "🎁",
        "Получение подарков",
        "Нажмите кнопку «Получить подарок» внизу экрана, чтобы создать новый подарок на поле."
    ),
    MERGE_GIFTS(
        "🔄",
        "Соединение подарков",
        "Нажмите на подарок, затем на соседний такой же подарок, чтобы соединить их в один более ценный!"
    ),
    COMBO_SYSTEM(
        "🔥",
        "Система комбо",
        "Соединяйте подарки быстро, чтобы набирать комбо! Чем выше комбо, тем больше награда."
    ),
    FEVER_MODE(
        "⚡",
        "Режим фурии",
        "Шкала фурии заполняется при соединениях. Когда она полная — активируется режим удвоенных наград!"
    ),
    ENERGY_SYSTEM(
        "🔋",
        "Энергия",
        "Для создания подарков нужна энергия. Она восстанавливается со временем."
    ),
    SELL_FEATURE(
        "💸",
        "Продажа подарков",
        "Если поле заполнено, можно продать подарок. Нажмите на подарок и используйте кнопку «Продать»."
    ),
    SHOP_AND_WHEEL(
        "🛒",
        "Магазин и колесо",
        "В магазине можно купить усилители. Колесо удачи даёт бесплатные награды каждый день!"
    ),
    PRESTIGE(
        "♻️",
        "Престиж",
        "На 30 уровне откроется престиж — сброс прогресса за постоянные бонусы. Стройте долгосрочную стратегию!"
    ),
    FINISHED(
        "🌟",
        "Удачи!",
        "Теперь вы знаете основы! Соединяйте подарки, повышайте уровень и собирайте коллекцию!"
    )
}

@Composable
fun TutorialOverlay(
    currentStep: TutorialStep,
    onNextStep: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "tutorial")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable(onClick = onNextStep),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = BackgroundCard
            ),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Emoji icon with glow
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    AccentGold.copy(alpha = pulseAlpha),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = currentStep.emoji,
                        fontSize = 48.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = currentStep.title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentGold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = currentStep.description,
                    fontSize = 15.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Progress dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TutorialStep.entries.forEach { step ->
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (step == currentStep) AccentGold
                                    else TextMuted
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onSkip) {
                        Text(
                            text = "Пропустить",
                            color = TextSecondary
                        )
                    }

                    Button(
                        onClick = onNextStep,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ButtonPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (currentStep == TutorialStep.FINISHED) "Начать!" else "Далее",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Tap hint at bottom
        Text(
            text = "Нажмите в любом месте для продолжения",
            fontSize = 12.sp,
            color = TextMuted,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        )
    }
}

/**
 * Hint bubble that appears contextually
 */
@Composable
fun HintBubble(
    text: String,
    emoji: String = "💡",
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(true) }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
    ) {
        Card(
            modifier = modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clickable {
                    visible = false
                    onDismiss()
                },
            colors = CardDefaults.cardColors(
                containerColor = AccentGold.copy(alpha = 0.95f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = emoji, fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = text,
                    fontSize = 13.sp,
                    color = BackgroundDark,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "✕",
                    fontSize = 14.sp,
                    color = BackgroundDark.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * Quick tip that shows briefly
 */
@Composable
fun QuickTip(
    text: String,
    emoji: String = "✨",
    durationMs: Long = 3000L,
    onDismiss: () -> Unit
) {
    var visible by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(durationMs)
        visible = false
        onDismiss()
    }

    AnimatedVisibility(
        visible = visible,
        enter = scaleIn() + fadeIn(),
        exit = scaleOut() + fadeOut()
    ) {
        Card(
            modifier = Modifier.padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = AccentPurple.copy(alpha = 0.9f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = emoji, fontSize = 18.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = text,
                    fontSize = 13.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
