package com.gymlog.app.audio

import android.media.AudioManager
import android.media.ToneGenerator
import kotlinx.coroutines.delay

/**
 * Tiny wrapper around Android's [ToneGenerator] for short audible beeps. We
 * only use it for the workout REST-timer completion signal. ToneGenerator is
 * acquired for the duration of each beep and released immediately so we don't
 * hold the audio focus across the rest of the app's lifetime.
 */
object RestAlarm {

    /**
     * Play three short beeps (the standard "your rest is over" signal).
     * Each beep is ~150 ms with ~150 ms of silence between them, then a
     * longer 400 ms beep at the end. The total duration is ~1 second.
     *
     * Safe to call from any thread; the ToneGenerator acquisition can briefly
     * fail on devices that can't allocate the audio resource, in which case
     * we silently swallow the error rather than crash mid-workout.
     */
    suspend fun beep() {
        val tone = try {
            ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
        } catch (t: Throwable) {
            null
        } ?: return
        try {
            tone.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
            delay(200)
            tone.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
            delay(200)
            tone.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
            delay(200)
            tone.startTone(ToneGenerator.TONE_PROP_BEEP, 400)
        } finally {
            tone.release()
        }
    }
}
