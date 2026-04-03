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
import com.carlauncher.musicplayer.adapter.MostPlayedAdapter
import com.carlauncher.musicplayer.adapter.PlaylistAdapter
import com.carlauncher.musicplayer.adapter.SongAdapter
import com.carlauncher.musicplayer.adapter.TodoAdapter
import com.carlauncher.musicplayer.model.Song
import com.carlauncher.musicplayer.model.SortOrder
import com.carlauncher.musicplayer.viewmodel.MusicViewModel

class SongListFragment : Fragment() {

    private val viewModel: MusicViewModel by activityViewModels()

    private lateinit var songAdapter: SongAdapter
    private lateinit var artistAdapter: ArtistAdapter
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var mostPlayedAdapter: MostPlayedAdapter
    private lateinit var playlistAdapter: PlaylistAdapter
    private lateinit var todoAdapter: TodoAdapter

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
    private lateinit var tabMostPlayed: TextView
    private lateinit var tabPlaylists: TextView
    private lateinit var tabTodo: TextView

    private var currentTab = Tab.SONGS

    enum class Tab { SONGS, ARTISTS, CATEGORIES, MOST_PLAYED, PLAYLISTS, TODO }

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
        tabMostPlayed = view.findViewById(R.id.tabMostPlayed)
        tabPlaylists = view.findViewById(R.id.tabPlaylists)
        tabTodo = view.findViewById(R.id.tabTodo)

