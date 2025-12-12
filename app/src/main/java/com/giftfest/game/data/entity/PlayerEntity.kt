package com.giftfest.game.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player")
data class PlayerEntity(
    @PrimaryKey
    val id: Int = 1,
    val level: Int = 1,
    val experience: Long = 0,
    val coins: Long = 100,
    val gems: Int = 10,
    val energy: Int = 50,
    val maxEnergy: Int = 50,
    val lastEnergyRegenTime: Long = System.currentTimeMillis(),
    val totalMerges: Int = 0,
    val highestGiftLevel: Int = 1,
    val dailyStreak: Int = 0,
    val lastDailyRewardTime: Long = 0,
    val lastOnlineTime: Long = System.currentTimeMillis(),

    // Combo system
    val currentCombo: Int = 0,
    val lastMergeTime: Long = 0,

    // Fever mode
    val feverProgress: Float = 0f,
    val isFeverActive: Boolean = false,
    val feverEndTime: Long = 0,

    // JSON stored data
    val unlockedGiftsJson: String = "[0,1,2]",
    val achievementsJson: String = "[]",
    val activeBoostersJson: String = "[]",
    val ownedBoostersJson: String = "{}",
    val activeQuestsJson: String = "[]",
    val completedQuestsToday: Int = 0,

    // Statistics
    val statsTotalMerges: Int = 0,
    val statsTotalCoinsEarned: Long = 0,
    val statsTotalGemsEarned: Int = 0,
    val statsTotalEnergySpent: Int = 0,
    val statsHighestCombo: Int = 0,
    val statsFeverTriggered: Int = 0,
    val statsSpecialGiftsUsed: Int = 0,
    val statsPlayTimeMinutes: Long = 0,
    val statsGamesPlayed: Int = 1,
    val statsBestMergeStreak: Int = 0,
    val statsLuckyWheelSpins: Int = 0,
    val statsQuestsCompleted: Int = 0,

    // Lucky wheel
    val lastWheelSpinTime: Long = 0,
    val freeSpinsAvailable: Int = 1
)
