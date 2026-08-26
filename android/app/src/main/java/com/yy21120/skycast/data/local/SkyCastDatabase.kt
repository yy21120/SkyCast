package com.yy21120.skycast.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [OpportunityCacheEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class SkyCastDatabase : RoomDatabase() {
    abstract fun opportunityCacheDao(): OpportunityCacheDao

    companion object {
        @Volatile
        private var instance: SkyCastDatabase? = null

        fun getInstance(context: Context): SkyCastDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    SkyCastDatabase::class.java,
                    "skycast.db",
                ).build().also { instance = it }
            }
    }
}
