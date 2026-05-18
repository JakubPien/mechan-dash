package com.mechanikdash.game.game

import android.content.Context
import android.media.MediaPlayer
import com.mechanikdash.game.R

class MusicController(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private var isMuted = false

    fun start() {
        if (mediaPlayer == null) {
            try {
                mediaPlayer = MediaPlayer.create(context, R.raw.soundtrack)?.apply {
                    isLooping = true
                    setVolume(0.5f, 0.5f)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (!isMuted) mediaPlayer?.start()
    }

    fun pause() { mediaPlayer?.pause() }
    fun resume() { if (!isMuted) mediaPlayer?.start() }
    fun stop() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    fun toggleMute(): Boolean {
        isMuted = !isMuted
        val volume = if (isMuted) 0f else 0.5f
        mediaPlayer?.setVolume(volume, volume)
        return isMuted
    }
}
