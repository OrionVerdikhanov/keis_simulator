package com.giftfest.game.ui.screen

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
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
 * Daily quest data class
 */
data class DailyQuest(
    val id: String,
    val emoji: String,
    val title: String,
    val description: String,
    val progress: Int,
    val target: Int,
    val rewardEmoji: String,
    val rewardText: String,
    val rewardAmount: Int,
    val isCompleted: Boolean = false,
    val isClaimed: Boolean = false
)

@Composable
fun QuestsScreen(
    dailyQuests: List<DailyQuest>,
    timeUntilReset: String,
    completedToday: Int,
    totalQuests: Int,
    onClaimReward: (String) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(BackgroundDark, Color(0xFF051208))
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        QuestsHeader(
            completedToday = completedToday,
            totalQuests = totalQuests,
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                QuestsInfoCard(
                    timeUntilReset = timeUntilReset,
                    completedToday = completedToday,
                    totalQuests = totalQuests
                )
            }

            item {
                Text(
                    text = "📋 Ежедневные задания",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            items(dailyQuests) { quest ->
                QuestItem(
                    quest = quest,
                    onClaimReward = { onClaimReward(quest.id) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                BonusRewardCard(
                    completedToday = completedToday,
                    totalQuests = totalQuests
                )
            }
        }
    }
}

@Composable
private fun QuestsHeader(
    completedToday: Int,
    totalQuests: Int,
    onBack: () -> Unit
) {
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

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "📜 Задания",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = AccentGold
            )
            Text(
                text = "Выполнено: $completedToday из $totalQuests",
                fontSize = 12.sp,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun QuestsInfoCard(
    timeUntilReset: String,
    completedToday: Int,
    totalQuests: Int
) {
    val progress = completedToday.toFloat() / totalQuests.toFloat()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = BackgroundCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Обновление через",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = timeUntilReset,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentOrange
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Прогресс",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = "$completedToday / $totalQuests",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentGold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(BackgroundCell)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(AccentGold, AccentOrange)
                            )
                        )
                )
            }
        }
    }
}

@Composable
private fun QuestItem(
    quest: DailyQuest,
    onClaimReward: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "quest")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    val progress = quest.progress.toFloat() / quest.target.toFloat()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (quest.isCompleted && !quest.isClaimed) {
                    Modifier.border(
                        width = 2.dp,
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                AccentGold.copy(alpha = glowAlpha),
                                AccentOrange.copy(alpha = glowAlpha)
                            )
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                } else Modifier
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (quest.isClaimed)
                BackgroundCard.copy(alpha = 0.5f)
            else
                BackgroundCard
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Quest emoji icon
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                AccentPurple.copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (quest.isClaimed) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = AccentGreen,
                        modifier = Modifier.size(32.dp)
                    )
                } else {
                    Text(text = quest.emoji, fontSize = 28.sp)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = quest.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (quest.isClaimed) TextMuted else TextPrimary
                )
                Text(
                    text = quest.description,
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Progress bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(BackgroundCell)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .background(
                                if (quest.isCompleted) AccentGreen else AccentBlue
                            )
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${quest.progress} / ${quest.target}",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Reward section
            if (quest.isClaimed) {
                Text(
                    text = "✓",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentGreen
                )
            } else if (quest.isCompleted) {
                Button(
                    onClick = onClaimReward,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentGold
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Забрать",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = BackgroundDark
                    )
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = quest.rewardEmoji, fontSize = 20.sp)
                    Text(
                        text = "+${quest.rewardAmount}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentGold
                    )
                }
            }
        }
    }
}

@Composable
private fun BonusRewardCard(
    completedToday: Int,
    totalQuests: Int
) {
    val allCompleted = completedToday >= totalQuests

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (allCompleted)
                AccentGold.copy(alpha = 0.2f)
            else
                BackgroundCard
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🎁 Бонус за все задания",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                BonusItem(emoji = "💰", value = "500", label = "Монет")
                BonusItem(emoji = "💎", value = "10", label = "Кристаллов")
                BonusItem(emoji = "⚡", value = "20", label = "Энергии")
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (allCompleted) {
                Button(
                    onClick = { },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentGold
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "🎉 Забрать бонус!",
                        fontWeight = FontWeight.Bold,
                        color = BackgroundDark
                    )
                }
            } else {
                Text(
                    text = "Выполните все задания, чтобы получить бонус",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun BonusItem(
    emoji: String,
    value: String,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = emoji, fontSize = 24.sp)
        Text(
            text = "+$value",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = AccentGold
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = TextSecondary
        )
    }
}

/**
 * Sample daily quests for demonstration
 */
fun getSampleDailyQuests(): List<DailyQuest> = listOf(
    DailyQuest(
        id = "merge_10",
        emoji = "🔄",
        title = "Соединитель",
        description = "Соедините 10 подарков",
        progress = 3,
        target = 10,
        rewardEmoji = "💰",
        rewardText = "монет",
        rewardAmount = 100
    ),
    DailyQuest(
        id = "combo_5",
        emoji = "🔥",
        title = "Комбо-мастер",
        description = "Достигните 5x комбо",
        progress = 2,
        target = 5,
        rewardEmoji = "💎",
        rewardText = "кристаллов",
        rewardAmount = 5
    ),
    DailyQuest(
        id = "level_3",
        emoji = "⭐",
        title = "Улучшатель",
        description = "Создайте подарок 3 уровня",
        progress = 1,
        target = 1,
        rewardEmoji = "⚡",
        rewardText = "энергии",
        rewardAmount = 10,
        isCompleted = true
    ),
    DailyQuest(
        id = "fever_1",
        emoji = "⚡",
        title = "Фурия",
        description = "Активируйте режим фурии",
        progress = 0,
        target = 1,
        rewardEmoji = "💰",
        rewardText = "монет",
        rewardAmount = 200
    ),
    DailyQuest(
        id = "sell_5",
        emoji = "💸",
        title = "Торговец",
        description = "Продайте 5 подарков",
        progress = 0,
        target = 5,
        rewardEmoji = "💎",
        rewardText = "кристаллов",
        rewardAmount = 3
    )
)
