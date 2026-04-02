package com.carlauncher.musicplayer.adapter

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.carlauncher.musicplayer.R
import com.carlauncher.musicplayer.db.TodoEntity

class TodoAdapter(
    private val onToggle: (TodoEntity, Boolean) -> Unit,
    private val onDelete: (TodoEntity) -> Unit,
    private val onEdit: (TodoEntity) -> Unit
) : RecyclerView.Adapter<TodoAdapter.ViewHolder>() {

    private var items = listOf<TodoEntity>()

    fun submitList(list: List<TodoEntity>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_todo, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cbDone: CheckBox = itemView.findViewById(R.id.cbDone)
        private val tvContent: TextView = itemView.findViewById(R.id.tvContent)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)

        fun bind(todo: TodoEntity) {
            cbDone.setOnCheckedChangeListener(null)
            cbDone.isChecked = todo.done
            tvContent.text = todo.content

            if (todo.done) {
                tvContent.paintFlags = tvContent.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                tvContent.setTextColor(itemView.context.getColor(R.color.text_hint))
            } else {
                tvContent.paintFlags = tvContent.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                tvContent.setTextColor(itemView.context.getColor(R.color.text_primary))
            }

            cbDone.setOnCheckedChangeListener { _, isChecked ->
                onToggle(todo, isChecked)
            }

            btnDelete.setOnClickListener { onDelete(todo) }

            itemView.setOnClickListener { onEdit(todo) }
        }
    }
}
