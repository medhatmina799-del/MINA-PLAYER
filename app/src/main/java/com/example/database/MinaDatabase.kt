package com.example.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [DbPlaylist::class, DbMediaTrack::class], version = 3, exportSchema = false)
abstract class MinaDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun mediaTrackDao(): MediaTrackDao

    companion object {
        @Volatile
        private var INSTANCE: MinaDatabase? = null

        fun getDatabase(context: Context): MinaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MinaDatabase::class.java,
                    "mina_player_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
