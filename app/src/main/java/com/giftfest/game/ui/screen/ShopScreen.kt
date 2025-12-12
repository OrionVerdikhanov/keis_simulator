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
import androidx.compose.material.icons.filled.Star
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
import com.giftfest.game.domain.model.BoosterType
import com.giftfest.game.ui.theme.*

@Composable
fun ShopScreen(
    coins: Long,
    gems: Int,
    ownedBoosters: Map<BoosterType, Int>,
    onPurchaseBooster: (BoosterType, Boolean) -> Unit,
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
        ShopHeader(coins = coins, gems = gems, onBack = onBack)

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "⚡ Boosters",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(BoosterType.entries) { booster ->
                BoosterShopItem(
                    booster = booster,
                    owned = ownedBoosters[booster] ?: 0,
                    onPurchaseWithCoins = { onPurchaseBooster(booster, false) },
                    onPurchaseWithGems = { onPurchaseBooster(booster, true) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "💎 Gem Packs",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        GemPacksRow()

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun ShopHeader(
    coins: Long,
    gems: Int,
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

        Text(
            text = "🛒 Shop",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = AccentGold,
            modifier = Modifier.weight(1f)
        )

        CurrencyDisplay(icon = "💰", value = formatNumber(coins), color = AccentGold)
        Spacer(modifier = Modifier.width(8.dp))
        CurrencyDisplay(icon = "💎", value = "$gems", color = AccentBlue)
    }
}

@Composable
private fun CurrencyDisplay(icon: String, value: String, color: Color) {
    Row(
        modifier = Modifier
            .background(BackgroundCard, RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = icon, fontSize = 14.sp)
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun BoosterShopItem(
    booster: BoosterType,
    owned: Int,
    onPurchaseWithCoins: () -> Unit,
    onPurchaseWithGems: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "booster")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
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
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        AccentGold.copy(alpha = glowAlpha),
                        AccentPurple.copy(alpha = glowAlpha)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = BackgroundCard),
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
                    .size(56.dp)
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
                Text(text = booster.emoji, fontSize = 32.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = booster.displayName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    if (owned > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "×$owned",
                            fontSize = 12.sp,
                            color = AccentGold,
                            modifier = Modifier
                                .background(AccentGold.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = getBoosterDescription(booster),
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Duration: ${booster.durationMinutes} min",
                    fontSize = 11.sp,
                    color = AccentBlue
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PurchaseButton(
                    icon = "💰",
                    price = "${booster.coinPrice}",
                    color = AccentGold,
                    onClick = onPurchaseWithCoins
                )
                PurchaseButton(
                    icon = "💎",
                    price = "${booster.gemPrice}",
                    color = AccentBlue,
                    onClick = onPurchaseWithGems
                )
            }
        }
    }
}

@Composable
private fun PurchaseButton(
    icon: String,
    price: String,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.2f))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = icon, fontSize = 12.sp)
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = price,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun GemPacksRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GemPack(
            gems = 50,
            price = "Free Ad",
            color = ExpBar,
            modifier = Modifier.weight(1f)
        )
        GemPack(
            gems = 200,
            price = "$0.99",
            color = AccentBlue,
            modifier = Modifier.weight(1f)
        )
        GemPack(
            gems = 500,
            price = "$1.99",
            color = AccentPurple,
            isPopular = true,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun GemPack(
    gems: Int,
    price: String,
    color: Color,
    isPopular: Boolean = false,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "gem")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isPopular) 1.02f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Card(
        modifier = modifier
            .scale(scale)
            .then(
                if (isPopular) {
                    Modifier.border(2.dp, AccentGold, RoundedCornerShape(12.dp))
                } else Modifier
            ),
        colors = CardDefaults.cardColors(containerColor = BackgroundCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isPopular) {
                Text(
                    text = "BEST VALUE",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentGold
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            Text(text = "💎", fontSize = 28.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$gems",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = price,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        }
    }
}

private fun getBoosterDescription(booster: BoosterType): String {
    return when (booster) {
        BoosterType.DOUBLE_XP -> "Earn 2x experience from all merges"
        BoosterType.DOUBLE_COINS -> "Earn 2x coins from all merges"
        BoosterType.ENERGY_FREEZE -> "Energy consumption paused"
        BoosterType.AUTO_MERGE -> "Gifts auto-merge when possible"
        BoosterType.LUCKY_SPAWN -> "Higher chance for rare & special gifts"
        BoosterType.COMBO_KEEPER -> "Combos don't reset between merges"
    }
}

private fun formatNumber(number: Long): String {
    return when {
        number >= 1_000_000 -> String.format("%.1fM", number / 1_000_000.0)
        number >= 1_000 -> String.format("%.1fK", number / 1_000.0)
        else -> number.toString()
    }
}
