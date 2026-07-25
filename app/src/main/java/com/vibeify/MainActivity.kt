package com.vibeify

import android.Manifest
import android.content.pm.PackageManager
import android.database.Cursor
import android.media.MediaPlayer
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
    private lateinit var songList: ListView
    private lateinit var btnPrevious: Button
    private lateinit var btnNext: Button
    private lateinit var seekBar: SeekBar
    private val handler = Handler(Looper.getMainLooper())

    private val titles = mutableListOf<String>()
    private val paths = mutableListOf<String>()
    private var currentSong = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnPlay = findViewById(R.id.btnPlay)
        txtNowPlaying = findViewById(R.id.txtNowPlaying)
        txtArtist = findViewById(R.id.txtArtist)
        songList = findViewById(R.id.songList)
        btnPrevious = findViewById(R.id.btnPrevious)
        btnNext = findViewById(R.id.btnNext)
        seekBar = findViewById(R.id.seekBar)

        val homeScreen = findViewById<View>(R.id.homeScreen)
        val playerScreen = findViewById<View>(R.id.playerScreen)
        val libraryScreen = findViewById<View>(R.id.libraryScreen)
        val settingsScreen = findViewById<View>(R.id.settingsScreen)

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

        navHome.setOnClickListener {
            showScreen(homeScreen)
        }

        navPlayer.setOnClickListener {
            showScreen(playerScreen)
        }

        navLibrary.setOnClickListener {
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
                val next = if (currentSong + 1 < titles.size) currentSong + 1 else 0
                playSong(next)
            }
        }

        btnPrevious.setOnClickListener {
            if (titles.isNotEmpty()) {
                val previous = if (currentSong - 1 >= 0) currentSong - 1 else titles.size - 1
                playSong(previous)
            }
        }

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

        val projection = arrayOf(
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DATA
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

            while (it.moveToNext()) {
                titles.add(it.getString(titleIndex))
                paths.add(it.getString(dataIndex))
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
        btnPlay.text = "❚❚"

        seekBar.max = mediaPlayer?.duration ?: 0
        updateSeekBar()

        mediaPlayer?.setOnCompletionListener {
            if (titles.isNotEmpty()) {
                val next = if (currentSong + 1 < titles.size) currentSong + 1 else 0
                playSong(next)
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


    private fun updateSeekBar() {
        handler.removeCallbacksAndMessages(null)

        handler.post(object : Runnable {
            override fun run() {
                mediaPlayer?.let {
                    if (it.isPlaying) {
                        seekBar.progress = it.currentPosition
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
