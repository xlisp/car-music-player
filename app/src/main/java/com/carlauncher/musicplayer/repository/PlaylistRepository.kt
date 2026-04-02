package com.carlauncher.musicplayer.repository

import android.content.Context
import com.carlauncher.musicplayer.db.AppDatabase
import com.carlauncher.musicplayer.db.PlaylistEntity
import com.carlauncher.musicplayer.db.PlaylistSongEntity
import com.carlauncher.musicplayer.model.Song

class PlaylistRepository(context: Context) {

    private val dao = AppDatabase.getInstance(context).playlistDao()

    data class PlaylistWithCount(
        val id: Long,
        val name: String,
        val songCount: Int,
        val updatedAt: Long
    )

    suspend fun getAllPlaylists(): List<PlaylistWithCount> {
        val playlists = dao.getAllPlaylists()
        return playlists.map { playlist ->
            PlaylistWithCount(
                id = playlist.id,
                name = playlist.name,
                songCount = dao.getSongCount(playlist.id),
                updatedAt = playlist.updatedAt
            )
        }
    }

    suspend fun createPlaylist(name: String): Long {
        return dao.insertPlaylist(PlaylistEntity(name = name))
    }

    suspend fun renamePlaylist(id: Long, name: String) {
        dao.renamePlaylist(id, name)
    }

    suspend fun deletePlaylist(id: Long) {
        dao.deletePlaylist(id)
    }

    suspend fun getPlaylistSongs(playlistId: Long): List<PlaylistSongEntity> {
        return dao.getSongsForPlaylist(playlistId)
    }

    suspend fun addSongToPlaylist(playlistId: Long, song: Song) {
        dao.addSongToPlaylist(
            PlaylistSongEntity(
                playlistId = playlistId,
                songPath = song.path,
                songTitle = song.title,
                songArtist = song.artist
            )
        )
        dao.touchPlaylist(playlistId)
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songPath: String) {
        dao.removeSongFromPlaylist(playlistId, songPath)
        dao.touchPlaylist(playlistId)
    }
}
