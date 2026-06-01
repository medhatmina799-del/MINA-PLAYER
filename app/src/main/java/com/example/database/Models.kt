package com.example.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class DbPlaylist(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val trackCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "media_tracks")
data class DbMediaTrack(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val url: String, // path/uri/web URL
    val type: String, // "AUDIO" or "VIDEO"
    val playlistName: String? = null, // Association to a playlist
    val isSample: Boolean = false, // mark preloaded samples
    val timestamp: Long = System.currentTimeMillis(),
    val thumbnailUrl: String? = null
)
