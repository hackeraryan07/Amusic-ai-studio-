package com.example.playback

import android.media.MediaMetadataRetriever
import android.util.Log
import android.util.LruCache

object AlbumArtHelper {
    // Keep up to 150 items in memory to fast-render track listing thumbnails
    private val cache = LruCache<String, ByteArray>(150)

    fun getCachedByteArray(path: String?): ByteArray? {
        if (path.isNullOrEmpty()) return null
        return cache.get(path)
    }

    fun getByteArray(path: String?): ByteArray? {
        if (path.isNullOrEmpty()) return null
        val cached = cache.get(path)
        if (cached != null) return cached

        var retriever: MediaMetadataRetriever? = null
        return try {
            retriever = MediaMetadataRetriever().apply {
                setDataSource(path)
            }
            val bytes = retriever.embeddedPicture
            if (bytes != null) {
                cache.put(path, bytes)
            }
            bytes
        } catch (e: Exception) {
            Log.e("AlbumArtHelper", "Error extracting album art from $path: ${e.message}")
            null
        } finally {
            try {
                retriever?.release()
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    fun clearCache() {
        cache.evictAll()
    }
}
