package com.giftfest.game.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.giftfest.game.domain.model.*
import com.giftfest.game.ui.theme.*

@Composable
fun GiftCell(
    cell: BoardCell,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)

    val infiniteTransition = rememberInfiniteTransition(label = "cell_animation")

    // Selection animation
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isSelected) 1.08f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    // Special gift glow
    val specialGlow by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    // Cell type colors
    val cellBackground = when (cell.cellType) {
        CellType.GOLDEN -> listOf(Color(0xFF3D2E00), Color(0xFF1A1400))
        CellType.EXPERIENCE -> listOf(Color(0xFF002E00), Color(0xFF001400))
        CellType.MYSTERY -> listOf(Color(0xFF2E002E), Color(0xFF140014))
        CellType.FROZEN -> listOf(Color(0xFF002E3D), Color(0xFF001420))
        else -> if (cell.isLocked) listOf(Color(0xFF2D2D2D), Color(0xFF1A1A1A))
        else listOf(BackgroundCell, BackgroundCard)
    }

    val cellBorderColor = when (cell.cellType) {
        CellType.GOLDEN -> AccentGold.copy(alpha = 0.5f)
        CellType.EXPERIENCE -> ExpBar.copy(alpha = 0.5f)
        CellType.MYSTERY -> AccentPurple.copy(alpha = 0.5f)
        CellType.FROZEN -> AccentBlue.copy(alpha = 0.5f)
        else -> Color.Transparent
    }

    val borderColor = when {
        isSelected -> CellSelected
        cell.isLocked -> Color.Gray.copy(alpha = 0.5f)
        cell.gift?.isSpecial == true -> Color(0xFFFFD700).copy(alpha = specialGlow)
        cell.gift != null -> {
            val giftType = GiftType.fromLevel(cell.gift.typeIndex)
            Color(giftType.rarity.color)
        }
        cell.cellType != CellType.NORMAL -> cellBorderColor
        else -> Color.Transparent
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .scale(if (isSelected) scale else 1f)
            .then(
                if (cell.gift?.isSpecial == true) {
                    Modifier.shadow(
                        elevation = (8 * specialGlow).dp,
                        shape = shape,
                        spotColor = Color(0xFFFFD700)
                    )
                } else Modifier
            )
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(colors = cellBackground),
                shape = shape
            )
            .border(
                width = when {
                    isSelected -> 3.dp
                    cell.gift?.isSpecial == true -> 2.dp
                    cell.gift != null -> 2.dp
                    else -> 1.dp
                },
                color = borderColor,
                shape = shape
            )
            .clickable(enabled = !cell.isLocked) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        // Cell type indicator
        if (cell.cellType != CellType.NORMAL && !cell.isLocked && cell.gift == null) {
            CellTypeIndicator(cell.cellType)
        }

        when {
            cell.isLocked -> LockedCellContent(cell.unlockRequirement)
            cell.gift != null -> GiftContent(cell.gift, cell.cellType)
        }
    }
}

@Composable
private fun CellTypeIndicator(cellType: CellType) {
    val (icon, color) = when (cellType) {
        CellType.GOLDEN -> "💰" to AccentGold
        CellType.EXPERIENCE -> "📚" to ExpBar
        CellType.MYSTERY -> "❓" to AccentPurple
        CellType.FROZEN -> "❄️" to AccentBlue
        else -> "" to Color.Transparent
    }

    Text(
        text = icon,
        fontSize = 20.sp,
        modifier = Modifier.padding(4.dp)
    )
}

@Composable
private fun LockedCellContent(requirement: UnlockRequirement?) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "Locked",
            tint = Color.Gray,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = when (requirement) {
                is UnlockRequirement.Level -> "LVL ${requirement.requiredLevel}"
                is UnlockRequirement.InviteFriends -> "${requirement.count} friends"
                is UnlockRequirement.Coins -> "${requirement.amount}"
                is UnlockRequirement.Gems -> "${requirement.amount} 💎"
                is UnlockRequirement.Merges -> "${requirement.count} merges"
                null -> ""
            },
            fontSize = 8.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun GiftContent(gift: CellGift, cellType: CellType) {
    val giftType = GiftType.fromLevel(gift.typeIndex)
    val rarityColor = Color(giftType.rarity.color)

    val infiniteTransition = rememberInfiniteTransition(label = "gift_animation")
    val specialRotation by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rotation"
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Gift emoji or special emoji
        val displayEmoji = if (gift.isSpecial && gift.specialType != null) {
            gift.specialType.emoji
        } else {
            giftType.baseEmoji
        }

        Text(
            text = displayEmoji,
            fontSize = 28.sp,
            textAlign = TextAlign.Center,
            modifier = if (gift.isSpecial) {
                Modifier.scale(1.1f)
            } else Modifier
        )

        // Level badge
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(2.dp)
                .background(
                    color = rarityColor,
                    shape = RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 3.dp, vertical = 1.dp)
        ) {
            Text(
                text = "${gift.displayLevel}",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        // Special indicator
        if (gift.isSpecial) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(2.dp)
                    .size(14.dp)
                    .background(
                        color = Color(0xFFFFD700),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Special",
                    tint = Color.White,
                    modifier = Modifier.size(10.dp)
                )
            }
        }

        // Cell type bonus indicator
        if (cellType == CellType.GOLDEN || cellType == CellType.EXPERIENCE) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(2.dp)
            ) {
                Text(
                    text = if (cellType == CellType.GOLDEN) "2x💰" else "2x📚",
                    fontSize = 7.sp
                )
            }
        }
    }
}
