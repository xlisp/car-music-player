package com.carlauncher.musicplayer.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.carlauncher.musicplayer.model.*
import com.carlauncher.musicplayer.repository.MusicRepository
import com.carlauncher.musicplayer.repository.PlayHistoryManager
import com.carlauncher.musicplayer.db.TodoEntity
import com.carlauncher.musicplayer.repository.PlaylistRepository
import com.carlauncher.musicplayer.repository.TodoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    val repository = MusicRepository(application)
    val playHistoryManager = PlayHistoryManager(application)
    val playlistRepository = PlaylistRepository(application)
    val todoRepository = TodoRepository(application)

    // 所有歌曲
    private val _allSongs = MutableLiveData<List<Song>>(emptyList())

    // 显示的歌曲（经过搜索、排序后）
    private val _displayedSongs = MutableLiveData<List<Song>>(emptyList())
    val displayedSongs: LiveData<List<Song>> = _displayedSongs

    // 歌手列表
    private val _artists = MutableLiveData<List<Artist>>(emptyList())
    val artists: LiveData<List<Artist>> = _artists

    // 分类列表
    private val _categories = MutableLiveData<List<Pair<MusicCategory, Int>>>(emptyList())
    val categories: LiveData<List<Pair<MusicCategory, Int>>> = _categories

    // 歌手歌曲
    private val _artistSongs = MutableLiveData<List<Song>>(emptyList())
    val artistSongs: LiveData<List<Song>> = _artistSongs

    // 分类歌曲
    private val _categorySongs = MutableLiveData<List<Song>>(emptyList())
    val categorySongs: LiveData<List<Song>> = _categorySongs

    // 常听歌曲（按播放频率排序）
    private val _mostPlayedSongs = MutableLiveData<List<Song>>(emptyList())
    val mostPlayedSongs: LiveData<List<Song>> = _mostPlayedSongs

    // 播放列表
    private val _playlists = MutableLiveData<List<PlaylistRepository.PlaylistWithCount>>(emptyList())
    val playlists: LiveData<List<PlaylistRepository.PlaylistWithCount>> = _playlists

    // 播放列表中的歌曲
    private val _playlistSongs = MutableLiveData<List<Song>>(emptyList())
    val playlistSongs: LiveData<List<Song>> = _playlistSongs

    // TODO 列表
    private val _todos = MutableLiveData<List<TodoEntity>>(emptyList())
    val todos: LiveData<List<TodoEntity>> = _todos

    // 当前播放的歌曲
    private val _currentPlayingSong = MutableLiveData<Song?>()
    val currentPlayingSong: LiveData<Song?> = _currentPlayingSong

    // 播放状态
    private val _isPlaying = MutableLiveData(false)
    val isPlaying: LiveData<Boolean> = _isPlaying

    // 加载状态
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    // 排序
    private val _currentSortOrder = MutableLiveData(SortOrder.DATE_NEWEST)
    val currentSortOrder: LiveData<SortOrder> = _currentSortOrder

    // 搜索关键词
    private var searchQuery = ""

    // 播放回调
    var onPlayRequest: ((List<Song>, Int) -> Unit)? = null
    var onPlayModeChanged: ((PlayMode) -> Unit)? = null

    /**
     * 扫描音乐
     */
    fun scanMusic() {
        _isLoading.value = true
        viewModelScope.launch {
            val songs = withContext(Dispatchers.IO) {
                repository.scanMusic()
            }
            _allSongs.value = songs
            applyFilterAndSort()
            _isLoading.value = false
        }
    }

    /**
     * 搜索
     */
    fun search(query: String) {
        searchQuery = query
        applyFilterAndSort()
    }

    /**
     * 设置排序方式
     */
    fun setSortOrder(order: SortOrder) {
        _currentSortOrder.value = order
        applyFilterAndSort()
    }

    /**
     * 显示所有歌曲
     */
    fun showAllSongs() {
        searchQuery = ""
        applyFilterAndSort()
    }

    /**
     * 加载歌手列表
     */
    fun loadArtists() {
        _artists.value = repository.getArtists()
    }

    /**
     * 加载分类列表
     */
    fun loadCategories() {
        _categories.value = repository.getCategories()
    }

    /**
     * 显示歌手的歌曲
     */
    fun showArtistSongs(artistName: String) {
        _artistSongs.value = repository.getSongsByArtist(artistName)
    }

    /**
     * 显示分类的歌曲
     */
    fun showCategorySongs(category: MusicCategory) {
        _categorySongs.value = repository.getSongsByCategory(category)
    }

    /**
     * 加载常听歌曲（播放次数>=2的歌曲，按播放频率降序）
     */
    fun loadMostPlayed() {
        _mostPlayedSongs.value = playHistoryManager.getMostPlayedSongs(repository.getAllSongs())
    }

    /**
     * 获取歌曲的播放次数
     */
    fun getPlayCount(path: String): Int {
        return playHistoryManager.getPlayCount(path)
    }

    /**
     * 播放常听歌曲列表
     */
    fun playMostPlayed() {
        val songs = _mostPlayedSongs.value ?: return
        if (songs.isEmpty()) return
        onPlayRequest?.invoke(songs, 0)
    }

    /**
     * 播放全部
     */
    fun playAll() {
        val songs = _displayedSongs.value ?: return
        if (songs.isEmpty()) return
        onPlayRequest?.invoke(songs, 0)
    }

    /**
     * 播放指定歌曲
     */
    fun playSong(@Suppress("UNUSED_PARAMETER") song: Song, positionInList: Int) {
        val songs = _displayedSongs.value ?: return
        onPlayRequest?.invoke(songs, positionInList)
    }

    /**
     * 播放歌手全部歌曲
     */
    fun playArtist(artistName: String) {
        val songs = repository.getSongsByArtist(artistName)
        if (songs.isEmpty()) return
        onPlayRequest?.invoke(songs, 0)
    }

    /**
     * 播放分类歌曲
     */
    fun playCategorySongs() {
        val songs = _categorySongs.value ?: return
        if (songs.isEmpty()) return
        onPlayRequest?.invoke(songs, 0)
    }

    /**
     * 更新当前播放歌曲
     */
    fun setCurrentPlayingSong(song: Song?) {
        _currentPlayingSong.value = song
    }

    /**
     * 更新播放状态
     */
    fun setPlayingState(playing: Boolean) {
        _isPlaying.value = playing
    }

    // ========== 播放列表 ==========

    /**
     * 加载所有播放列表
     */
    fun loadPlaylists() {
        viewModelScope.launch {
            _playlists.value = withContext(Dispatchers.IO) {
                playlistRepository.getAllPlaylists()
            }
        }
    }

    /**
     * 创建播放列表
     */
    fun createPlaylist(name: String, onCreated: ((Long) -> Unit)? = null) {
        viewModelScope.launch {
            val id = withContext(Dispatchers.IO) {
                playlistRepository.createPlaylist(name)
            }
            loadPlaylists()
            onCreated?.invoke(id)
        }
    }

    /**
     * 重命名播放列表
     */
    fun renamePlaylist(id: Long, name: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                playlistRepository.renamePlaylist(id, name)
            }
            loadPlaylists()
        }
    }

    /**
     * 删除播放列表
     */
    fun deletePlaylist(id: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                playlistRepository.deletePlaylist(id)
            }
            loadPlaylists()
        }
    }

    /**
     * 加载播放列表中的歌曲（匹配当前扫描到的歌曲）
     */
    fun loadPlaylistSongs(playlistId: Long) {
        viewModelScope.launch {
            val entries = withContext(Dispatchers.IO) {
                playlistRepository.getPlaylistSongs(playlistId)
            }
            val allSongs = repository.getAllSongs()
            val pathToSong = allSongs.associateBy { it.path }
            _playlistSongs.value = entries.mapNotNull { pathToSong[it.songPath] }
        }
    }

    /**
     * 添加歌曲到播放列表
     */
    fun addSongToPlaylist(playlistId: Long, song: Song, onDone: (() -> Unit)? = null) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                playlistRepository.addSongToPlaylist(playlistId, song)
            }
            loadPlaylists()
            onDone?.invoke()
        }
    }

    /**
     * 从播放列表移除歌曲
     */
    fun removeSongFromPlaylist(playlistId: Long, songPath: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                playlistRepository.removeSongFromPlaylist(playlistId, songPath)
            }
            loadPlaylistSongs(playlistId)
            loadPlaylists()
        }
    }

    /**
     * 播放播放列表中所有歌曲
     */
    fun playPlaylistSongs() {
        val songs = _playlistSongs.value ?: return
        if (songs.isEmpty()) return
        onPlayRequest?.invoke(songs, 0)
    }

    // ========== TODO 记事本 ==========

    fun loadTodos() {
        viewModelScope.launch {
            _todos.value = withContext(Dispatchers.IO) {
                todoRepository.getAll()
            }
        }
    }

    fun addTodo(content: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                todoRepository.add(content)
            }
            loadTodos()
        }
    }

    fun updateTodoContent(id: Long, content: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                todoRepository.updateContent(id, content)
            }
            loadTodos()
        }
    }

    fun toggleTodoDone(id: Long, done: Boolean) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                todoRepository.toggleDone(id, done)
            }
            loadTodos()
        }
    }

    fun deleteTodo(id: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                todoRepository.delete(id)
            }
            loadTodos()
        }
    }

    fun clearDoneTodos() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                todoRepository.clearDone()
            }
            loadTodos()
        }
    }

    /**
     * 应用搜索和排序
     */
    private fun applyFilterAndSort() {
        var songs = if (searchQuery.isBlank()) {
            _allSongs.value ?: emptyList()
        } else {
            repository.searchSongs(searchQuery)
        }

        songs = sortSongs(songs, _currentSortOrder.value ?: SortOrder.DATE_NEWEST)
        _displayedSongs.value = songs
    }

    private fun sortSongs(songs: List<Song>, order: SortOrder): List<Song> {
        return when (order) {
            SortOrder.TITLE_ASC -> songs.sortedBy { it.title.lowercase() }
            SortOrder.TITLE_DESC -> songs.sortedByDescending { it.title.lowercase() }
            SortOrder.ARTIST_ASC -> songs.sortedBy { it.artist.lowercase() }
            SortOrder.ARTIST_DESC -> songs.sortedByDescending { it.artist.lowercase() }
            SortOrder.DATE_NEWEST -> songs.sortedByDescending { it.dateAdded }
            SortOrder.DATE_OLDEST -> songs.sortedBy { it.dateAdded }
            SortOrder.DURATION_SHORT -> songs.sortedBy { it.duration }
            SortOrder.DURATION_LONG -> songs.sortedByDescending { it.duration }
        }
    }
}
