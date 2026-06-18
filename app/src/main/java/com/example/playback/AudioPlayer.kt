package com.example.playback

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.database.TrackEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AudioPlayer private constructor(private val context: Context) {
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

    private val _playQueue = MutableStateFlow<List<TrackEntity>>(emptyList())
    val playQueue: StateFlow<List<TrackEntity>> = _playQueue

    fun setPlayQueue(queue: List<TrackEntity>) {
        _playQueue.value = queue
    }

    fun playTrack(track: TrackEntity) {
        stop()
        _currentTrack.value = track
        _duration.value = track.duration.toInt()
        
        try {
            mediaPlayer = MediaPlayer().apply {
                if (track.isDemo) {
                    // For demo tracks, we use public audio streaming endpoints
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
                    updateServiceState()
                }
                
                setOnCompletionListener {
                    _isPlaying.value = false
                    _currentPosition.value = 0
                    stopProgressTracker()
                    updateServiceState()
                    // Auto play next song on completion
                    playNext()
                }
                
                setOnErrorListener { _, what, extra ->
                    Log.e("AudioPlayer", "MediaPlayer error: what = $what, extra = $extra")
                    _isPlaying.value = false
                    stopProgressTracker()
                    updateServiceState()
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
                updateServiceState()
            }
        }
    }

    fun resume() {
        mediaPlayer?.let {
            if (!it.isPlaying) {
                it.start()
                _isPlaying.value = true
                startProgressTracker()
                updateServiceState()
            } else {
                updateServiceState()
            }
        } ?: run {
            val queue = _playQueue.value
            if (queue.isNotEmpty()) {
                val track = _currentTrack.value ?: queue.first()
                playTrack(track)
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
        updateServiceState()
    }

    fun playNext() {
        val queue = _playQueue.value
        val current = _currentTrack.value ?: return
        if (queue.isEmpty()) return

        val currentIndex = queue.indexOfFirst { it.id == current.id }
        if (currentIndex != -1 && currentIndex < queue.size - 1) {
            playTrack(queue[currentIndex + 1])
        } else if (queue.isNotEmpty()) {
            playTrack(queue.first()) // wrap around
        }
    }

    fun playPrevious() {
        val queue = _playQueue.value
        val current = _currentTrack.value ?: return
        if (queue.isEmpty()) return

        val currentIndex = queue.indexOfFirst { it.id == current.id }
        if (currentIndex > 0) {
            playTrack(queue[currentIndex - 1])
        } else if (queue.isNotEmpty()) {
            playTrack(queue.last()) // wrap around
        }
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

    private fun updateServiceState() {
        val serviceIntent = Intent(context, PlaybackService::class.java).apply {
            action = PlaybackService.ACTION_UPDATE_STATE
        }
        try {
            ContextCompat.startForegroundService(context, serviceIntent)
        } catch (e: Exception) {
            Log.e("AudioPlayer", "Failed to start/update PlaybackService", e)
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: AudioPlayer? = null

        fun getInstance(context: Context): AudioPlayer {
            return INSTANCE ?: synchronized(this) {
                val instance = AudioPlayer(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
