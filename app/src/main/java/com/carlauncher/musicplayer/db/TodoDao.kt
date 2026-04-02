package com.carlauncher.musicplayer.db

import androidx.room.*

@Dao
interface TodoDao {

    @Query("SELECT * FROM todos ORDER BY done ASC, createdAt DESC")
    suspend fun getAll(): List<TodoEntity>

    @Insert
    suspend fun insert(todo: TodoEntity): Long

    @Query("UPDATE todos SET content = :content WHERE id = :id")
    suspend fun updateContent(id: Long, content: String)

    @Query("UPDATE todos SET done = :done WHERE id = :id")
    suspend fun setDone(id: Long, done: Boolean)

    @Query("DELETE FROM todos WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM todos WHERE done = 1")
    suspend fun clearDone()
}
