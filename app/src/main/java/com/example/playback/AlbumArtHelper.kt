package com.example.playback

import android.media.MediaMetadataRetriever
import android.util.Log

object AlbumArtHelper {
    fun getByteArray(path: String?): ByteArray? {
        if (path.isNullOrEmpty()) return null
        var retriever: MediaMetadataRetriever? = null
        return try {
            retriever = MediaMetadataRetriever().apply {
                setDataSource(path)
            }
            retriever.embeddedPicture
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
}
