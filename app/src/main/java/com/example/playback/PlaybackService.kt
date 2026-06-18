package com.example.playback

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.bumptech.glide.Glide
import com.example.MainActivity
import com.example.R
import com.example.data.database.TrackEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import java.io.File

class PlaybackService : Service() {

    companion object {
        const val CHANNEL_ID = "amusic_playback_channel"
        const val NOTIFICATION_ID = 101

        const val ACTION_UPDATE_STATE = "com.example.playback.action.UPDATE_STATE"
        const val ACTION_PLAY = "com.example.playback.action.PLAY"
        const val ACTION_PAUSE = "com.example.playback.action.PAUSE"
        const val ACTION_TOGGLE = "com.example.playback.action.TOGGLE"
        const val ACTION_NEXT = "com.example.playback.action.NEXT"
        const val ACTION_PREVIOUS = "com.example.playback.action.PREVIOUS"
        const val ACTION_STOP = "com.example.playback.action.STOP"
    }

    private lateinit var audioPlayer: AudioPlayer
    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var trackingJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        Log.d("PlaybackService", "onCreate: Service starting")
        audioPlayer = AudioPlayer.getInstance(this)
        createNotificationChannel()
        setupMediaSession()
        observePlaybackState()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.d("PlaybackService", "onStartCommand action: $action")
        when (action) {
            ACTION_UPDATE_STATE -> {
                updateNotificationAndSession()
            }
            ACTION_PLAY -> {
                audioPlayer.resume()
            }
            ACTION_PAUSE -> {
                audioPlayer.pause()
            }
            ACTION_TOGGLE -> {
                if (audioPlayer.isPlaying.value) {
                    audioPlayer.pause()
                } else {
                    audioPlayer.resume()
                }
            }
            ACTION_NEXT -> {
                audioPlayer.playNext()
            }
            ACTION_PREVIOUS -> {
                audioPlayer.playPrevious()
            }
            ACTION_STOP -> {
                audioPlayer.stop()
                @Suppress("DEPRECATION")
                stopForeground(true)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "AMusic Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Media controls for background audio playing"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun setupMediaSession() {
        mediaSession = MediaSession(this, "AMusicSession").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() {
                    audioPlayer.resume()
                }

                override fun onPause() {
                    audioPlayer.pause()
                }

                override fun onSkipToNext() {
                    audioPlayer.playNext()
                }

                override fun onSkipToPrevious() {
                    audioPlayer.playPrevious()
                }

                override fun onSeekTo(pos: Long) {
                    audioPlayer.seekTo(pos.toInt())
                }
            })
            isActive = true
        }
    }

    private fun observePlaybackState() {
        trackingJob = serviceScope.launch {
            audioPlayer.currentTrack.collectLatest { _ ->
                updateNotificationAndSession()
            }
        }
        serviceScope.launch {
            audioPlayer.isPlaying.collectLatest { _ ->
                updateNotificationAndSession()
            }
        }
        serviceScope.launch {
            audioPlayer.currentPosition.collectLatest { pos ->
                updateSessionPlaybackState(pos)
            }
        }
    }

    private fun updateSessionPlaybackState(position: Int) {
        val state = if (audioPlayer.isPlaying.value) {
            PlaybackState.STATE_PLAYING
        } else if (audioPlayer.currentTrack.value != null) {
            PlaybackState.STATE_PAUSED
        } else {
            PlaybackState.STATE_NONE
        }

        val stateBuilder = PlaybackState.Builder()
            .setActions(
                PlaybackState.ACTION_PLAY or
                PlaybackState.ACTION_PAUSE or
                PlaybackState.ACTION_PLAY_PAUSE or
                PlaybackState.ACTION_SKIP_TO_NEXT or
                PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                PlaybackState.ACTION_SEEK_TO
            )
            .setState(state, position.toLong(), 1.0f)
        mediaSession?.setPlaybackState(stateBuilder.build())
    }

    private fun updateNotificationAndSession() {
        val track = audioPlayer.currentTrack.value
        val isPlaying = audioPlayer.isPlaying.value

        if (track == null) {
            @Suppress("DEPRECATION")
            stopForeground(true)
            return
        }

        serviceScope.launch {
            val bitmap: Bitmap? = withContext(Dispatchers.IO) {
                try {
                    val artBytes = AlbumArtHelper.getByteArray(track.path)
                    if (artBytes != null) {
                        Glide.with(this@PlaybackService)
                            .asBitmap()
                            .load(artBytes)
                            .submit(300, 300)
                            .get()
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    null
                }
            }

            val metadataBuilder = MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, track.title)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, track.artist)
                .putString(MediaMetadata.METADATA_KEY_ALBUM, track.album)
                .putLong(MediaMetadata.METADATA_KEY_DURATION, track.duration)

            if (bitmap != null) {
                metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, bitmap)
            }
            mediaSession?.setMetadata(metadataBuilder.build())
            updateSessionPlaybackState(audioPlayer.currentPosition.value)

            val notificationIntent = Intent(this@PlaybackService, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                this@PlaybackService,
                0,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val pPrev = PendingIntent.getService(
                this@PlaybackService,
                11,
                Intent(this@PlaybackService, PlaybackService::class.java).apply { action = ACTION_PREVIOUS },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val pPlayPause = PendingIntent.getService(
                this@PlaybackService,
                12,
                Intent(this@PlaybackService, PlaybackService::class.java).apply { action = ACTION_TOGGLE },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val pNext = PendingIntent.getService(
                this@PlaybackService,
                13,
                Intent(this@PlaybackService, PlaybackService::class.java).apply { action = ACTION_NEXT },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val pStop = PendingIntent.getService(
                this@PlaybackService,
                14,
                Intent(this@PlaybackService, PlaybackService::class.java).apply { action = ACTION_STOP },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val playPauseIcon = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
            val playPauseTitle = if (isPlaying) "Pause" else "Play"

            val notificationBuilder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Notification.Builder(this@PlaybackService, CHANNEL_ID)
            } else {
                @Suppress("DEPRECATION")
                Notification.Builder(this@PlaybackService)
            }

            notificationBuilder
                .setContentTitle(track.title)
                .setContentText(track.artist)
                .setSubText(track.album)
                .setContentIntent(pendingIntent)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setOngoing(isPlaying)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setShowWhen(false)
                .setDeleteIntent(pStop)

            if (bitmap != null) {
                notificationBuilder.setLargeIcon(bitmap)
            }

            notificationBuilder.addAction(
                Notification.Action.Builder(
                    android.R.drawable.ic_media_previous,
                    "Previous",
                    pPrev
                ).build()
            )
            notificationBuilder.addAction(
                Notification.Action.Builder(
                    playPauseIcon,
                    playPauseTitle,
                    pPlayPause
                ).build()
            )
            notificationBuilder.addAction(
                Notification.Action.Builder(
                    android.R.drawable.ic_media_next,
                    "Next",
                    pNext
                ).build()
            )
            notificationBuilder.addAction(
                Notification.Action.Builder(
                    android.R.drawable.ic_menu_close_clear_cancel,
                    "Stop",
                    pStop
                ).build()
            )

            val mediaStyle = Notification.MediaStyle()
                .setMediaSession(mediaSession?.sessionToken)
                .setShowActionsInCompactView(0, 1, 2)

            notificationBuilder.setStyle(mediaStyle)

            val notification = notificationBuilder.build()

            if (!isPlaying) {
                if (Build.VERSION.SDK_INT >= 33) {
                    stopForeground(2) // Service.STOP_FOREGROUND_DETACH
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(false)
                }
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(NOTIFICATION_ID, notification)
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        trackingJob?.cancel()
        serviceScope.cancel()
        mediaSession?.isActive = false
        mediaSession?.release()
        mediaSession = null
        Log.d("PlaybackService", "Service destroyed")
    }
}
