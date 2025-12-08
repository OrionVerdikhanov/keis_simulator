package com.giftfest.game.domain.model

/**
 * Represents the current state of the game
 */
data class GameState(
    val playerLevel: Int = 1,
    val experience: Long = 0,
    val experienceToNextLevel: Long = 100,
    val coins: Long = 0,
    val energy: Int = 40,
    val maxEnergy: Int = 40,
    val lastEnergyRegenTime: Long = System.currentTimeMillis(),
    val board: List<BoardCell> = emptyList(),
    val unlockedGifts: Set<Int> = setOf(0, 1), // Start with first two gift types unlocked
    val totalMerges: Int = 0,
    val highestGiftLevel: Int = 1,
    val dailyStreak: Int = 0,
    val lastDailyRewardTime: Long = 0,
    val achievements: List<Achievement> = emptyList()
) {
    companion object {
        const val BOARD_SIZE = 12 // 4x3 grid
        const val BOARD_COLUMNS = 4
        const val BOARD_ROWS = 3
        const val ENERGY_REGEN_TIME_MS = 420000L // 7 minutes per energy
        const val SPAWN_ENERGY_COST = 5
    }
}

/**
 * Represents a single cell on the game board
 */
data class BoardCell(
    val index: Int,
    val gift: CellGift? = null,
    val isLocked: Boolean = false,
    val unlockRequirement: UnlockRequirement? = null
)

/**
 * Represents a gift placed on the board
 */
data class CellGift(
    val typeIndex: Int,
    val level: Int
) {
    val displayLevel: Int get() = level + 1
}

/**
 * Requirements to unlock a cell
 */
sealed class UnlockRequirement {
    data class Level(val requiredLevel: Int) : UnlockRequirement()
    data class InviteFriends(val count: Int) : UnlockRequirement()
    data class Coins(val amount: Long) : UnlockRequirement()
}

/**
 * Player achievements
 */
data class Achievement(
    val id: String,
    val name: String,
    val description: String,
    val isUnlocked: Boolean = false,
    val progress: Int = 0,
    val target: Int = 1,
    val reward: AchievementReward
)

sealed class AchievementReward {
    data class Coins(val amount: Long) : AchievementReward()
    data class Energy(val amount: Int) : AchievementReward()
    data class UnlockGift(val giftTypeIndex: Int) : AchievementReward()
}
