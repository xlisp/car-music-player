package com.carlauncher.musicplayer.ui

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.carlauncher.musicplayer.R
import com.carlauncher.musicplayer.adapter.ArtistAdapter
import com.carlauncher.musicplayer.adapter.CategoryAdapter
import com.carlauncher.musicplayer.adapter.SongAdapter
import com.carlauncher.musicplayer.model.SortOrder
import com.carlauncher.musicplayer.viewmodel.MusicViewModel

class SongListFragment : Fragment() {

    private val viewModel: MusicViewModel by activityViewModels()

    private lateinit var songAdapter: SongAdapter
    private lateinit var artistAdapter: ArtistAdapter
    private lateinit var categoryAdapter: CategoryAdapter

    // Views
    private lateinit var rvSongs: RecyclerView
    private lateinit var emptyView: View
    private lateinit var loadingView: View
    private lateinit var searchBar: View
    private lateinit var etSearch: EditText
    private lateinit var tvSongCount: TextView
    private lateinit var actionBar: View

    private lateinit var tabSongs: TextView
    private lateinit var tabArtists: TextView
    private lateinit var tabCategories: TextView

    private var currentTab = Tab.SONGS

    enum class Tab { SONGS, ARTISTS, CATEGORIES }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_song_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        setupAdapters()
        setupTabs()
        setupSearch(view)
        setupSort(view)
        observeViewModel()
    }

    private fun initViews(view: View) {
        rvSongs = view.findViewById(R.id.rvSongs)
        emptyView = view.findViewById(R.id.emptyView)
        loadingView = view.findViewById(R.id.loadingView)
        searchBar = view.findViewById(R.id.searchBar)
        etSearch = view.findViewById(R.id.etSearch)
        tvSongCount = view.findViewById(R.id.tvSongCount)
        actionBar = view.findViewById(R.id.actionBar)

        tabSongs = view.findViewById(R.id.tabSongs)
        tabArtists = view.findViewById(R.id.tabArtists)
        tabCategories = view.findViewById(R.id.tabCategories)

        view.findViewById<TextView>(R.id.btnPlayAll).setOnClickListener {
            viewModel.playAll()
        }
    }

    private fun setupAdapters() {
        songAdapter = SongAdapter { song, position ->
            viewModel.playSong(song, position)
        }

        artistAdapter = ArtistAdapter(
            onArtistClick = { artist ->
                // 显示歌手的歌曲列表
                showArtistSongs(artist.name)
            },
            onPlayAllClick = { artist ->
                viewModel.playArtist(artist.name)
            }
        )

        categoryAdapter = CategoryAdapter { category ->
            viewModel.showCategorySongs(category)
            showCategorySongsView(category.displayName)
        }

        rvSongs.layoutManager = LinearLayoutManager(context)
        rvSongs.adapter = songAdapter
    }

    private fun setupTabs() {
        tabSongs.setOnClickListener { switchTab(Tab.SONGS) }
        tabArtists.setOnClickListener { switchTab(Tab.ARTISTS) }
        tabCategories.setOnClickListener { switchTab(Tab.CATEGORIES) }
    }

    private fun switchTab(tab: Tab) {
        currentTab = tab

        // 更新标签样式
        listOf(tabSongs, tabArtists, tabCategories).forEach {
            it.setBackgroundResource(R.drawable.bg_tab_unselected)
            it.setTextColor(requireContext().getColor(R.color.text_secondary))
        }

        val selectedTab = when (tab) {
            Tab.SONGS -> tabSongs
            Tab.ARTISTS -> tabArtists
            Tab.CATEGORIES -> tabCategories
        }
        selectedTab.setBackgroundResource(R.drawable.bg_tab_selected)
        selectedTab.setTextColor(requireContext().getColor(R.color.text_primary))

        when (tab) {
            Tab.SONGS -> {
                rvSongs.layoutManager = LinearLayoutManager(context)
                rvSongs.adapter = songAdapter
                actionBar.visibility = View.VISIBLE
                viewModel.showAllSongs()
            }
            Tab.ARTISTS -> {
                rvSongs.layoutManager = LinearLayoutManager(context)
                rvSongs.adapter = artistAdapter
                actionBar.visibility = View.GONE
                viewModel.loadArtists()
            }
            Tab.CATEGORIES -> {
                rvSongs.layoutManager = GridLayoutManager(context, 3)
                rvSongs.adapter = categoryAdapter
                actionBar.visibility = View.GONE
                viewModel.loadCategories()
            }
        }
    }

    private fun setupSearch(view: View) {
        val btnSearch: ImageButton = view.findViewById(R.id.btnSearch)
        val btnCloseSearch: ImageButton = view.findViewById(R.id.btnCloseSearch)

        btnSearch.setOnClickListener {
            searchBar.visibility = View.VISIBLE
            etSearch.requestFocus()
        }

        btnCloseSearch.setOnClickListener {
            searchBar.visibility = View.GONE
            etSearch.text.clear()
            viewModel.search("")
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.search(s?.toString() ?: "")
                // 搜索时切换到歌曲tab
                if (currentTab != Tab.SONGS) {
                    switchTab(Tab.SONGS)
                }
            }
        })
    }

    private fun setupSort(view: View) {
        val btnSort: ImageButton = view.findViewById(R.id.btnSort)
        btnSort.setOnClickListener { showSortDialog() }
    }

    private fun showSortDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_sort, null)
        val dialog = AlertDialog.Builder(requireContext(), R.style.Theme_CarMusicPlayer)
            .setView(dialogView)
            .create()

        val rgSort = dialogView.findViewById<RadioGroup>(R.id.rgSort)

        // 设置当前选中的排序方式
        val currentSort = viewModel.currentSortOrder.value ?: SortOrder.DATE_NEWEST
        val checkedId = when (currentSort) {
            SortOrder.TITLE_ASC -> R.id.rbTitleAsc
            SortOrder.TITLE_DESC -> R.id.rbTitleDesc
            SortOrder.ARTIST_ASC -> R.id.rbArtistAsc
            SortOrder.ARTIST_DESC -> R.id.rbArtistDesc
            SortOrder.DATE_NEWEST -> R.id.rbDateNewest
            SortOrder.DATE_OLDEST -> R.id.rbDateOldest
            SortOrder.DURATION_SHORT -> R.id.rbDurationShort
            SortOrder.DURATION_LONG -> R.id.rbDurationLong
        }
        rgSort.check(checkedId)

        rgSort.setOnCheckedChangeListener { _, id ->
            val sortOrder = when (id) {
                R.id.rbTitleAsc -> SortOrder.TITLE_ASC
                R.id.rbTitleDesc -> SortOrder.TITLE_DESC
                R.id.rbArtistAsc -> SortOrder.ARTIST_ASC
                R.id.rbArtistDesc -> SortOrder.ARTIST_DESC
                R.id.rbDateNewest -> SortOrder.DATE_NEWEST
                R.id.rbDateOldest -> SortOrder.DATE_OLDEST
                R.id.rbDurationShort -> SortOrder.DURATION_SHORT
                R.id.rbDurationLong -> SortOrder.DURATION_LONG
                else -> SortOrder.TITLE_ASC
            }
            viewModel.setSortOrder(sortOrder)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showArtistSongs(artistName: String) {
        val fragment = ArtistSongsFragment.newInstance(artistName)
        parentFragmentManager.beginTransaction()
            .replace(R.id.contentFrame, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun showCategorySongsView(categoryName: String) {
        val fragment = ArtistSongsFragment.newInstance(categoryName, isCategory = true)
        parentFragmentManager.beginTransaction()
            .replace(R.id.contentFrame, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun observeViewModel() {
        viewModel.displayedSongs.observe(viewLifecycleOwner) { songs ->
            if (currentTab == Tab.SONGS) {
                songAdapter.submitList(songs)
                tvSongCount.text = "${songs.size} 首歌曲"

                rvSongs.visibility = if (songs.isNotEmpty()) View.VISIBLE else View.GONE
                emptyView.visibility = if (songs.isEmpty() && !viewModel.isLoading.value!!) View.VISIBLE else View.GONE
            }
        }

        viewModel.artists.observe(viewLifecycleOwner) { artists ->
            if (currentTab == Tab.ARTISTS) {
                artistAdapter.submitList(artists)
            }
        }

        viewModel.categories.observe(viewLifecycleOwner) { categories ->
            if (currentTab == Tab.CATEGORIES) {
                categoryAdapter.submitList(categories)
            }
        }

        viewModel.currentPlayingSong.observe(viewLifecycleOwner) { song ->
            songAdapter.currentPlayingSongId = song?.id ?: -1
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            loadingView.visibility = if (loading) View.VISIBLE else View.GONE
            if (loading) {
                rvSongs.visibility = View.GONE
                emptyView.visibility = View.GONE
            }
        }
    }
}
