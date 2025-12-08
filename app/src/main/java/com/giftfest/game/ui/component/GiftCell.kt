package com.giftfest.game.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.giftfest.game.domain.model.BoardCell
import com.giftfest.game.domain.model.CellGift
import com.giftfest.game.domain.model.GiftType
import com.giftfest.game.domain.model.UnlockRequirement
import com.giftfest.game.ui.theme.*

@Composable
fun GiftCell(
    cell: BoardCell,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)

    // Selection animation
    val infiniteTransition = rememberInfiniteTransition(label = "selection")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val borderColor = when {
        isSelected -> CellSelected
        cell.isLocked -> Color.Gray.copy(alpha = 0.5f)
        cell.gift != null -> {
            val giftType = GiftType.fromLevel(cell.gift.typeIndex)
            Color(giftType.rarity.color)
        }
        else -> Color.Transparent
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .scale(if (isSelected) scale else 1f)
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(
                    colors = if (cell.isLocked) {
                        listOf(Color(0xFF2D2D2D), Color(0xFF1A1A1A))
                    } else {
                        listOf(BackgroundCell, BackgroundCard)
                    }
                ),
                shape = shape
            )
            .border(
                width = if (isSelected || cell.gift != null) 2.dp else 1.dp,
                color = borderColor,
                shape = shape
            )
            .clickable(enabled = !cell.isLocked || cell.gift == null) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        when {
            cell.isLocked -> {
                LockedCellContent(cell.unlockRequirement)
            }
            cell.gift != null -> {
                GiftContent(cell.gift)
            }
        }
    }
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
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = when (requirement) {
                is UnlockRequirement.Level -> "LVL ${requirement.requiredLevel}"
                is UnlockRequirement.InviteFriends -> "${requirement.count} friends"
                is UnlockRequirement.Coins -> "${requirement.amount}"
                null -> ""
            },
            fontSize = 10.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun GiftContent(gift: CellGift) {
    val giftType = GiftType.fromLevel(gift.typeIndex)
    val rarityColor = Color(giftType.rarity.color)

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Gift emoji
        Text(
            text = giftType.baseEmoji,
            fontSize = 36.sp,
            textAlign = TextAlign.Center
        )

        // Level badge
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .background(
                    color = rarityColor,
                    shape = RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text(
                text = "${gift.displayLevel}",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}
