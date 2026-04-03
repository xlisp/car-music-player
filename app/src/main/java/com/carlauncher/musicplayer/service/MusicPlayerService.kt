package com.carlauncher.musicplayer.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Binder
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.NotificationCompat
import com.carlauncher.musicplayer.MainActivity
import com.carlauncher.musicplayer.R
import com.carlauncher.musicplayer.model.PlayMode
import com.carlauncher.musicplayer.model.Song
import com.carlauncher.musicplayer.recommendation.RecommendationEngine
import com.carlauncher.musicplayer.repository.PlayHistoryManager

class MusicPlayerService : Service(), MediaPlayer.OnCompletionListener,
    MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener {

    companion object {
        const val CHANNEL_ID = "car_music_channel"
        const val NOTIFICATION_ID = 1
        const val ACTION_PLAY_PAUSE = "com.carlauncher.musicplayer.PLAY_PAUSE"
        const val ACTION_NEXT = "com.carlauncher.musicplayer.NEXT"
        const val ACTION_PREV = "com.carlauncher.musicplayer.PREV"
    }

    private val binder = MusicBinder()
    private var mediaPlayer: MediaPlayer? = null
    private var mediaSession: MediaSessionCompat? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null

    // 播放状态
    private var playlist = mutableListOf<Song>()
    private var currentIndex = -1
    private var playMode = PlayMode.SEQUENTIAL
    private var isPrepared = false

    // 推荐引擎
    private var recommendationEngine: RecommendationEngine? = null
    private var allSongsForRecommend: List<Song> = emptyList()

    // 播放历史管理器
    private lateinit var playHistoryManager: PlayHistoryManager

    // 回调
    var onPlayStateChanged: ((Boolean) -> Unit)? = null
    var onSongChanged: ((Song?) -> Unit)? = null
    var onProgressUpdate: ((Int, Int) -> Unit)? = null
    var onPlayHistoryUpdated: (() -> Unit)? = null

    val currentSong: Song?
        get() = if (currentIndex in playlist.indices) playlist[currentIndex] else null

    fun getPlayHistoryManager(): PlayHistoryManager = playHistoryManager

    val isPlaying: Boolean
        get() = mediaPlayer?.isPlaying == true

    val currentPosition: Int
        get() = if (isPrepared) mediaPlayer?.currentPosition ?: 0 else 0

    val totalDuration: Int
        get() = if (isPrepared) mediaPlayer?.duration ?: 0 else 0

    inner class MusicBinder : Binder() {
        fun getService(): MusicPlayerService = this@MusicPlayerService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        playHistoryManager = PlayHistoryManager(applicationContext)
        createNotificationChannel()
        setupMediaSession()
        setupAudioFocus()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> togglePlayPause()
            ACTION_NEXT -> playNext()
            ACTION_PREV -> playPrevious()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        mediaPlayer?.release()
        mediaPlayer = null
        mediaSession?.release()
        abandonAudioFocus()
        super.onDestroy()
    }

    // ========== 播放控制 ==========

    fun setPlaylist(songs: List<Song>, startIndex: Int = 0) {
        playlist.clear()
        playlist.addAll(songs)
        if (startIndex in playlist.indices) {
            currentIndex = startIndex
            playCurrent()
        }
    }

    fun setRecommendationData(allSongs: List<Song>) {
        allSongsForRecommend = allSongs
        recommendationEngine = RecommendationEngine(
            allSongs,
            playHistoryManager.getArtistPlayCounts()
        )
    }

    fun play(song: Song) {
        val index = playlist.indexOf(song)
        if (index >= 0) {
            currentIndex = index
            playCurrent()
        } else {
            playlist.add(song)
            currentIndex = playlist.size - 1
            playCurrent()
        }
    }

    fun togglePlayPause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
            } else {
                if (requestAudioFocus()) {
                    it.start()
                }
            }
            onPlayStateChanged?.invoke(it.isPlaying)
            updateNotification()
        }
    }

    fun playNext() {
        if (playlist.isEmpty()) return

        when (playMode) {
            PlayMode.SEQUENTIAL -> {
                currentIndex++
                if (currentIndex >= playlist.size) {
                    currentIndex = 0
                }
            }
            PlayMode.LOOP_ALL -> {
                currentIndex = (currentIndex + 1) % playlist.size
            }
            PlayMode.LOOP_SINGLE -> {
                // 保持当前index
            }
            PlayMode.SHUFFLE -> {
                currentIndex = (0 until playlist.size).random()
            }
            PlayMode.SMART_RECOMMEND -> {
                val nextSong = recommendationEngine?.getNextRecommendation(currentSong)
                if (nextSong != null) {
                    val idx = playlist.indexOf(nextSong)
                    if (idx >= 0) {
                        currentIndex = idx
                    } else {
                        playlist.add(nextSong)
                        currentIndex = playlist.size - 1
                    }
                } else {
                    currentIndex = (currentIndex + 1) % playlist.size
                }
            }
        }
        playCurrent()
    }

    fun playPrevious() {
        if (playlist.isEmpty()) return

        // 如果播放超过3秒，重新播放当前歌曲
        if (currentPosition > 3000) {
            seekTo(0)
            return
        }

        currentIndex--
        if (currentIndex < 0) currentIndex = playlist.size - 1
        playCurrent()
    }

    fun seekTo(position: Int) {
        if (isPrepared) {
            mediaPlayer?.seekTo(position)
        }
    }

    fun setPlayMode(mode: PlayMode) {
        playMode = mode
    }

    fun getPlayMode(): PlayMode = playMode

    private fun playCurrent() {
        if (currentIndex !in playlist.indices) return

        isPrepared = false
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            setOnCompletionListener(this@MusicPlayerService)
            setOnPreparedListener(this@MusicPlayerService)
            setOnErrorListener(this@MusicPlayerService)
        }

        try {
            val song = playlist[currentIndex]
            mediaPlayer?.setDataSource(applicationContext, song.uri)
            mediaPlayer?.prepareAsync()
            onSongChanged?.invoke(song)
        } catch (e: Exception) {
            e.printStackTrace()
            // 如果当前歌曲无法播放，跳到下一首
            if (playlist.size > 1) {
                currentIndex = (currentIndex + 1) % playlist.size
                playCurrent()
            }
        }
    }

    // ========== MediaPlayer 回调 ==========

    override fun onPrepared(mp: MediaPlayer?) {
        isPrepared = true
        if (requestAudioFocus()) {
            mp?.start()
            onPlayStateChanged?.invoke(true)
            updateNotification()
            // 歌曲开始播放时就记录历史，而不是仅在播完时记录
            currentSong?.let {
                playHistoryManager.recordPlay(it)
                onPlayHistoryUpdated?.invoke()
            }
        }
    }

    override fun onCompletion(mp: MediaPlayer?) {
        // 播放完成时仅更新推荐引擎（播放历史已在onPrepared中记录）
        currentSong?.let {
            recommendationEngine?.recordPlay(it)
        }
        playNext()
    }

    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        isPrepared = false
        // 出错时跳到下一首
        if (playlist.size > 1) {
            currentIndex = (currentIndex + 1) % playlist.size
            playCurrent()
        }
        return true
    }

    // ========== 通知 ==========

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "音乐播放", NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "车载音乐播放通知"
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIntent = PendingIntent.getService(
            this, 1,
            Intent(this, MusicPlayerService::class.java).apply { action = ACTION_PLAY_PAUSE },
            PendingIntent.FLAG_IMMUTABLE
        )
        val nextIntent = PendingIntent.getService(
            this, 2,
            Intent(this, MusicPlayerService::class.java).apply { action = ACTION_NEXT },
            PendingIntent.FLAG_IMMUTABLE
        )
        val prevIntent = PendingIntent.getService(
            this, 3,
            Intent(this, MusicPlayerService::class.java).apply { action = ACTION_PREV },
            PendingIntent.FLAG_IMMUTABLE
        )

        val song = currentSong
        val playIcon = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(song?.title ?: "车载音乐")
            .setContentText(song?.artist ?: "")
            .setSmallIcon(R.drawable.ic_music_note)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_previous, "上一首", prevIntent)
            .addAction(playIcon, "播放/暂停", playPauseIntent)
            .addAction(R.drawable.ic_next, "下一首", nextIntent)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession?.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    fun updateNotification() {
        val notification = buildNotification()
        startForeground(NOTIFICATION_ID, notification)
    }

    // ========== 音频焦点 ==========

    private fun setupAudioFocus() {
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setOnAudioFocusChangeListener { focusChange ->
                when (focusChange) {
                    AudioManager.AUDIOFOCUS_LOSS -> {
                        mediaPlayer?.pause()
                        onPlayStateChanged?.invoke(false)
                    }
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                        mediaPlayer?.pause()
                        onPlayStateChanged?.invoke(false)
                    }
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                        mediaPlayer?.setVolume(0.3f, 0.3f)
                    }
                    AudioManager.AUDIOFOCUS_GAIN -> {
                        mediaPlayer?.setVolume(1f, 1f)
                        mediaPlayer?.start()
                        onPlayStateChanged?.invoke(true)
                    }
                }
            }
            .build()
    }

    private fun requestAudioFocus(): Boolean {
        val result = audioManager?.requestAudioFocus(audioFocusRequest!!)
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun abandonAudioFocus() {
        audioFocusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
    }

    // ========== MediaSession ==========

    private fun setupMediaSession() {
        mediaSession = MediaSessionCompat(this, "CarMusicPlayer").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() { togglePlayPause() }
                override fun onPause() { togglePlayPause() }
                override fun onSkipToNext() { playNext() }
                override fun onSkipToPrevious() { playPrevious() }
                override fun onSeekTo(pos: Long) { seekTo(pos.toInt()) }
            })
            isActive = true
        }
    }
}
