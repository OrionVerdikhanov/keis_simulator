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
     */
    suspend fun spawnGift(): SpawnResult {
        val player = repository.getPlayerSync() ?: return SpawnResult.Error("Player not found")

        val hasEnergyFreeze = player.activeBoostersJson.contains("ENERGY_FREEZE")
        val energyCost = if (hasEnergyFreeze) 0 else GameState.SPAWN_ENERGY_COST

        if (player.energy < energyCost) {
            return SpawnResult.NotEnoughEnergy
        }

        val emptyCell = repository.getRandomEmptyCell()
            ?: return SpawnResult.BoardFull

        if (energyCost > 0) {
            repository.updateEnergy(
                player.energy - energyCost,
                System.currentTimeMillis()
            )
            repository.updateStatsEnergySpent(energyCost)
        }

        val hasLuckySpawn = player.activeBoostersJson.contains("LUCKY_SPAWN")
        val luckyBonus = player.luckyChanceBonus

        val (giftType, isSpecial, specialType) = generateGift(player.level, hasLuckySpawn, luckyBonus)

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
     * Sell a gift for coins and optionally spawn a new one
     */
    suspend fun sellGift(cellIndex: Int, spawnNew: Boolean = true): SellResult {
        val player = repository.getPlayerSync() ?: return SellResult.Error("Player not found")
        val board = repository.getBoardSync()
        val cell = board.find { it.index == cellIndex }
            ?: return SellResult.Error("Cell not found")

        if (cell.giftTypeIndex == null || cell.giftLevel == null) {
            return SellResult.Error("No gift to sell")
        }

        // Calculate sell price with prestige bonus
        val gift = CellGift(cell.giftTypeIndex, cell.giftLevel, cell.isSpecialGift)
        val sellPrice = gift.getSellPrice(player.sellPriceBonus)

        // Clear the cell
        repository.clearCell(cellIndex)

        // Update coins
        repository.updateCoins(player.coins + sellPrice)

        // Update statistics
        repository.incrementGiftsSold(sellPrice)

        // Spawn a new level 1 gift if requested
        var spawnedGift: SpawnResult.Success? = null
        if (spawnNew) {
            val hasLuckySpawn = player.activeBoostersJson.contains("LUCKY_SPAWN")
            val (giftType, isSpecial, specialType) = generateGift(player.level, hasLuckySpawn, player.luckyChanceBonus)

            repository.updateCellGift(
                cellIndex,
                giftType,
                0,
                isSpecial,
                specialType?.name
            )

            spawnedGift = SpawnResult.Success(
                cellIndex = cellIndex,
                giftType = giftType,
                level = 0,
                isSpecial = isSpecial,
                specialType = specialType
            )
        }

        return SellResult.Success(
            coinsGained = sellPrice,
            giftLevel = cell.giftLevel,
            spawnedNew = spawnedGift
        )
    }

    /**
     * Perform prestige reset - keep permanent bonuses, reset progress
     */
    suspend fun performPrestige(): PrestigeResult {
        val player = repository.getPlayerSync() ?: return PrestigeResult.Error("Player not found")

        // Check requirements
        if (player.level < GameState.PRESTIGE_LEVEL_REQUIREMENT) {
            return PrestigeResult.NotEligible(
                currentLevel = player.level,
                requiredLevel = GameState.PRESTIGE_LEVEL_REQUIREMENT
            )
        }

        // Calculate prestige points earned based on level and achievements
        val basePoints = player.level - GameState.PRESTIGE_LEVEL_REQUIREMENT + 10
        val bonusPoints = (player.totalMerges / 100) + (player.highestGiftLevel * 2)
        val totalPoints = basePoints + bonusPoints

        val newPrestigeLevel = player.prestigeLevel + 1
        val newPrestigePoints = player.prestigePoints + totalPoints

        // Reset player progress but keep prestige data
        repository.performPrestigeReset(
            newPrestigeLevel = newPrestigeLevel,
            newPrestigePoints = newPrestigePoints,
            startingCoins = player.startingCoins,
            startingGems = player.startingGems,
            maxEnergyBonus = player.maxEnergyBonus
        )

        // Increment prestige count in statistics
        repository.incrementPrestigeResets()

        return PrestigeResult.Success(
            newPrestigeLevel = newPrestigeLevel,
            pointsEarned = totalPoints,
            totalPoints = newPrestigePoints
        )
    }

    /**
     * Purchase a prestige upgrade with prestige points
     */
    suspend fun purchasePrestigeUpgrade(upgrade: PrestigeUpgrade): PrestigeUpgradeResult {
        val player = repository.getPlayerSync() ?: return PrestigeUpgradeResult.Error("Player not found")

        val currentLevel = player.getUpgradeLevel(upgrade)
        if (currentLevel >= upgrade.maxLevel) {
            return PrestigeUpgradeResult.MaxLevel
        }

        val cost = upgrade.baseCost * (currentLevel + 1)
        if (player.prestigePoints < cost) {
            return PrestigeUpgradeResult.NotEnoughPoints(
                required = cost,
                current = player.prestigePoints
            )
        }

        // Deduct points and apply upgrade
        repository.purchasePrestigeUpgrade(upgrade, cost, currentLevel + 1)

        return PrestigeUpgradeResult.Success(
            upgrade = upgrade,
            newLevel = currentLevel + 1,
            pointsSpent = cost
        )
    }

    /**
     * Check and claim available milestones
     */
    suspend fun checkMilestones(): List<Milestone> {
        val player = repository.getPlayerSync() ?: return emptyList()
        val claimable = mutableListOf<Milestone>()

        getAllMilestones().forEach { milestone ->
            if (player.claimedMilestones.contains(milestone.id)) return@forEach

            val isAchieved = when (val req = milestone.requirement) {
                is MilestoneRequirement.Level -> player.level >= req.level
                is MilestoneRequirement.Merges -> player.totalMerges >= req.count
                is MilestoneRequirement.HighestGift -> player.highestGiftLevel >= req.level
                is MilestoneRequirement.Coins -> player.totalCoinsEarned >= req.amount
                is MilestoneRequirement.Combo -> player.highestCombo >= req.count
                is MilestoneRequirement.FeverCount -> player.feverTriggered >= req.count
                is MilestoneRequirement.Prestige -> player.prestigeLevel >= req.level
                is MilestoneRequirement.DailyStreak -> player.dailyStreak >= req.days
                is MilestoneRequirement.Collection -> player.unlockedGiftsCount >= req.count
            }

            if (isAchieved) {
                claimable.add(milestone)
            }
        }

        return claimable
    }

    /**
     * Claim a milestone reward
     */
    suspend fun claimMilestone(milestoneId: String): MilestoneClaimResult {
        val player = repository.getPlayerSync() ?: return MilestoneClaimResult.Error("Player not found")

        if (player.claimedMilestones.contains(milestoneId)) {
            return MilestoneClaimResult.AlreadyClaimed
        }

        val milestone = getAllMilestones().find { it.id == milestoneId }
            ?: return MilestoneClaimResult.Error("Milestone not found")

        // Apply reward
        when (val reward = milestone.reward) {
            is MilestoneReward.Coins -> repository.updateCoins(player.coins + reward.amount)
            is MilestoneReward.Gems -> repository.updateGems(player.gems + reward.amount)
            is MilestoneReward.Energy -> {
                val newEnergy = (player.energy + reward.amount).coerceAtMost(player.maxEnergy + player.maxEnergyBonus)
                repository.updateEnergy(newEnergy, player.lastEnergyRegenTime)
            }
            is MilestoneReward.PrestigePoints -> repository.addPrestigePoints(reward.amount)
            is MilestoneReward.MaxEnergyBonus -> repository.addMaxEnergyBonus(reward.amount)
            is MilestoneReward.UnlockCell -> repository.unlockCell(reward.cellIndex)
            is MilestoneReward.SpecialGift -> {
                val emptyCell = repository.getRandomEmptyCell()
                if (emptyCell != null) {
                    repository.updateCellGift(emptyCell.index, 0, 0, true, reward.type.name)
                }
            }
            is MilestoneReward.Title -> { /* Store in player preferences */ }
        }

        repository.claimMilestone(milestoneId)

        return MilestoneClaimResult.Success(milestone)
    }

    /**
     * Get daily calendar rewards
     */
    suspend fun claimCalendarReward(): CalendarClaimResult {
        val player = repository.getPlayerSync() ?: return CalendarClaimResult.Error("Player not found")

        if (player.calendarClaimed) {
            return CalendarClaimResult.AlreadyClaimed
        }

        val calendarDay = getCalendarDay(player.calendarDay)

        // Apply rewards
        when (val reward = calendarDay.reward) {
            is CalendarReward.Coins -> repository.updateCoins(player.coins + reward.amount)
            is CalendarReward.Gems -> repository.updateGems(player.gems + reward.amount)
            is CalendarReward.Energy -> {
                val newEnergy = (player.energy + reward.amount).coerceAtMost(player.maxEnergy + player.maxEnergyBonus)
                repository.updateEnergy(newEnergy, player.lastEnergyRegenTime)
            }
            is CalendarReward.Booster -> {
                val newBooster = ActiveBooster(reward.type, System.currentTimeMillis() + reward.type.durationMs)
                repository.updateActiveBoosters(listOf(newBooster))
            }
            is CalendarReward.PrestigePoints -> repository.addPrestigePoints(reward.amount)
            is CalendarReward.Multiple -> {
                reward.rewards.forEach { subReward ->
                    when (subReward) {
                        is CalendarReward.Coins -> repository.updateCoins(player.coins + subReward.amount)
                        is CalendarReward.Gems -> repository.updateGems(player.gems + subReward.amount)
                        is CalendarReward.Energy -> {
                            val newEnergy = (player.energy + subReward.amount).coerceAtMost(player.maxEnergy + player.maxEnergyBonus)
                            repository.updateEnergy(newEnergy, player.lastEnergyRegenTime)
                        }
                        else -> {}
                    }
                }
            }
        }

        // Advance calendar
        val nextDay = if (player.calendarDay >= 30) 1 else player.calendarDay + 1
        repository.updateCalendar(nextDay, true)

        return CalendarClaimResult.Success(calendarDay)
    }

    /**
     * Reset calendar claimed status (called daily)
     */
    suspend fun resetDailyCalendar() {
        repository.updateCalendar(repository.getPlayerSync()?.calendarDay ?: 1, false)
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

        // Calculate rewards with all multipliers including prestige
        val giftType = GiftType.fromLevel(fromCell.giftTypeIndex)
        var expMultiplier = comboMultiplier * player.expMultiplier
        var coinsMultiplier = comboMultiplier * player.coinMultiplier

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

        // Multiplier gift bonus
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

        // Season points
        val seasonPoints = newLevel * 5

        // Clear source and update target
        repository.clearCell(fromIndex)
        repository.updateCellGift(toIndex, fromCell.giftTypeIndex, newLevel, false, null)

        // Update player stats
        repository.incrementMerges(newLevel)
        repository.updateCombo(newCombo, now)
        repository.updateFever(feverProgress.coerceIn(0f, 1f), isFeverActive, feverEndTime)
        repository.updateStatsOnMerge(coinsGain, newCombo)
        repository.addSeasonPoints(seasonPoints)

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
            triggeredFever = triggeredFever,
            seasonPoints = seasonPoints
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

        repository.clearCell(cellIndex)

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

        val newBooster = ActiveBooster(
            type = BoosterType.ENERGY_FREEZE,
            expiresAt = System.currentTimeMillis() + 60000
        )

        val currentBoosters = mutableListOf<ActiveBooster>()
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

        val canFreeSpin = player.freeSpinsAvailable > 0 ||
                (now - player.lastWheelSpinTime > dayMs)

        if (!canFreeSpin && player.gems < 10) {
            return WheelSpinResult.NotEnoughGems
        }

        if (!canFreeSpin) {
            repository.updateGems(player.gems - 10)
        }

        repository.updateWheelSpin(now, if (canFreeSpin) 0 else player.freeSpinsAvailable)
        repository.incrementWheelSpins()

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
            WheelPrize(WheelPrizeType.PRESTIGE_POINTS, 5, 3),
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
                val boosterType = BoosterType.entries.random()
                val newBooster = ActiveBooster(boosterType, System.currentTimeMillis() + boosterType.durationMs)
                repository.updateActiveBoosters(listOf(newBooster))
            }
            WheelPrizeType.PRESTIGE_POINTS -> repository.addPrestigePoints(selectedPrize.amount)
            else -> {}
        }

        return WheelSpinResult.Success(selectedPrize)
    }

    suspend fun calculateOfflineRewards(): OfflineRewardResult {
        val player = repository.getPlayerSync() ?: return OfflineRewardResult(0, 0, 0)

        val now = System.currentTimeMillis()
        val offlineTime = now - player.lastOnlineTime
        val offlineMinutes = (offlineTime / 60000).toInt()

        if (offlineMinutes < 5) {
            repository.updateLastOnlineTime(now)
            return OfflineRewardResult(0, 0, 0)
        }

        val cappedMinutes = offlineMinutes.coerceAtMost(480)

        val coins = (cappedMinutes * 2 * player.coinMultiplier).toLong()
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

    suspend fun purchaseBooster(type: BoosterType, useGems: Boolean): BoosterPurchaseResult {
        val player = repository.getPlayerSync() ?: return BoosterPurchaseResult.Error("Player not found")

        val cost = if (useGems) type.gemCost else type.coinCost.toInt()
        val hasEnough = if (useGems) player.gems >= cost else player.coins >= cost

        if (!hasEnough) {
            return if (useGems) BoosterPurchaseResult.NotEnoughGems else BoosterPurchaseResult.NotEnoughCoins
        }

        if (useGems) {
            repository.updateGems(player.gems - cost)
        } else {
            repository.updateCoins(player.coins - cost)
        }

        val newBooster = ActiveBooster(type, System.currentTimeMillis() + type.durationMs)
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
        val maxEnergy = player.maxEnergy + player.maxEnergyBonus
        if (player.energy >= maxEnergy) return 0

        if (player.activeBoostersJson.contains("ENERGY_FREEZE")) return 0

        val now = System.currentTimeMillis()
        val regenTime = (GameState.ENERGY_REGEN_TIME_MS * player.energyRegenBonus).toLong()
        val timePassed = now - player.lastEnergyRegenTime
        val energyToRegen = (timePassed / regenTime).toInt()

        if (energyToRegen > 0) {
            val newEnergy = (player.energy + energyToRegen).coerceAtMost(maxEnergy)
            val newRegenTime = player.lastEnergyRegenTime + (energyToRegen * regenTime)
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

        val coinsReward = (100L * newStreak * player.coinMultiplier).toLong()
        val energyReward = 10 * newStreak
        val gemsReward = if (newStreak >= 7) 20 else newStreak

        repository.updateDailyReward(newStreak, now)
        repository.updateCoins(player.coins + coinsReward)
        repository.updateGems(player.gems + gemsReward)

        val maxEnergy = player.maxEnergy + player.maxEnergyBonus
        val newEnergy = (player.energy + energyReward).coerceAtMost(maxEnergy)
        repository.updateEnergy(newEnergy, player.lastEnergyRegenTime)

        // Reset calendar claimed status
        repository.updateCalendar(player.calendarDay, false)

        return DailyRewardResult.Success(
            streak = newStreak,
            coinsEarned = coinsReward,
            energyEarned = energyReward,
            gemsEarned = gemsReward
        )
    }

    private fun generateGift(playerLevel: Int, luckySpawn: Boolean, luckyBonus: Float): Triple<Int, Boolean, SpecialGiftType?> {
        val maxTier = ((playerLevel - 1) / 5).coerceIn(0, 5)

        val specialChance = (if (luckySpawn) 0.15f else 0.05f) + luckyBonus
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
        // Quest progress implementation
    }

    private fun getAllMilestones(): List<Milestone> {
        return listOf(
            Milestone("level_5", "Rising Star", "Reach level 5", "⭐", MilestoneRequirement.Level(5), MilestoneReward.Coins(500)),
            Milestone("level_10", "Gift Hunter", "Reach level 10", "🎯", MilestoneRequirement.Level(10), MilestoneReward.Gems(20)),
            Milestone("level_20", "Gift Master", "Reach level 20", "🏆", MilestoneRequirement.Level(20), MilestoneReward.PrestigePoints(10)),
            Milestone("level_30", "Gift Legend", "Reach level 30", "👑", MilestoneRequirement.Level(30), MilestoneReward.Gems(100)),
            Milestone("merges_100", "Merger I", "Perform 100 merges", "🔄", MilestoneRequirement.Merges(100), MilestoneReward.Coins(300)),
            Milestone("merges_500", "Merger II", "Perform 500 merges", "🔄", MilestoneRequirement.Merges(500), MilestoneReward.Gems(15)),
            Milestone("merges_1000", "Merger III", "Perform 1000 merges", "🔄", MilestoneRequirement.Merges(1000), MilestoneReward.PrestigePoints(20)),
            Milestone("gift_5", "Collector I", "Create a level 5 gift", "🎁", MilestoneRequirement.HighestGift(5), MilestoneReward.Energy(50)),
            Milestone("gift_8", "Collector II", "Create a level 8 gift", "🎁", MilestoneRequirement.HighestGift(8), MilestoneReward.Gems(30)),
            Milestone("gift_10", "Collector III", "Create a level 10 gift", "🎁", MilestoneRequirement.HighestGift(10), MilestoneReward.PrestigePoints(50)),
            Milestone("combo_5", "Combo Starter", "Reach 5x combo", "🔥", MilestoneRequirement.Combo(5), MilestoneReward.Coins(200)),
            Milestone("combo_10", "Combo Master", "Reach 10x combo", "🔥", MilestoneRequirement.Combo(10), MilestoneReward.Gems(25)),
            Milestone("fever_5", "Fever Fan", "Trigger fever 5 times", "🌡️", MilestoneRequirement.FeverCount(5), MilestoneReward.Energy(30)),
            Milestone("fever_20", "Fever Master", "Trigger fever 20 times", "🌡️", MilestoneRequirement.FeverCount(20), MilestoneReward.PrestigePoints(15)),
            Milestone("prestige_1", "Reborn", "Prestige once", "♻️", MilestoneRequirement.Prestige(1), MilestoneReward.Gems(50)),
            Milestone("prestige_5", "Veteran", "Prestige 5 times", "♻️", MilestoneRequirement.Prestige(5), MilestoneReward.PrestigePoints(100)),
            Milestone("streak_7", "Dedicated", "7-day login streak", "📅", MilestoneRequirement.DailyStreak(7), MilestoneReward.Gems(35)),
            Milestone("collection_6", "Half Way", "Unlock 6 gift types", "📦", MilestoneRequirement.Collection(6), MilestoneReward.Coins(1000)),
            Milestone("collection_12", "Complete!", "Unlock all 12 gift types", "📦", MilestoneRequirement.Collection(12), MilestoneReward.PrestigePoints(100))
        )
    }

    private fun getCalendarDay(day: Int): CalendarDay {
        val rewards = listOf(
            CalendarReward.Coins(100),
            CalendarReward.Energy(20),
            CalendarReward.Coins(200),
            CalendarReward.Gems(5),
            CalendarReward.Energy(30),
            CalendarReward.Coins(300),
            CalendarReward.Multiple(listOf(CalendarReward.Coins(500), CalendarReward.Gems(10))), // Day 7
            CalendarReward.Energy(25),
            CalendarReward.Coins(400),
            CalendarReward.Gems(10),
            CalendarReward.Energy(35),
            CalendarReward.Coins(500),
            CalendarReward.Gems(15),
            CalendarReward.Multiple(listOf(CalendarReward.Coins(1000), CalendarReward.Gems(20), CalendarReward.Energy(50))), // Day 14
            CalendarReward.Coins(600),
            CalendarReward.Energy(40),
            CalendarReward.Gems(15),
            CalendarReward.Coins(700),
            CalendarReward.Energy(45),
            CalendarReward.Gems(20),
            CalendarReward.Multiple(listOf(CalendarReward.Coins(1500), CalendarReward.Gems(30))), // Day 21
            CalendarReward.Coins(800),
            CalendarReward.Energy(50),
            CalendarReward.Gems(25),
            CalendarReward.Coins(1000),
            CalendarReward.Energy(60),
            CalendarReward.Gems(30),
            CalendarReward.Multiple(listOf(CalendarReward.Coins(2000), CalendarReward.Gems(50), CalendarReward.Energy(100))), // Day 28
            CalendarReward.PrestigePoints(10),
            CalendarReward.Multiple(listOf(CalendarReward.Coins(5000), CalendarReward.Gems(100), CalendarReward.PrestigePoints(25))) // Day 30
        )

        val index = (day - 1).coerceIn(0, rewards.size - 1)
        val isSpecial = day % 7 == 0 || day == 30

        return CalendarDay(day = day, reward = rewards[index], isSpecial = isSpecial)
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
        val triggeredFever: Boolean = false,
        val seasonPoints: Int = 0
    ) : MergeResult()
    data object CannotMerge : MergeResult()
    data object MaxLevelReached : MergeResult()
    data class Error(val message: String) : MergeResult()
}

sealed class SellResult {
    data class Success(
        val coinsGained: Long,
        val giftLevel: Int,
        val spawnedNew: SpawnResult.Success?
    ) : SellResult()
    data class Error(val message: String) : SellResult()
}

sealed class PrestigeResult {
    data class Success(
        val newPrestigeLevel: Int,
        val pointsEarned: Int,
        val totalPoints: Int
    ) : PrestigeResult()
    data class NotEligible(
        val currentLevel: Int,
        val requiredLevel: Int
    ) : PrestigeResult()
    data class Error(val message: String) : PrestigeResult()
}

sealed class PrestigeUpgradeResult {
    data class Success(
        val upgrade: PrestigeUpgrade,
        val newLevel: Int,
        val pointsSpent: Int
    ) : PrestigeUpgradeResult()
    data object MaxLevel : PrestigeUpgradeResult()
    data class NotEnoughPoints(val required: Int, val current: Int) : PrestigeUpgradeResult()
    data class Error(val message: String) : PrestigeUpgradeResult()
}

sealed class MilestoneClaimResult {
    data class Success(val milestone: Milestone) : MilestoneClaimResult()
    data object AlreadyClaimed : MilestoneClaimResult()
    data class Error(val message: String) : MilestoneClaimResult()
}

sealed class CalendarClaimResult {
    data class Success(val day: CalendarDay) : CalendarClaimResult()
    data object AlreadyClaimed : CalendarClaimResult()
    data class Error(val message: String) : CalendarClaimResult()
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
