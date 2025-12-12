package com.giftfest.game.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.giftfest.game.data.dao.BoardDao
import com.giftfest.game.data.dao.PlayerDao
import com.giftfest.game.data.entity.BoardCellEntity
import com.giftfest.game.data.entity.PlayerEntity

@Database(
    entities = [PlayerEntity::class, BoardCellEntity::class],
    version = 2,
    exportSchema = false
)
abstract class GameDatabase : RoomDatabase() {
    abstract fun playerDao(): PlayerDao
    abstract fun boardDao(): BoardDao

    companion object {
        @Volatile
        private var INSTANCE: GameDatabase? = null

        fun getInstance(context: Context): GameDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GameDatabase::class.java,
                    "gift_fest_database_v2"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
