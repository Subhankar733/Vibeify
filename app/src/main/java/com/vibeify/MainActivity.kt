package com.vibeify

import android.media.MediaPlayer
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnPlay = findViewById<Button>(R.id.btnPlay)

        btnPlay.setOnClickListener {
            if (!isPlaying) {
                playAudio()
                btnPlay.text = "PAUSE ⏸"
                isPlaying = true
                Toast.makeText(this, "Playing Vibeify Music!", Toast.LENGTH_SHORT).show()
            } else {
                mediaPlayer?.pause()
                btnPlay.text = "PLAY VIBE 🎧"
                isPlaying = false
            }
        }
    }

    private fun playAudio() {
        if (mediaPlayer == null) {
            val sampleUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"
            mediaPlayer = MediaPlayer().apply {
                setDataSource(sampleUrl)
                prepareAsync()
                setOnPreparedListener { start() }
            }
        } else {
            mediaPlayer?.start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
