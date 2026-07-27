package com.gymlog.app.ui

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Exercises : Screen("exercises")
    data object ExerciseNew : Screen("exercises/new")
    data object ExerciseDetail : Screen("exercises/detail/{id}") {
        fun build(id: Long) = "exercises/detail/$id"
    }
    data object Presets : Screen("presets")
    data object PresetDetail : Screen("presets/detail/{id}") {
        fun build(id: Long) = "presets/detail/$id"
    }
    data object PresetEdit : Screen("presets/edit/{id}") {
        fun build(id: Long) = "presets/edit/$id"
    }
    data object Sessions : Screen("sessions")
    data object SessionDetail : Screen("sessions/detail/{id}") {
        fun build(id: Long) = "sessions/detail/$id"
    }
    data object NewSession : Screen("sessions/new/{presetId}") {
        fun build(presetId: Long? = null) = "sessions/new/${presetId ?: -1L}"
    }
    data object Settings : Screen("settings")
}

/** Stable key used by the bottom navigation tab. */
const val TAB_NEW_SESSION = "tab_new_session"
