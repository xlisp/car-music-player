package com.carlauncher.musicplayer.recommendation

import com.carlauncher.musicplayer.model.Song
import kotlin.math.abs

/**
 * 智能推荐引擎
 *
 * 推荐策略：
 * 1. 同歌手的其他歌曲（权重：高）
 * 2. 同流派/分类的歌曲（权重：高）
 * 3. 同年代的歌曲（权重：中）
 * 4. 同专辑的歌曲（权重：中）
 * 5. 最近常听的歌手的歌曲（权重：低）
 *
 * 例如：播放完"十年"(陈奕迅, 2003, 粤语流行) → 推荐"夜曲"(周杰伦, 2005, 华语流行)
 * 因为它们年代相近、都是华语区流行歌曲
 */
class RecommendationEngine(
    private val allSongs: List<Song>,
    initialArtistPlayCounts: Map<String, Int> = emptyMap()
) {

    // 播放历史记录（最近播放的歌曲ID列表）
    private val playHistory = mutableListOf<Long>()
    private val maxHistorySize = 100

    // 歌手播放次数统计（从持久化历史初始化）
    private val artistPlayCount = initialArtistPlayCounts.toMutableMap()

    /**
     * 记录一次播放
     */
    fun recordPlay(song: Song) {
        playHistory.add(song.id)
        if (playHistory.size > maxHistorySize) {
            playHistory.removeAt(0)
        }
        artistPlayCount[song.artist] = (artistPlayCount[song.artist] ?: 0) + 1
    }

    /**
     * 根据当前歌曲推荐下一首
     */
    fun getNextRecommendation(currentSong: Song?): Song? {
        if (currentSong == null || allSongs.size <= 1) return null

        // 计算每首候选歌曲的推荐分数
        val candidates = allSongs
            .filter { it.id != currentSong.id }
            .filter { it.id !in getRecentlyPlayed(5) } // 排除最近5首播放过的
            .map { candidate -> candidate to calculateScore(currentSong, candidate) }
            .sortedByDescending { it.second }

        if (candidates.isEmpty()) return null

        // 从前10名中随机选择，增加一点随机性，避免总是推荐同一首
        val topCandidates = candidates.take(10)
        val weights = topCandidates.map { it.second.coerceAtLeast(1) }
        val totalWeight = weights.sum()

        if (totalWeight <= 0) return topCandidates.random().first

        var random = (1..totalWeight).random()
        for (i in topCandidates.indices) {
            random -= weights[i]
            if (random <= 0) return topCandidates[i].first
        }

        return topCandidates.first().first
    }

    /**
     * 计算两首歌的相似度分数
     */
    private fun calculateScore(current: Song, candidate: Song): Int {
        var score = 0

        // 1. 同歌手 +30分
        if (current.artist == candidate.artist) {
            score += 30
        }

        // 2. 同分类/流派 +25分
        if (current.category == candidate.category) {
            score += 25
        }

        // 3. 年代相近 (5年内+20, 10年内+10)
        if (current.year > 0 && candidate.year > 0) {
            val yearDiff = abs(current.year - candidate.year)
            score += when {
                yearDiff <= 3 -> 20
                yearDiff <= 5 -> 15
                yearDiff <= 10 -> 10
                yearDiff <= 15 -> 5
                else -> 0
            }
        }

        // 4. 同专辑 +15分
        if (current.album == candidate.album && current.album != "未知专辑") {
            score += 15
        }

        // 5. 用户偏好的歌手 +10分
        val artistCount = artistPlayCount[candidate.artist] ?: 0
        if (artistCount > 0) {
            score += (artistCount * 2).coerceAtMost(10)
        }

        // 6. 时长相近（差距在1分钟内）+5分
        val durationDiff = abs(current.duration - candidate.duration)
        if (durationDiff < 60000) {
            score += 5
        }

        // 7. 歌手名相似（如歌手名包含相同关键字）+8分
        if (areArtistsSimilar(current.artist, candidate.artist)) {
            score += 8
        }

        return score
    }

    /**
     * 判断两个歌手是否相似
     * 例如：同一个华语歌手的不同艺名、featuring合作等
     */
    private fun areArtistsSimilar(artist1: String, artist2: String): Boolean {
        if (artist1 == artist2) return false

        // 检查是否有featuring关系
        val featPatterns = listOf("feat.", "ft.", "featuring", "&", "/", "、", "，")
        for (pattern in featPatterns) {
            val artists1 = artist1.split(pattern).map { it.trim() }
            val artists2 = artist2.split(pattern).map { it.trim() }
            if (artists1.any { a1 -> artists2.any { a2 -> a1 == a2 } }) {
                return true
            }
        }
        return false
    }

    /**
     * 获取最近播放的歌曲ID
     */
    private fun getRecentlyPlayed(count: Int): List<Long> {
        return playHistory.takeLast(count)
    }
}
