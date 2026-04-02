package com.carlauncher.musicplayer

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.carlauncher.musicplayer.model.PlayMode
import com.carlauncher.musicplayer.service.MusicPlayerService
import com.carlauncher.musicplayer.service.UsbReceiver
import com.carlauncher.musicplayer.ui.SongListFragment
import com.carlauncher.musicplayer.viewmodel.MusicViewModel

class MainActivity : AppCompatActivity() {

    private val viewModel: MusicViewModel by viewModels()
    private var musicService: MusicPlayerService? = null
    private var serviceBound = false
    private val handler = Handler(Looper.getMainLooper())
    private val usbReceiver = UsbReceiver()

    // Player panel views
    private lateinit var ivAlbumArt: ImageView
    private lateinit var tvSongTitle: TextView
    private lateinit var tvArtistName: TextView
    private lateinit var seekBar: SeekBar
    private lateinit var tvCurrentTime: TextView
    private lateinit var tvTotalTime: TextView
    private lateinit var btnPlayPause: ImageButton
    private lateinit var btnPrevious: ImageButton
    private lateinit var btnNext: ImageButton
    private lateinit var btnPlayMode: ImageButton
    private lateinit var btnRefresh: ImageButton
    private lateinit var tvPlayMode: TextView

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicPlayerService.MusicBinder
            musicService = binder.getService()
            serviceBound = true
            setupServiceCallbacks()
            musicService?.setRecommendationData(viewModel.repository.getAllSongs())
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            musicService = null
            serviceBound = false
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            viewModel.scanMusic()
        } else {
            Toast.makeText(this, "需要存储权限才能读取音乐文件", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_main)

        initPlayerViews()
        setupPlayerControls()
        setupViewModel()
        showSongListFragment()
        bindMusicService()
        registerUsbReceiver()
        checkPermissionsAndScan()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        if (serviceBound) {
            unbindService(serviceConnection)
            serviceBound = false
        }
        try {
            unregisterReceiver(usbReceiver)
        } catch (_: Exception) {}
    }

    private fun initPlayerViews() {
        ivAlbumArt = findViewById(R.id.ivAlbumArt)
        tvSongTitle = findViewById(R.id.tvSongTitle)
        tvArtistName = findViewById(R.id.tvArtistName)
        seekBar = findViewById(R.id.seekBar)
        tvCurrentTime = findViewById(R.id.tvCurrentTime)
        tvTotalTime = findViewById(R.id.tvTotalTime)
        btnPlayPause = findViewById(R.id.btnPlayPause)
        btnPrevious = findViewById(R.id.btnPrevious)
        btnNext = findViewById(R.id.btnNext)
        btnPlayMode = findViewById(R.id.btnPlayMode)
        btnRefresh = findViewById(R.id.btnRefresh)
        tvPlayMode = findViewById(R.id.tvPlayMode)

        // 启用跑马灯
        tvSongTitle.isSelected = true
    }

    private fun setupPlayerControls() {
        btnPlayPause.setOnClickListener {
            musicService?.togglePlayPause()
        }

        btnPrevious.setOnClickListener {
            musicService?.playPrevious()
        }

        btnNext.setOnClickListener {
            musicService?.playNext()
        }

        btnPlayMode.setOnClickListener {
            cyclePlayMode()
        }

        btnRefresh.setOnClickListener {
            viewModel.scanMusic()
            Toast.makeText(this, "正在重新扫描…", Toast.LENGTH_SHORT).show()
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && musicService != null) {
                    val duration = musicService!!.totalDuration
                    val position = (progress.toFloat() / 100 * duration).toInt()
                    musicService!!.seekTo(position)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupViewModel() {
        viewModel.onPlayRequest = { songs, startIndex ->
            musicService?.setPlaylist(songs, startIndex)
            musicService?.setRecommendationData(viewModel.repository.getAllSongs())
        }

        viewModel.isPlaying.observe(this) { playing ->
            btnPlayPause.setImageResource(
                if (playing) R.drawable.ic_pause else R.drawable.ic_play
            )
        }

        viewModel.currentPlayingSong.observe(this) { song ->
            if (song != null) {
                tvSongTitle.text = song.title
                tvArtistName.text = song.artist
                tvTotalTime.text = song.durationText
            } else {
                tvSongTitle.text = "未在播放"
                tvArtistName.text = "车载音乐播放器"
                tvTotalTime.text = "0:00"
            }
        }
    }

    private fun setupServiceCallbacks() {
        musicService?.onSongChanged = { song ->
            runOnUiThread {
                viewModel.setCurrentPlayingSong(song)
            }
        }

        musicService?.onPlayStateChanged = { playing ->
            runOnUiThread {
                viewModel.setPlayingState(playing)
                if (playing) startProgressUpdater() else stopProgressUpdater()
            }
        }
    }

    private fun showSongListFragment() {
        if (supportFragmentManager.findFragmentById(R.id.contentFrame) == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.contentFrame, SongListFragment())
                .commit()
        }
    }

    private fun bindMusicService() {
        val intent = Intent(this, MusicPlayerService::class.java)
        startForegroundService(intent)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun registerUsbReceiver() {
        usbReceiver.onUsbMounted = {
            // USB插入，延迟1秒后重新扫描
            handler.postDelayed({
                viewModel.scanMusic()
                Toast.makeText(this, "检测到USB设备，正在扫描…", Toast.LENGTH_SHORT).show()
            }, 1000)
        }
        usbReceiver.onUsbUnmounted = {
            runOnUiThread {
                Toast.makeText(this, "USB设备已移除", Toast.LENGTH_SHORT).show()
                viewModel.scanMusic()
            }
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_MEDIA_MOUNTED)
            addAction(Intent.ACTION_MEDIA_UNMOUNTED)
            addAction(Intent.ACTION_MEDIA_REMOVED)
            addDataScheme("file")
        }
        registerReceiver(usbReceiver, filter)
    }

    private fun checkPermissionsAndScan() {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (permissions.isEmpty()) {
            viewModel.scanMusic()
        } else {
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }

    // ========== 播放模式循环 ==========

    private fun cyclePlayMode() {
        val currentMode = musicService?.getPlayMode() ?: PlayMode.SEQUENTIAL
        val nextMode = when (currentMode) {
            PlayMode.SEQUENTIAL -> PlayMode.LOOP_ALL
            PlayMode.LOOP_ALL -> PlayMode.LOOP_SINGLE
            PlayMode.LOOP_SINGLE -> PlayMode.SHUFFLE
            PlayMode.SHUFFLE -> PlayMode.SMART_RECOMMEND
            PlayMode.SMART_RECOMMEND -> PlayMode.SEQUENTIAL
        }
        musicService?.setPlayMode(nextMode)
        updatePlayModeUI(nextMode)
        Toast.makeText(this, nextMode.displayName, Toast.LENGTH_SHORT).show()
    }

    private fun updatePlayModeUI(mode: PlayMode) {
        val iconRes = when (mode) {
            PlayMode.SEQUENTIAL -> R.drawable.ic_repeat
            PlayMode.LOOP_ALL -> R.drawable.ic_repeat
            PlayMode.LOOP_SINGLE -> R.drawable.ic_repeat_one
            PlayMode.SHUFFLE -> R.drawable.ic_shuffle
            PlayMode.SMART_RECOMMEND -> R.drawable.ic_smart
        }
        btnPlayMode.setImageResource(iconRes)
        tvPlayMode.text = mode.displayName

        // 智能推荐模式高亮
        val tint = if (mode == PlayMode.SMART_RECOMMEND) {
            getColor(R.color.accent)
        } else if (mode == PlayMode.SEQUENTIAL) {
            getColor(R.color.text_secondary)
        } else {
            getColor(R.color.text_primary)
        }
        btnPlayMode.setColorFilter(tint)
    }

    // ========== 进度条更新 ==========

    private val progressRunnable = object : Runnable {
        override fun run() {
            musicService?.let { service ->
                if (service.isPlaying) {
                    val position = service.currentPosition
                    val duration = service.totalDuration
                    if (duration > 0) {
                        seekBar.progress = (position.toFloat() / duration * 100).toInt()
                        tvCurrentTime.text = formatTime(position)
                    }
                }
            }
            handler.postDelayed(this, 500)
        }
    }

    private fun startProgressUpdater() {
        handler.removeCallbacks(progressRunnable)
        handler.post(progressRunnable)
    }

    private fun stopProgressUpdater() {
        handler.removeCallbacks(progressRunnable)
    }

    private fun formatTime(ms: Int): String {
        val minutes = ms / 1000 / 60
        val seconds = ms / 1000 % 60
        return "%d:%02d".format(minutes, seconds)
    }
}
