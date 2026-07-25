package com.vibeify

import android.Manifest
import android.content.pm.PackageManager
import android.database.Cursor
import android.media.MediaPlayer
import android.media.MediaMetadataRetriever
import android.graphics.BitmapFactory
import android.widget.ImageView
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.SeekBar
import android.view.View
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private lateinit var btnPlay: Button
    private lateinit var txtNowPlaying: TextView
    private lateinit var txtArtist: TextView
    private lateinit var imgAlbumArt: ImageView
    private lateinit var songList: ListView
    private lateinit var btnPrevious: Button
    private lateinit var btnNext: Button
    private lateinit var seekBar: SeekBar
    private lateinit var txtCurrentTime: TextView
    private lateinit var txtTotalTime: TextView
    private lateinit var btnShuffle: Button
    private lateinit var btnFavourite: Button
    private lateinit var btnRepeat: Button

    private var shuffleEnabled = false
    private var repeatEnabled = false

    private val favourites by lazy {
        getSharedPreferences("vibeify_favourites", MODE_PRIVATE)
    }
    private val handler = Handler(Looper.getMainLooper())

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

        val navHome = findViewById<Button>(R.id.navHome)
        val navPlayer = findViewById<Button>(R.id.navPlayer)
        val navLibrary = findViewById<Button>(R.id.navLibrary)
        val navSettings = findViewById<Button>(R.id.navSettings)

        fun showScreen(screen: View) {
            homeScreen.visibility = View.GONE
            playerScreen.visibility = View.GONE
            libraryScreen.visibility = View.GONE
            settingsScreen.visibility = View.GONE
            screen.visibility = View.VISIBLE
        }

        cardLikedSongs.setOnClickListener {
            showFavouriteSongs()
            showScreen(libraryScreen)
        }

        navHome.setOnClickListener {
            showScreen(homeScreen)
        }

        navPlayer.setOnClickListener {
            showScreen(playerScreen)
        }

        navLibrary.setOnClickListener {
            showAllSongs()
            showScreen(libraryScreen)
        }

        navSettings.setOnClickListener {
            showScreen(settingsScreen)
        }

        showScreen(homeScreen)

        btnPlay.setOnClickListener {
            val player = mediaPlayer ?: return@setOnClickListener

            if (player.isPlaying) {
                player.pause()
                btnPlay.text = "▶"
            } else {
                player.start()
                btnPlay.text = "❚❚"
            }
        }

        songList.setOnItemClickListener { _, _, position, _ ->
            playSong(position)
            showScreen(playerScreen)
        }

        btnNext.setOnClickListener {
            if (titles.isNotEmpty()) {
                playSong(getNextSongIndex())
            }
        }

        btnPrevious.setOnClickListener {
            if (titles.isNotEmpty()) {
                val previous = getPreviousSongIndex()
                playSong(previous)
            }
        }

        btnShuffle.setOnClickListener {
            shuffleEnabled = !shuffleEnabled
            updateModeButtons()
        }

        btnRepeat.setOnClickListener {
            repeatEnabled = !repeatEnabled
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
                if (fromUser) mediaPlayer?.seekTo(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        requestAudioPermission()
    }

    private fun requestAudioPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(Manifest.permission.READ_MEDIA_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                loadSongs()
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

        songList.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            titles
        )
    }

    private fun playSong(position: Int) {
        mediaPlayer?.release()

        currentSong = position
        mediaPlayer = MediaPlayer().apply {
            setDataSource(paths[position])
            prepare()
            start()
        }

        txtNowPlaying.text = titles[position]
        loadAlbumArt(paths[position])
        updateFavouriteButton()

        val artist = artists.getOrElse(position) { "Unknown Artist" }
        val album = albums.getOrElse(position) { "Unknown Album" }

        txtArtist.text = if (album == "Unknown Album") {
            artist
        } else {
            "$artist • $album"
        }

        btnPlay.text = "❚❚"

        seekBar.max = mediaPlayer?.duration ?: 0
        seekBar.progress = 0
        txtCurrentTime.text = formatTime(0L)
        txtTotalTime.text = formatTime(
            (mediaPlayer?.duration ?: durations.getOrElse(position) { 0L }.toInt()).toLong()
        )
        updateSeekBar()

        mediaPlayer?.setOnCompletionListener {
            if (titles.isNotEmpty()) {
                if (repeatEnabled && currentSong >= 0) {
                    playSong(currentSong)
                } else {
                    playSong(getNextSongIndex())
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 100 &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            loadSongs()
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

    private fun showAllSongs() {
        songList.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            titles
        )

        songList.setOnItemClickListener { _, _, position, _ ->
            playSong(position)
        }
    }

    private fun showFavouriteSongs() {
        val favouriteIndexes = paths.indices.filter { index ->
            favourites.getBoolean(paths[index], false)
        }

        val favouriteTitles = favouriteIndexes.map { index ->
            "♥ ${titles[index]}"
        }

        songList.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            favouriteTitles
        )

        songList.setOnItemClickListener { _, _, position, _ ->
            if (position in favouriteIndexes.indices) {
                playSong(favouriteIndexes[position])
            }
        }
    }

    private fun updateModeButtons() {
        btnShuffle.text = if (shuffleEnabled) "🔀 ON" else "🔀"
        btnRepeat.text = if (repeatEnabled) "🔁 ON" else "🔁"
    }

    private fun updateFavouriteButton() {
        if (currentSong < 0 || currentSong >= paths.size) {
            btnFavourite.text = "♡"
            return
        }

        val isFavourite = favourites.getBoolean(
            paths[currentSong],
            false
        )

        btnFavourite.text = if (isFavourite) "♥" else "♡"
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

    private fun updateSeekBar() {
        handler.removeCallbacksAndMessages(null)

        handler.post(object : Runnable {
            override fun run() {
                mediaPlayer?.let {
                    if (it.isPlaying) {
                        seekBar.progress = it.currentPosition
                        txtCurrentTime.text = formatTime(it.currentPosition.toLong())
                    }
                }
                handler.postDelayed(this, 500)
            }
        })
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        mediaPlayer?.release()
        mediaPlayer = null
        super.onDestroy()
    }
}
