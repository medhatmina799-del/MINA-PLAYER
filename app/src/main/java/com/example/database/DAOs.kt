package com.example.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY timestamp DESC")
    fun getAllPlaylists(): Flow<List<DbPlaylist>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: DbPlaylist)

    @Update
    suspend fun updatePlaylist(playlist: DbPlaylist)

    @Delete
    suspend fun deletePlaylist(playlist: DbPlaylist)

    @Query("DELETE FROM playlists")
    suspend fun deleteAllPlaylists()
}

@Dao
interface MediaTrackDao {
    @Query("SELECT * FROM media_tracks ORDER BY timestamp DESC")
    fun getAllTracks(): Flow<List<DbMediaTrack>>

    @Query("SELECT * FROM media_tracks WHERE type = 'AUDIO' ORDER BY timestamp DESC")
    fun getAllAudioTracks(): Flow<List<DbMediaTrack>>

    @Query("SELECT * FROM media_tracks WHERE type = 'VIDEO' ORDER BY timestamp DESC")
    fun getAllVideoTracks(): Flow<List<DbMediaTrack>>

    @Query("SELECT * FROM media_tracks WHERE playlistName = :playlistName ORDER BY timestamp DESC")
    fun getTracksForPlaylist(playlistName: String): Flow<List<DbMediaTrack>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: DbMediaTrack)

    @Delete
    suspend fun deleteTrack(track: DbMediaTrack)

    @Query("DELETE FROM media_tracks WHERE isSample = 1")
    suspend fun deleteSampleTracks()

    @Query("DELETE FROM media_tracks")
    suspend fun deleteAllTracks()

    @Query("DELETE FROM media_tracks WHERE playlistName = :playlistName")
    suspend fun deleteTracksForPlaylist(playlistName: String)
}
