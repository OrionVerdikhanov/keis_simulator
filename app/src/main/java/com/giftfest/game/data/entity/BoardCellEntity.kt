package com.giftfest.game.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "board_cells")
data class BoardCellEntity(
    @PrimaryKey
    val index: Int,
    val giftTypeIndex: Int? = null, // null means empty cell
    val giftLevel: Int? = null,
    val isLocked: Boolean = false,
    val unlockType: String? = null, // "level", "friends", "coins"
    val unlockValue: Int? = null
)
