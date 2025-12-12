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
     * Spawns a new gift on the board with combo and booster considerations
     */
    suspend fun spawnGift(): SpawnResult {
        val player = repository.getPlayerSync() ?: return SpawnResult.Error("Player not found")

        // Check if energy freeze booster is active
        val hasEnergyFreeze = player.activeBoostersJson.contains("ENERGY_FREEZE")
        val energyCost = if (hasEnergyFreeze) 0 else GameState.SPAWN_ENERGY_COST

        if (player.energy < energyCost) {
            return SpawnResult.NotEnoughEnergy
        }

        val emptyCell = repository.getRandomEmptyCell()
            ?: return SpawnResult.BoardFull

        // Consume energy
        if (energyCost > 0) {
            repository.updateEnergy(
                player.energy - energyCost,
                System.currentTimeMillis()
            )
            repository.updateStatsEnergySpent(energyCost)
        }

        // Check for lucky spawn booster
        val hasLuckySpawn = player.activeBoostersJson.contains("LUCKY_SPAWN")

        // Generate gift with potential special type
        val (giftType, isSpecial, specialType) = generateGift(player.level, hasLuckySpawn)

        repository.updateCellGift(
            emptyCell.index,
            giftType,
            0,
            isSpecial,
            specialType?.name
        )

        return SpawnResult.Success(
            cellIndex = emptyCell.index,
            giftType = giftType,
            level = 0,
            isSpecial = isSpecial,
            specialType = specialType
        )
    }

    /**
     * Enhanced merge with combo, fever, and special gift handling
     */
    suspend fun mergeGifts(fromIndex: Int, toIndex: Int): MergeResult {
        val board = repository.getBoardSync()
        val player = repository.getPlayerSync() ?: return MergeResult.Error("Player not found")

        val fromCell = board.find { it.index == fromIndex }
            ?: return MergeResult.Error("Source cell not found")
        val toCell = board.find { it.index == toIndex }
            ?: return MergeResult.Error("Target cell not found")

        if (fromCell.giftTypeIndex == null || fromCell.giftLevel == null) {
            return MergeResult.Error("Source cell is empty")
        }
        if (toCell.giftTypeIndex == null || toCell.giftLevel == null) {
            return MergeResult.Error("Target cell is empty")
        }

        // Rainbow gift can merge with anything
        val isRainbowMerge = fromCell.specialGiftType == "RAINBOW" || toCell.specialGiftType == "RAINBOW"

        if (!isRainbowMerge) {
            if (fromCell.giftTypeIndex != toCell.giftTypeIndex ||
                fromCell.giftLevel != toCell.giftLevel) {
                return MergeResult.CannotMerge
            }
        }

        val currentLevel = maxOf(fromCell.giftLevel, toCell.giftLevel)
        val newLevel = currentLevel + 1

        if (newLevel >= Gift.MAX_LEVEL) {
            return MergeResult.MaxLevelReached
        }

        val now = System.currentTimeMillis()

        // Calculate combo
        val comboKeeperActive = player.activeBoostersJson.contains("COMBO_KEEPER")
        val comboTimeout = if (comboKeeperActive) GameState.COMBO_TIMEOUT_MS * 2 else GameState.COMBO_TIMEOUT_MS
        val timeSinceLastMerge = now - player.lastMergeTime

        val newCombo = if (timeSinceLastMerge < comboTimeout && player.lastMergeTime > 0) {
            (player.currentCombo + 1).coerceAtMost(GameState.MAX_COMBO)
        } else {
            1
        }

        val comboMultiplier = 1f + (newCombo * 0.1f)

        // Check for fever mode
        var feverProgress = player.feverProgress + 0.1f
        var isFeverActive = player.isFeverActive
        var feverEndTime = player.feverEndTime
        var triggeredFever = false

        if (now > feverEndTime) {
            isFeverActive = false
        }

        if (feverProgress >= 1f && !isFeverActive) {
            isFeverActive = true
            feverEndTime = now + GameState.FEVER_DURATION_MS
            feverProgress = 0f
            triggeredFever = true
            repository.incrementFeverTriggered()
        }

        // Calculate rewards with all multipliers
        val giftType = GiftType.fromLevel(fromCell.giftTypeIndex)
        var expMultiplier = comboMultiplier
        var coinsMultiplier = comboMultiplier

        // Cell type bonuses
        if (toCell.cellType == "GOLDEN") coinsMultiplier *= 2f
        if (toCell.cellType == "EXPERIENCE") expMultiplier *= 2f

        // Fever mode bonus
        if (isFeverActive) {
            expMultiplier *= 2f
            coinsMultiplier *= 2f
        }

        // Booster bonuses
        if (player.activeBoostersJson.contains("DOUBLE_XP")) expMultiplier *= 2f
        if (player.activeBoostersJson.contains("DOUBLE_COINS")) coinsMultiplier *= 2f

        // Multiplier gift bonus (one-time)
        if (fromCell.specialGiftType == "MULTIPLIER" || toCell.specialGiftType == "MULTIPLIER") {
            expMultiplier *= 2f
            coinsMultiplier *= 2f
            repository.incrementSpecialGiftsUsed()
        }

        val baseExp = calculateExpGain(newLevel, giftType.rarity)
        val baseCoins = calculateCoinsGain(newLevel, giftType.rarity)
        val expGain = (baseExp * expMultiplier).toLong()
        val coinsGain = (baseCoins * coinsMultiplier).toLong()

        // Mystery cell random bonus
        var bonusGems = 0
        if (toCell.cellType == "MYSTERY" && Random.nextFloat() < 0.3f) {
            bonusGems = Random.nextInt(1, 5)
            repository.updateGems(player.gems + bonusGems)
        }

        // Clear source and update target
        repository.clearCell(fromIndex)
        repository.updateCellGift(toIndex, fromCell.giftTypeIndex, newLevel, false, null)

        // Update player stats
        repository.incrementMerges(newLevel)
        repository.updateCombo(newCombo, now)
        repository.updateFever(feverProgress.coerceIn(0f, 1f), isFeverActive, feverEndTime)
        repository.updateStatsOnMerge(coinsGain, newCombo)

        val newExp = player.experience + expGain
        val (finalExp, newPlayerLevel) = calculateLevelUp(newExp, player.level)
        repository.updateExperience(finalExp, newPlayerLevel)
        repository.updateCoins(player.coins + coinsGain)

        // Unlock new gift types
        if (newLevel >= 2 && fromCell.giftTypeIndex + 1 < GiftType.entries.size) {
            unlockNextGiftType(fromCell.giftTypeIndex + 1)
        }

        // Check unlocks
        if (newPlayerLevel > player.level) {
            checkCellUnlocks(newPlayerLevel, player.totalMerges + 1)
        }

        // Update quests
        updateQuestProgress(QuestType.MERGE_COUNT, 1)
        updateQuestProgress(QuestType.MERGE_LEVEL, newLevel)
        updateQuestProgress(QuestType.REACH_COMBO, newCombo)
        updateQuestProgress(QuestType.COLLECT_COINS, coinsGain.toInt())

        return MergeResult.Success(
            newLevel = newLevel,
            expGained = expGain,
            coinsGained = coinsGain,
            gemsGained = bonusGems,
            leveledUp = newPlayerLevel > player.level,
            newPlayerLevel = newPlayerLevel,
            combo = newCombo,
            triggeredFever = triggeredFever
        )
    }

    /**
     * Use a special gift ability
     */
    suspend fun useSpecialGift(cellIndex: Int): SpecialGiftResult {
        val board = repository.getBoardSync()
        val cell = board.find { it.index == cellIndex }
            ?: return SpecialGiftResult.Error("Cell not found")

        if (!cell.isSpecialGift || cell.specialGiftType == null) {
            return SpecialGiftResult.NotSpecial
        }

        val specialType = try {
            SpecialGiftType.valueOf(cell.specialGiftType)
        } catch (e: Exception) {
            return SpecialGiftResult.Error("Invalid special type")
        }

        repository.incrementSpecialGiftsUsed()
        updateQuestProgress(QuestType.USE_SPECIAL, 1)

        return when (specialType) {
            SpecialGiftType.BOMB -> useBombGift(cellIndex)
            SpecialGiftType.MAGNET -> useMagnetGift(cellIndex)
            SpecialGiftType.LIGHTNING -> useLightningGift()
            SpecialGiftType.CLOCK -> useClockGift()
            else -> SpecialGiftResult.Success(specialType, 0, emptyList())
        }
    }

    private suspend fun useBombGift(cellIndex: Int): SpecialGiftResult {
        val adjacentCells = repository.getAdjacentCells(cellIndex)
        var totalCoins = 0L

        repository.clearCell(cellIndex)

        for (cell in adjacentCells) {
            if (cell.giftTypeIndex != null && cell.giftLevel != null) {
                totalCoins += (cell.giftLevel + 1) * 10L
                repository.clearCell(cell.index)
            }
        }

        val player = repository.getPlayerSync()
        if (player != null) {
            repository.updateCoins(player.coins + totalCoins)
        }

        return SpecialGiftResult.Success(
            type = SpecialGiftType.BOMB,
            coinsGained = totalCoins,
            affectedCells = adjacentCells.map { it.index }
        )
    }

    private suspend fun useMagnetGift(cellIndex: Int): SpecialGiftResult {
        val cell = repository.getBoardSync().find { it.index == cellIndex }
            ?: return SpecialGiftResult.Error("Cell not found")

        if (cell.giftTypeIndex == null) return SpecialGiftResult.Error("No gift")

        val matchingCells = repository.getCellsWithMatchingGift(cell.giftTypeIndex, cell.giftLevel ?: 0)
        val affectedIndices = mutableListOf<Int>()

        // Move matching gifts adjacent to this one
        val emptyAdjacentCells = repository.getAdjacentCells(cellIndex).filter { it.giftTypeIndex == null && !it.isLocked }

        for ((index, matchCell) in matchingCells.withIndex()) {
            if (matchCell.index == cellIndex) continue
            if (index >= emptyAdjacentCells.size) break

            val targetCell = emptyAdjacentCells[index]
            repository.updateCellGift(targetCell.index, matchCell.giftTypeIndex, matchCell.giftLevel)
            repository.clearCell(matchCell.index)
            affectedIndices.add(matchCell.index)
            affectedIndices.add(targetCell.index)
        }

        repository.clearCell(cellIndex) // Clear the magnet

        return SpecialGiftResult.Success(
            type = SpecialGiftType.MAGNET,
            coinsGained = 0,
            affectedCells = affectedIndices
        )
    }

    private suspend fun useLightningGift(): SpecialGiftResult {
        val cellsWithGifts = repository.getCellsWithGifts()
        val grouped = cellsWithGifts.groupBy { "${it.giftTypeIndex}_${it.giftLevel}" }
        var mergeCount = 0

        for ((_, cells) in grouped) {
            if (cells.size >= 2) {
                // Auto-merge pairs
                val pairs = cells.chunked(2).filter { it.size == 2 }
                for (pair in pairs) {
                    val from = pair[0]
                    val to = pair[1]
                    if (from.giftLevel != null && from.giftLevel < Gift.MAX_LEVEL - 1) {
                        repository.clearCell(from.index)
                        repository.updateCellGift(to.index, to.giftTypeIndex, (to.giftLevel ?: 0) + 1)
                        mergeCount++
                    }
                }
            }
        }

        return SpecialGiftResult.Success(
            type = SpecialGiftType.LIGHTNING,
            coinsGained = mergeCount * 20L,
            affectedCells = emptyList()
        )
    }

    private suspend fun useClockGift(): SpecialGiftResult {
        val player = repository.getPlayerSync() ?: return SpecialGiftResult.Error("Player not found")

        // Add energy freeze booster for 1 minute
        val newBooster = ActiveBooster(
            type = BoosterType.ENERGY_FREEZE,
            expiresAt = System.currentTimeMillis() + 60000
        )

        val currentBoosters = mutableListOf<ActiveBooster>()
        // Parse existing and add new
        currentBoosters.add(newBooster)
        repository.updateActiveBoosters(currentBoosters)

        return SpecialGiftResult.Success(
            type = SpecialGiftType.CLOCK,
            coinsGained = 0,
            affectedCells = emptyList()
        )
    }

    /**
     * Spin the lucky wheel
     */
    suspend fun spinLuckyWheel(): WheelSpinResult {
        val player = repository.getPlayerSync() ?: return WheelSpinResult.Error("Player not found")

        val now = System.currentTimeMillis()
        val dayMs = 24 * 60 * 60 * 1000L

        // Check if free spin available
        val canFreeSpin = player.freeSpinsAvailable > 0 ||
                (now - player.lastWheelSpinTime > dayMs)

        if (!canFreeSpin && player.gems < 10) {
            return WheelSpinResult.NotEnoughGems
        }

        // Consume resource
        if (!canFreeSpin) {
            repository.updateGems(player.gems - 10)
        }

        repository.updateWheelSpin(now, if (canFreeSpin) 0 else player.freeSpinsAvailable)
        repository.incrementWheelSpins()

        // Generate prize
        val prizes = listOf(
            WheelPrize(WheelPrizeType.COINS, 100, 30),
            WheelPrize(WheelPrizeType.COINS, 250, 20),
            WheelPrize(WheelPrizeType.COINS, 500, 10),
            WheelPrize(WheelPrizeType.ENERGY, 10, 25),
            WheelPrize(WheelPrizeType.ENERGY, 25, 15),
            WheelPrize(WheelPrizeType.GEMS, 5, 15),
            WheelPrize(WheelPrizeType.GEMS, 15, 5),
            WheelPrize(WheelPrizeType.BOOSTER, 1, 10),
            WheelPrize(WheelPrizeType.SPECIAL_GIFT, 1, 5),
            WheelPrize(WheelPrizeType.JACKPOT, 1000, 1),
            WheelPrize(WheelPrizeType.NOTHING, 0, 20)
        )

        val totalWeight = prizes.sumOf { it.weight }
        var random = Random.nextInt(totalWeight)
        var selectedPrize = prizes.last()

        for (prize in prizes) {
            random -= prize.weight
            if (random < 0) {
                selectedPrize = prize
                break
            }
        }

        // Apply prize
        when (selectedPrize.type) {
            WheelPrizeType.COINS -> repository.updateCoins(player.coins + selectedPrize.amount)
            WheelPrizeType.GEMS -> repository.updateGems(player.gems + selectedPrize.amount)
            WheelPrizeType.ENERGY -> {
                val newEnergy = (player.energy + selectedPrize.amount).coerceAtMost(player.maxEnergy)
                repository.updateEnergy(newEnergy, player.lastEnergyRegenTime)
            }
            WheelPrizeType.JACKPOT -> {
                repository.updateCoins(player.coins + 1000)
                repository.updateGems(player.gems + 50)
            }
            WheelPrizeType.BOOSTER -> {
                // Give random booster
                val boosterType = BoosterType.entries.random()
                val newBooster = ActiveBooster(boosterType, System.currentTimeMillis() + boosterType.durationMs)
                repository.updateActiveBoosters(listOf(newBooster))
            }
            else -> { /* Nothing */ }
        }

        return WheelSpinResult.Success(selectedPrize)
    }

    /**
     * Calculate offline rewards
     */
    suspend fun calculateOfflineRewards(): OfflineRewardResult {
        val player = repository.getPlayerSync() ?: return OfflineRewardResult(0, 0, 0)

        val now = System.currentTimeMillis()
        val offlineTime = now - player.lastOnlineTime
        val offlineMinutes = (offlineTime / 60000).toInt()

        if (offlineMinutes < 5) {
            repository.updateLastOnlineTime(now)
            return OfflineRewardResult(0, 0, 0)
        }

        // Cap at 8 hours
        val cappedMinutes = offlineMinutes.coerceAtMost(480)

        val coins = (cappedMinutes * 2).toLong()
        val energy = (cappedMinutes / 10).coerceAtMost(20)

        repository.updateCoins(player.coins + coins)
        val newEnergy = (player.energy + energy).coerceAtMost(player.maxEnergy)
        repository.updateEnergy(newEnergy, now)
        repository.updateLastOnlineTime(now)
        repository.addPlayTime(cappedMinutes.toLong())

        return OfflineRewardResult(
            coins = coins,
            energy = energy,
            minutesOffline = cappedMinutes
        )
    }

    /**
     * Purchase and activate a booster
     */
    suspend fun purchaseBooster(type: BoosterType, useGems: Boolean): BoosterPurchaseResult {
        val player = repository.getPlayerSync() ?: return BoosterPurchaseResult.Error("Player not found")

        val cost = if (useGems) type.gemCost else type.coinCost.toInt()
        val hasEnough = if (useGems) player.gems >= cost else player.coins >= cost

        if (!hasEnough) {
            return if (useGems) BoosterPurchaseResult.NotEnoughGems else BoosterPurchaseResult.NotEnoughCoins
        }

        // Deduct cost
        if (useGems) {
            repository.updateGems(player.gems - cost)
        } else {
            repository.updateCoins(player.coins - cost)
        }

        // Activate booster
        val newBooster = ActiveBooster(type, System.currentTimeMillis() + type.durationMs)

        // Parse current active boosters and add new one
        val currentActive = mutableListOf(newBooster)
        repository.updateActiveBoosters(currentActive)

        return BoosterPurchaseResult.Success(type)
    }

    suspend fun moveGift(fromIndex: Int, toIndex: Int): Boolean {
        val board = repository.getBoardSync()
        val fromCell = board.find { it.index == fromIndex } ?: return false
        val toCell = board.find { it.index == toIndex } ?: return false

        if (fromCell.giftTypeIndex == null || fromCell.giftLevel == null) return false
        if (toCell.giftTypeIndex != null || toCell.isLocked || toCell.cellType == "FROZEN") return false

        repository.updateCellGift(toIndex, fromCell.giftTypeIndex, fromCell.giftLevel, fromCell.isSpecialGift, fromCell.specialGiftType)
        repository.clearCell(fromIndex)

        return true
    }

    suspend fun regenerateEnergy(): Int {
        val player = repository.getPlayerSync() ?: return 0
        if (player.energy >= player.maxEnergy) return 0

        // Check for energy freeze
        if (player.activeBoostersJson.contains("ENERGY_FREEZE")) return 0

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

    suspend fun claimDailyReward(): DailyRewardResult {
        val player = repository.getPlayerSync() ?: return DailyRewardResult.Error

        val now = System.currentTimeMillis()
        val dayMs = 24 * 60 * 60 * 1000L
        val timeSinceLastReward = now - player.lastDailyRewardTime

        if (timeSinceLastReward < dayMs && player.lastDailyRewardTime > 0) {
            return DailyRewardResult.AlreadyClaimed
        }

        val newStreak = if (timeSinceLastReward < 2 * dayMs) {
            (player.dailyStreak + 1).coerceAtMost(7)
        } else {
            1
        }

        // Enhanced rewards based on streak
        val coinsReward = 100L * newStreak
        val energyReward = 10 * newStreak
        val gemsReward = if (newStreak >= 7) 20 else newStreak

        repository.updateDailyReward(newStreak, now)
        repository.updateCoins(player.coins + coinsReward)
        repository.updateGems(player.gems + gemsReward)

        val newEnergy = (player.energy + energyReward).coerceAtMost(player.maxEnergy)
        repository.updateEnergy(newEnergy, player.lastEnergyRegenTime)

        return DailyRewardResult.Success(
            streak = newStreak,
            coinsEarned = coinsReward,
            energyEarned = energyReward,
            gemsEarned = gemsReward
        )
    }

    private fun generateGift(playerLevel: Int, luckySpawn: Boolean): Triple<Int, Boolean, SpecialGiftType?> {
        val maxTier = ((playerLevel - 1) / 5).coerceIn(0, 5)

        // 5% chance for special gift (10% with lucky spawn)
        val specialChance = if (luckySpawn) 0.15f else 0.05f
        if (Random.nextFloat() < specialChance) {
            val specialType = SpecialGiftType.entries.random()
            return Triple(Random.nextInt(maxTier + 1), true, specialType)
        }

        val weights = mutableListOf<Pair<Int, Int>>()
        for (i in 0..maxTier) {
            val weight = when (i) {
                0 -> 50
                1 -> 30
                2 -> 15
                3 -> 8
                4 -> 4
                else -> 2
            }
            weights.add(i to weight)
        }

        val totalWeight = weights.sumOf { it.second }
        var random = Random.nextInt(totalWeight)

        for ((tier, weight) in weights) {
            random -= weight
            if (random < 0) return Triple(tier, false, null)
        }

        return Triple(0, false, null)
    }

    private fun calculateExpGain(level: Int, rarity: GiftRarity): Long {
        return (15L * (level + 1) * rarity.expMultiplier).toLong()
    }

    private fun calculateCoinsGain(level: Int, rarity: GiftRarity): Long {
        return (8L * (level + 1) * rarity.expMultiplier).toLong()
    }

    private fun calculateLevelUp(experience: Long, currentLevel: Int): Pair<Long, Int> {
        var exp = experience
        var level = currentLevel

        while (true) {
            val expNeeded = (100 * level * (1 + level * 0.1)).toLong()
            if (exp >= expNeeded) {
                exp -= expNeeded
                level++
            } else {
                break
            }
        }

        return exp to level
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
            updateQuestProgress(QuestType.UNLOCK_GIFT_TYPE, currentUnlocked.size)
        }
    }

    private suspend fun checkCellUnlocks(playerLevel: Int, totalMerges: Int) {
        val board = repository.getBoardSync()

        for (cell in board) {
            if (!cell.isLocked) continue

            val shouldUnlock = when (cell.unlockType) {
                "level" -> cell.unlockValue?.let { playerLevel >= it } ?: false
                "merges" -> cell.unlockValue?.let { totalMerges >= it } ?: false
                else -> false
            }

            if (shouldUnlock) {
                repository.unlockCell(cell.index)
            }
        }
    }

    private suspend fun updateQuestProgress(type: QuestType, value: Int) {
        // Quest progress update would be implemented here
        // For now, this is a placeholder
    }
}

