package com.carlauncher.musicplayer.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.carlauncher.musicplayer.R
import com.carlauncher.musicplayer.adapter.SongAdapter
import com.carlauncher.musicplayer.viewmodel.MusicViewModel

class PlaylistSongsFragment : Fragment() {

    companion object {
        private const val ARG_PLAYLIST_ID = "playlist_id"
        private const val ARG_PLAYLIST_NAME = "playlist_name"

        fun newInstance(playlistId: Long, playlistName: String): PlaylistSongsFragment {
            return PlaylistSongsFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_PLAYLIST_ID, playlistId)
                    putString(ARG_PLAYLIST_NAME, playlistName)
                }
            }
        }
    }

    private val viewModel: MusicViewModel by activityViewModels()
    private lateinit var songAdapter: SongAdapter
    private var playlistId: Long = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_artist_songs, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        playlistId = arguments?.getLong(ARG_PLAYLIST_ID, 0) ?: 0
        val name = arguments?.getString(ARG_PLAYLIST_NAME) ?: return

        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvSubtitle: TextView = view.findViewById(R.id.tvSubtitle)
        val btnBack: ImageButton = view.findViewById(R.id.btnBack)
        val btnPlayAll: TextView = view.findViewById(R.id.btnPlayAll)
        val rvSongs: RecyclerView = view.findViewById(R.id.rvSongs)

        tvTitle.text = name

        songAdapter = SongAdapter { _, position ->
            val songs = viewModel.playlistSongs.value ?: return@SongAdapter
            viewModel.onPlayRequest?.invoke(songs, position)
        }

        rvSongs.layoutManager = LinearLayoutManager(context)
        rvSongs.adapter = songAdapter

        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnPlayAll.setOnClickListener {
            viewModel.playPlaylistSongs()
        }

        // Long press to remove song from playlist
        songAdapter.onSongLongClick = { song ->
            AlertDialog.Builder(requireContext(), R.style.Theme_CarMusicPlayer)
                .setTitle(song.title)
                .setItems(arrayOf(getString(R.string.remove_from_playlist))) { _, _ ->
                    viewModel.removeSongFromPlaylist(playlistId, song.path)
                    Toast.makeText(context, "已移除", Toast.LENGTH_SHORT).show()
                }
                .show()
        }

        viewModel.loadPlaylistSongs(playlistId)

        viewModel.playlistSongs.observe(viewLifecycleOwner) { songs ->
            songAdapter.submitList(songs)
            tvSubtitle.text = "${songs.size} 首歌曲"
        }

        viewModel.currentPlayingSong.observe(viewLifecycleOwner) { song ->
            songAdapter.currentPlayingSongId = song?.id ?: -1
        }
    }
}
