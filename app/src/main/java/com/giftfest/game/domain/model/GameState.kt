package com.giftfest.game.domain.model

/**
 * Represents the current state of the game - Enhanced Version with Prestige
 */
data class GameState(
    val playerLevel: Int = 1,
    val experience: Long = 0,
    val experienceToNextLevel: Long = 100,
    val coins: Long = 100,
    val gems: Int = 10,
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
    val freeSpinsAvailable: Int = 1,

    // === NEW: Prestige System ===
    val prestigeLevel: Int = 0,
    val prestigePoints: Int = 0,
    val permanentBonuses: PermanentBonuses = PermanentBonuses(),

    // === NEW: Milestones ===
    val claimedMilestones: Set<String> = emptySet(),

    // === NEW: Daily Calendar ===
    val calendarDay: Int = 1,
    val calendarClaimed: Boolean = false,

    // === NEW: Collection Bonuses ===
    val collectionBonusLevel: Int = 0,

    // === NEW: Season/Event ===
    val seasonPoints: Int = 0,
    val seasonLevel: Int = 1
) {
    companion object {
        const val BOARD_SIZE = 20
        const val BOARD_COLUMNS = 5
        const val BOARD_ROWS = 4
        const val ENERGY_REGEN_TIME_MS = 300000L
        const val SPAWN_ENERGY_COST = 3
        const val COMBO_TIMEOUT_MS = 3000L
        const val FEVER_DURATION_MS = 30000L
        const val MAX_COMBO = 10
        const val MAX_GIFT_LEVEL = 12
        const val PRESTIGE_LEVEL_REQUIREMENT = 30
    }
}

/**
 * Permanent bonuses from prestige
 */
data class PermanentBonuses(
    val coinMultiplier: Float = 1.0f,
    val expMultiplier: Float = 1.0f,
    val energyRegenBonus: Float = 1.0f,
    val maxEnergyBonus: Int = 0,
    val startingCoins: Long = 100,
    val startingGems: Int = 10,
    val luckyChanceBonus: Float = 0f,
    val sellPriceBonus: Float = 0f
)

/**
 * Milestone rewards
 */
data class Milestone(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val requirement: MilestoneRequirement,
    val reward: MilestoneReward,
    val isClaimed: Boolean = false
)

sealed class MilestoneRequirement {
    data class Level(val level: Int) : MilestoneRequirement()
    data class Merges(val count: Int) : MilestoneRequirement()
    data class HighestGift(val level: Int) : MilestoneRequirement()
    data class Coins(val amount: Long) : MilestoneRequirement()
    data class Combo(val count: Int) : MilestoneRequirement()
    data class FeverCount(val count: Int) : MilestoneRequirement()
    data class Prestige(val level: Int) : MilestoneRequirement()
    data class DailyStreak(val days: Int) : MilestoneRequirement()
    data class Collection(val count: Int) : MilestoneRequirement()
}

sealed class MilestoneReward {
    data class Coins(val amount: Long) : MilestoneReward()
    data class Gems(val amount: Int) : MilestoneReward()
    data class Energy(val amount: Int) : MilestoneReward()
    data class PrestigePoints(val amount: Int) : MilestoneReward()
    data class MaxEnergyBonus(val amount: Int) : MilestoneReward()
    data class UnlockCell(val cellIndex: Int) : MilestoneReward()
    data class SpecialGift(val type: SpecialGiftType) : MilestoneReward()
    data class Title(val title: String) : MilestoneReward()
}

/**
 * Daily calendar reward
 */
data class CalendarDay(
    val day: Int,
    val reward: CalendarReward,
    val isClaimed: Boolean = false,
    val isSpecial: Boolean = false
)

sealed class CalendarReward {
    data class Coins(val amount: Long) : CalendarReward()
    data class Gems(val amount: Int) : CalendarReward()
    data class Energy(val amount: Int) : CalendarReward()
    data class Booster(val type: BoosterType) : CalendarReward()
    data class PrestigePoints(val amount: Int) : CalendarReward()
    data class Multiple(val rewards: List<CalendarReward>) : CalendarReward()
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
    NORMAL,
    GOLDEN,
    EXPERIENCE,
    MYSTERY,
    FROZEN
}

/**
 * Special effects that can be applied to cells
 */
sealed class CellEffect {
    data class Multiplier(val value: Float, val expiresAt: Long) : CellEffect()
    data class Shield(val expiresAt: Long) : CellEffect()
}

/**
 * Represents a gift placed on the board
 */
data class CellGift(
    val typeIndex: Int,
    val level: Int,
    val isSpecial: Boolean = false,
    val specialType: SpecialGiftType? = null
) {
    val displayLevel: Int get() = level + 1

    /**
     * Calculate sell price for this gift
     */
    fun getSellPrice(prestigeBonus: Float = 0f): Long {
        val basePrice = when (level) {
            0 -> 5L
            1 -> 15L
            2 -> 40L
            3 -> 100L
            4 -> 250L
            5 -> 600L
            6 -> 1500L
            7 -> 4000L
            8 -> 10000L
            9 -> 25000L
            10 -> 60000L
            else -> 150000L
        }
        val rarityMultiplier = GiftType.fromLevel(typeIndex).rarity.expMultiplier
        return (basePrice * rarityMultiplier * (1f + prestigeBonus)).toLong()
    }
}