// Result classes
sealed class SpawnResult {
    data class Success(
        val cellIndex: Int,
        val giftType: Int,
        val level: Int,
        val isSpecial: Boolean = false,
        val specialType: SpecialGiftType? = null
    ) : SpawnResult()
    data object NotEnoughEnergy : SpawnResult()
    data object BoardFull : SpawnResult()
    data class Error(val message: String) : SpawnResult()
}

sealed class MergeResult {
    data class Success(
        val newLevel: Int,
        val expGained: Long,
        val coinsGained: Long,
        val gemsGained: Int = 0,
        val leveledUp: Boolean,
        val newPlayerLevel: Int,
        val combo: Int = 1,
        val triggeredFever: Boolean = false
    ) : MergeResult()
    data object CannotMerge : MergeResult()
    data object MaxLevelReached : MergeResult()
    data class Error(val message: String) : MergeResult()
}

sealed class DailyRewardResult {
    data class Success(
        val streak: Int,
        val coinsEarned: Long,
        val energyEarned: Int,
        val gemsEarned: Int = 0
    ) : DailyRewardResult()
    data object AlreadyClaimed : DailyRewardResult()
    data object Error : DailyRewardResult()
}

sealed class SpecialGiftResult {
    data class Success(
        val type: SpecialGiftType,
        val coinsGained: Long,
        val affectedCells: List<Int>
    ) : SpecialGiftResult()
    data object NotSpecial : SpecialGiftResult()
    data class Error(val message: String) : SpecialGiftResult()
}

sealed class WheelSpinResult {
    data class Success(val prize: WheelPrize) : WheelSpinResult()
    data object NotEnoughGems : WheelSpinResult()
    data class Error(val message: String) : WheelSpinResult()
}

data class OfflineRewardResult(
    val coins: Long,
    val energy: Int,
    val minutesOffline: Int
)

sealed class BoosterPurchaseResult {
    data class Success(val type: BoosterType) : BoosterPurchaseResult()
    data object NotEnoughCoins : BoosterPurchaseResult()
    data object NotEnoughGems : BoosterPurchaseResult()
    data class Error(val message: String) : BoosterPurchaseResult()
}
