package com.giftfest.game.domain.usecase

import com.giftfest.game.data.repository.GameRepository
import com.giftfest.game.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlin.random.Random

class GameUseCase(private val repository: GameRepository) {

    fun getGameState(): Flow<GameState> = repository.getGameState()

    suspend fun initializeGame() {
        repository.initializeGame()
    }

    /**
     * Spawns a new gift on the board
     * @return true if spawn was successful, false otherwise
     */
    suspend fun spawnGift(): SpawnResult {
        val player = repository.getPlayerSync() ?: return SpawnResult.Error("Player not found")

        // Check energy
        if (player.energy < GameState.SPAWN_ENERGY_COST) {
            return SpawnResult.NotEnoughEnergy
        }

        // Find empty cell
        val emptyCell = repository.getFirstEmptyCell()
            ?: return SpawnResult.BoardFull

        // Consume energy
        repository.updateEnergy(
            player.energy - GameState.SPAWN_ENERGY_COST,
            System.currentTimeMillis()
        )

        // Generate random gift (weighted by rarity)
        val giftType = generateRandomGiftType(player.level)

        // Place gift on board
        repository.updateCellGift(emptyCell.index, giftType, 0)

        return SpawnResult.Success(emptyCell.index, giftType, 0)
    }

    /**
     * Attempts to merge two gifts
     */
    suspend fun mergeGifts(fromIndex: Int, toIndex: Int): MergeResult {
        val board = repository.getBoardSync()

        val fromCell = board.find { it.index == fromIndex }
            ?: return MergeResult.Error("Source cell not found")
        val toCell = board.find { it.index == toIndex }
            ?: return MergeResult.Error("Target cell not found")

        // Validate cells have gifts
        if (fromCell.giftTypeIndex == null || fromCell.giftLevel == null) {
            return MergeResult.Error("Source cell is empty")
        }
        if (toCell.giftTypeIndex == null || toCell.giftLevel == null) {
            return MergeResult.Error("Target cell is empty")
        }

        // Check if gifts can be merged (same type and level)
        if (fromCell.giftTypeIndex != toCell.giftTypeIndex ||
            fromCell.giftLevel != toCell.giftLevel) {
            return MergeResult.CannotMerge
        }

        val currentLevel = fromCell.giftLevel
        val newLevel = currentLevel + 1

        // Check max level
        if (newLevel >= Gift.MAX_LEVEL) {
            return MergeResult.MaxLevelReached
        }

        val player = repository.getPlayerSync() ?: return MergeResult.Error("Player not found")

        // Clear source cell
        repository.clearCell(fromIndex)

        // Upgrade target cell
        repository.updateCellGift(toIndex, fromCell.giftTypeIndex, newLevel)

        // Calculate rewards
        val giftType = GiftType.fromLevel(fromCell.giftTypeIndex)
        val expGain = calculateExpGain(newLevel, giftType.rarity)
        val coinsGain = calculateCoinsGain(newLevel, giftType.rarity)

        // Update player stats
        repository.incrementMerges(newLevel)

        val newExp = player.experience + expGain
        val (finalExp, newPlayerLevel) = calculateLevelUp(newExp, player.level)
        repository.updateExperience(finalExp, newPlayerLevel)
        repository.updateCoins(player.coins + coinsGain)

        // Check if new gift type should be unlocked
        if (newLevel >= 2 && fromCell.giftTypeIndex + 1 < GiftType.entries.size) {
            unlockNextGiftType(fromCell.giftTypeIndex + 1)
        }

        // Check for cell unlocks based on level
        if (newPlayerLevel > player.level) {
            checkCellUnlocks(newPlayerLevel)
        }

        return MergeResult.Success(
            newLevel = newLevel,
            expGained = expGain,
            coinsGained = coinsGain,
            leveledUp = newPlayerLevel > player.level,
            newPlayerLevel = newPlayerLevel
        )
    }

    /**
     * Moves a gift from one cell to another empty cell
     */
    suspend fun moveGift(fromIndex: Int, toIndex: Int): Boolean {
        val board = repository.getBoardSync()

        val fromCell = board.find { it.index == fromIndex } ?: return false
        val toCell = board.find { it.index == toIndex } ?: return false

        // Source must have a gift
        if (fromCell.giftTypeIndex == null || fromCell.giftLevel == null) return false

        // Target must be empty and unlocked
        if (toCell.giftTypeIndex != null || toCell.isLocked) return false

        // Move the gift
        repository.updateCellGift(toIndex, fromCell.giftTypeIndex, fromCell.giftLevel)
        repository.clearCell(fromIndex)

        return true
    }

    /**
     * Regenerates energy based on time passed
     */
    suspend fun regenerateEnergy(): Int {
        val player = repository.getPlayerSync() ?: return 0

        if (player.energy >= player.maxEnergy) return 0

        val now = System.currentTimeMillis()
        val timePassed = now - player.lastEnergyRegenTime
        val energyToRegen = (timePassed / GameState.ENERGY_REGEN_TIME_MS).toInt()

        if (energyToRegen > 0) {
            val newEnergy = (player.energy + energyToRegen).coerceAtMost(player.maxEnergy)
            val newRegenTime = player.lastEnergyRegenTime + (energyToRegen * GameState.ENERGY_REGEN_TIME_MS)
            repository.updateEnergy(newEnergy, newRegenTime)
            return energyToRegen
        }

        return 0
    }

