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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.giftfest.game.domain.model.GameState
import com.giftfest.game.domain.model.PermanentBonuses
import com.giftfest.game.domain.model.PrestigeUpgrade
import com.giftfest.game.ui.theme.*

@Composable
fun PrestigeScreen(
    playerLevel: Int,
    prestigeLevel: Int,
    prestigePoints: Int,
    bonuses: PermanentBonuses,
    pointsPreview: Int,
    canPrestige: Boolean,
    onPrestige: () -> Unit,
    onPurchaseUpgrade: (PrestigeUpgrade) -> Unit,
    onBack: () -> Unit
) {
    var showConfirmDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF1A0A2E), Color(0xFF0A1F0D))
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        PrestigeHeader(
            prestigeLevel = prestigeLevel,
            prestigePoints = prestigePoints,
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                PrestigeInfoCard(
                    playerLevel = playerLevel,
                    pointsPreview = pointsPreview,
                    canPrestige = canPrestige,
                    onPrestigeClick = { showConfirmDialog = true }
                )
            }

            item {
                Text(
                    text = "⚡ Permanent Upgrades",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            items(PrestigeUpgrade.entries) { upgrade ->
                PrestigeUpgradeItem(
                    upgrade = upgrade,
                    currentLevel = getUpgradeLevel(upgrade, bonuses),
                    prestigePoints = prestigePoints,
                    onPurchase = { onPurchaseUpgrade(upgrade) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                CurrentBonusesCard(bonuses = bonuses)
            }
        }
    }

    if (showConfirmDialog) {
        PrestigeConfirmDialog(
            pointsToEarn = pointsPreview,
            onConfirm = {
                showConfirmDialog = false
                onPrestige()
            },
            onDismiss = { showConfirmDialog = false }
        )
    }
}

@Composable
private fun PrestigeHeader(
    prestigeLevel: Int,
    prestigePoints: Int,
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
                contentDescription = "Back",
                tint = TextPrimary
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "♻️ Prestige",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = AccentPurple
            )
            if (prestigeLevel > 0) {
                Text(
                    text = "Prestige Level $prestigeLevel",
                    fontSize = 12.sp,
                    color = AccentGold
                )
            }
        }

        Row(
            modifier = Modifier
                .background(BackgroundCard, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "⭐", fontSize = 16.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "$prestigePoints",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = AccentPurple
            )
        }
    }
}

