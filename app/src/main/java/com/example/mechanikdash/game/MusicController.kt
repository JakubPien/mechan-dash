package com.mechanikdash.game

import android.content.Context
import android.media.MediaPlayer
import com.mechanikdash.game.R

class MusicController(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var isMuted = false

    fun start() {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(context, R.raw.sigma_soundtrack).apply {
                isLooping = true
                setVolume(0.5f, 0.5f)
            }
        }
        if (!isMuted) mediaPlayer?.start()
    }

    fun pause() {
        mediaPlayer?.pause()
    }

    fun resume() {
        if (!isMuted) mediaPlayer?.start()
    }

    fun stop() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    fun toggleMute(): Boolean {
        isMuted = !isMuted
        if (isMuted) mediaPlayer?.setVolume(0f, 0f)
        else mediaPlayer?.setVolume(0.5f, 0.5f)
        return isMuted
    }
}
