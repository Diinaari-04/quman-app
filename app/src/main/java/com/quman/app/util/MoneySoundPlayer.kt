package com.quman.app.util

import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.util.Log
import com.quman.app.QumanApplication
import com.quman.app.R
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking

object MoneySoundPlayer {
    private const val TAG = "MoneySoundPlayer"
    private var mediaPlayer: MediaPlayer? = null

    /**
     * Plays the pleasant, positive chime for Money IN (incoming/received transactions).
     */
    fun playMoneyInSound(context: Context) {
        playSoundResource(context, R.raw.money_in)
    }

    /**
     * Plays the short, slightly urgent alert tone for Money OUT (outgoing/sent transactions).
     */
    fun playMoneyOutSound(context: Context) {
        playSoundResource(context, R.raw.money_out)
    }

    private fun playSoundResource(context: Context, resId: Int) {
        try {
            stop()

            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

            // Check user preference for bypassing silent mode
            val app = context.applicationContext as? QumanApplication
            val bypassSilentMode = try {
                runBlocking {
                    app?.userPreferences?.isBypassSilentMode?.firstOrNull() ?: false
                }
            } catch (e: Exception) {
                false
            }

            // Check Do Not Disturb (DND) state
            val isDndActive = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && notificationManager != null) {
                val filter = notificationManager.currentInterruptionFilter
                filter != NotificationManager.INTERRUPTION_FILTER_ALL && filter != NotificationManager.INTERRUPTION_FILTER_UNKNOWN
            } else {
                false
            }

            // If DND is actively engaged, respect it by default and do not play sound
            if (isDndActive) {
                Log.d(TAG, "DND active: respecting DND, not playing sound")
                return
            }

            // If bypass silent mode is disabled, respect normal ringer modes (vibrate / silent)
            if (!bypassSilentMode && audioManager != null) {
                if (audioManager.ringerMode == AudioManager.RINGER_MODE_SILENT ||
                    audioManager.ringerMode == AudioManager.RINGER_MODE_VIBRATE
                ) {
                    Log.d(TAG, "Device in silent/vibrate mode and bypass is disabled: silent")
                    return
                }
            }

            val usage = if (bypassSilentMode) {
                // USAGE_ALARM allows the sound to play through the alarm stream even if ringer is vibrate/silent
                AudioAttributes.USAGE_ALARM
            } else {
                AudioAttributes.USAGE_NOTIFICATION_EVENT
            }

            val contentType = if (bypassSilentMode) {
                AudioAttributes.CONTENT_TYPE_SONIFICATION
            } else {
                AudioAttributes.CONTENT_TYPE_SONIFICATION
            }

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(contentType)
                        .setUsage(usage)
                        .build()
                )

                val afd = context.resources.openRawResourceFd(resId)
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()

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
            Log.e(TAG, "Failed to play sound resource $resId", e)
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
