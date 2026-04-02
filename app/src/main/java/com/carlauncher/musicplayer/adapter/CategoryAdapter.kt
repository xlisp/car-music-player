package com.carlauncher.musicplayer.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.carlauncher.musicplayer.R
import com.carlauncher.musicplayer.model.MusicCategory

class CategoryAdapter(
    private val onCategoryClick: (MusicCategory) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {

    private var categories = listOf<Pair<MusicCategory, Int>>()

    private val bgColors = intArrayOf(
        R.color.category_bg_1,
        R.color.category_bg_2,
        R.color.category_bg_3,
        R.color.category_bg_4
    )

    fun submitList(list: List<Pair<MusicCategory, Int>>) {
        categories = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (category, count) = categories[position]
        holder.bind(category, count, position)
    }

    override fun getItemCount() = categories.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCategoryName: TextView = itemView.findViewById(R.id.tvCategoryName)
        private val tvCategoryCount: TextView = itemView.findViewById(R.id.tvCategoryCount)

        fun bind(category: MusicCategory, count: Int, position: Int) {
            tvCategoryName.text = category.displayName
            tvCategoryCount.text = "${count} 首"

            // 循环使用不同背景色
            val bgColorRes = bgColors[position % bgColors.size]
            val bgView = (itemView as ViewGroup).getChildAt(0)
            bgView.setBackgroundColor(itemView.context.getColor(bgColorRes))
            bgView.background?.let {
                // 保持圆角
            }

            itemView.setOnClickListener { onCategoryClick(category) }
        }
    }
}
