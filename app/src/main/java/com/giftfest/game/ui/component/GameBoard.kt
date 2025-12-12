package com.giftfest.game.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.giftfest.game.domain.model.BoardCell
import com.giftfest.game.domain.model.GameState
import com.giftfest.game.ui.theme.BackgroundCard

@Composable
fun GameBoard(
    board: List<BoardCell>,
    selectedCellIndex: Int?,
    onCellClick: (Int) -> Unit,
    isFeverActive: Boolean = false,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "board_animation")

    // Fever mode border animation
    val feverGlow by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fever_glow"
    )

    val feverColors = listOf(
        Color(0xFFFF6B6B),
        Color(0xFFFFE66D),
        Color(0xFF4ECDC4),
        Color(0xFF95E1D3)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .then(
                if (isFeverActive) {
                    Modifier.background(
                        brush = Brush.linearGradient(
                            colors = feverColors.map { it.copy(alpha = 0.3f * feverGlow) }
                        )
                    )
                } else Modifier.background(BackgroundCard.copy(alpha = 0.3f))
            )
            .padding(8.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (row in 0 until GameState.BOARD_ROWS) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (col in 0 until GameState.BOARD_COLUMNS) {
                        val index = row * GameState.BOARD_COLUMNS + col
                        val cell = board.getOrNull(index)

                        if (cell != null) {
                            GiftCell(
                                cell = cell,
                                isSelected = selectedCellIndex == index,
                                onClick = { onCellClick(index) },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}
