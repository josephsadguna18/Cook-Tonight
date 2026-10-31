package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [FavoriteRecipeEntity::class], version = 1, exportSchema = false)
abstract class CookTonightDatabase : RoomDatabase() {

    abstract fun favoriteRecipeDao(): FavoriteRecipeDao

    companion object {
        @Volatile
        private var INSTANCE: CookTonightDatabase? = null

        fun getDatabase(context: Context): CookTonightDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CookTonightDatabase::class.java,
                    "cook_tonight_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
