package com.gymlog.app

import android.content.Context
import com.gymlog.app.data.Exercise
import com.gymlog.app.data.ExerciseCategory
import com.gymlog.app.data.MachineSettingDef
import com.gymlog.app.data.Preset
import com.gymlog.app.data.PresetExercise
import com.gymlog.app.data.Repository
import com.gymlog.app.data.Session
import com.gymlog.app.data.SessionExercise
import com.gymlog.app.data.SessionSet
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * One-time idempotent seeders.
 *
 * Each seeder is gated by a separate SharedPreferences flag so future seeds don't
 * re-run the original work but always run themselves exactly once per install.
 */
object SampleWorkoutSeeder {

    private const val PREFS = "gym_log_seed"
    private const val KEY_SEEDED_V1_WORKOUT = "seeded_v1_workout_2026_07_26"
    private const val KEY_SEEDED_V1_ROUTINE = "seeded_v1_routine_basic_upper_body"

    /** Returns true if any seeding actually ran. */
    fun runIfNeeded(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        var ranAny = false

        if (!prefs.getBoolean(KEY_SEEDED_V1_WORKOUT, false)) {
            runBlocking { seedReferenceWorkout() }
            prefs.edit().putBoolean(KEY_SEEDED_V1_WORKOUT, true).apply()
            ranAny = true
        }
        if (!prefs.getBoolean(KEY_SEEDED_V1_ROUTINE, false)) {
            runBlocking { seedBasicUpperBodyRoutine() }
            prefs.edit().putBoolean(KEY_SEEDED_V1_ROUTINE, true).apply()
            ranAny = true
        }
        return ranAny
    }

