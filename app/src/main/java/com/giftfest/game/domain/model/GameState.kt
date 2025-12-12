package com.giftfest.game.domain.model

/**
 * Represents the current state of the game - Enhanced Version
 */
data class GameState(
    val playerLevel: Int = 1,
    val experience: Long = 0,
    val experienceToNextLevel: Long = 100,
    val coins: Long = 100,
    val gems: Int = 10, // Premium currency
    val energy: Int = 50,
    val maxEnergy: Int = 50,
    val lastEnergyRegenTime: Long = System.currentTimeMillis(),
    val board: List<BoardCell> = emptyList(),
    val unlockedGifts: Set<Int> = setOf(0, 1, 2),
    val totalMerges: Int = 0,
    val highestGiftLevel: Int = 1,
    val dailyStreak: Int = 0,
    val lastDailyRewardTime: Long = 0,
    val lastOnlineTime: Long = System.currentTimeMillis(),

    // Combo system
    val currentCombo: Int = 0,
    val comboMultiplier: Float = 1.0f,
    val lastMergeTime: Long = 0,

    // Fever mode
    val feverProgress: Float = 0f,
    val isFeverActive: Boolean = false,
    val feverEndTime: Long = 0,

    // Boosters
    val activeBoosters: List<ActiveBooster> = emptyList(),
    val ownedBoosters: Map<BoosterType, Int> = emptyMap(),

    // Quests
    val activeQuests: List<Quest> = emptyList(),
    val completedQuestsToday: Int = 0,

    // Statistics
    val statistics: PlayerStatistics = PlayerStatistics(),

    // Lucky wheel
    val lastWheelSpinTime: Long = 0,
    val freeSpinsAvailable: Int = 1
) {
    companion object {
        const val BOARD_SIZE = 20 // 5x4 grid
        const val BOARD_COLUMNS = 5
        const val BOARD_ROWS = 4
        const val ENERGY_REGEN_TIME_MS = 300000L // 5 minutes per energy
        const val SPAWN_ENERGY_COST = 3
        const val COMBO_TIMEOUT_MS = 3000L // 3 seconds to maintain combo
        const val FEVER_DURATION_MS = 30000L // 30 seconds fever mode
        const val MAX_COMBO = 10
    }
}

/**
 * Represents a single cell on the game board
 */
data class BoardCell(
    val index: Int,
    val gift: CellGift? = null,
    val isLocked: Boolean = false,
    val unlockRequirement: UnlockRequirement? = null,
    val cellType: CellType = CellType.NORMAL,
    val specialEffect: CellEffect? = null
)

/**
 * Different cell types with special properties
 */
enum class CellType {
    NORMAL,          // Regular cell
    GOLDEN,          // 2x coins from merges here
    EXPERIENCE,      // 2x XP from merges here
    MYSTERY,         // Random bonus on merge
    FROZEN           // Temporarily disabled
}

/**
 * Special effects that can be applied to cells
 */
sealed class CellEffect {
    data class Multiplier(val value: Float, val expiresAt: Long) : CellEffect()
    data class Shield(val expiresAt: Long) : CellEffect()
}

/**
 * Represents a gift placed on the board - Enhanced with special abilities
 */
data class CellGift(
    val typeIndex: Int,
    val level: Int,
    val isSpecial: Boolean = false,
    val specialType: SpecialGiftType? = null
) {
    val displayLevel: Int get() = level + 1
}

/**
 * Special gift types with unique abilities
 */
enum class SpecialGiftType(val emoji: String, val description: String) {
    BOMB("💣", "Destroys adjacent gifts and converts to coins"),
    MAGNET("🧲", "Attracts same-type gifts together"),
    RAINBOW("🌈", "Can merge with any gift type"),
    MULTIPLIER("✨", "Doubles rewards from next merge"),
    LIGHTNING("⚡", "Instantly merges all matching pairs"),
    CLOCK("⏰", "Freezes energy consumption for 1 minute")
}

/**
 * Requirements to unlock a cell
 */
sealed class UnlockRequirement {
    data class Level(val requiredLevel: Int) : UnlockRequirement()
    data class InviteFriends(val count: Int) : UnlockRequirement()
    data class Coins(val amount: Long) : UnlockRequirement()
    data class Gems(val amount: Int) : UnlockRequirement()
    data class Merges(val count: Int) : UnlockRequirement()
}

