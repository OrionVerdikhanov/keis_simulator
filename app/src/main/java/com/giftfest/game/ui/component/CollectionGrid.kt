package com.giftfest.game.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.giftfest.game.domain.model.GiftType
import com.giftfest.game.ui.theme.*

@Composable
fun CollectionGrid(
    unlockedGifts: Set<Int>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "COLLECTION",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Text(
            text = "${unlockedGifts.size} / ${GiftType.entries.size} gifts unlocked",
            fontSize = 14.sp,
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(GiftType.entries.toList()) { giftType ->
                val isUnlocked = unlockedGifts.contains(giftType.ordinal)
                CollectionItem(
                    giftType = giftType,
                    isUnlocked = isUnlocked
                )
            }
        }
    }
}

@Composable
private fun CollectionItem(
    giftType: GiftType,
    isUnlocked: Boolean
) {
    val shape = RoundedCornerShape(12.dp)
    val rarityColor = Color(giftType.rarity.color)

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(shape)
            .background(
                color = if (isUnlocked) BackgroundCard else BackgroundCell.copy(alpha = 0.5f)
            )
            .border(
                width = 2.dp,
                color = if (isUnlocked) rarityColor else Color.Gray.copy(alpha = 0.3f),
                shape = shape
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isUnlocked) giftType.baseEmoji else "?",
                fontSize = if (isUnlocked) 28.sp else 24.sp,
                color = if (isUnlocked) Color.Unspecified else Color.Gray
            )

            if (isUnlocked) {
                Text(
                    text = giftType.displayName,
                    fontSize = 8.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }

        // Rarity indicator
        if (isUnlocked) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(8.dp)
                    .background(
                        color = rarityColor,
                        shape = RoundedCornerShape(4.dp)
                    )
            )
        }
    }
}
