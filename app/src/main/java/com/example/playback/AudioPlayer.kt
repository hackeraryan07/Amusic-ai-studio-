package com.example.playback

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import com.example.data.database.TrackEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AudioPlayer(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var progressJob: Job? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentTrack = MutableStateFlow<TrackEntity?>(null)
    val currentTrack: StateFlow<TrackEntity?> = _currentTrack

    private val _currentPosition = MutableStateFlow(0)
    val currentPosition: StateFlow<Int> = _currentPosition

    private val _duration = MutableStateFlow(0)
    val duration: StateFlow<Int> = _duration

    fun playTrack(track: TrackEntity) {
        stop()
        _currentTrack.value = track
        _duration.value = track.duration.toInt()
        
        try {
            mediaPlayer = MediaPlayer().apply {
                if (track.isDemo) {
                    // For demo tracks, we use public audio streaming endpoints to showcase a fully functional experience
                    setDataSource(track.path)
                } else {
                    // For local files we assign the exact URI/FilePath
                    val uri = Uri.parse(track.path)
                    setDataSource(context, uri)
                }
                
                setOnPreparedListener { mp ->
                    mp.start()
                    _isPlaying.value = true
                    _duration.value = mp.duration
                    startProgressTracker()
                }
                
                setOnCompletionListener {
                    _isPlaying.value = false
                    _currentPosition.value = 0
                    stopProgressTracker()
                }
                
                setOnErrorListener { _, what, extra ->
                    Log.e("AudioPlayer", "MediaPlayer error: what = $what, extra = $extra")
                    _isPlaying.value = false
                    stopProgressTracker()
                    false
                }
                
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e("AudioPlayer", "Error preparing media player", e)
            _isPlaying.value = false
            _currentTrack.value = null
        }
    }

    fun pause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                _isPlaying.value = false
                stopProgressTracker()
            }
        }
    }

    fun resume() {
        mediaPlayer?.let {
            if (!it.isPlaying) {
                it.start()
                _isPlaying.value = true
                startProgressTracker()
            }
        }
    }

    fun seekTo(positionMs: Int) {
        mediaPlayer?.let {
            it.seekTo(positionMs)
            _currentPosition.value = positionMs
        }
    }

    fun stop() {
        stopProgressTracker()
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
        }
        mediaPlayer = null
        _isPlaying.value = false
        _currentPosition.value = 0
    }

    private fun startProgressTracker() {
        stopProgressTracker()
        progressJob = coroutineScope.launch {
            while (isActive) {
                mediaPlayer?.let {
                    if (it.isPlaying) {
                        _currentPosition.value = it.currentPosition
                    }
                }
                delay(250)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    fun updateCurrentTrackFavorite(isFavorite: Boolean) {
        _currentTrack.value = _currentTrack.value?.copy(isFavorite = isFavorite)
    }

    fun release() {
        stop()
        coroutineScope.cancel()
    }
}
