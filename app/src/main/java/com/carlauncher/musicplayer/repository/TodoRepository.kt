package com.carlauncher.musicplayer.repository

import android.content.Context
import com.carlauncher.musicplayer.db.AppDatabase
import com.carlauncher.musicplayer.db.TodoEntity

class TodoRepository(context: Context) {

    private val dao = AppDatabase.getInstance(context).todoDao()

    suspend fun getAll(): List<TodoEntity> = dao.getAll()

    suspend fun add(content: String): Long = dao.insert(TodoEntity(content = content))

    suspend fun updateContent(id: Long, content: String) = dao.updateContent(id, content)

    suspend fun toggleDone(id: Long, done: Boolean) = dao.setDone(id, done)

    suspend fun delete(id: Long) = dao.delete(id)

    suspend fun clearDone() = dao.clearDone()
}
