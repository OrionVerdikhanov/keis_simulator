package com.giftfest.game.data.dao

import androidx.room.*
import com.giftfest.game.data.entity.BoardCellEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BoardDao {
    @Query("SELECT * FROM board_cells ORDER BY `index` ASC")
    fun getAllCells(): Flow<List<BoardCellEntity>>

    @Query("SELECT * FROM board_cells ORDER BY `index` ASC")
    suspend fun getAllCellsSync(): List<BoardCellEntity>

    @Query("SELECT * FROM board_cells WHERE `index` = :index")
    suspend fun getCell(index: Int): BoardCellEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCell(cell: BoardCellEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCells(cells: List<BoardCellEntity>)

    @Update
    suspend fun updateCell(cell: BoardCellEntity)

    @Query("UPDATE board_cells SET giftTypeIndex = :typeIndex, giftLevel = :level, isSpecialGift = :isSpecial, specialGiftType = :specialType WHERE `index` = :cellIndex")
    suspend fun updateCellGift(cellIndex: Int, typeIndex: Int?, level: Int?, isSpecial: Boolean = false, specialType: String? = null)

    @Query("UPDATE board_cells SET giftTypeIndex = NULL, giftLevel = NULL, isSpecialGift = 0, specialGiftType = NULL WHERE `index` = :cellIndex")
    suspend fun clearCell(cellIndex: Int)

    @Query("UPDATE board_cells SET isLocked = :isLocked WHERE `index` = :cellIndex")
    suspend fun updateCellLock(cellIndex: Int, isLocked: Boolean)

    @Query("UPDATE board_cells SET cellType = :cellType WHERE `index` = :cellIndex")
    suspend fun updateCellType(cellIndex: Int, cellType: String)

    @Query("UPDATE board_cells SET effectType = :effectType, effectValue = :effectValue, effectExpiresAt = :expiresAt WHERE `index` = :cellIndex")
    suspend fun updateCellEffect(cellIndex: Int, effectType: String?, effectValue: Float?, expiresAt: Long?)

    @Query("UPDATE board_cells SET effectType = NULL, effectValue = NULL, effectExpiresAt = NULL WHERE `index` = :cellIndex")
    suspend fun clearCellEffect(cellIndex: Int)

    @Query("DELETE FROM board_cells")
    suspend fun clearAllCells()

    @Query("SELECT COUNT(*) FROM board_cells WHERE giftTypeIndex IS NULL AND isLocked = 0 AND cellType != 'FROZEN'")
    suspend fun getEmptyCellCount(): Int

    @Query("SELECT * FROM board_cells WHERE giftTypeIndex IS NULL AND isLocked = 0 AND cellType != 'FROZEN' ORDER BY `index` ASC LIMIT 1")
    suspend fun getFirstEmptyCell(): BoardCellEntity?

    @Query("SELECT * FROM board_cells WHERE giftTypeIndex IS NULL AND isLocked = 0 AND cellType != 'FROZEN' ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomEmptyCell(): BoardCellEntity?

    @Query("SELECT * FROM board_cells WHERE giftTypeIndex = :typeIndex AND giftLevel = :level AND isLocked = 0")
    suspend fun getCellsWithMatchingGift(typeIndex: Int, level: Int): List<BoardCellEntity>

    @Query("SELECT * FROM board_cells WHERE giftTypeIndex IS NOT NULL AND isLocked = 0")
    suspend fun getCellsWithGifts(): List<BoardCellEntity>

    @Query("SELECT * FROM board_cells WHERE cellType = :cellType")
    suspend fun getCellsByType(cellType: String): List<BoardCellEntity>

    @Query("SELECT * FROM board_cells WHERE isSpecialGift = 1")
    suspend fun getSpecialGiftCells(): List<BoardCellEntity>

    // Get adjacent cells for bomb effect
    @Query("SELECT * FROM board_cells WHERE `index` IN (:indices)")
    suspend fun getCellsByIndices(indices: List<Int>): List<BoardCellEntity>
}
