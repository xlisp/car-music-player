package com.carlauncher.musicplayer.repository

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import android.os.storage.StorageManager
import android.provider.MediaStore
import com.carlauncher.musicplayer.model.Album
import com.carlauncher.musicplayer.model.Artist
import com.carlauncher.musicplayer.model.MusicCategory
import com.carlauncher.musicplayer.model.Song
import java.io.File

class MusicRepository(private val context: Context) {

    private var allSongs = mutableListOf<Song>()

    fun getAllSongs(): List<Song> = allSongs.toList()

    /**
     * 扫描所有存储设备上的音乐文件（包括U盘）
     */
    fun scanMusic(): List<Song> {
        allSongs.clear()

        // 1. 通过MediaStore扫描
        scanFromMediaStore()

        // 2. 直接扫描USB/外部存储路径
        scanExternalStorages()

        // 去重：先按路径去重
        allSongs = allSongs.distinctBy { it.path }.toMutableList()

        // 再按文件大小去重（大小一致视为同一首歌，保留第一个）
        val seenSizes = mutableSetOf<Long>()
        allSongs = allSongs.filter { song ->
            if (song.size <= 0) true
            else seenSizes.add(song.size)
        }.toMutableList()

        return allSongs.toList()
    }

    private fun scanFromMediaStore() {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.SIZE
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} > 30000"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val pathCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val yearCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val path = cursor.getString(pathCol) ?: continue

                if (!File(path).exists()) continue

                val song = Song(
                    id = id,
                    title = cursor.getString(titleCol) ?: File(path).nameWithoutExtension,
                    artist = cursor.getString(artistCol) ?: "未知歌手",
                    album = cursor.getString(albumCol) ?: "未知专辑",
                    duration = cursor.getLong(durationCol),
                    uri = Uri.fromFile(File(path)),
                    path = path,
                    year = cursor.getInt(yearCol),
                    dateAdded = cursor.getLong(dateCol),
                    size = cursor.getLong(sizeCol)
                )
                allSongs.add(song)
            }
        }
    }

    /**
     * 直接扫描外部存储和U盘路径
     */
    private fun scanExternalStorages() {
        val storagePaths = getExternalStoragePaths()
        for (storagePath in storagePaths) {
            val dir = File(storagePath)
            if (dir.exists() && dir.canRead()) {
                scanDirectory(dir)
            }
        }
    }

    private fun getExternalStoragePaths(): List<String> {
        val paths = mutableSetOf<String>()

        // 标准外部存储
        Environment.getExternalStorageDirectory()?.absolutePath?.let { paths.add(it) }

        // 通过StorageManager获取所有挂载的存储设备
        try {
            val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
            val volumes = storageManager.storageVolumes
            for (volume in volumes) {
                try {
                    val getPath = volume.javaClass.getMethod("getPath")
                    val path = getPath.invoke(volume) as? String
                    if (path != null) paths.add(path)
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}

        // 常见USB挂载路径
        val commonUsbPaths = listOf(
            "/storage/usb0", "/storage/usb1", "/storage/usb2",
            "/mnt/usb_storage", "/mnt/usb0", "/mnt/usb1",
            "/mnt/media_rw/usb", "/mnt/media_rw",
            "/storage/UsbDriveA", "/storage/UsbDriveB",
            "/storage/usbdisk", "/storage/usbotg"
        )
        for (usbPath in commonUsbPaths) {
            if (File(usbPath).exists()) {
                paths.add(usbPath)
            }
        }

        // /storage/ 下的所有子目录（可能是U盘）
        File("/storage/").listFiles()?.forEach { file ->
            if (file.isDirectory && file.name != "emulated" && file.name != "self") {
                paths.add(file.absolutePath)
            }
        }

        return paths.toList()
    }

    private val audioExtensions = setOf("mp3", "flac", "wav", "aac", "ogg", "wma", "m4a", "ape")

    private fun scanDirectory(dir: File, depth: Int = 0) {
        if (depth > 8) return // 防止过深递归

        try {
            dir.listFiles()?.forEach { file ->
                if (file.isDirectory && !file.name.startsWith(".")) {
                    scanDirectory(file, depth + 1)
                } else if (file.isFile && file.extension.lowercase() in audioExtensions) {
                    // 检查是否已通过MediaStore扫描到
                    if (allSongs.none { it.path == file.absolutePath }) {
                        extractSongFromFile(file)?.let { allSongs.add(it) }
                    }
                }
            }
        } catch (_: Exception) {}
    }

    private var nextId = 100000L

    private fun extractSongFromFile(file: File): Song? {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(file.absolutePath)

            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?: file.nameWithoutExtension
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?: "未知歌手"
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                ?: "未知专辑"
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            val year = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)
                ?.toIntOrNull() ?: 0
            val genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE) ?: ""

            retriever.release()

            if (duration < 30000) return null // 过滤短音频

            Song(
                id = nextId++,
                title = title,
                artist = artist,
                album = album,
                duration = duration,
                uri = Uri.fromFile(file),
                path = file.absolutePath,
                genre = genre,
                year = year,
                dateAdded = file.lastModified() / 1000,
                size = file.length()
            )
        } catch (e: Exception) {
            null
        }
    }

    // ========== 查询方法 ==========

    fun searchSongs(query: String): List<Song> {
        if (query.isBlank()) return allSongs
        val q = query.lowercase().trim()
        return allSongs.filter {
            it.title.lowercase().contains(q) ||
            it.artist.lowercase().contains(q) ||
            it.album.lowercase().contains(q)
        }
    }

    fun getArtists(): List<Artist> {
        return allSongs
            .groupBy { it.artist }
            .map { (name, songs) -> Artist(name, songs.size, songs) }
            .sortedByDescending { it.songCount }
    }

    fun getSongsByArtist(artistName: String): List<Song> {
        return allSongs.filter { it.artist == artistName }
    }

    fun getCategories(): List<Pair<MusicCategory, Int>> {
        return allSongs
            .groupBy { it.category }
            .map { (cat, songs) -> cat to songs.size }
            .sortedByDescending { it.second }
    }

    fun getSongsByCategory(category: MusicCategory): List<Song> {
        return allSongs.filter { it.category == category }
    }

    fun getAlbums(): List<Album> {
        return allSongs
            .groupBy { it.album }
            .map { (name, songs) -> Album(name, songs.first().artist, songs) }
            .sortedBy { it.name }
    }
}