/**
 * Special gift types with unique abilities
 */
enum class SpecialGiftType(val emoji: String, val description: String) {
    BOMB("💣", "Уничтожает соседние подарки и превращает в монеты"),
    MAGNET("🧲", "Притягивает одинаковые подарки"),
    RAINBOW("🌈", "Соединяется с любым типом подарка"),
    MULTIPLIER("✨", "Удваивает награду за следующее соединение"),
    LIGHTNING("⚡", "Мгновенно соединяет все пары"),
    CLOCK("⏰", "Замораживает расход энергии на 1 минуту")
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
    data class Prestige(val level: Int) : UnlockRequirement()
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
    DOUBLE_XP("Двойной опыт", "📚", 300000L, 500, 5) {
        override val durationMinutes: Int get() = 5
    },
    DOUBLE_COINS("Двойные монеты", "💰", 300000L, 500, 5) {
        override val durationMinutes: Int get() = 5
    },
    ENERGY_FREEZE("Заморозка энергии", "❄️", 600000L, 800, 8) {
        override val durationMinutes: Int get() = 10
    },
    AUTO_MERGE("Авто-соединение", "🤖", 60000L, 300, 3) {
        override val durationMinutes: Int get() = 1
    },
    LUCKY_SPAWN("Удачный подарок", "🍀", 180000L, 400, 4) {
        override val durationMinutes: Int get() = 3
    },
    COMBO_KEEPER("Хранитель комбо", "🔥", 120000L, 350, 3) {
        override val durationMinutes: Int get() = 2
    };

    abstract val durationMinutes: Int
    val coinPrice: Long get() = coinCost
    val gemPrice: Int get() = gemCost
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
    val expiresAt: Long? = null
) {
    val isCompleted: Boolean get() = progress >= target
    val progressPercent: Float get() = (progress.toFloat() / target).coerceIn(0f, 1f)
}

enum class QuestType {
    MERGE_COUNT,
    MERGE_LEVEL,
    COLLECT_COINS,
    REACH_COMBO,
    USE_SPECIAL,
    TRIGGER_FEVER,
    SPEND_ENERGY,
    UNLOCK_GIFT_TYPE,
    SELL_GIFTS,
    REACH_LEVEL
}

sealed class QuestReward {
    data class Coins(val amount: Long) : QuestReward()
    data class Gems(val amount: Int) : QuestReward()
    data class Energy(val amount: Int) : QuestReward()
    data class Booster(val type: BoosterType, val count: Int) : QuestReward()
    data class SpecialGift(val type: SpecialGiftType) : QuestReward()
    data class PrestigePoints(val amount: Int) : QuestReward()
}

/**
 * Player statistics for profile
 */
data class PlayerStatistics(
    val totalMerges: Int = 0,
    val totalCoinsEarned: Long = 0,
    val totalGemsEarned: Int = 0,
    val totalExpEarned: Long = 0,
    val totalEnergySpent: Int = 0,
    val highestCombo: Int = 0,
    val feverActivations: Int = 0,
    val specialGiftsUsed: Int = 0,
    val playTimeMinutes: Long = 0,
    val gamesPlayed: Int = 1,
    val bestMergeStreak: Int = 0,
    val wheelSpins: Int = 0,
    val questsCompleted: Int = 0,
    val giftsSold: Int = 0,
    val totalSellCoins: Long = 0,
    val prestigeResets: Int = 0
)

/**
 * Lucky wheel prize
 */
data class WheelPrize(
    val type: WheelPrizeType,
    val amount: Int,
    val weight: Int
)

enum class WheelPrizeType(val emoji: String) {
    COINS("💰"),
    GEMS("💎"),
    ENERGY("⚡"),
    BOOSTER("🎁"),
    SPECIAL_GIFT("🌟"),
    JACKPOT("🎰"),
    PRESTIGE_POINTS("⭐"),
    NOTHING("😅")
}

/**
 * Player achievements
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
    data class PrestigePoints(val amount: Int) : AchievementReward()
}

/**
 * Prestige upgrade options
 */
enum class PrestigeUpgrade(
    val displayName: String,
    val description: String,
    val icon: String,
    val maxLevel: Int,
    val baseCost: Int
) {
    COIN_MULTIPLIER("Мастер монет", "+10% монет за уровень", "💰", 10, 5),
    EXP_MULTIPLIER("Рост опыта", "+10% опыта за уровень", "📚", 10, 5),
    ENERGY_REGEN("Быстрая энергия", "-5% времени регенерации", "⚡", 10, 8),
    MAX_ENERGY("Бак энергии", "+5 к макс. энергии", "🔋", 10, 10),
    STARTING_COINS("Богатый старт", "+100 начальных монет", "🏦", 5, 15),
    STARTING_GEMS("Старт с кристаллами", "+5 начальных кристаллов", "💎", 5, 20),
    LUCKY_CHANCE("Звезда удачи", "+2% шанс особого подарка", "🍀", 5, 25),
    SELL_BONUS("Торговец", "+10% к цене продажи", "🏪", 5, 12)
}
