package com.example.database

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class MinaRepository(
    private val playlistDao: PlaylistDao,
    private val mediaTrackDao: MediaTrackDao
) {
    val allPlaylists: Flow<List<DbPlaylist>> = playlistDao.getAllPlaylists()
    val allTracks: Flow<List<DbMediaTrack>> = mediaTrackDao.getAllTracks()
    val allAudioTracks: Flow<List<DbMediaTrack>> = mediaTrackDao.getAllAudioTracks()
    val allVideoTracks: Flow<List<DbMediaTrack>> = mediaTrackDao.getAllVideoTracks()

    fun getTracksForPlaylist(playlistName: String): Flow<List<DbMediaTrack>> {
        return mediaTrackDao.getTracksForPlaylist(playlistName)
    }

    suspend fun insertPlaylist(playlist: DbPlaylist) {
        playlistDao.insertPlaylist(playlist)
    }

    suspend fun updatePlaylist(playlist: DbPlaylist) {
        playlistDao.updatePlaylist(playlist)
    }

    suspend fun deletePlaylist(playlist: DbPlaylist) {
        playlistDao.deletePlaylist(playlist)
        // Also clean up associated tracks
        mediaTrackDao.deleteTracksForPlaylist(playlist.name)
    }

    suspend fun insertTrack(track: DbMediaTrack) {
        mediaTrackDao.insertTrack(track)
    }

    suspend fun deleteTrack(track: DbMediaTrack) {
        mediaTrackDao.deleteTrack(track)
    }

    // Keep database completely empty on first launch and delete existing samples
    suspend fun prepopulateDatabaseIfEmpty() {
        mediaTrackDao.deleteSampleTracks()
    }

    suspend fun clearAllPlaylistsAndTracks() {
        mediaTrackDao.deleteAllTracks()
        playlistDao.deleteAllPlaylists()
    }
}
