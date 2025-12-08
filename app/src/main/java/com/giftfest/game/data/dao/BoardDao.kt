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

    @Query("UPDATE board_cells SET giftTypeIndex = :typeIndex, giftLevel = :level WHERE `index` = :cellIndex")
    suspend fun updateCellGift(cellIndex: Int, typeIndex: Int?, level: Int?)

    @Query("UPDATE board_cells SET giftTypeIndex = NULL, giftLevel = NULL WHERE `index` = :cellIndex")
    suspend fun clearCell(cellIndex: Int)

    @Query("UPDATE board_cells SET isLocked = :isLocked WHERE `index` = :cellIndex")
    suspend fun updateCellLock(cellIndex: Int, isLocked: Boolean)

    @Query("DELETE FROM board_cells")
    suspend fun clearAllCells()

    @Query("SELECT COUNT(*) FROM board_cells WHERE giftTypeIndex IS NULL AND isLocked = 0")
    suspend fun getEmptyCellCount(): Int

    @Query("SELECT * FROM board_cells WHERE giftTypeIndex IS NULL AND isLocked = 0 ORDER BY `index` ASC LIMIT 1")
    suspend fun getFirstEmptyCell(): BoardCellEntity?
}
