package com.vibeify

import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import androidx.core.content.ContextCompat
import android.database.Cursor
import android.media.MediaMetadataRetriever
import android.graphics.BitmapFactory
import android.widget.ImageView
import android.widget.ImageButton
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.SeekBar
import android.view.View
import android.os.Handler
import android.os.Looper
import androidx.palette.graphics.Palette
import androidx.core.graphics.ColorUtils
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var btnPlay: ImageButton
    private lateinit var txtNowPlaying: TextView
    private lateinit var txtArtist: TextView
    private lateinit var imgAlbumArt: ImageView
    private lateinit var songList: ListView
    private lateinit var btnPrevious: ImageButton
    private lateinit var btnNext: ImageButton
    private lateinit var seekBar: SeekBar
    private lateinit var txtCurrentTime: TextView
    private lateinit var txtTotalTime: TextView
    private lateinit var btnShuffle: ImageButton
    private lateinit var btnFavourite: ImageButton
    private lateinit var btnRepeat: ImageButton

    private var shuffleEnabled = false
    private var repeatEnabled = false

    private val favourites by lazy {
        getSharedPreferences("vibeify_favourites", MODE_PRIVATE)
    }
    private val handler = Handler(Looper.getMainLooper())

    private val songs = mutableListOf<Song>()

    // Temporary compatibility bridge while playback moves to Song.
    private val titles = mutableListOf<String>()
    private val paths = mutableListOf<String>()
    private val artists = mutableListOf<String>()
    private val albums = mutableListOf<String>()
    private val durations = mutableListOf<Long>()

    private var currentSong = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnPlay = findViewById(R.id.btnPlay)
        txtNowPlaying = findViewById(R.id.txtNowPlaying)
        txtArtist = findViewById(R.id.txtArtist)
        imgAlbumArt = findViewById(R.id.imgAlbumArt)
        songList = findViewById(R.id.songList)
        btnPrevious = findViewById(R.id.btnPrevious)
        btnNext = findViewById(R.id.btnNext)
        seekBar = findViewById(R.id.seekBar)
        txtCurrentTime = findViewById(R.id.txtCurrentTime)
        txtTotalTime = findViewById(R.id.txtTotalTime)
        btnShuffle = findViewById(R.id.btnShuffle)
        btnFavourite = findViewById(R.id.btnFavourite)
        btnRepeat = findViewById(R.id.btnRepeat)

        val homeScreen = findViewById<View>(R.id.homeScreen)
        val playerScreen = findViewById<View>(R.id.playerScreen)
        val libraryScreen = findViewById<View>(R.id.libraryScreen)
        val settingsScreen = findViewById<View>(R.id.settingsScreen)

        val cardLikedSongs = findViewById<View>(R.id.cardLikedSongs)
        val cardLocalLibrary = findViewById<View>(R.id.cardLocalLibrary)
        val cardQuickPick = findViewById<View>(R.id.cardQuickPick)

        val settingsPlayback =
            findViewById<View>(R.id.settingsPlayback)
        val settingsLocalMusic =
            findViewById<View>(R.id.settingsLocalMusic)
        val settingsVisualAtmosphere =
            findViewById<View>(R.id.settingsVisualAtmosphere)
        val settingsAbout =
            findViewById<View>(R.id.settingsAbout)

        val txtLibraryTitle = findViewById<TextView>(R.id.txtLibraryTitle)
        val txtLibrarySubtitle = findViewById<TextView>(R.id.txtLibrarySubtitle)

        val navHome = findViewById<ImageButton>(R.id.navHome)
        val navPlayer = findViewById<ImageButton>(R.id.navPlayer)
        val navLibrary = findViewById<ImageButton>(R.id.navLibrary)
        val navSettings = findViewById<ImageButton>(R.id.navSettings)

        fun showScreen(screen: View) {
            homeScreen.visibility = View.GONE
            playerScreen.visibility = View.GONE
            libraryScreen.visibility = View.GONE
            settingsScreen.visibility = View.GONE
            screen.visibility = View.VISIBLE
        }

        fun updateNavigation(active: ImageButton) {
            val items = listOf(navHome, navPlayer, navLibrary, navSettings)

            items.forEach { item ->
                item.setBackgroundResource(R.drawable.v2_nav_idle)
                item.alpha = 0.48f
                item.scaleX = 0.90f
                item.scaleY = 0.90f
            }

            active.setBackgroundResource(R.drawable.v2_nav_active)
            active.alpha = 1.0f
            active.scaleX = 1.0f
            active.scaleY = 1.0f
        }

        cardLikedSongs.setOnClickListener {
            txtLibraryTitle.text = "LIKED SONGS"
            txtLibrarySubtitle.text = "Your favourite tracks"
            showFavouriteSongs()
            showScreen(libraryScreen)
        }

        cardLocalLibrary.setOnClickListener {
            txtLibraryTitle.text = "YOUR LIBRARY"
            txtLibrarySubtitle.text = "Songs on your device"
            showAllSongs()
            showScreen(libraryScreen)
        }

        cardQuickPick.setOnClickListener {
            if (titles.isNotEmpty()) {
                val index = if (currentSong >= 0 && currentSong < titles.size) {
                    currentSong
                } else {
                    0
                }

                playSong(index)
                showScreen(playerScreen)
            }
        }

        navHome.setOnClickListener {
            showScreen(homeScreen)
            updateNavigation(navHome)
        }

        navPlayer.setOnClickListener {
            showScreen(playerScreen)
            updateNavigation(navPlayer)
        }

        navLibrary.setOnClickListener {
            txtLibraryTitle.text = "YOUR LIBRARY"
            txtLibrarySubtitle.text = "Songs on your device"
            showAllSongs()
            showScreen(libraryScreen)
            updateNavigation(navLibrary)
        }

        navSettings.setOnClickListener {
            showScreen(settingsScreen)
            updateNavigation(navSettings)
        }

        settingsPlayback.setOnClickListener {
            showScreen(playerScreen)
            updateNavigation(navPlayer)

            if (MusicService.currentPath != null) {
                syncPlayerUiFromService()
                updateSeekBar()
            }
        }

        settingsLocalMusic.setOnClickListener {
            txtLibraryTitle.text = "YOUR LIBRARY"
            txtLibrarySubtitle.text = "Songs on your device"
            showAllSongs()
            showScreen(libraryScreen)
            updateNavigation(navLibrary)
        }

        settingsVisualAtmosphere.setOnClickListener {
            imgAlbumArt.animate()
                .alpha(0.65f)
                .setDuration(180)
                .withEndAction {
                    imgAlbumArt.animate()
                        .alpha(1.0f)
                        .setDuration(220)
                        .start()
                }
                .start()
        }

        settingsAbout.setOnClickListener {
            android.widget.Toast.makeText(
                this,
                "Vibeify • Local music, your way.",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }

        showScreen(homeScreen)
        updateNavigation(navHome)

        btnPlay.setOnClickListener {
            if (MusicService.currentPath == null) {
                if (titles.isNotEmpty()) {
                    val index =
                        if (currentSong in titles.indices) currentSong else 0
                    playSong(index)
                }
            } else if (MusicService.isPlaying()) {
                sendPlaybackAction(MusicService.ACTION_PAUSE)
                btnPlay.setImageResource(R.drawable.ic_play)
            } else {
                sendPlaybackAction(MusicService.ACTION_PLAY)
                btnPlay.setImageResource(R.drawable.ic_pause)
            }
        }

        btnNext.setOnClickListener {
            if (MusicService.currentPath != null) {
                sendPlaybackAction(MusicService.ACTION_NEXT)
            } else if (titles.isNotEmpty()) {
                playSong(0)
            }
        }

        btnPrevious.setOnClickListener {
            if (MusicService.currentPath != null) {
                sendPlaybackAction(MusicService.ACTION_PREVIOUS)
            } else if (titles.isNotEmpty()) {
                playSong(0)
            }
        }

        btnShuffle.setOnClickListener {
            MusicService.toggleShuffle()
            shuffleEnabled = MusicService.shuffleEnabled
            updateModeButtons()
        }

        btnRepeat.setOnClickListener {
            MusicService.toggleRepeat()
            repeatEnabled = MusicService.repeatEnabled
            updateModeButtons()
        }

        btnFavourite.setOnClickListener {
            if (currentSong >= 0 && currentSong < paths.size) {
                val path = paths[currentSong]
                val isFavourite = favourites.getBoolean(path, false)

                favourites.edit()
                    .putBoolean(path, !isFavourite)
                    .apply()

                updateFavouriteButton()
            }
        }

        updateModeButtons()
        updateFavouriteButton()

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) MusicService.seekTo(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        requestAudioPermission()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                101
            )
        }
    }

    private fun requestAudioPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(Manifest.permission.READ_MEDIA_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                loadSongs()
                requestNotificationPermission()
            } else {
                requestPermissions(
                    arrayOf(Manifest.permission.READ_MEDIA_AUDIO),
                    100
                )
            }
        } else {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                loadSongs()
            } else {
                requestPermissions(
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    100
                )
            }
        }
    }

    private fun loadSongs() {
        titles.clear()
        paths.clear()
        artists.clear()
        albums.clear()
        durations.clear()

        val projection = arrayOf(
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION
        )

        val cursor: Cursor? = contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            MediaStore.Audio.Media.IS_MUSIC + " != 0",
            null,
            MediaStore.Audio.Media.TITLE + " ASC"
        )

        cursor?.use {
            val titleIndex = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val dataIndex = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val artistIndex = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumIndex = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationIndex = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

            while (it.moveToNext()) {
                titles.add(it.getString(titleIndex) ?: "Unknown title")
                paths.add(it.getString(dataIndex))

                val artist = it.getString(artistIndex)
                val album = it.getString(albumIndex)

                artists.add(
                    if (artist.isNullOrBlank() || artist == "<unknown>")
                        "Unknown Artist"
                    else artist
                )

                albums.add(
                    if (album.isNullOrBlank())
                        "Unknown Album"
                    else album
                )

                durations.add(it.getLong(durationIndex))
            }
        }

        songs.clear()

        paths.indices.forEach { index ->
            songs.add(
                Song(
                    title = titles[index],
                    path = paths[index],
                    artist = artists[index],
                    album = albums[index],
                    duration = durations[index]
                )
            )
        }

        songList.adapter = SongAdapter(this, songs)
    }

    private fun playSong(position: Int) {
        if (position !in paths.indices) return

        currentSong = position

        val title = titles.getOrElse(position) { "Unknown title" }
        val artist = artists.getOrElse(position) { "Unknown Artist" }
        val album = albums.getOrElse(position) { "Unknown Album" }

        txtNowPlaying.text = title
        txtArtist.text =
            if (album == "Unknown Album") artist
            else "$artist • $album"

        loadAlbumArt(paths[position])
        updateFavouriteButton()

        val intent = Intent(this, MusicService::class.java).apply {
            action = MusicService.ACTION_PLAY

            putStringArrayListExtra(
                MusicService.EXTRA_PATHS,
                ArrayList(paths)
            )

            putStringArrayListExtra(
                MusicService.EXTRA_TITLES,
                ArrayList(titles)
            )

            putStringArrayListExtra(
                MusicService.EXTRA_ARTISTS,
                ArrayList(artists)
            )

            putExtra(
                MusicService.EXTRA_INDEX,
                position
            )
        }

        ContextCompat.startForegroundService(this, intent)

        btnPlay.setImageResource(R.drawable.ic_pause)

        val knownDuration =
            durations.getOrElse(position) { 0L }
                .coerceAtMost(Int.MAX_VALUE.toLong())
                .toInt()

        seekBar.max = knownDuration
        seekBar.progress = 0

        txtCurrentTime.text = formatTime(0L)
        txtTotalTime.text = formatTime(knownDuration.toLong())

        updateSeekBar()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 100) {
            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                loadSongs()
            }

            requestNotificationPermission()
        }
    }


    private fun getNextSongIndex(): Int {
        if (titles.isEmpty()) return 0

        if (shuffleEnabled && titles.size > 1) {
            var next: Int
            do {
                next = kotlin.random.Random.nextInt(titles.size)
            } while (next == currentSong)

            return next
        }

        return if (currentSong + 1 < titles.size) {
            currentSong + 1
        } else {
            0
        }
    }

    private fun getPreviousSongIndex(): Int {
        if (titles.isEmpty()) return 0

        if (shuffleEnabled && titles.size > 1) {
            var previous: Int
            do {
                previous = kotlin.random.Random.nextInt(titles.size)
            } while (previous == currentSong)

            return previous
        }

        return if (currentSong - 1 >= 0) {
            currentSong - 1
        } else {
            titles.size - 1
        }
    }

    private fun openSongFromLibrary(song: Song) {
        val index = songs.indexOfFirst { it.path == song.path }

        if (index < 0) return

        playSong(index)

        findViewById<View>(R.id.homeScreen).visibility = View.GONE
        findViewById<View>(R.id.libraryScreen).visibility = View.GONE
        findViewById<View>(R.id.settingsScreen).visibility = View.GONE
        findViewById<View>(R.id.playerScreen).visibility = View.VISIBLE

        val navItems = listOf(
            findViewById<ImageButton>(R.id.navHome),
            findViewById<ImageButton>(R.id.navPlayer),
            findViewById<ImageButton>(R.id.navLibrary),
            findViewById<ImageButton>(R.id.navSettings)
        )

        navItems.forEach {
            it.setBackgroundResource(R.drawable.v10_nav_idle)
            it.alpha = 0.58f
        }

        findViewById<ImageButton>(R.id.navPlayer).apply {
            setBackgroundResource(R.drawable.v10_nav_active)
            alpha = 1.0f
        }
    }

    private fun showAllSongs() {
        songList.adapter = SongAdapter(this, songs)

        songList.setOnItemClickListener { _, _, position, _ ->
            val selected =
                songs.getOrNull(position)
                    ?: return@setOnItemClickListener

            openSongFromLibrary(selected)
        }
    }

    private fun showFavouriteSongs() {
        val favouriteSongs = songs.filter { song ->
            favourites.getBoolean(song.path, false)
        }

        songList.adapter = SongAdapter(this, favouriteSongs)

        songList.setOnItemClickListener { _, _, position, _ ->
            val selected =
                favouriteSongs.getOrNull(position)
                    ?: return@setOnItemClickListener

            openSongFromLibrary(selected)
        }
    }

    private fun updateModeButtons() {
        shuffleEnabled = MusicService.shuffleEnabled
        repeatEnabled = MusicService.repeatEnabled


        btnShuffle.setBackgroundResource(
            if (shuffleEnabled)
                R.drawable.v10_mode_on
            else
                R.drawable.v10_mode_off
        )

        btnRepeat.setBackgroundResource(
            if (repeatEnabled)
                R.drawable.v10_mode_on
            else
                R.drawable.v10_mode_off
        )

        btnShuffle.alpha =
            if (shuffleEnabled) 1.0f else 0.72f

        btnRepeat.alpha =
            if (repeatEnabled) 1.0f else 0.72f
    }

    private fun updateFavouriteButton() {
        if (currentSong < 0 || currentSong >= paths.size) {
            btnFavourite.setImageResource(R.drawable.ic_favourite)
            return
        }

        val isFavourite = favourites.getBoolean(
            paths[currentSong],
            false
        )

        btnFavourite.setImageResource(R.drawable.ic_favourite)
        btnFavourite.setBackgroundResource(
            if (isFavourite)
                R.drawable.v10_mode_on
            else
                R.drawable.v10_mode_off
        )
        btnFavourite.alpha = if (isFavourite) 1.0f else 0.72f
    }

    private fun applyAlbumPalette(bitmap: Bitmap) {
        Palette.from(bitmap)
            .maximumColorCount(24)
            .generate { palette ->

                if (palette == null || isFinishing || isDestroyed) {
                    return@generate
                }

                val rawAccent =
                    palette.vibrantSwatch?.rgb
                        ?: palette.lightVibrantSwatch?.rgb
                        ?: palette.mutedSwatch?.rgb
                        ?: palette.darkVibrantSwatch?.rgb
                        ?: palette.dominantSwatch?.rgb
                        ?: Color.rgb(180, 190, 210)

                // Keep the album's personality, but prevent unusably dark accents.
                val accent =
                    if (ColorUtils.calculateLuminance(rawAccent) < 0.16) {
                        ColorUtils.blendARGB(rawAccent, Color.WHITE, 0.34f)
                    } else {
                        rawAccent
                    }

                val deep =
                    ColorUtils.blendARGB(accent, Color.BLACK, 0.78f)

                val ambient =
                    ColorUtils.setAlphaComponent(accent, 72)

                val glow =
                    ColorUtils.setAlphaComponent(accent, 155)

                val dock =
                    ColorUtils.setAlphaComponent(deep, 220)

                val playerAmbient =
                    findViewById<View>(R.id.playerAmbient)

                val playerArtGlow =
                    findViewById<View>(R.id.playerArtGlow)

                val playerPaletteOrb =
                    findViewById<View>(R.id.playerPaletteOrb)

                val playerVibeLine =
                    findViewById<View>(R.id.playerVibeLine)

                val playerControlDock =
                    findViewById<View>(R.id.playerControlDock)

                // V2 GLOBAL ALBUM ATMOSPHERE
                val rebornRoot =
                    findViewById<View>(R.id.rebornRoot)

                val homeScreen =
                    findViewById<View>(R.id.homeScreen)

                val playerScreen =
                    findViewById<View>(R.id.playerScreen)

                val libraryScreen =
                    findViewById<View>(R.id.libraryScreen)

                val settingsScreen =
                    findViewById<View>(R.id.settingsScreen)

                val rebornNav =
                    findViewById<View>(R.id.rebornNav)

                val vibeDockArea =
                    findViewById<View>(R.id.vibeDockArea)

                val globalCanvas =
                    androidx.core.graphics.ColorUtils.blendARGB(
                        android.graphics.Color.rgb(7, 7, 9),
                        accent,
                        0.14f
                    )

                val screenAtmosphere =
                    androidx.core.graphics.ColorUtils.blendARGB(
                        android.graphics.Color.rgb(8, 8, 10),
                        accent,
                        0.09f
                    )

                val dockAtmosphere =
                    androidx.core.graphics.ColorUtils.blendARGB(
                        android.graphics.Color.rgb(15, 15, 18),
                        accent,
                        0.22f
                    )

                rebornRoot.setBackgroundColor(globalCanvas)

                homeScreen.setBackgroundColor(screenAtmosphere)
                playerScreen.setBackgroundColor(screenAtmosphere)
                libraryScreen.setBackgroundColor(screenAtmosphere)
                settingsScreen.setBackgroundColor(screenAtmosphere)

                vibeDockArea.setBackgroundColor(globalCanvas)

                rebornNav.backgroundTintList =
                    ColorStateList.valueOf(dockAtmosphere)

                playerAmbient.backgroundTintList =
                    ColorStateList.valueOf(ambient)

                playerArtGlow.backgroundTintList =
                    ColorStateList.valueOf(glow)

                playerPaletteOrb.backgroundTintList =
                    ColorStateList.valueOf(accent)

                playerVibeLine.backgroundTintList =
                    ColorStateList.valueOf(accent)

                playerControlDock.backgroundTintList =
                    ColorStateList.valueOf(dock)

                btnPlay.backgroundTintList =
                    ColorStateList.valueOf(accent)

                seekBar.progressTintList =
                    ColorStateList.valueOf(accent)

                seekBar.thumbTintList =
                    ColorStateList.valueOf(accent)
            }
    }

    private fun loadAlbumArt(path: String) {
        val retriever = MediaMetadataRetriever()

        try {
            retriever.setDataSource(path)

            val art = retriever.embeddedPicture

            if (art != null) {
                val bitmap = BitmapFactory.decodeByteArray(
                    art,
                    0,
                    art.size
                )

                if (bitmap != null) {
                    imgAlbumArt.setImageBitmap(bitmap)
                    imgAlbumArt.scaleType = ImageView.ScaleType.CENTER_CROP
                    applyAlbumPalette(bitmap)
                    return
                }
            }

            showDefaultAlbumArt()

        } catch (e: Exception) {
            showDefaultAlbumArt()
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
    }

    private fun showDefaultAlbumArt() {
        imgAlbumArt.setImageDrawable(null)
        imgAlbumArt.setBackgroundResource(R.drawable.album_art)
        imgAlbumArt.scaleType = ImageView.ScaleType.CENTER
    }

    private fun formatTime(milliseconds: Long): String {
        val totalSeconds = milliseconds / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%d:%02d", minutes, seconds)
    }

    private fun syncPlayerUiFromService() {
        val serviceIndex = MusicService.currentIndex()

        if (serviceIndex in paths.indices) {
            currentSong = serviceIndex

            val title = titles.getOrElse(serviceIndex) {
                MusicService.currentTitle
            }

            val artist = artists.getOrElse(serviceIndex) {
                MusicService.currentArtist
            }

            val album = albums.getOrElse(serviceIndex) {
                "Unknown Album"
            }

            txtNowPlaying.text = title

            txtArtist.text =
                if (album == "Unknown Album") {
                    artist
                } else {
                    "$artist • $album"
                }

            loadAlbumArt(paths[serviceIndex])
            updateFavouriteButton()
        } else if (MusicService.currentPath != null) {
            txtNowPlaying.text = MusicService.currentTitle
            txtArtist.text = MusicService.currentArtist

            MusicService.currentPath?.let { path ->
                loadAlbumArt(path)
            }
        }

        val duration = MusicService.duration()
        val position = MusicService.position()

        if (duration > 0) {
            seekBar.max = duration
            txtTotalTime.text = formatTime(duration.toLong())
        }

        seekBar.progress = position.coerceAtMost(seekBar.max)
        txtCurrentTime.text = formatTime(position.toLong())

        btnPlay.setImageResource(
            if (MusicService.isPlaying())
                R.drawable.ic_pause
            else
                R.drawable.ic_play
        )
    }

    override fun onResume() {
        super.onResume()

        if (MusicService.currentPath != null) {
            syncPlayerUiFromService()
            updateSeekBar()
        }
    }

    private fun updateSeekBar() {
        handler.removeCallbacksAndMessages(null)

        handler.post(object : Runnable {
            override fun run() {
                val serviceIndex = MusicService.currentIndex()

                if (serviceIndex in paths.indices &&
                    serviceIndex != currentSong
                ) {
                    currentSong = serviceIndex

                    val title =
                        titles.getOrElse(serviceIndex) {
                            MusicService.currentTitle
                        }

                    val artist =
                        artists.getOrElse(serviceIndex) {
                            MusicService.currentArtist
                        }

                    val album =
                        albums.getOrElse(serviceIndex) {
                            "Unknown Album"
                        }

                    txtNowPlaying.text = title

                    txtArtist.text =
                        if (album == "Unknown Album") {
                            artist
                        } else {
                            "$artist • $album"
                        }

                    loadAlbumArt(paths[serviceIndex])
                    updateFavouriteButton()
                }

                val duration = MusicService.duration()
                val position = MusicService.position()

                if (duration > 0) {
                    seekBar.max = duration
                    txtTotalTime.text =
                        formatTime(duration.toLong())
                }

                seekBar.progress =
                    position.coerceAtMost(seekBar.max)

                txtCurrentTime.text =
                    formatTime(position.toLong())

                btnPlay.setImageResource(
            if (MusicService.isPlaying())
                R.drawable.ic_pause
            else
                R.drawable.ic_play
        )

                handler.postDelayed(this, 500)
            }
        })
    }

    private fun sendPlaybackAction(actionName: String) {
        val intent = Intent(this, MusicService::class.java).apply {
            action = actionName
        }

        startService(intent)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
