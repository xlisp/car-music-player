package com.carlauncher.musicplayer.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.carlauncher.musicplayer.R
import com.carlauncher.musicplayer.adapter.SongAdapter
import com.carlauncher.musicplayer.viewmodel.MusicViewModel

class ArtistSongsFragment : Fragment() {

    companion object {
        private const val ARG_NAME = "name"
        private const val ARG_IS_CATEGORY = "is_category"

        fun newInstance(name: String, isCategory: Boolean = false): ArtistSongsFragment {
            return ArtistSongsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_NAME, name)
                    putBoolean(ARG_IS_CATEGORY, isCategory)
                }
            }
        }
    }

    private val viewModel: MusicViewModel by activityViewModels()
    private lateinit var songAdapter: SongAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_artist_songs, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val name = arguments?.getString(ARG_NAME) ?: return
        val isCategory = arguments?.getBoolean(ARG_IS_CATEGORY, false) ?: false

        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvSubtitle: TextView = view.findViewById(R.id.tvSubtitle)
        val btnBack: ImageButton = view.findViewById(R.id.btnBack)
        val btnPlayAll: TextView = view.findViewById(R.id.btnPlayAll)
        val rvSongs: RecyclerView = view.findViewById(R.id.rvSongs)

        tvTitle.text = name

        songAdapter = SongAdapter { _, position ->
            val songs = if (isCategory) {
                viewModel.categorySongs.value
            } else {
                viewModel.artistSongs.value
            } ?: return@SongAdapter
            viewModel.onPlayRequest?.invoke(songs, position)
        }

        rvSongs.layoutManager = LinearLayoutManager(context)
        rvSongs.adapter = songAdapter

        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // 观察数据
        val songsSource = if (isCategory) viewModel.categorySongs else viewModel.artistSongs

        if (!isCategory) {
            viewModel.showArtistSongs(name)
        }

        songsSource.observe(viewLifecycleOwner) { songs ->
            songAdapter.submitList(songs)
            tvSubtitle.text = "${songs.size} 首歌曲"
        }

        viewModel.currentPlayingSong.observe(viewLifecycleOwner) { song ->
            songAdapter.currentPlayingSongId = song?.id ?: -1
        }

        btnPlayAll.setOnClickListener {
            if (isCategory) {
                viewModel.playCategorySongs()
            } else {
                viewModel.playArtist(name)
            }
        }
    }
}
