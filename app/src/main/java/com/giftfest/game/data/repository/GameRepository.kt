package com.giftfest.game.data.repository

import com.giftfest.game.data.dao.BoardDao
import com.giftfest.game.data.dao.PlayerDao
import com.giftfest.game.data.entity.BoardCellEntity
import com.giftfest.game.data.entity.PlayerEntity
import com.giftfest.game.domain.model.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class GameRepository(
    private val playerDao: PlayerDao,
    private val boardDao: BoardDao
) {
    private val gson = Gson()

    fun getGameState(): Flow<GameState> {
        return combine(
            playerDao.getPlayer(),
            boardDao.getAllCells()
        ) { player, cells ->
            if (player == null) {
                GameState()
            } else {
                mapToGameState(player, cells)
            }
        }
    }

    suspend fun initializeGame() {
        val existingPlayer = playerDao.getPlayerSync()
        if (existingPlayer == null) {
            // Create new player with initial quests
            val initialQuests = listOf(
                Quest(
                    id = "quest_merge_5",
                    type = QuestType.MERGE_COUNT,
                    title = "First Steps",
                    description = "Merge 5 gifts",
                    target = 5,
                    reward = QuestReward.Coins(100)
                ),
                Quest(
                    id = "quest_combo_3",
                    type = QuestType.REACH_COMBO,
                    title = "Combo Starter",
                    description = "Reach a 3x combo",
                    target = 3,
                    reward = QuestReward.Energy(10)
                ),
                Quest(
                    id = "quest_level_3",
                    type = QuestType.MERGE_LEVEL,
                    title = "Level Up Gift",
                    description = "Create a level 3 gift",
                    target = 3,
                    reward = QuestReward.Gems(5)
                )
            )

            playerDao.insertPlayer(
                PlayerEntity(
                    activeQuestsJson = gson.toJson(initialQuests)
                )
            )

            // Initialize 5x4 board with special cells and unlock requirements
            val cells = (0 until GameState.BOARD_SIZE).map { index ->
                when (index) {
                    // First 12 cells unlocked
                    in 0..11 -> {
                        val cellType = when (index) {
                            5 -> "GOLDEN"      // Center-ish golden cell
                            9 -> "EXPERIENCE"  // XP bonus cell
                            else -> "NORMAL"
                        }
                        BoardCellEntity(index = index, cellType = cellType)
                    }
                    // Cells 12-14: Unlock with levels
                    12 -> BoardCellEntity(
                        index = index,
                        isLocked = true,
                        unlockType = "level",
                        unlockValue = 5,
                        cellType = "NORMAL"
                    )
                    13 -> BoardCellEntity(
                        index = index,
                        isLocked = true,
                        unlockType = "level",
                        unlockValue = 8,
                        cellType = "MYSTERY"
                    )
                    14 -> BoardCellEntity(
                        index = index,
                        isLocked = true,
                        unlockType = "level",
                        unlockValue = 12,
                        cellType = "GOLDEN"
                    )
                    // Cells 15-17: Unlock with merges
                    15 -> BoardCellEntity(
                        index = index,
                        isLocked = true,
                        unlockType = "merges",
                        unlockValue = 50,
                        cellType = "NORMAL"
                    )
                    16 -> BoardCellEntity(
                        index = index,
                        isLocked = true,
                        unlockType = "merges",
                        unlockValue = 100,
                        cellType = "EXPERIENCE"
                    )
                    17 -> BoardCellEntity(
                        index = index,
                        isLocked = true,
                        unlockType = "coins",
                        unlockValue = 1000,
                        cellType = "NORMAL"
                    )
                    // Last cells: Premium unlock
                    18 -> BoardCellEntity(
                        index = index,
                        isLocked = true,
                        unlockType = "gems",
                        unlockValue = 50,
                        cellType = "GOLDEN"
                    )
                    else -> BoardCellEntity(
                        index = index,
                        isLocked = true,
                        unlockType = "level",
                        unlockValue = 20,
                        cellType = "MYSTERY"
                    )
                }
            }
            boardDao.insertCells(cells)
        }
    }

    // Player operations
    suspend fun getPlayerSync(): PlayerEntity? = playerDao.getPlayerSync()
    suspend fun updatePlayer(player: PlayerEntity) = playerDao.updatePlayer(player)
    suspend fun updateEnergy(energy: Int, lastRegenTime: Long) = playerDao.updateEnergy(energy, lastRegenTime)
    suspend fun updateExperience(exp: Long, level: Int) = playerDao.updateExperience(exp, level)
    suspend fun updateCoins(coins: Long) = playerDao.updateCoins(coins)
    suspend fun updateGems(gems: Int) = playerDao.updateGems(gems)
    suspend fun updateCurrency(coins: Long, gems: Int) = playerDao.updateCurrency(coins, gems)
    suspend fun incrementMerges(giftLevel: Int) = playerDao.incrementMerges(giftLevel)
    suspend fun updateDailyReward(streak: Int, time: Long) = playerDao.updateDailyReward(streak, time)

    // Combo operations
    suspend fun updateCombo(combo: Int, time: Long) = playerDao.updateCombo(combo, time)
    suspend fun resetCombo() = playerDao.resetCombo()

    // Fever operations
    suspend fun updateFever(progress: Float, isActive: Boolean, endTime: Long) =
        playerDao.updateFever(progress, isActive, endTime)
    suspend fun updateFeverProgress(progress: Float) = playerDao.updateFeverProgress(progress)
    suspend fun incrementFeverTriggered() = playerDao.incrementFeverTriggered()

    // Booster operations
    suspend fun updateActiveBoosters(boosters: List<ActiveBooster>) {
        playerDao.updateActiveBoosters(gson.toJson(boosters))
    }
    suspend fun updateOwnedBoosters(boosters: Map<BoosterType, Int>) {
        val map = boosters.mapKeys { it.key.name }
        playerDao.updateOwnedBoosters(gson.toJson(map))
    }

    // Quest operations
    suspend fun updateActiveQuests(quests: List<Quest>) {
        playerDao.updateActiveQuests(gson.toJson(quests))
    }
    suspend fun incrementQuestsCompleted() = playerDao.incrementQuestsCompleted()

    // Statistics
    suspend fun updateStatsOnMerge(coins: Long, combo: Int) = playerDao.updateStatsOnMerge(coins, combo)
    suspend fun updateStatsEnergySpent(energy: Int) = playerDao.updateStatsEnergySpent(energy)
    suspend fun incrementSpecialGiftsUsed() = playerDao.incrementSpecialGiftsUsed()
    suspend fun incrementWheelSpins() = playerDao.incrementWheelSpins()
    suspend fun addPlayTime(minutes: Long) = playerDao.addPlayTime(minutes)

    // Lucky wheel
    suspend fun updateWheelSpin(time: Long, spins: Int) = playerDao.updateWheelSpin(time, spins)
    suspend fun updateLastOnlineTime(time: Long) = playerDao.updateLastOnlineTime(time)

    // Board operations
    suspend fun getBoardSync(): List<BoardCellEntity> = boardDao.getAllCellsSync()

    suspend fun updateCellGift(cellIndex: Int, typeIndex: Int?, level: Int?, isSpecial: Boolean = false, specialType: String? = null) {
        boardDao.updateCellGift(cellIndex, typeIndex, level, isSpecial, specialType)
    }

    suspend fun clearCell(cellIndex: Int) = boardDao.clearCell(cellIndex)
    suspend fun getEmptyCellCount(): Int = boardDao.getEmptyCellCount()
    suspend fun getFirstEmptyCell(): BoardCellEntity? = boardDao.getFirstEmptyCell()
    suspend fun getRandomEmptyCell(): BoardCellEntity? = boardDao.getRandomEmptyCell()
    suspend fun unlockCell(cellIndex: Int) = boardDao.updateCellLock(cellIndex, false)
    suspend fun getCellsWithGifts(): List<BoardCellEntity> = boardDao.getCellsWithGifts()
    suspend fun getCellsWithMatchingGift(typeIndex: Int, level: Int) = boardDao.getCellsWithMatchingGift(typeIndex, level)
    suspend fun getSpecialGiftCells() = boardDao.getSpecialGiftCells()

    suspend fun getAdjacentCells(cellIndex: Int): List<BoardCellEntity> {
        val row = cellIndex / GameState.BOARD_COLUMNS
        val col = cellIndex % GameState.BOARD_COLUMNS
        val adjacentIndices = mutableListOf<Int>()

        // Up, down, left, right, and diagonals
        for (dr in -1..1) {
            for (dc in -1..1) {
                if (dr == 0 && dc == 0) continue
                val newRow = row + dr
                val newCol = col + dc
                if (newRow in 0 until GameState.BOARD_ROWS && newCol in 0 until GameState.BOARD_COLUMNS) {
                    adjacentIndices.add(newRow * GameState.BOARD_COLUMNS + newCol)
                }
            }
        }
        return boardDao.getCellsByIndices(adjacentIndices)
    }

    suspend fun updateUnlockedGifts(unlockedGifts: Set<Int>) {
        val json = gson.toJson(unlockedGifts.toList())
        playerDao.updateUnlockedGifts(json)
    }

    private fun mapToGameState(player: PlayerEntity, cells: List<BoardCellEntity>): GameState {
        val unlockedGifts: Set<Int> = parseJsonSet(player.unlockedGiftsJson)

        val activeBoosters: List<ActiveBooster> = try {
            val type = object : TypeToken<List<Map<String, Any>>>() {}.type
            val list: List<Map<String, Any>> = gson.fromJson(player.activeBoostersJson, type) ?: emptyList()
            list.mapNotNull { map ->
                try {
                    ActiveBooster(
                        type = BoosterType.valueOf(map["type"] as String),
                        expiresAt = (map["expiresAt"] as Number).toLong()
                    )
                } catch (e: Exception) { null }
            }.filter { it.isActive() }
        } catch (e: Exception) { emptyList() }

        val ownedBoosters: Map<BoosterType, Int> = try {
            val type = object : TypeToken<Map<String, Int>>() {}.type
            val map: Map<String, Int> = gson.fromJson(player.ownedBoostersJson, type) ?: emptyMap()
            map.mapNotNull { (key, value) ->
                try { BoosterType.valueOf(key) to value } catch (e: Exception) { null }
            }.toMap()
        } catch (e: Exception) { emptyMap() }

        val activeQuests: List<Quest> = try {
            val type = object : TypeToken<List<Quest>>() {}.type
            gson.fromJson(player.activeQuestsJson, type) ?: emptyList()
        } catch (e: Exception) { emptyList() }

        val boardCells = cells.map { cell ->
            BoardCell(
                index = cell.index,
                gift = if (cell.giftTypeIndex != null && cell.giftLevel != null) {
                    CellGift(
                        typeIndex = cell.giftTypeIndex,
                        level = cell.giftLevel,
                        isSpecial = cell.isSpecialGift,
                        specialType = cell.specialGiftType?.let {
                            try { SpecialGiftType.valueOf(it) } catch (e: Exception) { null }
                        }
                    )
                } else null,
                isLocked = cell.isLocked,
                unlockRequirement = when (cell.unlockType) {
                    "level" -> cell.unlockValue?.let { UnlockRequirement.Level(it) }
                    "friends" -> cell.unlockValue?.let { UnlockRequirement.InviteFriends(it) }
                    "coins" -> cell.unlockValue?.let { UnlockRequirement.Coins(it.toLong()) }
                    "gems" -> cell.unlockValue?.let { UnlockRequirement.Gems(it) }
                    "merges" -> cell.unlockValue?.let { UnlockRequirement.Merges(it) }
                    else -> null
                },
                cellType = try { CellType.valueOf(cell.cellType) } catch (e: Exception) { CellType.NORMAL },
                specialEffect = when (cell.effectType) {
                    "MULTIPLIER" -> cell.effectValue?.let { v ->
                        cell.effectExpiresAt?.let { exp -> CellEffect.Multiplier(v, exp) }
                    }
                    "SHIELD" -> cell.effectExpiresAt?.let { CellEffect.Shield(it) }
                    else -> null
                }
            )
        }

        val statistics = PlayerStatistics(
            totalMerges = player.statsTotalMerges,
            totalCoinsEarned = player.statsTotalCoinsEarned,
            totalGemsEarned = player.statsTotalGemsEarned,
            totalEnergySpent = player.statsTotalEnergySpent,
            highestCombo = player.statsHighestCombo,
            feverModeTriggered = player.statsFeverTriggered,
            specialGiftsUsed = player.statsSpecialGiftsUsed,
            playTimeMinutes = player.statsPlayTimeMinutes,
            gamesPlayed = player.statsGamesPlayed,
            bestMergeStreak = player.statsBestMergeStreak,
            luckyWheelSpins = player.statsLuckyWheelSpins,
            questsCompleted = player.statsQuestsCompleted
        )

        return GameState(
            playerLevel = player.level,
            experience = player.experience,
            experienceToNextLevel = calculateExpToNextLevel(player.level),
            coins = player.coins,
            gems = player.gems,
            energy = player.energy,
            maxEnergy = player.maxEnergy,
            lastEnergyRegenTime = player.lastEnergyRegenTime,
            board = boardCells,
            unlockedGifts = unlockedGifts,
            totalMerges = player.totalMerges,
            highestGiftLevel = player.highestGiftLevel,
            dailyStreak = player.dailyStreak,
            lastDailyRewardTime = player.lastDailyRewardTime,
            lastOnlineTime = player.lastOnlineTime,
            currentCombo = player.currentCombo,
            comboMultiplier = 1f + (player.currentCombo * 0.1f),
            lastMergeTime = player.lastMergeTime,
            feverProgress = player.feverProgress,
            isFeverActive = player.isFeverActive,
            feverEndTime = player.feverEndTime,
            activeBoosters = activeBoosters,
            ownedBoosters = ownedBoosters,
            activeQuests = activeQuests,
            completedQuestsToday = player.completedQuestsToday,
            statistics = statistics,
            lastWheelSpinTime = player.lastWheelSpinTime,
            freeSpinsAvailable = player.freeSpinsAvailable
        )
    }

    private fun parseJsonSet(json: String): Set<Int> {
        return try {
            val type = object : TypeToken<List<Int>>() {}.type
            gson.fromJson<List<Int>>(json, type).toSet()
        } catch (e: Exception) {
            setOf(0, 1, 2)
        }
    }

    private fun calculateExpToNextLevel(level: Int): Long {
        return (100 * level * (1 + level * 0.1)).toLong()
    }
}