    /** Darren's 7/26/2026 workout: Treadmill warmup → Rowing → Triceps → Fly → Pull Down → Treadmill cooldown. */
    private suspend fun seedReferenceWorkout() {
        val ctx = AppContext.context!!
        val repo = Repository(ctx)

        // 07/26/2026 12:00 noon local time.
        val cal = java.util.Calendar.getInstance().apply {
            set(2026, java.util.Calendar.JULY, 26, 12, 0, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val sessionTime = cal.timeInMillis

        // --- Create / reuse exercises + their machine setting defs ---
        suspend fun ensureExercise(name: String, category: ExerciseCategory, settings: List<String>): Long {
            val existing = repo.exerciseDao.observeAll().first().firstOrNull { it.name == name && it.category == category }
            if (existing != null) return existing.id
            val newId = repo.addExercise(Exercise(name = name, category = category, notes = "Seeded from 07/26/2026 workout log"))
            settings.forEach { def ->
                repo.addSettingDef(MachineSettingDef(exerciseId = newId, name = def))
            }
            return newId
        }

        val treadmillId = ensureExercise("Treadmill",       ExerciseCategory.CARDIO, listOf("Speed", "Incline", "Duration"))
        val rowingId     = ensureExercise("Rowing Machine", ExerciseCategory.WEIGHT_MACHINE, listOf("Seat height", "Chest pad depth"))
        val tricepsId   = ensureExercise("Triceps",         ExerciseCategory.WEIGHT_MACHINE, listOf("Seat height"))
        val flyId       = ensureExercise("Fly",             ExerciseCategory.WEIGHT_MACHINE, listOf("Arms position", "Seat height"))
        val pullId      = ensureExercise("Pull Down",       ExerciseCategory.WEIGHT_MACHINE, listOf("Seat height"))

        val sessionId = repo.createSession(
            Session(
                date = sessionTime,
                name = "Workout · 7/26/2026",
                presetId = null
            )
        )

        fun settingsJson(m: Map<String, String>): String {
            val obj = org.json.JSONObject()
            m.forEach { (k, v) -> obj.put(k, v) }
            return obj.toString()
        }
        suspend fun addSe(exerciseId: Long, position: Int): Long =
            repo.addSessionExercise(SessionExercise(sessionId = sessionId, exerciseId = exerciseId, position = position))
        suspend fun addStrengthSet(seId: Long, n: Int, weight: Double, reps: Int, settings: Map<String, String>) {
            repo.addSet(SessionSet(
                sessionExerciseId = seId,
                setNumber = n,
                reps = reps,
                weight = weight,
                settingsValues = settingsJson(settings),
                durationSeconds = null,
                distance = null,
                completed = true
            ))
        }
        suspend fun addCardioSet(seId: Long, durationMin: Int, settings: Map<String, String>) {
            repo.addSet(SessionSet(
                sessionExerciseId = seId,
                setNumber = 1,
                reps = null,
                weight = null,
                settingsValues = settingsJson(settings),
                durationSeconds = durationMin * 60,
                distance = null,
                completed = true
            ))
        }

        // Treadmill warmup
        addSe(treadmillId, 0).also { seId ->
            addCardioSet(seId, 20, mapOf("Speed" to "2.8", "Incline" to "6"))
        }
        // Rowing machine
        addSe(rowingId, 1).also { seId ->
            addStrengthSet(seId, 1, 85.0, 20, mapOf("Seat height" to "3", "Chest pad depth" to "3"))
            addStrengthSet(seId, 2, 100.0, 15, mapOf("Seat height" to "3", "Chest pad depth" to "3"))
            addStrengthSet(seId, 3, 100.0, 15, mapOf("Seat height" to "3", "Chest pad depth" to "3"))
        }
        // Triceps
        addSe(tricepsId, 2).also { seId ->
            addStrengthSet(seId, 1, 55.0, 15, mapOf("Seat height" to "6"))
            addStrengthSet(seId, 2, 55.0, 15, mapOf("Seat height" to "6"))
            addStrengthSet(seId, 3, 55.0, 12, mapOf("Seat height" to "6"))
        }
        // Fly
        addSe(flyId, 3).also { seId ->
            addStrengthSet(seId, 1, 70.0, 15, mapOf("Arms position" to "3", "Seat height" to "6"))
            addStrengthSet(seId, 2, 70.0, 12, mapOf("Arms position" to "3", "Seat height" to "6"))
            addStrengthSet(seId, 3, 70.0, 10, mapOf("Arms position" to "3", "Seat height" to "6"))
        }
        // Pull Down
        addSe(pullId, 4).also { seId ->
            addStrengthSet(seId, 1, 55.0, 20, mapOf("Seat height" to "4"))
            addStrengthSet(seId, 2, 70.0, 12, mapOf("Seat height" to "4"))
            addStrengthSet(seId, 3, 70.0, 12, mapOf("Seat height" to "4"))
        }
        // Treadmill cooldown
        addSe(treadmillId, 5).also { seId ->
            addCardioSet(seId, 10, mapOf("Speed" to "2.6", "Incline" to "6"))
        }
    }

    /**
     * Preset called "Basic Upper Body" — uses the four weight-machine exercises from
     * the user's 7/26/2026 log (Rowing Machine, Triceps, Fly, Pull Down) with sensible
     * default weights/reps and 3 sets each.
     *
     * Only created if a preset by this name does not already exist (so the user could
     * delete it and re-trigger the seed by clearing app data).
     */
    private suspend fun seedBasicUpperBodyRoutine() {
        val ctx = AppContext.context!!
        val repo = Repository(ctx)

        val existing = repo.presets().first().firstOrNull { it.name.equals("Basic Upper Body", ignoreCase = true) }
        if (existing != null) return

        suspend fun ensureExercise(name: String, category: ExerciseCategory, settings: List<String>): Long {
            val match = repo.exerciseDao.observeAll().first()
                .firstOrNull { it.name == name && it.category == category }
            if (match != null) return match.id
            val newId = repo.addExercise(Exercise(name = name, category = category, notes = "Basic Upper Body routine"))
            settings.forEach { repo.addSettingDef(MachineSettingDef(exerciseId = newId, name = it)) }
            return newId
        }

        val rowingId  = ensureExercise("Rowing Machine", ExerciseCategory.WEIGHT_MACHINE, listOf("Seat height", "Chest pad depth"))
        val tricepsId = ensureExercise("Triceps",         ExerciseCategory.WEIGHT_MACHINE, listOf("Seat height"))
        val flyId     = ensureExercise("Fly",             ExerciseCategory.WEIGHT_MACHINE, listOf("Arms position", "Seat height"))
        val pullId    = ensureExercise("Pull Down",       ExerciseCategory.WEIGHT_MACHINE, listOf("Seat height"))

        val presetId = repo.addPreset(Preset(name = "Basic Upper Body"))

        data class PresetSpec(val exerciseId: Long, val sets: Int, val defaultWeight: Double?, val defaultReps: Int?)
        val specs = listOf(
            PresetSpec(rowingId,  sets = 3, defaultWeight = 100.0, defaultReps = 15),
            PresetSpec(tricepsId, sets = 3, defaultWeight = 55.0,  defaultReps = 12),
            PresetSpec(flyId,     sets = 3, defaultWeight = 70.0,  defaultReps = 12),
            PresetSpec(pullId,    sets = 3, defaultWeight = 70.0,  defaultReps = 12)
        )
        specs.forEachIndexed { idx, spec ->
            repo.addPresetExercise(
                PresetExercise(
                    presetId = presetId,
                    exerciseId = spec.exerciseId,
                    defaultWeight = spec.defaultWeight,
                    defaultReps = spec.defaultReps,
                    defaultSets = spec.sets,
                    position = idx
                )
            )
        }
    }
}

/**
 * Process-global app Context, set in [GymLogApplication.onCreate].
 * Needed because the seeder's static entry point doesn't have a Context otherwise.
 */
object AppContext {
    @Volatile var context: Context? = null
}
