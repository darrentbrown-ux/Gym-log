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
    private const val KEY_SEEDED_V150_ASHLEY = "seeded_v150_ashley_example_routine"

    /** Returns true if any seeding actually ran. */
    suspend fun runIfNeeded(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        var ranAny = false

        if (!prefs.getBoolean(KEY_SEEDED_V1_WORKOUT, false)) {
            seedReferenceWorkout()
            prefs.edit().putBoolean(KEY_SEEDED_V1_WORKOUT, true).apply()
            ranAny = true
        }
        if (!prefs.getBoolean(KEY_SEEDED_V1_ROUTINE, false)) {
            seedBasicUpperBodyRoutine()
            prefs.edit().putBoolean(KEY_SEEDED_V1_ROUTINE, true).apply()
            ranAny = true
        }
        if (!prefs.getBoolean(KEY_SEEDED_V150_ASHLEY, false)) {
            seedAshleyExampleRoutine()
            prefs.edit().putBoolean(KEY_SEEDED_V150_ASHLEY, true).apply()
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
                // v1.5.4: removed the `·` middle dot from the seeded name. Some
                // devices / fonts rendered it as a non-English glyph in the
                // exported CSV, which the user wanted to clean up.
                name = "Workout 7/26/2026",
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

    /**
     * "Ashley Example" routine — created in v1.5.0 from a spec the user sent
     * transcribed from a real workout. The 10 exercises cover the four groups:
     *
     *  - Weight machines: Leg Extension, Leg Curl, Lateral Pulldown, Chest press,
     *    Triceps extensions, Hip abduction
     *  - Free weights:    Dumbbells chest supported row, Dumbbells biceps,
     *                     Dumbbells lateral pulldown
     *  - Calisthenics:    Captain chair
     *
     * For each row, `defaultWeight` is the most-frequent working weight in the
     * user's list (the first set's weight) and `defaultReps` is the most-frequent
     * rep count. Per-set weight and rep deviations are kept in `notes` so the
     * workout screen can show them.
     *
     * Only created if a preset by this name does not already exist.
     */
    private suspend fun seedAshleyExampleRoutine() {
        val ctx = AppContext.context!!
        val repo = Repository(ctx)

        val existing = repo.presets().first().firstOrNull { it.name.equals("Ashley Example", ignoreCase = true) }
        if (existing != null) return

        suspend fun ensureExercise(name: String, category: ExerciseCategory, settings: List<String>): Long {
            val match = repo.exerciseDao.observeAll().first()
                .firstOrNull { it.name == name && it.category == category }
            if (match != null) return match.id
            val newId = repo.addExercise(Exercise(name = name, category = category, notes = "Ashley Example routine"))
            settings.forEach { repo.addSettingDef(MachineSettingDef(exerciseId = newId, name = it)) }
            return newId
        }

        // ---- Weight machine exercises ----
        val legExtId = ensureExercise("Leg extensions",     ExerciseCategory.WEIGHT_MACHINE, listOf("Seat height", "Arm position"))
        val legCurlId = ensureExercise("Leg curl",          ExerciseCategory.WEIGHT_MACHINE, listOf("Seat height", "Arm position"))
        val latPullId = ensureExercise("Lateral Pulldown",  ExerciseCategory.WEIGHT_MACHINE, listOf("Seat height", "Arm position"))
        val chestPressId = ensureExercise("Chest press",    ExerciseCategory.WEIGHT_MACHINE, listOf("Seat height", "Arm position"))
        val triExtId = ensureExercise("Triceps extensions", ExerciseCategory.WEIGHT_MACHINE, listOf("Seat height", "Arm position"))
        val hipAbdId = ensureExercise("Hip abduction",      ExerciseCategory.WEIGHT_MACHINE, listOf("Seat height", "Arm position"))

        // ---- Free weight exercises (one set each) ----
        val dumbRowId    = ensureExercise("Dumbbells chest supported row", ExerciseCategory.FREE_WEIGHTS, listOf("Arm position"))
        val dumbBicId    = ensureExercise("Dumbbells biceps",              ExerciseCategory.FREE_WEIGHTS, listOf("Arm position"))
        val dumbLatPullId = ensureExercise("Dumbbells lateral pulldown",   ExerciseCategory.FREE_WEIGHTS, listOf("Arm position"))

        // ---- Calisthenics ----
        val captainChairId = ensureExercise("Captain's chair", ExerciseCategory.CALISTHENICS, emptyList())

        val presetId = repo.addPreset(Preset(name = "Ashley Example"))

        // Per-exercise per-set table. The first column of each row is the "default"
        // we expose via defaultWeight / defaultReps / defaultSets; subsequent columns
        // are per-set overrides encoded in the `notes` envelope.
        //
        // weight-format: 90.0  → 90 lb, 65.0 → 65 lb, etc.
        // For "Hip abduction" the "out, lifted / out, forward / out, normal" are
        // position notes — we encode them under the "Arm position" setting so the
        // workout screen shows them.
        val specs = listOf(
            // Leg extensions: 90×12, 90×12, 90×8, 90×8, 90×7
            PresetSpecWithSets(
                exerciseId = legExtId, sets = 5,
                setsData = listOf(
                    90.0 to 12, 90.0 to 12, 90.0 to 8, 90.0 to 8, 90.0 to 7
                )
            ),
            // Leg curl: 65×12, 65×8, 55×10
            PresetSpecWithSets(
                exerciseId = legCurlId, sets = 3,
                setsData = listOf(65.0 to 12, 65.0 to 8, 55.0 to 10)
            ),
            // Lateral Pulldown: 50×12 ×3
            PresetSpecWithSets(
                exerciseId = latPullId, sets = 3,
                setsData = listOf(50.0 to 12, 50.0 to 12, 50.0 to 12)
            ),
            // Chest press: 30×12 ×3
            PresetSpecWithSets(
                exerciseId = chestPressId, sets = 3,
                setsData = listOf(30.0 to 12, 30.0 to 12, 30.0 to 12)
            ),
            // Triceps extensions: 25×12, 30×12, 30×8
            // (the user wrote "Set 2 30lb 8 reps" twice — read as Set 2 = 30×12,
            //  Set 3 = 30×8 because weight jumps and 8 < 12 reads as a working set)
            PresetSpecWithSets(
                exerciseId = triExtId, sets = 3,
                setsData = listOf(25.0 to 12, 30.0 to 12, 30.0 to 8)
            ),
            // Hip abduction: 6 sets at 175 lb with position notes
            PresetSpecWithSets(
                exerciseId = hipAbdId, sets = 6,
                setsData = listOf(175.0 to 12, 175.0 to 12, 175.0 to 10, 175.0 to 9, 175.0 to 9, 175.0 to 9),
                setSettings = listOf(
                    mapOf("Arm position" to "out, lifted"),
                    mapOf("Arm position" to "out, lifted"),
                    mapOf("Arm position" to "out, forward"),
                    mapOf("Arm position" to "out, forward"),
                    mapOf("Arm position" to "out, normal"),
                    mapOf("Arm position" to "out, normal")
                )
            ),
            // Dumbbells chest supported row: 20×10
            PresetSpecWithSets(
                exerciseId = dumbRowId, sets = 1,
                setsData = listOf(20.0 to 10)
            ),
            // Dumbbells biceps: 10×10
            PresetSpecWithSets(
                exerciseId = dumbBicId, sets = 1,
                setsData = listOf(10.0 to 10)
            ),
            // Dumbbells lateral pulldown: 10×10
            PresetSpecWithSets(
                exerciseId = dumbLatPullId, sets = 1,
                setsData = listOf(10.0 to 10)
            ),
            // Captain chair: 4 reps
            PresetSpecWithSets(
                exerciseId = captainChairId, sets = 1,
                setsData = listOf(null to 4)
            )
        )

        specs.forEachIndexed { idx, spec ->
            // First set drives the default weight/reps exposed on the routine row.
            val firstSet = spec.setsData.first()
            val defaultWeight = firstSet.first
            val defaultReps = firstSet.second
            val notes = encodeAshleySets(spec.setsData, spec.setSettings)
            repo.addPresetExercise(
                PresetExercise(
                    presetId = presetId,
                    exerciseId = spec.exerciseId,
                    defaultWeight = defaultWeight,
                    defaultReps = defaultReps,
                    defaultSets = spec.sets,
                    position = idx,
                    notes = notes
                )
            )
        }
    }

    /** Row spec for the Ashley routine: which exercise, how many sets, and a list of (weight, reps) tuples. */
    private data class PresetSpecWithSets(
        val exerciseId: Long,
        val sets: Int,
        val setsData: List<Pair<Double?, Int?>>,
        val setSettings: List<Map<String, String>> = emptyList()
    )

    /**
     * Encode the per-set weight/reps table (and optional per-set setting overrides) as
     * a JSON envelope stored in `PresetExercise.notes`. The format mirrors the
     * `gym_log_defaults:` envelope so the routine row can show the typical set
     * details when the user opens the routine.
     *
     *   gym_log_sets:{"sets":[{"w":90,"r":12},{"w":90,"r":12},...]}
     */
    private fun encodeAshleySets(
        sets: List<Pair<Double?, Int?>>,
        setSettings: List<Map<String, String>>
    ): String {
        if (sets.isEmpty()) return ""
        val array = org.json.JSONArray()
        sets.forEachIndexed { i, (w, r) ->
            val obj = org.json.JSONObject()
            if (w != null) obj.put("w", w)
            if (r != null) obj.put("r", r)
            if (i < setSettings.size && setSettings[i].isNotEmpty()) {
                val sObj = org.json.JSONObject()
                setSettings[i].forEach { (k, v) -> sObj.put(k, v) }
                obj.put("s", sObj)
            }
            array.put(obj)
        }
        return "gym_log_sets:$array"
    }
}

/**
 * Process-global app Context, set in [GymLogApplication.onCreate].
 * Needed because the seeder's static entry point doesn't have a Context otherwise.
 */
object AppContext {
    @Volatile var context: Context? = null
}
