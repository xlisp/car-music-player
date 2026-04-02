package com.carlauncher.musicplayer.repository

import android.content.Context
import android.content.SharedPreferences
import com.carlauncher.musicplayer.model.Song
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * 播放历史记录管理器
 * 使用 SharedPreferences 持久化存储播放记录，用于统计播放频率
 */
class PlayHistoryManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("play_history", Context.MODE_PRIVATE)
    private val gson = Gson()

    // 内存缓存
    private var records: MutableMap<String, PlayRecord> = loadRecords()

    data class PlayRecord(
        val path: String,
        val title: String,
        val artist: String,
        val album: String,
        var playCount: Int,
        var lastPlayedTime: Long
    )

    /**
     * 记录一次播放
     */
    fun recordPlay(song: Song) {
        val existing = records[song.path]
        if (existing != null) {
            existing.playCount++
            existing.lastPlayedTime = System.currentTimeMillis()
        } else {
            records[song.path] = PlayRecord(
                path = song.path,
                title = song.title,
                artist = song.artist,
                album = song.album,
                playCount = 1,
                lastPlayedTime = System.currentTimeMillis()
            )
        }
        saveRecords()
    }

    /**
     * 获取播放次数最多的歌曲路径列表（降序）
     */
    fun getMostPlayedPaths(minPlayCount: Int = 2): List<Pair<String, Int>> {
        return records.values
            .filter { it.playCount >= minPlayCount }
            .sortedByDescending { it.playCount }
            .map { it.path to it.playCount }
    }

    /**
     * 获取指定歌曲的播放次数
     */
    fun getPlayCount(path: String): Int {
        return records[path]?.playCount ?: 0
    }

    /**
     * 获取所有播放记录（用于推荐引擎）
     */
    fun getAllRecords(): Map<String, PlayRecord> = records.toMap()

    /**
     * 从已扫描的歌曲列表中筛选出常听歌曲，按播放次数降序
     */
    fun getMostPlayedSongs(allSongs: List<Song>, minPlayCount: Int = 2): List<Song> {
        val pathToCount = records.values
            .filter { it.playCount >= minPlayCount }
            .associate { it.path to it.playCount }

        return allSongs
            .filter { it.path in pathToCount }
            .sortedByDescending { pathToCount[it.path] ?: 0 }
    }

    /**
     * 获取歌手播放统计（用于推荐引擎）
     */
    fun getArtistPlayCounts(): Map<String, Int> {
        val artistCounts = mutableMapOf<String, Int>()
        for (record in records.values) {
            artistCounts[record.artist] = (artistCounts[record.artist] ?: 0) + record.playCount
        }
        return artistCounts
    }

    private fun loadRecords(): MutableMap<String, PlayRecord> {
        val json = prefs.getString("records", null) ?: return mutableMapOf()
        return try {
            val type = object : TypeToken<MutableMap<String, PlayRecord>>() {}.type
            gson.fromJson(json, type) ?: mutableMapOf()
        } catch (e: Exception) {
            mutableMapOf()
        }
    }

    private fun saveRecords() {
        prefs.edit().putString("records", gson.toJson(records)).apply()
    }
}
