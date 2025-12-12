package com.giftfest.game.ui.screen

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.giftfest.game.domain.model.GiftType
import com.giftfest.game.domain.model.PlayerStatistics
import com.giftfest.game.ui.theme.*

@Composable
fun StatsScreen(
    statistics: PlayerStatistics,
    level: Int,
    totalMerges: Int,
    highestGiftLevel: Int,
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
        StatsHeader(onBack = onBack)

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                PlayerLevelCard(level = level)
            }

            item {
                GameStatsCard(
                    totalMerges = totalMerges,
                    highestGiftLevel = highestGiftLevel,
                    statistics = statistics
                )
            }

            item {
                EarningsCard(statistics = statistics)
            }

            item {
                AchievementsPreview(statistics = statistics)
            }

            item {
                CollectionPreview(highestGiftLevel = highestGiftLevel)
            }
        }
    }
}

@Composable
private fun StatsHeader(onBack: () -> Unit) {
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
                contentDescription = "Back",
                tint = TextPrimary
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = "📊 Statistics",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = AccentGold
        )
    }
}

@Composable
private fun PlayerLevelCard(level: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "level")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 2.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        AccentGold.copy(alpha = glowAlpha),
                        AccentOrange.copy(alpha = glowAlpha)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = BackgroundCard),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(AccentGold, AccentOrange)
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$level",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = BackgroundDark
                    )
                    Text(
                        text = "LEVEL",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = BackgroundDark.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(20.dp))

            Column {
                Text(
                    text = getRankTitle(level),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = getNextRankInfo(level),
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun GameStatsCard(
    totalMerges: Int,
    highestGiftLevel: Int,
    statistics: PlayerStatistics
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = BackgroundCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "🎮 Game Stats",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    icon = "🔄",
                    value = formatNumber(totalMerges.toLong()),
                    label = "Total Merges"
                )
                StatItem(
                    icon = "⭐",
                    value = "$highestGiftLevel",
                    label = "Best Gift"
                )
                StatItem(
                    icon = "🔥",
                    value = "${statistics.highestCombo}",
                    label = "Max Combo"
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    icon = "🎯",
                    value = "${statistics.feverActivations}",
                    label = "Fever Times"
                )
                StatItem(
                    icon = "🎡",
                    value = "${statistics.wheelSpins}",
                    label = "Wheel Spins"
                )
                StatItem(
                    icon = "⚡",
                    value = "${statistics.specialGiftsUsed}",
                    label = "Specials Used"
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    icon: String,
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(90.dp)
    ) {
        Text(text = icon, fontSize = 24.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 18.sp,
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

@Composable
private fun EarningsCard(statistics: PlayerStatistics) {
    Card(
        colors = CardDefaults.cardColors(containerColor = BackgroundCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "💰 Total Earnings",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                EarningItem(
                    icon = "💰",
                    value = formatNumber(statistics.totalCoinsEarned),
                    label = "Coins",
                    color = AccentGold
                )
                EarningItem(
                    icon = "💎",
                    value = formatNumber(statistics.totalGemsEarned.toLong()),
                    label = "Gems",
                    color = AccentBlue
                )
                EarningItem(
                    icon = "📚",
                    value = formatNumber(statistics.totalExpEarned),
                    label = "Experience",
                    color = ExpBar
                )
            }
        }
    }
}

@Composable
private fun EarningItem(
    icon: String,
    value: String,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(100.dp)
    ) {
        Text(text = icon, fontSize = 28.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = TextSecondary
        )
    }
}

@Composable
private fun AchievementsPreview(statistics: PlayerStatistics) {
    Card(
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
                Text(
                    text = "🏆 Achievements",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "${calculateAchievements(statistics)}/15",
                    fontSize = 14.sp,
                    color = AccentGold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AchievementBadge(emoji = "🎯", unlocked = statistics.feverActivations >= 1)
                AchievementBadge(emoji = "🔥", unlocked = statistics.highestCombo >= 5)
                AchievementBadge(emoji = "💎", unlocked = statistics.totalGemsEarned >= 100)
                AchievementBadge(emoji = "⭐", unlocked = statistics.totalCoinsEarned >= 10000)
                AchievementBadge(emoji = "🚀", unlocked = statistics.specialGiftsUsed >= 10)
            }
        }
    }
}

@Composable
private fun AchievementBadge(emoji: String, unlocked: Boolean) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(
                if (unlocked) AccentGold.copy(alpha = 0.3f)
                else BackgroundCell
            )
            .then(
                if (unlocked) Modifier.border(2.dp, AccentGold, CircleShape)
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = emoji,
            fontSize = 20.sp,
            color = if (unlocked) Color.Unspecified else Color.Gray
        )
    }
}

@Composable
private fun CollectionPreview(highestGiftLevel: Int) {
    Card(
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
                Text(
                    text = "🎁 Gift Collection",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "$highestGiftLevel/12",
                    fontSize = 14.sp,
                    color = AccentGold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                (1..12).forEach { level ->
                    val giftType = GiftType.fromLevel(level - 1)
                    val unlocked = level <= highestGiftLevel

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (unlocked) Color(giftType.rarity.color).copy(alpha = 0.3f)
                                else BackgroundCell
                            )
                            .then(
                                if (unlocked) Modifier.border(
                                    1.dp,
                                    Color(giftType.rarity.color),
                                    RoundedCornerShape(8.dp)
                                )
                                else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (unlocked) {
                            Text(
                                text = giftType.baseEmoji,
                                fontSize = 14.sp
                            )
                        } else {
                            Text(
                                text = "?",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun calculateAchievements(statistics: PlayerStatistics): Int {
    var count = 0
    if (statistics.feverActivations >= 1) count++
    if (statistics.feverActivations >= 10) count++
    if (statistics.highestCombo >= 5) count++
    if (statistics.highestCombo >= 10) count++
    if (statistics.totalGemsEarned >= 100) count++
    if (statistics.totalGemsEarned >= 1000) count++
    if (statistics.totalCoinsEarned >= 10000) count++
    if (statistics.totalCoinsEarned >= 100000) count++
    if (statistics.specialGiftsUsed >= 10) count++
    if (statistics.specialGiftsUsed >= 50) count++
    if (statistics.wheelSpins >= 10) count++
    return count
}

private fun getRankTitle(level: Int): String {
    return when {
        level >= 50 -> "🌟 Gift Master"
        level >= 40 -> "💫 Gift Legend"
        level >= 30 -> "✨ Gift Expert"
        level >= 20 -> "🎯 Gift Pro"
        level >= 10 -> "🎁 Gift Collector"
        level >= 5 -> "📦 Gift Opener"
        else -> "🆕 Newcomer"
    }
}

private fun getNextRankInfo(level: Int): String {
    return when {
        level >= 50 -> "Maximum rank achieved!"
        level >= 40 -> "10 levels to Gift Master"
        level >= 30 -> "10 levels to Gift Legend"
        level >= 20 -> "10 levels to Gift Expert"
        level >= 10 -> "10 levels to Gift Pro"
        level >= 5 -> "5 levels to Gift Collector"
        else -> "${5 - level} levels to Gift Opener"
    }
}

private fun formatNumber(number: Long): String {
    return when {
        number >= 1_000_000 -> String.format("%.1fM", number / 1_000_000.0)
        number >= 1_000 -> String.format("%.1fK", number / 1_000.0)
        else -> number.toString()
    }
}
