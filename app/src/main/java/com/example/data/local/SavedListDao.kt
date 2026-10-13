package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedListDao {
  @Query("SELECT * FROM saved_lists ORDER BY timestamp DESC")
  fun getAllSavedLists(): Flow<List<SavedListEntity>>

  @Query("SELECT * FROM saved_lists WHERE id = :id")
  suspend fun getById(id: Long): SavedListEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertSavedList(savedList: SavedListEntity): Long

  @Delete
  suspend fun deleteSavedList(savedList: SavedListEntity)

  @Query("DELETE FROM saved_lists WHERE id = :id")
  suspend fun deleteById(id: Long)

  @Query("DELETE FROM saved_lists")
  suspend fun deleteAll()
}
