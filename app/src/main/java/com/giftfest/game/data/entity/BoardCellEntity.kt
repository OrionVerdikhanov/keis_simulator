package com.giftfest.game.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "board_cells")
data class BoardCellEntity(
    @PrimaryKey
    val index: Int,
    val giftTypeIndex: Int? = null,
    val giftLevel: Int? = null,
    val isLocked: Boolean = false,
    val unlockType: String? = null, // "level", "friends", "coins", "gems", "merges"
    val unlockValue: Int? = null,
    val cellType: String = "NORMAL", // NORMAL, GOLDEN, EXPERIENCE, MYSTERY, FROZEN
    val isSpecialGift: Boolean = false,
    val specialGiftType: String? = null, // BOMB, MAGNET, RAINBOW, MULTIPLIER, LIGHTNING, CLOCK
    val effectType: String? = null,
    val effectValue: Float? = null,
    val effectExpiresAt: Long? = null
)
