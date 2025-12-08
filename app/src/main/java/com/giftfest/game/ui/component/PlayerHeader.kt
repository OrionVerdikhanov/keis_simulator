package com.giftfest.game.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
fun PlayerHeader(
    level: Int,
    experience: Long,
    experienceToNextLevel: Long,
    coins: Long,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Level badge
        LevelBadge(level = level)

        Spacer(modifier = Modifier.width(12.dp))

        // Experience bar
        Column(
            modifier = Modifier.weight(1f)
        ) {
            LinearProgressIndicator(
                progress = { (experience.toFloat() / experienceToNextLevel.toFloat()).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = ExpBar,
                trackColor = BackgroundCell
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$experience / $experienceToNextLevel XP",
                fontSize = 10.sp,
                color = TextSecondary
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Coins
        CoinsBadge(coins = coins)
    }
}

@Composable
private fun LevelBadge(level: Int) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(AccentGold, AccentOrange)
                ),
                shape = CircleShape
            )
            .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$level",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = BackgroundDark
            )
            Text(
                text = "LVL",
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = BackgroundDark
            )
        }
    }
}

@Composable
private fun CoinsBadge(coins: Long) {
    Row(
        modifier = Modifier
            .background(
                color = BackgroundCard,
                shape = RoundedCornerShape(20.dp)
            )
            .border(1.dp, AccentGold.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = "Coins",
            tint = AccentGold,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = formatNumber(coins),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
    }
}

private fun formatNumber(number: Long): String {
    return when {
        number >= 1_000_000 -> String.format("%.1fM", number / 1_000_000.0)
        number >= 1_000 -> String.format("%.1fK", number / 1_000.0)
        else -> number.toString()
    }
}
