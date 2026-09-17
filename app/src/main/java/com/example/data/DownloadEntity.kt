package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "download_history")
data class DownloadEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val title: String,
  val sourceUrl: String,
  val downloadUrl: String,
  val thumbnailUrl: String,
  val platform: String,
  val formatLabel: String,
  val fileSize: String,
  val duration: String,
  val status: String, // "COMPLETED", "FAILED", "DOWNLOADING"
  val timestamp: Long = System.currentTimeMillis()
)
