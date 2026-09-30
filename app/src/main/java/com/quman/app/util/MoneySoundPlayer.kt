package com.quman.app.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.util.Log
import com.quman.app.R

object MoneySoundPlayer {
    private const val TAG = "MoneySoundPlayer"
    private var mediaPlayer: MediaPlayer? = null

    /**
     * Plays the distinctive, loud money alert sound (R.raw.money_alert) for
     * incoming (Green) or outgoing (Red) money transactions.
     */
    fun playMoneyAlertSound(context: Context) {
        try {
            // Stop any currently running instance
            stop()

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                        .setLegacyStreamType(AudioManager.STREAM_NOTIFICATION)
                        .build()
                )

                val afd = context.resources.openRawResourceFd(R.raw.money_alert)
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()

                // Set full volume for distinctive, loud alerts
                setVolume(1.0f, 1.0f)

                setOnCompletionListener { mp ->
                    try {
                        mp.release()
                    } catch (e: Exception) {
                        Log.w(TAG, "Error releasing MediaPlayer: ${e.message}")
                    }
                    if (mediaPlayer === mp) {
                        mediaPlayer = null
                    }
                }

                setOnErrorListener { mp, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    try {
                        mp.release()
                    } catch (e: Exception) {
                        Log.w(TAG, "Error releasing on error: ${e.message}")
                    }
                    if (mediaPlayer === mp) {
                        mediaPlayer = null
                    }
                    true
                }

                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play money alert sound", e)
        }
    }

    fun stop() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping sound", e)
        } finally {
            mediaPlayer = null
        }
    }
}
