package com.giftfest.game.data.dao

import androidx.room.*
import com.giftfest.game.data.entity.PlayerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerDao {
    @Query("SELECT * FROM player WHERE id = 1")
    fun getPlayer(): Flow<PlayerEntity?>

    @Query("SELECT * FROM player WHERE id = 1")
    suspend fun getPlayerSync(): PlayerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayer(player: PlayerEntity)

    @Update
    suspend fun updatePlayer(player: PlayerEntity)

    @Query("UPDATE player SET energy = :energy, lastEnergyRegenTime = :lastRegenTime WHERE id = 1")
    suspend fun updateEnergy(energy: Int, lastRegenTime: Long)

    @Query("UPDATE player SET experience = :exp, level = :level WHERE id = 1")
    suspend fun updateExperience(exp: Long, level: Int)

    @Query("UPDATE player SET coins = :coins WHERE id = 1")
    suspend fun updateCoins(coins: Long)

    @Query("UPDATE player SET totalMerges = totalMerges + 1, highestGiftLevel = MAX(highestGiftLevel, :giftLevel) WHERE id = 1")
    suspend fun incrementMerges(giftLevel: Int)

    @Query("UPDATE player SET dailyStreak = :streak, lastDailyRewardTime = :time WHERE id = 1")
    suspend fun updateDailyReward(streak: Int, time: Long)

    @Query("UPDATE player SET unlockedGiftsJson = :json WHERE id = 1")
    suspend fun updateUnlockedGifts(json: String)

    @Query("UPDATE player SET achievementsJson = :json WHERE id = 1")
    suspend fun updateAchievements(json: String)
}
