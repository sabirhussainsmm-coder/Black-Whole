package com.example.data

import kotlinx.coroutines.flow.Flow

class DownloadRepository(private val downloadDao: DownloadDao) {
  val allDownloads: Flow<List<DownloadEntity>> = downloadDao.getAllDownloads()

  suspend fun insert(download: DownloadEntity): Long {
    return downloadDao.insertDownload(download)
  }

  suspend fun deleteById(id: Long) {
    downloadDao.deleteById(id)
  }

  suspend fun clearAll() {
    downloadDao.clearAll()
  }
}