    /**
     * Claims daily reward
     */
    suspend fun claimDailyReward(): DailyRewardResult {
        val player = repository.getPlayerSync() ?: return DailyRewardResult.Error

        val now = System.currentTimeMillis()
        val dayMs = 24 * 60 * 60 * 1000L
        val timeSinceLastReward = now - player.lastDailyRewardTime

        if (timeSinceLastReward < dayMs && player.lastDailyRewardTime > 0) {
            return DailyRewardResult.AlreadyClaimed
        }

        // Check if streak should continue or reset
        val newStreak = if (timeSinceLastReward < 2 * dayMs) {
            (player.dailyStreak + 1).coerceAtMost(7)
        } else {
            1
        }

        // Calculate reward based on streak
        val coinsReward = 50L * newStreak
        val energyReward = 5 * newStreak

        repository.updateDailyReward(newStreak, now)
        repository.updateCoins(player.coins + coinsReward)

        val newEnergy = (player.energy + energyReward).coerceAtMost(player.maxEnergy)
        repository.updateEnergy(newEnergy, player.lastEnergyRegenTime)

        return DailyRewardResult.Success(
            streak = newStreak,
            coinsEarned = coinsReward,
            energyEarned = energyReward
        )
    }

    private fun generateRandomGiftType(playerLevel: Int): Int {
        // Higher level players can get higher tier gifts
        val maxTier = ((playerLevel - 1) / 5).coerceIn(0, 3)
        val weights = mutableListOf<Pair<Int, Int>>()

        for (i in 0..maxTier) {
            val weight = when (i) {
                0 -> 60 // Common (60%)
                1 -> 25 // Uncommon (25%)
                2 -> 12 // Rare (12%)
                else -> 3 // Epic (3%)
            }
            weights.add(i to weight)
        }

        val totalWeight = weights.sumOf { it.second }
        var random = Random.nextInt(totalWeight)

        for ((tier, weight) in weights) {
            random -= weight
            if (random < 0) return tier
        }

        return 0
    }

    private fun calculateExpGain(level: Int, rarity: GiftRarity): Long {
        val baseExp = 10L * (level + 1)
        return (baseExp * rarity.expMultiplier).toLong()
    }

    private fun calculateCoinsGain(level: Int, rarity: GiftRarity): Long {
        val baseCoins = 5L * (level + 1)
        return (baseCoins * rarity.expMultiplier).toLong()
    }

    private fun calculateLevelUp(experience: Long, currentLevel: Int): Pair<Long, Int> {
        var exp = experience
        var level = currentLevel

        while (true) {
            val expNeeded = calculateExpToNextLevel(level)
            if (exp >= expNeeded) {
                exp -= expNeeded
                level++
            } else {
                break
            }
        }

        return exp to level
    }

    private fun calculateExpToNextLevel(level: Int): Long {
        return (100 * level * (1 + level * 0.1)).toLong()
    }

    private suspend fun unlockNextGiftType(typeIndex: Int) {
        val player = repository.getPlayerSync() ?: return
        val currentUnlocked = player.unlockedGiftsJson
            .removeSurrounding("[", "]")
            .split(",")
            .filter { it.isNotBlank() }
            .map { it.trim().toInt() }
            .toMutableSet()

        if (!currentUnlocked.contains(typeIndex)) {
            currentUnlocked.add(typeIndex)
            repository.updateUnlockedGifts(currentUnlocked)
        }
    }

    private suspend fun checkCellUnlocks(playerLevel: Int) {
        val board = repository.getBoardSync()

        for (cell in board) {
            if (cell.isLocked && cell.unlockType == "level") {
                val requiredLevel = cell.unlockValue ?: continue
                if (playerLevel >= requiredLevel) {
                    repository.unlockCell(cell.index)
                }
            }
        }
    }
}

sealed class SpawnResult {
    data class Success(val cellIndex: Int, val giftType: Int, val level: Int) : SpawnResult()
    data object NotEnoughEnergy : SpawnResult()
    data object BoardFull : SpawnResult()
    data class Error(val message: String) : SpawnResult()
}

sealed class MergeResult {
    data class Success(
        val newLevel: Int,
        val expGained: Long,
        val coinsGained: Long,
        val leveledUp: Boolean,
        val newPlayerLevel: Int
    ) : MergeResult()
    data object CannotMerge : MergeResult()
    data object MaxLevelReached : MergeResult()
    data class Error(val message: String) : MergeResult()
}

sealed class DailyRewardResult {
    data class Success(
        val streak: Int,
        val coinsEarned: Long,
        val energyEarned: Int
    ) : DailyRewardResult()
    data object AlreadyClaimed : DailyRewardResult()
    data object Error : DailyRewardResult()
}
