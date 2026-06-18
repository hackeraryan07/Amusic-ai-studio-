package com.example.viewmodel

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.database.TrackEntity
import com.example.data.repository.TrackRepository
import com.example.playback.AudioPlayer
import com.example.playback.PlaybackService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

enum class SortType {
    NAME, MODIFIED_DATE, DURATION, ARTIST
}

class MusicViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val database = AppDatabase.getDatabase(context)
    private val repository = TrackRepository(database.trackDao())
    val audioPlayer = AudioPlayer.getInstance(context)

    // Current tracks listed in database
    val allTracks: StateFlow<List<TrackEntity>> = repository.allTracks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI Drawer state
    private val _isDrawerOpen = MutableStateFlow(false)
    val isDrawerOpen: StateFlow<Boolean> = _isDrawerOpen

    // Active Search query string
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    // Active Sort Type style
    private val _sortType = MutableStateFlow(SortType.NAME)
    val sortType: StateFlow<SortType> = _sortType

    // Metadata preferences persistence
    private val sharedPrefs = context.getSharedPreferences("amusic_preferences", Context.MODE_PRIVATE)

    private val _showAlbumArt = MutableStateFlow(sharedPrefs.getBoolean("show_album_art", true))
    val showAlbumArt: StateFlow<Boolean> = _showAlbumArt

    private val _showArtist = MutableStateFlow(sharedPrefs.getBoolean("show_artist", true))
    val showArtist: StateFlow<Boolean> = _showArtist

    private val _showDuration = MutableStateFlow(sharedPrefs.getBoolean("show_duration", true))
    val showDuration: StateFlow<Boolean> = _showDuration

    private val _showAlbumName = MutableStateFlow(sharedPrefs.getBoolean("show_album_name", true))
    val showAlbumName: StateFlow<Boolean> = _showAlbumName

    private val _cropAlbumArt = MutableStateFlow(sharedPrefs.getBoolean("crop_album_art", true))
    val cropAlbumArt: StateFlow<Boolean> = _cropAlbumArt

    // Loading status
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    // Permission status
    private val _hasPermission = MutableStateFlow(false)
    val hasPermission: StateFlow<Boolean> = _hasPermission

    // Active Playing Playlist queue linked directly to player singleton
    val playQueue: StateFlow<List<TrackEntity>> = audioPlayer.playQueue

    init {
        checkPermissionState()
        loadPreferences()
        
        // Clear all cached/preset/demo items on start to keep the database fully pure and scan local audio if permitted
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAll()
            if (_hasPermission.value) {
                scanForLocalAudio()
            }
        }
    }

    fun toggleDrawer() {
        _isDrawerOpen.value = !_isDrawerOpen.value
    }

    fun closeDrawer() {
        _isDrawerOpen.value = false
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateSortType(type: SortType) {
        _sortType.value = type
    }

    fun checkPermissionState() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        _hasPermission.value = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun onPermissionGranted() {
        _hasPermission.value = true
        scanForLocalAudio()
    }

    fun setMetadataSetting(key: String, value: Boolean) {
        sharedPrefs.edit().putBoolean(key, value).apply()
        when (key) {
            "show_album_art" -> _showAlbumArt.value = value
            "show_artist" -> _showArtist.value = value
            "show_duration" -> _showDuration.value = value
            "show_album_name" -> _showAlbumName.value = value
            "crop_album_art" -> _cropAlbumArt.value = value
        }
    }

    private fun loadPreferences() {
        _showAlbumArt.value = sharedPrefs.getBoolean("show_album_art", true)
        _showArtist.value = sharedPrefs.getBoolean("show_artist", true)
        _showDuration.value = sharedPrefs.getBoolean("show_duration", true)
        _showAlbumName.value = sharedPrefs.getBoolean("show_album_name", true)
        _cropAlbumArt.value = sharedPrefs.getBoolean("crop_album_art", true)
    }

    fun scanForLocalAudio() {
        _isScanning.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val localMusic = mutableListOf<TrackEntity>()
                val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                val projection = arrayOf(
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.ALBUM,
                    MediaStore.Audio.Media.DURATION,
                    MediaStore.Audio.Media.DATA,
                    MediaStore.Audio.Media.ALBUM_ID,
                    MediaStore.Audio.Media.DATE_MODIFIED
                )
                val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
                
                context.contentResolver.query(uri, projection, selection, null, null)?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                    val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                    val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                    val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                    val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                    val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                    val dateModifiedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)

                    while (cursor.moveToNext()) {
                        val id = cursor.getString(idCol)
                        val title = cursor.getString(titleCol) ?: "Unknown Track"
                        val artist = cursor.getString(artistCol) ?: "Unknown Artist"
                        val album = cursor.getString(albumCol) ?: "Unknown Album"
                        val duration = cursor.getLong(durationCol)
                        val path = cursor.getString(dataCol)
                        val albumId = cursor.getLong(albumIdCol)
                        val dateModified = cursor.getLong(dateModifiedCol)
                        
                        val file = File(path)
                        val folderName = file.parentFile?.name ?: "Main Storage"

                        // Exclude tracks shorter than 5 seconds
                        if (duration > 5000) {
                            localMusic.add(
                                TrackEntity(
                                    id = id,
                                    title = title,
                                    artist = artist,
                                    album = album,
                                    duration = duration,
                                    path = path,
                                    albumId = albumId,
                                    dateModified = dateModified,
                                    folderName = folderName,
                                    isFavorite = false,
                                    isDemo = false
                                )
                            )
                        }
                    }
                }

                // Discard stale data and insert newly scanned files
                repository.clearAll()
                if (localMusic.isNotEmpty()) {
                    repository.insertTracks(localMusic)
                }
                Log.d("MusicViewModel", "Scanned ${localMusic.size} local tracks successfully")
            } catch (e: Exception) {
                Log.e("MusicViewModel", "Error scanning local storage files", e)
            } finally {
                _isScanning.value = false
            }
        }
    }

    // Play track and configure playlist queue
    fun selectAndPlayTrack(track: TrackEntity, sourceList: List<TrackEntity>) {
        audioPlayer.setPlayQueue(sourceList)
        audioPlayer.playTrack(track)
    }

    fun togglePlayPause() {
        if (audioPlayer.isPlaying.value) {
            audioPlayer.pause()
        } else {
            val current = audioPlayer.currentTrack.value
            if (current != null) {
                audioPlayer.resume()
            } else if (audioPlayer.playQueue.value.isNotEmpty()) {
                audioPlayer.playTrack(audioPlayer.playQueue.value.first())
            } else if (allTracks.value.isNotEmpty()) {
                selectAndPlayTrack(allTracks.value.first(), allTracks.value)
            }
        }
    }

    fun playNext() {
        audioPlayer.playNext()
    }

    fun playPrevious() {
        audioPlayer.playPrevious()
    }

    fun toggleFavorite(trackId: String, isFavorite: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateFavorite(trackId, isFavorite)
            val current = audioPlayer.currentTrack.value
            if (current != null && current.id == trackId) {
                audioPlayer.updateCurrentTrackFavorite(isFavorite)
            }
        }
    }

    fun forceResetDatabase() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAll()
            if (_hasPermission.value) {
                scanForLocalAudio()
            }
        }
    }

    fun stopPlaybackService() {
        val intent = Intent(context, PlaybackService::class.java).apply {
            action = PlaybackService.ACTION_STOP
        }
        context.startService(intent)
    }

    override fun onCleared() {
        super.onCleared()
        // Do NOT release audio player on cleared, because playback is managed by our foreground service.
    }
}
