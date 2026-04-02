package com.carlauncher.musicplayer.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.carlauncher.musicplayer.R
import com.carlauncher.musicplayer.repository.PlaylistRepository

class PlaylistAdapter(
    private val onPlaylistClick: (PlaylistRepository.PlaylistWithCount) -> Unit,
    private val onPlaylistLongClick: (PlaylistRepository.PlaylistWithCount) -> Unit,
    private val onCreateClick: () -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_PLAYLIST = 0
        private const val TYPE_CREATE = 1
    }

    private var playlists = listOf<PlaylistRepository.PlaylistWithCount>()

    private val bgColors = intArrayOf(
        R.color.category_bg_1,
        R.color.category_bg_2,
        R.color.category_bg_3,
        R.color.category_bg_4
    )

    fun submitList(list: List<PlaylistRepository.PlaylistWithCount>) {
        playlists = list
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == playlists.size) TYPE_CREATE else TYPE_PLAYLIST
    }

    override fun getItemCount() = playlists.size + 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_CREATE) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_playlist_create, parent, false)
            CreateViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_category, parent, false)
            PlaylistViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is PlaylistViewHolder) {
            val playlist = playlists[position]
            holder.bind(playlist, position)
        } else if (holder is CreateViewHolder) {
            holder.itemView.setOnClickListener { onCreateClick() }
        }
    }

    inner class PlaylistViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCategoryName: TextView = itemView.findViewById(R.id.tvCategoryName)
        private val tvCategoryCount: TextView = itemView.findViewById(R.id.tvCategoryCount)

        fun bind(playlist: PlaylistRepository.PlaylistWithCount, position: Int) {
            tvCategoryName.text = playlist.name
            tvCategoryCount.text = "${playlist.songCount} 首"

            val bgColorRes = bgColors[position % bgColors.size]
            val bgView = (itemView as ViewGroup).getChildAt(0)
            bgView.setBackgroundColor(itemView.context.getColor(bgColorRes))

            itemView.setOnClickListener { onPlaylistClick(playlist) }
            itemView.setOnLongClickListener {
                onPlaylistLongClick(playlist)
                true
            }
        }
    }

    inner class CreateViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}
