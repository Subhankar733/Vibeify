package com.vibeify

import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class SongAdapter(
    context: Context,
    private val songs: List<Song>
) : ArrayAdapter<Song>(context, R.layout.item_song, songs) {

    override fun getView(
        position: Int,
        convertView: View?,
        parent: ViewGroup
    ): View {

        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_song, parent, false)

        val song = songs[position]

        val mark = view.findViewById<TextView>(R.id.songRowMark)
        val title = view.findViewById<TextView>(R.id.songRowTitle)
        val meta = view.findViewById<TextView>(R.id.songRowMeta)
        val duration = view.findViewById<TextView>(R.id.songRowDuration)

        val isPlaying =
            MusicService.currentPath == song.path &&
            MusicService.isPlaying()

        val isCurrent =
            MusicService.currentPath == song.path

        mark.text =
            if (isPlaying) "♪"
            else "%02d".format(position + 1)

        title.text = song.title
        meta.text =
            if (isPlaying) "NOW PLAYING  •  ${song.subtitle}"
            else if (isCurrent) "PAUSED  •  ${song.subtitle}"
            else song.subtitle

        duration.text = song.durationText

        title.alpha = if (isCurrent) 1.0f else 0.90f
        meta.alpha = if (isCurrent) 1.0f else 0.72f
        mark.alpha = if (isCurrent) 1.0f else 0.55f

        title.setTypeface(
            null,
            if (isCurrent) Typeface.BOLD
            else Typeface.NORMAL
        )

        return view
    }
}
