package com.example.viewmodel

import android.Manifest
import android.app.Application
import android.content.Context
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
    val audioPlayer = AudioPlayer(context)

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

    // Loading status
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    // Permission status
    private val _hasPermission = MutableStateFlow(false)
    val hasPermission: StateFlow<Boolean> = _hasPermission

    // Active Playing Playlist queue
    private val _playQueue = MutableStateFlow<List<TrackEntity>>(emptyList())
    val playQueue: StateFlow<List<TrackEntity>> = _playQueue

    init {
        checkPermissionState()
        loadPreferences()
        // If permission is already granted, scan immediately
        if (_hasPermission.value) {
            scanForLocalAudio()
        } else {
            // Seed base demo tracks so the app runs with gorgeous tracks on startup with or without permissions
            seedDemoTracks()
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
        }
    }

    private fun loadPreferences() {
        _showAlbumArt.value = sharedPrefs.getBoolean("show_album_art", true)
        _showArtist.value = sharedPrefs.getBoolean("show_artist", true)
        _showDuration.value = sharedPrefs.getBoolean("show_duration", true)
        _showAlbumName.value = sharedPrefs.getBoolean("show_album_name", true)
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
                // Filter music type only
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

                        // Exclude tracks shorter than 5 seconds (typically notifications)
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

                // Clean existing non-demo cached track records in database
                repository.clearNonDemoTracks()

                // Insert the discovered files
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

    private fun seedDemoTracks() {
        viewModelScope.launch(Dispatchers.IO) {
            val demoList = listOf(
                TrackEntity(
                    id = "demo_1",
                    title = "Horizon Dreamer",
                    artist = "SoundHelix",
                    album = "Helix Odyssey",
                    duration = 372000,
                    path = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
                    albumId = 1001L,
                    dateModified = 1718000000L,
                    folderName = "Streaming Demo",
                    isFavorite = false,
                    isDemo = true
                ),
                TrackEntity(
                    id = "demo_2",
                    title = "Retro Wavecrest",
                    artist = "SoundHelix",
                    album = "Helix Odyssey",
                    duration = 423000,
                    path = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
                    albumId = 1001L,
                    dateModified = 1718010000L,
                    folderName = "Streaming Demo",
                    isFavorite = true, // pre-favorite one for the demo experience
                    isDemo = true
                ),
                TrackEntity(
                    id = "demo_3",
                    title = "Infinite Echoes",
                    artist = "SoundHelix",
                    album = "Ambience World",
                    duration = 344000,
                    path = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
                    albumId = 1002L,
                    dateModified = 1718020000L,
                    folderName = "Streaming Demo",
                    isFavorite = false,
                    isDemo = true
                ),
                TrackEntity(
                    id = "demo_4",
                    title = "Synth Horizon",
                    artist = "SoundHelix",
                    album = "Ambience World",
                    duration = 302000,
                    path = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
                    albumId = 1002L,
                    dateModified = 1718030000L,
                    folderName = "Streaming Demo",
                    isFavorite = false,
                    isDemo = true
                ),
                TrackEntity(
                    id = "demo_5",
                    title = "Digital Oasis",
                    artist = "SoundHelix",
                    album = "Synthwave Beats",
                    duration = 361000,
                    path = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3",
                    albumId = 1003L,
                    dateModified = 1718040000L,
                    folderName = "Streaming Demo",
                    isFavorite = false,
                    isDemo = true
                )
            )
            repository.insertTracks(demoList)
        }
    }

    // Play track and configure playlists queue
    fun selectAndPlayTrack(track: TrackEntity, sourceList: List<TrackEntity>) {
        _playQueue.value = sourceList
        audioPlayer.playTrack(track)
    }

    fun togglePlayPause() {
        if (audioPlayer.isPlaying.value) {
            audioPlayer.pause()
        } else {
            val current = audioPlayer.currentTrack.value
            if (current != null) {
                audioPlayer.resume()
            } else if (_playQueue.value.isNotEmpty()) {
                audioPlayer.playTrack(_playQueue.value.first())
            } else if (allTracks.value.isNotEmpty()) {
                selectAndPlayTrack(allTracks.value.first(), allTracks.value)
            }
        }
    }

    fun playNext() {
        val queue = _playQueue.value
        val current = audioPlayer.currentTrack.value ?: return
        if (queue.isEmpty()) return

        val currentIndex = queue.indexOfFirst { it.id == current.id }
        if (currentIndex != -1 && currentIndex < queue.size - 1) {
            audioPlayer.playTrack(queue[currentIndex + 1])
        } else if (queue.isNotEmpty()) {
            audioPlayer.playTrack(queue.first()) // wrap around
        }
    }

    fun playPrevious() {
        val queue = _playQueue.value
        val current = audioPlayer.currentTrack.value ?: return
        if (queue.isEmpty()) return

        val currentIndex = queue.indexOfFirst { it.id == current.id }
        if (currentIndex > 0) {
            audioPlayer.playTrack(queue[currentIndex - 1])
        } else if (queue.isNotEmpty()) {
            audioPlayer.playTrack(queue.last()) // wrap around
        }
    }

    fun toggleFavorite(trackId: String, isFavorite: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateFavorite(trackId, isFavorite)
            // also update current track state if it is currently playing
            val current = audioPlayer.currentTrack.value
            if (current != null && current.id == trackId) {
                audioPlayer.updateCurrentTrackFavorite(isFavorite)
            }
        }
    }

    fun forceResetDatabase() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAll()
            seedDemoTracks()
            if (_hasPermission.value) {
                scanForLocalAudio()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
    }
}
