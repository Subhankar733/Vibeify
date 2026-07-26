package com.vibeify

data class Song(
    val title: String,
    val path: String,
    val artist: String,
    val album: String,
    val duration: Long
) {
    val subtitle: String
        get() = when {
            artist == "Unknown Artist" && album == "Unknown Album" ->
                "Local audio"

            album == "Unknown Album" ->
                artist

            artist == "Unknown Artist" ->
                album

            else ->
                "$artist • $album"
        }

    val durationText: String
        get() {
            val totalSeconds = duration.coerceAtLeast(0L) / 1000L
            val minutes = totalSeconds / 60L
            val seconds = totalSeconds % 60L
            return "%d:%02d".format(minutes, seconds)
        }
}