@Composable
private fun PrestigeInfoCard(
    playerLevel: Int,
    pointsPreview: Int,
    canPrestige: Boolean,
    onPrestigeClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "prestige")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (canPrestige) {
                    Modifier.border(
                        width = 2.dp,
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                AccentPurple.copy(alpha = glowAlpha),
                                AccentGold.copy(alpha = glowAlpha)
                            )
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                } else Modifier
            ),
        colors = CardDefaults.cardColors(
            containerColor = BackgroundCard
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "♻️",
                fontSize = 48.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Reset for Rewards",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Reset your progress to earn Prestige Points\nand unlock permanent bonuses!",
                fontSize = 13.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Current Level",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = "$playerLevel",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (canPrestige) AccentGold else TextPrimary
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Required",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = "${GameState.PRESTIGE_LEVEL_REQUIREMENT}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentPurple
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Points to Earn",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = if (canPrestige) "+$pointsPreview" else "---",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (canPrestige) AccentGold else TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onPrestigeClick,
                enabled = canPrestige,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentPurple,
                    disabledContainerColor = ButtonDisabled
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (canPrestige) {
                    Text(
                        text = "♻️ PRESTIGE NOW",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Level ${GameState.PRESTIGE_LEVEL_REQUIREMENT} Required",
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun PrestigeUpgradeItem(
    upgrade: PrestigeUpgrade,
    currentLevel: Int,
    prestigePoints: Int,
    onPurchase: () -> Unit
) {
    val cost = upgrade.baseCost * (currentLevel + 1)
    val isMaxLevel = currentLevel >= upgrade.maxLevel
    val canAfford = prestigePoints >= cost

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = BackgroundCard
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                Text(text = upgrade.icon, fontSize = 28.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = upgrade.displayName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$currentLevel/${upgrade.maxLevel}",
                        fontSize = 11.sp,
                        color = AccentGold,
                        modifier = Modifier
                            .background(AccentGold.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = upgrade.description,
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Progress bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(BackgroundCell)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(currentLevel.toFloat() / upgrade.maxLevel)
                            .background(AccentPurple)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            if (isMaxLevel) {
                Text(
                    text = "MAX",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentGold,
                    modifier = Modifier
                        .background(AccentGold.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            } else {
                Button(
                    onClick = onPurchase,
                    enabled = canAfford,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (canAfford) AccentPurple else ButtonDisabled
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(text = "⭐", fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$cost",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun CurrentBonusesCard(bonuses: PermanentBonuses) {
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
                text = "📊 Current Bonuses",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                BonusItem(icon = "💰", value = "+${((bonuses.coinMultiplier - 1) * 100).toInt()}%", label = "Coins")
                BonusItem(icon = "📚", value = "+${((bonuses.expMultiplier - 1) * 100).toInt()}%", label = "XP")
                BonusItem(icon = "⚡", value = "-${((1 - bonuses.energyRegenBonus) * 100).toInt()}%", label = "Regen")
                BonusItem(icon = "🔋", value = "+${bonuses.maxEnergyBonus}", label = "Max E")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                BonusItem(icon = "🏦", value = "${bonuses.startingCoins}", label = "Start $")
                BonusItem(icon = "💎", value = "${bonuses.startingGems}", label = "Start G")
                BonusItem(icon = "🍀", value = "+${(bonuses.luckyChanceBonus * 100).toInt()}%", label = "Lucky")
                BonusItem(icon = "🏪", value = "+${(bonuses.sellPriceBonus * 100).toInt()}%", label = "Sell")
            }
        }
    }
}

@Composable
private fun BonusItem(icon: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = icon, fontSize = 20.sp)
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = AccentPurple
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = TextSecondary
        )
    }
}

@Composable
private fun PrestigeConfirmDialog(
    pointsToEarn: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BackgroundCard,
        title = {
            Text(
                text = "♻️ Confirm Prestige",
                fontWeight = FontWeight.Bold,
                color = AccentPurple
            )
        },
        text = {
            Column {
                Text(
                    text = "This will reset your progress:",
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("• Level will reset to 1", color = AccentRed)
                Text("• Coins will reset", color = AccentRed)
                Text("• Board will be cleared", color = AccentRed)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "You will keep:",
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("• All permanent bonuses", color = AccentGreen)
                Text("• Prestige Points (+$pointsToEarn)", color = AccentGold)
                Text("• Unlocked upgrades", color = AccentGreen)
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
            ) {
                Text("Prestige!")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

private fun getUpgradeLevel(upgrade: PrestigeUpgrade, bonuses: PermanentBonuses): Int {
    return when (upgrade) {
        PrestigeUpgrade.COIN_MULTIPLIER -> ((bonuses.coinMultiplier - 1) * 10).toInt()
        PrestigeUpgrade.EXP_MULTIPLIER -> ((bonuses.expMultiplier - 1) * 10).toInt()
        PrestigeUpgrade.ENERGY_REGEN -> ((1 - bonuses.energyRegenBonus) * 20).toInt()
        PrestigeUpgrade.MAX_ENERGY -> bonuses.maxEnergyBonus / 5
        PrestigeUpgrade.STARTING_COINS -> ((bonuses.startingCoins - 100) / 100).toInt()
        PrestigeUpgrade.STARTING_GEMS -> ((bonuses.startingGems - 10) / 5)
        PrestigeUpgrade.LUCKY_CHANCE -> (bonuses.luckyChanceBonus * 50).toInt()
        PrestigeUpgrade.SELL_BONUS -> (bonuses.sellPriceBonus * 10).toInt()
    }
}
