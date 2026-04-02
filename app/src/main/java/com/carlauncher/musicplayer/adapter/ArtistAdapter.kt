package com.carlauncher.musicplayer.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.carlauncher.musicplayer.R
import com.carlauncher.musicplayer.model.Artist

class ArtistAdapter(
    private val onArtistClick: (Artist) -> Unit,
    private val onPlayAllClick: (Artist) -> Unit
) : ListAdapter<Artist, ArtistAdapter.ViewHolder>(ArtistDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_artist, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvArtistName: TextView = itemView.findViewById(R.id.tvArtistName)
        private val tvSongCount: TextView = itemView.findViewById(R.id.tvSongCount)
        private val btnPlayArtist: TextView = itemView.findViewById(R.id.btnPlayArtist)

        fun bind(artist: Artist) {
            tvArtistName.text = artist.name
            tvSongCount.text = "${artist.songCount} 首歌曲"

            itemView.setOnClickListener { onArtistClick(artist) }
            btnPlayArtist.setOnClickListener { onPlayAllClick(artist) }
        }
    }

    class ArtistDiffCallback : DiffUtil.ItemCallback<Artist>() {
        override fun areItemsTheSame(oldItem: Artist, newItem: Artist) = oldItem.name == newItem.name
        override fun areContentsTheSame(oldItem: Artist, newItem: Artist) = oldItem == newItem
    }
}
