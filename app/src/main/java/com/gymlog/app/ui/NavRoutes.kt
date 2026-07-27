package com.gymlog.app.ui

import com.gymlog.app.ui.screens.NewExercisePrefill

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Exercises : Screen("exercises")
    data object ExerciseNew : Screen("exercises/new?prefillName={prefillName}&prefillCategory={prefillCategory}") {
        // Pre-fill args are encoded into the URL; -1L = no preset id, "_" = empty string.
        fun build(prefill: NewExercisePrefill? = null): String {
            if (prefill == null) return "exercises/new?prefillName=_&prefillCategory=_"
            val cat = prefill.category.name
            return "exercises/new?prefillName=${java.net.URLEncoder.encode(prefill.name, "UTF-8")}&prefillCategory=$cat"
        }
    }
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