        view.findViewById<TextView>(R.id.btnPlayAll).setOnClickListener {
            if (currentTab == Tab.MOST_PLAYED) {
                viewModel.playMostPlayed()
            } else {
                viewModel.playAll()
            }
        }
    }

    private fun setupAdapters() {
        songAdapter = SongAdapter { song, position ->
            viewModel.playSong(song, position)
        }

        // Long press on song -> add to playlist
        songAdapter.onSongLongClick = { song ->
            showAddToPlaylistDialog(song)
        }

        artistAdapter = ArtistAdapter(
            onArtistClick = { artist ->
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

        mostPlayedAdapter = MostPlayedAdapter(
            getPlayCount = { path -> viewModel.getPlayCount(path) },
            onSongClick = { _, position ->
                val songs = viewModel.mostPlayedSongs.value ?: return@MostPlayedAdapter
                viewModel.onPlayRequest?.invoke(songs, position)
            }
        )

        mostPlayedAdapter.onSongLongClick = { song ->
            showAddToPlaylistDialog(song)
        }

        playlistAdapter = PlaylistAdapter(
            onPlaylistClick = { playlist ->
                showPlaylistSongs(playlist.id, playlist.name)
            },
            onPlaylistLongClick = { playlist ->
                showPlaylistOptionsDialog(playlist)
            },
            onCreateClick = {
                showCreatePlaylistDialog()
            }
        )

        todoAdapter = TodoAdapter(
            onToggle = { todo, done -> viewModel.toggleTodoDone(todo.id, done) },
            onDelete = { todo -> viewModel.deleteTodo(todo.id) },
            onEdit = { todo -> showEditTodoDialog(todo) }
        )

        rvSongs.layoutManager = LinearLayoutManager(context)
        rvSongs.adapter = songAdapter
    }

    private fun setupTabs() {
        tabSongs.setOnClickListener { switchTab(Tab.SONGS) }
        tabArtists.setOnClickListener { switchTab(Tab.ARTISTS) }
        tabCategories.setOnClickListener { switchTab(Tab.CATEGORIES) }
        tabMostPlayed.setOnClickListener { switchTab(Tab.MOST_PLAYED) }
        tabPlaylists.setOnClickListener { switchTab(Tab.PLAYLISTS) }
        tabTodo.setOnClickListener { switchTab(Tab.TODO) }
    }

    private val allTabs: List<TextView>
        get() = listOf(tabSongs, tabArtists, tabCategories, tabMostPlayed, tabPlaylists, tabTodo)

    private fun switchTab(tab: Tab) {
        currentTab = tab

        // 重置列表和空视图状态，防止上一个tab的状态残留
        rvSongs.visibility = View.VISIBLE
        emptyView.visibility = View.GONE

        allTabs.forEach {
            it.setBackgroundResource(R.drawable.bg_tab_unselected)
            it.setTextColor(requireContext().getColor(R.color.text_secondary))
        }

        val selectedTab = when (tab) {
            Tab.SONGS -> tabSongs
            Tab.ARTISTS -> tabArtists
            Tab.CATEGORIES -> tabCategories
            Tab.MOST_PLAYED -> tabMostPlayed
            Tab.PLAYLISTS -> tabPlaylists
            Tab.TODO -> tabTodo
        }
        selectedTab.setBackgroundResource(R.drawable.bg_tab_selected)
        selectedTab.setTextColor(requireContext().getColor(R.color.text_primary))

        // Reset play-all button when leaving TODO tab
        if (tab != Tab.TODO) {
            view?.findViewById<TextView>(R.id.btnPlayAll)?.let {
                it.text = "\u25B6 全部播放"
                it.setOnClickListener {
                    if (currentTab == Tab.MOST_PLAYED) viewModel.playMostPlayed() else viewModel.playAll()
                }
            }
        }

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
            Tab.MOST_PLAYED -> {
                rvSongs.layoutManager = LinearLayoutManager(context)
                rvSongs.adapter = mostPlayedAdapter
                actionBar.visibility = View.VISIBLE
                viewModel.loadMostPlayed()
            }
            Tab.PLAYLISTS -> {
                rvSongs.layoutManager = GridLayoutManager(context, 3)
                rvSongs.adapter = playlistAdapter
                actionBar.visibility = View.GONE
                viewModel.loadPlaylists()
            }
            Tab.TODO -> {
                rvSongs.layoutManager = LinearLayoutManager(context)
                rvSongs.adapter = todoAdapter
                actionBar.visibility = View.VISIBLE
                tvSongCount.text = ""
                view?.findViewById<TextView>(R.id.btnPlayAll)?.let {
                    it.text = "+ 添加"
                    it.setOnClickListener { showAddTodoDialog() }
                }
                viewModel.loadTodos()
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

    private fun showPlaylistSongs(playlistId: Long, playlistName: String) {
        val fragment = PlaylistSongsFragment.newInstance(playlistId, playlistName)
        parentFragmentManager.beginTransaction()
            .replace(R.id.contentFrame, fragment)
            .addToBackStack(null)
            .commit()
    }

    // ========== 播放列表对话框 ==========

    private fun showCreatePlaylistDialog() {
        val editText = EditText(requireContext()).apply {
            hint = getString(R.string.playlist_name_hint)
            setTextColor(requireContext().getColor(R.color.text_primary))
            setHintTextColor(requireContext().getColor(R.color.text_hint))
            setBackgroundResource(R.drawable.bg_search_bar)
            setPadding(32, 24, 32, 24)
        }

        AlertDialog.Builder(requireContext(), R.style.Theme_CarMusicPlayer)
            .setTitle(R.string.create_playlist_title)
            .setView(editText)
            .setPositiveButton(R.string.btn_confirm) { _, _ ->
                val name = editText.text.toString().trim()
                if (name.isNotEmpty()) {
                    viewModel.createPlaylist(name)
                }
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    private fun showPlaylistOptionsDialog(playlist: com.carlauncher.musicplayer.repository.PlaylistRepository.PlaylistWithCount) {
        val options = arrayOf(
            getString(R.string.btn_rename),
            getString(R.string.btn_delete)
        )
        AlertDialog.Builder(requireContext(), R.style.Theme_CarMusicPlayer)
            .setTitle(playlist.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showRenamePlaylistDialog(playlist)
                    1 -> showDeletePlaylistDialog(playlist)
                }
            }
            .show()
    }

    private fun showRenamePlaylistDialog(playlist: com.carlauncher.musicplayer.repository.PlaylistRepository.PlaylistWithCount) {
        val editText = EditText(requireContext()).apply {
            setText(playlist.name)
            setTextColor(requireContext().getColor(R.color.text_primary))
            setHintTextColor(requireContext().getColor(R.color.text_hint))
            setBackgroundResource(R.drawable.bg_search_bar)
            setPadding(32, 24, 32, 24)
            selectAll()
        }

        AlertDialog.Builder(requireContext(), R.style.Theme_CarMusicPlayer)
            .setTitle(R.string.rename_playlist_title)
            .setView(editText)
            .setPositiveButton(R.string.btn_confirm) { _, _ ->
                val name = editText.text.toString().trim()
                if (name.isNotEmpty()) {
                    viewModel.renamePlaylist(playlist.id, name)
                }
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    private fun showDeletePlaylistDialog(playlist: com.carlauncher.musicplayer.repository.PlaylistRepository.PlaylistWithCount) {
        AlertDialog.Builder(requireContext(), R.style.Theme_CarMusicPlayer)
            .setTitle(R.string.delete_playlist_title)
            .setMessage(getString(R.string.delete_playlist_confirm, playlist.name))
            .setPositiveButton(R.string.btn_delete) { _, _ ->
                viewModel.deletePlaylist(playlist.id)
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    private fun showAddToPlaylistDialog(song: Song) {
        viewModel.loadPlaylists()
        // Wait a moment for playlists to load, then show dialog
        viewModel.playlists.observe(viewLifecycleOwner) observer@{ playlists ->
            // Only show once
            viewModel.playlists.removeObservers(viewLifecycleOwner)
            // Re-observe for tab updates
            reObservePlaylists()

            if (playlists.isEmpty()) {
                // No playlists yet, offer to create one
                showCreatePlaylistThenAddDialog(song)
                return@observer
            }

            val names = playlists.map { it.name }.toTypedArray()
            AlertDialog.Builder(requireContext(), R.style.Theme_CarMusicPlayer)
                .setTitle(R.string.add_to_playlist)
                .setItems(names) { _, which ->
                    val playlist = playlists[which]
                    viewModel.addSongToPlaylist(playlist.id, song) {
                        Toast.makeText(context, getString(R.string.added_to_playlist, playlist.name), Toast.LENGTH_SHORT).show()
                    }
                }
                .setNeutralButton(R.string.create_playlist) { _, _ ->
                    showCreatePlaylistThenAddDialog(song)
                }
                .setNegativeButton(R.string.btn_cancel, null)
                .show()
        }
    }

    private fun showCreatePlaylistThenAddDialog(song: Song) {
        val editText = EditText(requireContext()).apply {
            hint = getString(R.string.playlist_name_hint)
            setTextColor(requireContext().getColor(R.color.text_primary))
            setHintTextColor(requireContext().getColor(R.color.text_hint))
            setBackgroundResource(R.drawable.bg_search_bar)
            setPadding(32, 24, 32, 24)
        }

        AlertDialog.Builder(requireContext(), R.style.Theme_CarMusicPlayer)
            .setTitle(R.string.create_playlist_title)
            .setView(editText)
            .setPositiveButton(R.string.btn_confirm) { _, _ ->
                val name = editText.text.toString().trim()
                if (name.isNotEmpty()) {
                    viewModel.createPlaylist(name) { playlistId ->
                        viewModel.addSongToPlaylist(playlistId, song) {
                            Toast.makeText(context, getString(R.string.added_to_playlist, name), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    // ========== TODO 对话框 ==========

    private fun showAddTodoDialog() {
        val editText = EditText(requireContext()).apply {
            hint = getString(R.string.todo_add_hint)
            setTextColor(requireContext().getColor(R.color.text_primary))
            setHintTextColor(requireContext().getColor(R.color.text_hint))
            setBackgroundResource(R.drawable.bg_search_bar)
            setPadding(32, 24, 32, 24)
        }

        AlertDialog.Builder(requireContext(), R.style.Theme_CarMusicPlayer)
            .setTitle(R.string.todo_add_title)
            .setView(editText)
            .setPositiveButton(R.string.btn_confirm) { _, _ ->
                val content = editText.text.toString().trim()
                if (content.isNotEmpty()) {
                    viewModel.addTodo(content)
                }
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    private fun showEditTodoDialog(todo: com.carlauncher.musicplayer.db.TodoEntity) {
        val editText = EditText(requireContext()).apply {
            setText(todo.content)
            setTextColor(requireContext().getColor(R.color.text_primary))
            setHintTextColor(requireContext().getColor(R.color.text_hint))
            setBackgroundResource(R.drawable.bg_search_bar)
            setPadding(32, 24, 32, 24)
            selectAll()
        }

        AlertDialog.Builder(requireContext(), R.style.Theme_CarMusicPlayer)
            .setTitle(R.string.todo_edit_title)
            .setView(editText)
            .setPositiveButton(R.string.btn_confirm) { _, _ ->
                val content = editText.text.toString().trim()
                if (content.isNotEmpty()) {
                    viewModel.updateTodoContent(todo.id, content)
                }
            }
            .setNeutralButton(R.string.btn_delete) { _, _ ->
                viewModel.deleteTodo(todo.id)
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    // ========== ViewModel 观察 ==========

    private fun reObservePlaylists() {
        viewModel.playlists.observe(viewLifecycleOwner) { playlists ->
            if (currentTab == Tab.PLAYLISTS) {
                playlistAdapter.submitList(playlists)
                rvSongs.visibility = View.VISIBLE
                emptyView.visibility = View.GONE
            }
        }
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
                rvSongs.visibility = if (artists.isNotEmpty()) View.VISIBLE else View.GONE
                emptyView.visibility = if (artists.isEmpty() && !viewModel.isLoading.value!!) View.VISIBLE else View.GONE
            }
        }

        viewModel.categories.observe(viewLifecycleOwner) { categories ->
            if (currentTab == Tab.CATEGORIES) {
                categoryAdapter.submitList(categories)
                rvSongs.visibility = if (categories.isNotEmpty()) View.VISIBLE else View.GONE
                emptyView.visibility = if (categories.isEmpty() && !viewModel.isLoading.value!!) View.VISIBLE else View.GONE
            }
        }

        viewModel.mostPlayedSongs.observe(viewLifecycleOwner) { songs ->
            if (currentTab == Tab.MOST_PLAYED) {
                mostPlayedAdapter.submitList(songs)
                tvSongCount.text = "${songs.size} 首常听"

                rvSongs.visibility = if (songs.isNotEmpty()) View.VISIBLE else View.GONE
                emptyView.visibility = if (songs.isEmpty() && !viewModel.isLoading.value!!) View.VISIBLE else View.GONE
            }
        }

        reObservePlaylists()

        viewModel.todos.observe(viewLifecycleOwner) { todos ->
            if (currentTab == Tab.TODO) {
                todoAdapter.submitList(todos)
                tvSongCount.text = "${todos.size} 条记录"
                rvSongs.visibility = if (todos.isNotEmpty()) View.VISIBLE else View.GONE
                emptyView.visibility = if (todos.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        viewModel.currentPlayingSong.observe(viewLifecycleOwner) { song ->
            songAdapter.currentPlayingSongId = song?.id ?: -1
            mostPlayedAdapter.currentPlayingSongId = song?.id ?: -1
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
