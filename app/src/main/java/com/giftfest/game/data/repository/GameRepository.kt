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
import kotlinx.coroutines.flow.map

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
            // Create new player
            playerDao.insertPlayer(PlayerEntity())

            // Initialize board with some locked cells
            val cells = (0 until GameState.BOARD_SIZE).map { index ->
                when {
                    index < 8 -> BoardCellEntity(index = index)
                    index == 8 -> BoardCellEntity(
                        index = index,
                        isLocked = true,
                        unlockType = "level",
                        unlockValue = 5
                    )
                    index == 9 -> BoardCellEntity(
                        index = index,
                        isLocked = true,
                        unlockType = "level",
                        unlockValue = 10
                    )
                    index == 10 -> BoardCellEntity(
                        index = index,
                        isLocked = true,
                        unlockType = "level",
                        unlockValue = 15
                    )
                    else -> BoardCellEntity(
                        index = index,
                        isLocked = true,
                        unlockType = "friends",
                        unlockValue = 15
                    )
                }
            }
            boardDao.insertCells(cells)
        }
    }

    suspend fun getPlayerSync(): PlayerEntity? = playerDao.getPlayerSync()

    suspend fun getBoardSync(): List<BoardCellEntity> = boardDao.getAllCellsSync()

    suspend fun updatePlayer(player: PlayerEntity) {
        playerDao.updatePlayer(player)
    }

    suspend fun updateEnergy(energy: Int, lastRegenTime: Long) {
        playerDao.updateEnergy(energy, lastRegenTime)
    }

    suspend fun updateExperience(exp: Long, level: Int) {
        playerDao.updateExperience(exp, level)
    }

    suspend fun updateCoins(coins: Long) {
        playerDao.updateCoins(coins)
    }

    suspend fun incrementMerges(giftLevel: Int) {
        playerDao.incrementMerges(giftLevel)
    }

    suspend fun updateCellGift(cellIndex: Int, typeIndex: Int?, level: Int?) {
        boardDao.updateCellGift(cellIndex, typeIndex, level)
    }

    suspend fun clearCell(cellIndex: Int) {
        boardDao.clearCell(cellIndex)
    }

    suspend fun getEmptyCellCount(): Int = boardDao.getEmptyCellCount()

    suspend fun getFirstEmptyCell(): BoardCellEntity? = boardDao.getFirstEmptyCell()

    suspend fun unlockCell(cellIndex: Int) {
        boardDao.updateCellLock(cellIndex, false)
    }

    suspend fun updateUnlockedGifts(unlockedGifts: Set<Int>) {
        val json = gson.toJson(unlockedGifts.toList())
        playerDao.updateUnlockedGifts(json)
    }

    suspend fun updateDailyReward(streak: Int, time: Long) {
        playerDao.updateDailyReward(streak, time)
    }

    private fun mapToGameState(player: PlayerEntity, cells: List<BoardCellEntity>): GameState {
        val unlockedGifts: Set<Int> = try {
            val type = object : TypeToken<List<Int>>() {}.type
            gson.fromJson<List<Int>>(player.unlockedGiftsJson, type).toSet()
        } catch (e: Exception) {
            setOf(0, 1)
        }

        val boardCells = cells.map { cell ->
            BoardCell(
                index = cell.index,
                gift = if (cell.giftTypeIndex != null && cell.giftLevel != null) {
                    CellGift(cell.giftTypeIndex, cell.giftLevel)
                } else null,
                isLocked = cell.isLocked,
                unlockRequirement = when (cell.unlockType) {
                    "level" -> cell.unlockValue?.let { UnlockRequirement.Level(it) }
                    "friends" -> cell.unlockValue?.let { UnlockRequirement.InviteFriends(it) }
                    "coins" -> cell.unlockValue?.let { UnlockRequirement.Coins(it.toLong()) }
                    else -> null
                }
            )
        }

        return GameState(
            playerLevel = player.level,
            experience = player.experience,
            experienceToNextLevel = calculateExpToNextLevel(player.level),
            coins = player.coins,
            energy = player.energy,
            maxEnergy = player.maxEnergy,
            lastEnergyRegenTime = player.lastEnergyRegenTime,
            board = boardCells,
            unlockedGifts = unlockedGifts,
            totalMerges = player.totalMerges,
            highestGiftLevel = player.highestGiftLevel,
            dailyStreak = player.dailyStreak,
            lastDailyRewardTime = player.lastDailyRewardTime
        )
    }

    private fun calculateExpToNextLevel(level: Int): Long {
        return (100 * level * (1 + level * 0.1)).toLong()
    }
}
