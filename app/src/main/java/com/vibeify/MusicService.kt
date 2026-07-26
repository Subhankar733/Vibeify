package com.vibeify

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class MusicService : Service() {

    companion object {
        const val ACTION_PLAY = "com.vibeify.action.PLAY"
        const val ACTION_PAUSE = "com.vibeify.action.PAUSE"
        const val ACTION_NEXT = "com.vibeify.action.NEXT"
        const val ACTION_PREVIOUS = "com.vibeify.action.PREVIOUS"
        const val ACTION_STOP = "com.vibeify.action.STOP"

        const val EXTRA_PATH = "path"
        const val EXTRA_TITLE = "title"
        const val EXTRA_ARTIST = "artist"
        const val EXTRA_PATHS = "paths"
        const val EXTRA_TITLES = "titles"
        const val EXTRA_ARTISTS = "artists"
        const val EXTRA_INDEX = "index"

        private const val CHANNEL_ID = "vibeify_playback"
        private const val NOTIFICATION_ID = 1001

        var mediaPlayer: MediaPlayer? = null
            private set

        var currentPath: String? = null
            private set

        var currentTitle: String = "Vibeify"
            private set

        var currentArtist: String = "Local Music"
            private set

        private val playlistPaths = mutableListOf<String>()
        private val playlistTitles = mutableListOf<String>()
        private val playlistArtists = mutableListOf<String>()
        private var currentIndex = -1

        var shuffleEnabled = false
            private set

        var repeatEnabled = false
            private set

        fun toggleShuffle() {
            shuffleEnabled = !shuffleEnabled
        }

        fun toggleRepeat() {
            repeatEnabled = !repeatEnabled
        }

        fun currentIndex(): Int {
            return currentIndex
        }

        fun isPlaying(): Boolean {
            return try {
                mediaPlayer?.isPlaying == true
            } catch (_: Exception) {
                false
            }
        }

        fun duration(): Int {
            return try {
                mediaPlayer?.duration ?: 0
            } catch (_: Exception) {
                0
            }
        }

        fun position(): Int {
            return try {
                mediaPlayer?.currentPosition ?: 0
            } catch (_: Exception) {
                0
            }
        }

        fun seekTo(position: Int) {
            try {
                mediaPlayer?.seekTo(position)
            } catch (_: Exception) {
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        when (intent?.action) {

            ACTION_PLAY -> {
                val incomingPaths =
                    intent.getStringArrayListExtra(EXTRA_PATHS)
                val incomingTitles =
                    intent.getStringArrayListExtra(EXTRA_TITLES)
                val incomingArtists =
                    intent.getStringArrayListExtra(EXTRA_ARTISTS)

                if (!incomingPaths.isNullOrEmpty()) {
                    playlistPaths.clear()
                    playlistPaths.addAll(incomingPaths)

                    playlistTitles.clear()
                    playlistTitles.addAll(
                        incomingTitles ?: arrayListOf()
                    )

                    playlistArtists.clear()
                    playlistArtists.addAll(
                        incomingArtists ?: arrayListOf()
                    )

                    currentIndex =
                        intent.getIntExtra(EXTRA_INDEX, 0)
                            .coerceIn(0, playlistPaths.lastIndex)

                    playIndex(currentIndex)
                } else {
                    val path = intent.getStringExtra(EXTRA_PATH)

                    if (!path.isNullOrBlank() && path != currentPath) {
                        currentTitle =
                            intent.getStringExtra(EXTRA_TITLE)
                                ?: "Unknown title"

                        currentArtist =
                            intent.getStringExtra(EXTRA_ARTIST)
                                ?: "Unknown Artist"

                        playNewSong(path)
                    } else {
                        resumePlayback()
                    }
                }
            }

            ACTION_NEXT -> {
                playNext()
            }

            ACTION_PREVIOUS -> {
                playPrevious()
            }

            ACTION_PAUSE -> {
                pausePlayback()
            }

            ACTION_STOP -> {
                stopPlayback()
                stopForeground(true)
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private fun playNewSong(path: String) {
        try {
            mediaPlayer?.release()

            currentPath = path

            mediaPlayer = MediaPlayer().apply {
                setDataSource(path)
                prepare()
                start()

                setOnCompletionListener {
                    if (repeatEnabled && currentIndex >= 0) {
                        playIndex(currentIndex)
                    } else {
                        playNext()
                    }
                }
            }

            startForeground(
                NOTIFICATION_ID,
                createNotification()
            )

        } catch (_: Exception) {
            stopPlayback()
            stopSelf()
        }
    }

    private fun playIndex(index: Int) {
        if (playlistPaths.isEmpty()) return

        currentIndex =
            index.coerceIn(0, playlistPaths.lastIndex)

        currentTitle =
            playlistTitles.getOrElse(currentIndex) {
                "Unknown title"
            }

        currentArtist =
            playlistArtists.getOrElse(currentIndex) {
                "Unknown Artist"
            }

        playNewSong(playlistPaths[currentIndex])
    }

    private fun playNext() {
        if (playlistPaths.isEmpty()) return

        val next =
            if (shuffleEnabled && playlistPaths.size > 1) {
                var candidate: Int
                do {
                    candidate = kotlin.random.Random.nextInt(playlistPaths.size)
                } while (candidate == currentIndex)
                candidate
            } else if (currentIndex + 1 < playlistPaths.size) {
                currentIndex + 1
            } else {
                0
            }

        playIndex(next)
    }

    private fun playPrevious() {
        if (playlistPaths.isEmpty()) return

        val previous =
            if (shuffleEnabled && playlistPaths.size > 1) {
                var candidate: Int
                do {
                    candidate = kotlin.random.Random.nextInt(playlistPaths.size)
                } while (candidate == currentIndex)
                candidate
            } else if (currentIndex - 1 >= 0) {
                currentIndex - 1
            } else {
                playlistPaths.lastIndex
            }

        playIndex(previous)
    }

    private fun resumePlayback() {
        try {
            mediaPlayer?.let {
                if (!it.isPlaying) {
                    it.start()
                }

                startForeground(
                    NOTIFICATION_ID,
                    createNotification()
                )
            }
        } catch (_: Exception) {
        }
    }

    private fun pausePlayback() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.pause()
                }
            }

            updateNotification()

        } catch (_: Exception) {
        }
    }

    private fun stopPlayback() {
        try {
            mediaPlayer?.stop()
        } catch (_: Exception) {
        }

        try {
            mediaPlayer?.release()
        } catch (_: Exception) {
        }

        mediaPlayer = null
        currentPath = null
    }

    private fun createNotification(): Notification {

        val openAppIntent = Intent(this, MainActivity::class.java)

        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE
        )

        val previousIntent =
            Intent(this, MusicService::class.java).apply {
                action = ACTION_PREVIOUS
            }

        val previousPendingIntent = PendingIntent.getService(
            this,
            10,
            previousIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE
        )

        val nextIntent =
            Intent(this, MusicService::class.java).apply {
                action = ACTION_NEXT
            }

        val nextPendingIntent = PendingIntent.getService(
            this,
            11,
            nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE
        )

        val toggleIntent = Intent(this, MusicService::class.java).apply {
            action =
                if (isPlaying()) ACTION_PAUSE
                else ACTION_PLAY
        }

        val togglePendingIntent = PendingIntent.getService(
            this,
            1,
            toggleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, MusicService::class.java).apply {
            action = ACTION_STOP
        }

        val stopPendingIntent = PendingIntent.getService(
            this,
            2,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(currentTitle)
            .setContentText(currentArtist)
            .setContentIntent(openAppPendingIntent)
            .setOnlyAlertOnce(true)
            .setOngoing(isPlaying())
            .addAction(
                android.R.drawable.ic_media_previous,
                "Previous",
                previousPendingIntent
            )
            .addAction(
                if (isPlaying())
                    android.R.drawable.ic_media_pause
                else
                    android.R.drawable.ic_media_play,
                if (isPlaying()) "Pause" else "Play",
                togglePendingIntent
            )
            .addAction(
                android.R.drawable.ic_media_next,
                "Next",
                nextPendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop",
                stopPendingIntent
            )
            .build()
    }

    private fun updateNotification() {
        val manager =
            getSystemService(NOTIFICATION_SERVICE)
                as NotificationManager

        manager.notify(
            NOTIFICATION_ID,
            createNotification()
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                CHANNEL_ID,
                "Music playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Vibeify background music playback"
                setSound(null, null)
            }

            val manager =
                getSystemService(NOTIFICATION_SERVICE)
                    as NotificationManager

            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        stopPlayback()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
