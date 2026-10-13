package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_lists")
data class SavedListEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0L,
  val title: String,
  val timestamp: Long = System.currentTimeMillis(),
  val totalTested: Int,
  val onlineCount: Int,
  val urlsJoined: String, // newline-separated online URLs
  val averageLatencyMs: Long = 0L
) {
  val urlsList: List<String>
    get() = if (urlsJoined.isBlank()) emptyList() else urlsJoined.split("\n").filter { it.isNotBlank() }
}
