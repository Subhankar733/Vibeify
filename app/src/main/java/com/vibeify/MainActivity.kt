package com.vibeify

import android.media.MediaPlayer
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibeify.ui.theme.*

class MainActivity : ComponentActivity() {
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VibeifyTheme {
                MusicPlayerScreen(
                    onPlayPause = { isPlaying ->
                        if (isPlaying) {
                            playSampleAudio()
                        } else {
                            mediaPlayer?.pause()
                        }
                    }
                )
            }
        }
    }

    private fun playSampleAudio() {
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

@Composable
fun VibeifyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = DarkBackground,
            surface = CardSurface,
            primary = AccentNeon
        ),
        content = content
    )
}

@Composable
fun MusicPlayerScreen(onPlayPause: (Boolean) -> Unit) {
    var isPlaying by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0.3f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DarkBackground, Color(0xFF161522), DarkBackground)
                )
            )
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "NOW PLAYING",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(top = 16.dp)
            )

            Box(
                modifier = Modifier
                    .size(280.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(AccentNeon, AccentCyan)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🎧",
                    fontSize = 80.sp
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Vibeify Waves",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Online Stream Test • Vibeify",
                    fontSize = 14.sp,
                    color = TextMuted
                )
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = progress,
                    onValueChange = { progress = it },
                    colors = SliderDefaults.colors(
                        thumbColor = AccentCyan,
                        activeTrackColor = AccentNeon,
                        inactiveTrackColor = CardSurface
                    )
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { },
                    colors = ButtonDefaults.buttonColors(containerColor = CardSurface)
                ) {
                    Text("⏮", fontSize = 20.sp, color = TextWhite)
                }

                Button(
                    onClick = {
                        isPlaying = !isPlaying
                        onPlayPause(isPlaying)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentNeon),
                    modifier = Modifier.height(56.dp)
                ) {
                    Text(if (isPlaying) "⏸" else "▶", fontSize = 24.sp, color = TextWhite)
                }

                Button(
                    onClick = { },
                    colors = ButtonDefaults.buttonColors(containerColor = CardSurface)
                ) {
                    Text("⏭", fontSize = 20.sp, color = TextWhite)
                }
            }
        }
    }
}
