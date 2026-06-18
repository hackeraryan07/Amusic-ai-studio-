package com.example.data.repository

import com.example.data.database.TrackDao
import com.example.data.database.TrackEntity
import kotlinx.coroutines.flow.Flow

class TrackRepository(private val trackDao: TrackDao) {
    val allTracks: Flow<List<TrackEntity>> = trackDao.getAllTracks()

    suspend fun insertTracks(tracks: List<TrackEntity>) {
        trackDao.insertTracks(tracks)
    }

    suspend fun updateFavorite(trackId: String, isFavorite: Boolean) {
        trackDao.updateFavorite(trackId, isFavorite)
    }

    suspend fun clearNonDemoTracks() {
        trackDao.clearNonDemoTracks()
    }

    suspend fun clearAll() {
        trackDao.clearAll()
    }
}