/**
 * Active booster with duration
 */
data class ActiveBooster(
    val type: BoosterType,
    val expiresAt: Long
) {
    fun isActive(): Boolean = System.currentTimeMillis() < expiresAt
}

/**
 * Available booster types
 */
enum class BoosterType(
    val displayName: String,
    val emoji: String,
    val durationMs: Long,
    val coinCost: Long,
    val gemCost: Int
) {
    DOUBLE_XP("Double XP", "📚", 300000L, 500, 5),              // 5 min
    DOUBLE_COINS("Double Coins", "💰", 300000L, 500, 5),        // 5 min
    ENERGY_FREEZE("Energy Freeze", "❄️", 600000L, 800, 8),      // 10 min
    AUTO_MERGE("Auto Merge", "🤖", 60000L, 300, 3),              // 1 min
    LUCKY_SPAWN("Lucky Spawn", "🍀", 180000L, 400, 4),           // 3 min - better gifts
    COMBO_KEEPER("Combo Keeper", "🔥", 120000L, 350, 3)          // 2 min - combos don't reset
}

/**
 * Quest/Mission data
 */
data class Quest(
    val id: String,
    val type: QuestType,
    val title: String,
    val description: String,
    val target: Int,
    val progress: Int = 0,
    val reward: QuestReward,
    val expiresAt: Long? = null // null = permanent quest
) {
    val isCompleted: Boolean get() = progress >= target
    val progressPercent: Float get() = (progress.toFloat() / target).coerceIn(0f, 1f)
}

enum class QuestType {
    MERGE_COUNT,        // Merge X times
    MERGE_LEVEL,        // Create a gift of level X
    COLLECT_COINS,      // Collect X coins
    REACH_COMBO,        // Reach X combo
    USE_SPECIAL,        // Use X special gifts
    TRIGGER_FEVER,      // Trigger fever mode X times
    SPEND_ENERGY,       // Spend X energy
    UNLOCK_GIFT_TYPE    // Unlock X gift types
}

sealed class QuestReward {
    data class Coins(val amount: Long) : QuestReward()
    data class Gems(val amount: Int) : QuestReward()
    data class Energy(val amount: Int) : QuestReward()
    data class Booster(val type: BoosterType, val count: Int) : QuestReward()
    data class SpecialGift(val type: SpecialGiftType) : QuestReward()
}

/**
 * Player statistics for profile
 */
data class PlayerStatistics(
    val totalMerges: Int = 0,
    val totalCoinsEarned: Long = 0,
    val totalGemsEarned: Int = 0,
    val totalEnergySpent: Int = 0,
    val highestCombo: Int = 0,
    val feverModeTriggered: Int = 0,
    val specialGiftsUsed: Int = 0,
    val playTimeMinutes: Long = 0,
    val gamesPlayed: Int = 1,
    val bestMergeStreak: Int = 0,
    val luckyWheelSpins: Int = 0,
    val questsCompleted: Int = 0
)

/**
 * Lucky wheel prize
 */
data class WheelPrize(
    val type: WheelPrizeType,
    val amount: Int,
    val weight: Int // Higher = more common
)

enum class WheelPrizeType(val emoji: String) {
    COINS("💰"),
    GEMS("💎"),
    ENERGY("⚡"),
    BOOSTER("🎁"),
    SPECIAL_GIFT("🌟"),
    JACKPOT("🎰"),
    NOTHING("😅")
}

/**
 * Player achievements - Enhanced
 */
data class Achievement(
    val id: String,
    val name: String,
    val description: String,
    val icon: String,
    val isUnlocked: Boolean = false,
    val progress: Int = 0,
    val target: Int = 1,
    val reward: AchievementReward,
    val tier: AchievementTier = AchievementTier.BRONZE
)

enum class AchievementTier(val color: Long) {
    BRONZE(0xFFCD7F32),
    SILVER(0xFFC0C0C0),
    GOLD(0xFFFFD700),
    DIAMOND(0xFFB9F2FF)
}

sealed class AchievementReward {
    data class Coins(val amount: Long) : AchievementReward()
    data class Gems(val amount: Int) : AchievementReward()
    data class Energy(val amount: Int) : AchievementReward()
    data class UnlockGift(val giftTypeIndex: Int) : AchievementReward()
    data class Booster(val type: BoosterType, val count: Int) : AchievementReward()
    data class Title(val title: String) : AchievementReward()
}
