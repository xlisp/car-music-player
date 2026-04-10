package com.carlauncher.musicplayer.repository

import android.content.Context
import android.net.Uri
import com.carlauncher.musicplayer.model.Song
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

/**
 * 歌曲缓存管理器
 * 将扫描结果持久化到文件，避免每次启动都重新扫描
 */
class MusicCacheManager(private val context: Context) {

    private val gson = Gson()
    private val cacheFile = File(context.filesDir, "song_cache.json")

    private data class CachedSong(
        val id: Long,
        val title: String,
        val artist: String,
        val album: String,
        val duration: Long,
        val path: String,
        val genre: String = "",
        val year: Int = 0,
        val dateAdded: Long = 0,
        val size: Long = 0
    )

    fun saveSongs(songs: List<Song>) {
        try {
            val cached = songs.map {
                CachedSong(it.id, it.title, it.artist, it.album, it.duration,
                    it.path, it.genre, it.year, it.dateAdded, it.size)
            }
            cacheFile.writeText(gson.toJson(cached))
        } catch (_: Exception) {}
    }

    fun loadSongs(): List<Song>? {
        if (!cacheFile.exists()) return null
        return try {
            val json = cacheFile.readText()
            val type = object : TypeToken<List<CachedSong>>() {}.type
            val cached: List<CachedSong> = gson.fromJson(json, type)
            cached.map {
                Song(
                    id = it.id,
                    title = it.title,
                    artist = it.artist,
                    album = it.album,
                    duration = it.duration,
                    uri = Uri.fromFile(File(it.path)),
                    path = it.path,
                    genre = it.genre,
                    year = it.year,
                    dateAdded = it.dateAdded,
                    size = it.size
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    fun hasCache(): Boolean = cacheFile.exists()

    fun clearCache() {
        cacheFile.delete()
    }
}
