package com.carlauncher.musicplayer.model

import android.net.Uri

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,        // milliseconds
    val uri: Uri,
    val path: String,
    val genre: String = "",
    val year: Int = 0,
    val dateAdded: Long = 0,   // timestamp
    val size: Long = 0
) {
    val durationText: String
        get() {
            val minutes = duration / 1000 / 60
            val seconds = duration / 1000 % 60
            return "%d:%02d".format(minutes, seconds)
        }

    val category: MusicCategory
        get() = MusicCategory.fromGenre(genre, year)
}

data class Artist(
    val name: String,
    val songCount: Int,
    val songs: List<Song> = emptyList()
)

data class Album(
    val name: String,
    val artist: String,
    val songs: List<Song> = emptyList()
)

/**
 * 音乐分类 - 用于按类型播放功能
 * 结合流派和年代进行智能分类
 */
enum class MusicCategory(val displayName: String, val keywords: List<String>) {
    CHINESE_POP("华语流行", listOf("mandopop", "c-pop", "chinese", "华语", "流行", "国语")),
    CANTONESE_POP("粤语流行", listOf("cantopop", "cantonese", "粤语")),
    ROCK("摇滚", listOf("rock", "摇滚", "punk", "metal", "alternative")),
    BALLAD("抒情", listOf("ballad", "抒情", "slow", "情歌")),
    ELECTRONIC("电子", listOf("electronic", "edm", "dance", "techno", "house", "电子")),
    RNB_SOUL("R&B/Soul", listOf("r&b", "rnb", "soul", "funk", "节奏布鲁斯")),
    HIP_HOP("说唱/嘻哈", listOf("hip-hop", "hip hop", "rap", "说唱", "嘻哈")),
    JAZZ("爵士", listOf("jazz", "爵士", "blues", "布鲁斯")),
    CLASSICAL("古典", listOf("classical", "古典", "symphony", "orchestra")),
    FOLK("民谣", listOf("folk", "民谣", "acoustic", "民歌")),
    JAPANESE("日韩", listOf("j-pop", "k-pop", "jpop", "kpop", "anime", "日语", "韩语", "日本", "韩国")),
    WESTERN_POP("欧美流行", listOf("pop", "western")),
    NOSTALGIC_2000("00后经典", listOf()),
    NOSTALGIC_90("90年代", listOf()),
    OTHER("其他", listOf());

    companion object {
        fun fromGenre(genre: String, year: Int): MusicCategory {
            if (genre.isBlank() && year == 0) return OTHER
            val lowerGenre = genre.lowercase()

            // 先按流派匹配
            for (cat in entries) {
                if (cat.keywords.any { lowerGenre.contains(it) }) {
                    return cat
                }
            }

            // 再按年代匹配
            return when {
                year in 1990..1999 -> NOSTALGIC_90
                year in 2000..2010 -> NOSTALGIC_2000
                else -> OTHER
            }
        }
    }
}

enum class SortOrder(val displayName: String) {
    TITLE_ASC("歌曲名 A-Z"),
    TITLE_DESC("歌曲名 Z-A"),
    ARTIST_ASC("歌手 A-Z"),
    ARTIST_DESC("歌手 Z-A"),
    DATE_NEWEST("最新添加"),
    DATE_OLDEST("最早添加"),
    DURATION_SHORT("时长 短→长"),
    DURATION_LONG("时长 长→短")
}

enum class PlayMode(val displayName: String) {
    SEQUENTIAL("顺序播放"),
    LOOP_ALL("列表循环"),
    LOOP_SINGLE("单曲循环"),
    SHUFFLE("随机播放"),
    SMART_RECOMMEND("智能推荐")
}
