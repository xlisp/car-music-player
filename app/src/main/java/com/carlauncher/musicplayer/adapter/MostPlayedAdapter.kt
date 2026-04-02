package com.carlauncher.musicplayer.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.carlauncher.musicplayer.R
import com.carlauncher.musicplayer.model.Song

/**
 * 常听歌曲列表适配器 - 显示播放次数
 */
class MostPlayedAdapter(
    private val getPlayCount: (String) -> Int,
    private val onSongClick: (Song, Int) -> Unit
) : ListAdapter<Song, MostPlayedAdapter.ViewHolder>(SongDiffCallback()) {

    var currentPlayingSongId: Long = -1
        set(value) {
            val oldId = field
            field = value
            currentList.forEachIndexed { index, song ->
                if (song.id == oldId || song.id == value) {
                    notifyItemChanged(index)
                }
            }
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_song, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val song = getItem(position)
        holder.bind(song, position)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvIndex: TextView = itemView.findViewById(R.id.tvIndex)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        private val tvArtist: TextView = itemView.findViewById(R.id.tvArtist)
        private val tvDuration: TextView = itemView.findViewById(R.id.tvDuration)

        fun bind(song: Song, position: Int) {
            val isPlaying = song.id == currentPlayingSongId
            val playCount = getPlayCount(song.path)

            tvIndex.text = if (isPlaying) "♪" else "${position + 1}"
            tvTitle.text = song.title
            tvArtist.text = "${song.artist} · 播放${playCount}次"
            tvDuration.text = song.durationText

            if (isPlaying) {
                itemView.setBackgroundResource(R.drawable.bg_playing_item)
                tvTitle.setTextColor(itemView.context.getColor(R.color.accent))
                tvIndex.setTextColor(itemView.context.getColor(R.color.accent))
            } else {
                itemView.background = null
                tvTitle.setTextColor(itemView.context.getColor(R.color.text_primary))
                tvIndex.setTextColor(itemView.context.getColor(R.color.text_hint))
            }

            itemView.setOnClickListener {
                onSongClick(song, position)
            }
        }
    }

    class SongDiffCallback : DiffUtil.ItemCallback<Song>() {
        override fun areItemsTheSame(oldItem: Song, newItem: Song) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Song, newItem: Song) = oldItem == newItem
    }
}
