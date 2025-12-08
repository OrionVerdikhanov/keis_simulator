package com.giftfest.game.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.giftfest.game.domain.model.BoardCell
import com.giftfest.game.domain.model.GameState

@Composable
fun GameBoard(
    board: List<BoardCell>,
    selectedCellIndex: Int?,
    onCellClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (row in 0 until GameState.BOARD_ROWS) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
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
