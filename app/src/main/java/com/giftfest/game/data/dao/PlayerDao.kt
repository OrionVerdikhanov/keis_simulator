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

    @Query("UPDATE player SET gems = :gems WHERE id = 1")
    suspend fun updateGems(gems: Int)

    @Query("UPDATE player SET coins = :coins, gems = :gems WHERE id = 1")
    suspend fun updateCurrency(coins: Long, gems: Int)

    @Query("UPDATE player SET totalMerges = totalMerges + 1, highestGiftLevel = MAX(highestGiftLevel, :giftLevel) WHERE id = 1")
    suspend fun incrementMerges(giftLevel: Int)

    @Query("UPDATE player SET dailyStreak = :streak, lastDailyRewardTime = :time WHERE id = 1")
    suspend fun updateDailyReward(streak: Int, time: Long)

    @Query("UPDATE player SET unlockedGiftsJson = :json WHERE id = 1")
    suspend fun updateUnlockedGifts(json: String)

    @Query("UPDATE player SET achievementsJson = :json WHERE id = 1")
    suspend fun updateAchievements(json: String)

    // Combo system
    @Query("UPDATE player SET currentCombo = :combo, lastMergeTime = :time WHERE id = 1")
    suspend fun updateCombo(combo: Int, time: Long)

    @Query("UPDATE player SET currentCombo = 0 WHERE id = 1")
    suspend fun resetCombo()

    // Fever mode
    @Query("UPDATE player SET feverProgress = :progress, isFeverActive = :isActive, feverEndTime = :endTime WHERE id = 1")
    suspend fun updateFever(progress: Float, isActive: Boolean, endTime: Long)

    @Query("UPDATE player SET feverProgress = :progress WHERE id = 1")
    suspend fun updateFeverProgress(progress: Float)

    // Boosters
    @Query("UPDATE player SET activeBoostersJson = :json WHERE id = 1")
    suspend fun updateActiveBoosters(json: String)

    @Query("UPDATE player SET ownedBoostersJson = :json WHERE id = 1")
    suspend fun updateOwnedBoosters(json: String)

    // Quests
    @Query("UPDATE player SET activeQuestsJson = :json WHERE id = 1")
    suspend fun updateActiveQuests(json: String)

    @Query("UPDATE player SET completedQuestsToday = :count WHERE id = 1")
    suspend fun updateCompletedQuestsToday(count: Int)

    // Statistics
    @Query("UPDATE player SET statsTotalMerges = statsTotalMerges + 1, statsTotalCoinsEarned = statsTotalCoinsEarned + :coins, statsHighestCombo = MAX(statsHighestCombo, :combo) WHERE id = 1")
    suspend fun updateStatsOnMerge(coins: Long, combo: Int)

    @Query("UPDATE player SET statsTotalEnergySpent = statsTotalEnergySpent + :energy WHERE id = 1")
    suspend fun updateStatsEnergySpent(energy: Int)

    @Query("UPDATE player SET statsFeverTriggered = statsFeverTriggered + 1 WHERE id = 1")
    suspend fun incrementFeverTriggered()

    @Query("UPDATE player SET statsSpecialGiftsUsed = statsSpecialGiftsUsed + 1 WHERE id = 1")
    suspend fun incrementSpecialGiftsUsed()

    @Query("UPDATE player SET statsLuckyWheelSpins = statsLuckyWheelSpins + 1 WHERE id = 1")
    suspend fun incrementWheelSpins()

    @Query("UPDATE player SET statsQuestsCompleted = statsQuestsCompleted + 1 WHERE id = 1")
    suspend fun incrementQuestsCompleted()

    @Query("UPDATE player SET statsPlayTimeMinutes = statsPlayTimeMinutes + :minutes WHERE id = 1")
    suspend fun addPlayTime(minutes: Long)

    // Lucky wheel
    @Query("UPDATE player SET lastWheelSpinTime = :time, freeSpinsAvailable = :spins WHERE id = 1")
    suspend fun updateWheelSpin(time: Long, spins: Int)

    // Online time
    @Query("UPDATE player SET lastOnlineTime = :time WHERE id = 1")
    suspend fun updateLastOnlineTime(time: Long)
}
