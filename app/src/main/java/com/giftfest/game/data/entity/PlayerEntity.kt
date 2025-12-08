package com.giftfest.game.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player")
data class PlayerEntity(
    @PrimaryKey
    val id: Int = 1, // Single player game, always ID 1
    val level: Int = 1,
    val experience: Long = 0,
    val coins: Long = 0,
    val energy: Int = 40,
    val maxEnergy: Int = 40,
    val lastEnergyRegenTime: Long = System.currentTimeMillis(),
    val totalMerges: Int = 0,
    val highestGiftLevel: Int = 1,
    val dailyStreak: Int = 0,
    val lastDailyRewardTime: Long = 0,
    val unlockedGiftsJson: String = "[0,1]", // JSON array of unlocked gift type indices
    val achievementsJson: String = "[]" // JSON array of achievement IDs that are unlocked
)
