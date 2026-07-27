package com.gymlog.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * User preferences that don't fit naturally into the relational schema:
 * defaults the user picks once and that drive UI behaviour (e.g. the REST
 * timer's default length).
 *
 * Backed by SharedPreferences. Defaults to 60s rest which is what most lifters
 * use between sets of compound movements.
 */
class AppPrefs(context: Context) {
    private val sp: SharedPreferences =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    private val _restSeconds = MutableStateFlow(sp.getInt(KEY_REST_SECONDS, 60).coerceIn(5, 600))
    /** Default rest period used when the user taps the REST button on the workout screen. */
    val restSeconds: StateFlow<Int> = _restSeconds.asStateFlow()

    fun setRestSeconds(value: Int) {
        val clamped = value.coerceIn(5, 600)
        sp.edit().putInt(KEY_REST_SECONDS, clamped).apply()
        _restSeconds.value = clamped
    }

    companion object {
        private const val FILE = "gym_log_prefs"
        private const val KEY_REST_SECONDS = "default_rest_seconds"
    }
}
