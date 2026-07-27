package com.gymlog.app

import android.content.Context
import com.gymlog.app.data.Exercise
import com.gymlog.app.data.ExerciseCategory
import com.gymlog.app.data.MachineSettingDef
import com.gymlog.app.data.Repository
import com.gymlog.app.data.Session
import com.gymlog.app.data.SessionExercise
import com.gymlog.app.data.SessionSet
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * One-time idempotent seeder for the user's reference workout (07/26/2026).
 *
 * Runs on app startup; gated by a SharedPreferences flag so it only fires once per install.
 * The flag is set after a successful seed run, so re-installs / clear-data re-seed cleanly.
 */
object SampleWorkoutSeeder {

    private const val PREFS = "gym_log_seed"
    private const val KEY_SEEDED_V1 = "seeded_v1_workout_2026_07_26"

    /** Returns true if seeding actually ran. */
    fun runIfNeeded(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_SEEDED_V1, false)) return false
        runBlocking { seed() }
        prefs.edit().putBoolean(KEY_SEEDED_V1, true).apply()
        return true
    }

    private suspend fun seed() {
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

        val treadmill1Id = ensureExercise("Treadmill", ExerciseCategory.CARDIO, listOf("Speed", "Incline", "Duration"))
        val treadmill2Id = ensureExercise("Treadmill", ExerciseCategory.CARDIO, listOf("Speed", "Incline", "Duration"))
        // (Same row — exercise row only stores settings *definitions*; values live on sets.)

        val rowingId  = ensureExercise("Rowing Machine", ExerciseCategory.WEIGHT_MACHINE, listOf("Seat height", "Chest pad depth"))
        val tricepsId = ensureExercise("Triceps",         ExerciseCategory.WEIGHT_MACHINE, listOf("Seat height"))
        val flyId     = ensureExercise("Fly",             ExerciseCategory.WEIGHT_MACHINE, listOf("Arms position", "Seat height"))
        val pullId    = ensureExercise("Pull Down",       ExerciseCategory.WEIGHT_MACHINE, listOf("Seat height"))

        // --- The session row ---
        val sessionId = repo.createSession(
            Session(
                date = sessionTime,
                name = "Workout · 7/26/2026",
                presetId = null
            )
        )

        // Reusable builder: a set = its setting value map + weight + reps
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

        // --- Walk through the workout in order ---
        // 1. Treadmill warmup
        addSe(treadmill1Id, 0).also { seId ->
            addCardioSet(seId, 20, mapOf("Speed" to "2.8", "Incline" to "6"))
        }
        // 2. Rowing machine
        addSe(rowingId, 1).also { seId ->
            addStrengthSet(seId, 1, 85.0, 20, mapOf("Seat height" to "3", "Chest pad depth" to "3"))
            addStrengthSet(seId, 2, 100.0, 15, mapOf("Seat height" to "3", "Chest pad depth" to "3"))
            addStrengthSet(seId, 3, 100.0, 15, mapOf("Seat height" to "3", "Chest pad depth" to "3"))
        }
        // 3. Triceps
        addSe(tricepsId, 2).also { seId ->
            addStrengthSet(seId, 1, 55.0, 15, mapOf("Seat height" to "6"))
            addStrengthSet(seId, 2, 55.0, 15, mapOf("Seat height" to "6"))
            addStrengthSet(seId, 3, 55.0, 12, mapOf("Seat height" to "6"))
        }
        // 4. Fly
        addSe(flyId, 3).also { seId ->
            addStrengthSet(seId, 1, 70.0, 15, mapOf("Arms position" to "3", "Seat height" to "6"))
            addStrengthSet(seId, 2, 70.0, 12, mapOf("Arms position" to "3", "Seat height" to "6"))
            addStrengthSet(seId, 3, 70.0, 10, mapOf("Arms position" to "3", "Seat height" to "6"))
        }
        // 5. Pull Down
        addSe(pullId, 4).also { seId ->
            addStrengthSet(seId, 1, 55.0, 20, mapOf("Seat height" to "4"))
            addStrengthSet(seId, 2, 70.0, 12, mapOf("Seat height" to "4"))
            addStrengthSet(seId, 3, 70.0, 12, mapOf("Seat height" to "4"))
        }
        // 6. Treadmill cooldown
        addSe(treadmill2Id, 5).also { seId ->
            addCardioSet(seId, 10, mapOf("Speed" to "2.6", "Incline" to "6"))
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
