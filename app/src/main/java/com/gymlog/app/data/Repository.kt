package com.gymlog.app.data

import android.content.Context
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.first

/**
 * Single entry point used by ViewModels.  Wraps the three DAOs and persists JSON + CSV exports
 * to the app's cache so FileProvider can hand a content:// URI to the user.
 */
class Repository(private val context: Context) {

    private val db = GymLogDatabase.get(context)

    val exerciseDao = db.exerciseDao()
    val settingDefDao = db.machineSettingDefDao()
    val presetDao = db.presetDao()
    val sessionDao = db.sessionDao()

    fun exercises() = exerciseDao.observeAll()
    fun exercisesByCategory(c: ExerciseCategory) = exerciseDao.observeByCategory(c)
    fun settingsFor(exerciseId: Long) = settingDefDao.observe(exerciseId)
    suspend fun settingsSnapshot(exerciseId: Long) = settingDefDao.list(exerciseId)

    fun presets() = presetDao.observeAll()
    fun presetExercises(presetId: Long) = presetDao.observePresetExercises(presetId)

    fun sessions() = sessionDao.observeAll()
    fun sessionExercises(sessionId: Long) = sessionDao.observeSessionExerciseDetail(sessionId)
    fun sets(sessionExerciseId: Long) = sessionDao.observeSets(sessionExerciseId)

    // ---------- Exercise CRUD ----------
    suspend fun addExercise(e: Exercise): Long = exerciseDao.insert(e)
    suspend fun updateExercise(e: Exercise) = exerciseDao.update(e)
    suspend fun deleteExercise(e: Exercise) = exerciseDao.delete(e)

    /**
     * Rename all exercises whose name matches `oldName` (case-insensitive) to
     * `newName`. Returns the number of rows renamed. Used by data migrations
     * like the v1.5.3 "Captain chair" → "Captain's chair" rename.
     */
    suspend fun renameExerciseByName(oldName: String, newName: String): Int =
        exerciseDao.renameByExactName(oldName, newName)

    // ---------- Setting defs ----------
    suspend fun addSettingDef(d: MachineSettingDef): Long = settingDefDao.insert(d)
    suspend fun updateSettingDef(d: MachineSettingDef) = settingDefDao.update(d)
    suspend fun deleteSettingDef(d: MachineSettingDef) = settingDefDao.delete(d)

    // ---------- Presets ----------
    suspend fun addPreset(p: Preset): Long = presetDao.insert(p)
    suspend fun updatePreset(p: Preset) = presetDao.update(p)
    suspend fun deletePreset(p: Preset) = presetDao.delete(p)
    suspend fun getPreset(id: Long): Preset? = presetDao.get(id)
    suspend fun addPresetExercise(pe: PresetExercise): Long = presetDao.insertPresetExercise(pe)
    suspend fun updatePresetExercise(pe: PresetExercise) = presetDao.updatePresetExercise(pe)
    suspend fun deletePresetExercise(pe: PresetExercise) = presetDao.deletePresetExercise(pe)

    // ---------- Sessions ----------
    suspend fun createSession(s: Session): Long = sessionDao.insertSession(s)
    suspend fun updateSession(s: Session) = sessionDao.updateSession(s)
    suspend fun deleteSession(s: Session) = sessionDao.deleteSession(s)

    /**
     * Rename all sessions whose name matches `oldName` exactly to `newName`.
     * Returns rows updated. Used by the v1.5.4 data migration.
     */
    suspend fun renameSessionByName(oldName: String, newName: String): Int =
        sessionDao.renameByExactName(oldName, newName)

    /** Cascade-delete a session AND its session_exercises and session_sets. */
    suspend fun deleteSessionCascade(sessionId: Long) = sessionDao.deleteSessionCascade(sessionId)
    suspend fun getSession(id: Long): Session? = sessionDao.get(id)
    suspend fun addSessionExercise(se: SessionExercise): Long = sessionDao.insertSessionExercise(se)
    suspend fun updateSessionExercise(se: SessionExercise) = sessionDao.updateSessionExercise(se)
    suspend fun deleteSessionExercise(se: SessionExercise) = sessionDao.deleteSessionExercise(se)
    suspend fun addSet(s: SessionSet): Long = sessionDao.insertSet(s)
    suspend fun updateSet(s: SessionSet) = sessionDao.updateSet(s)
    suspend fun deleteSet(s: SessionSet) = sessionDao.deleteSet(s)

    // ---------- Full dump (used for backup + CSV) ----------
    // Reads every table once via SQLite directly (Flow-based DAOs are awkward for one-shot reads).
    private suspend fun fullDump(): BackupCodec.Dump {
        val ex = db.openHelper.readableDatabase.query("SELECT * FROM exercises").use { c ->
            buildList {
                while (c.moveToNext()) add(
                    Exercise(
                        id = c.getLong(c.getColumnIndexOrThrow("id")),
                        name = c.getString(c.getColumnIndexOrThrow("name")),
                        category = ExerciseCategory.valueOf(c.getString(c.getColumnIndexOrThrow("category"))),
                        notes = c.getString(c.getColumnIndexOrThrow("notes")) ?: ""
                    )
                )
            }
        }
        val sd = db.openHelper.readableDatabase.query("SELECT * FROM machine_setting_defs").use { c ->
            buildList {
                while (c.moveToNext()) add(
                    MachineSettingDef(
                        id = c.getLong(c.getColumnIndexOrThrow("id")),
                        exerciseId = c.getLong(c.getColumnIndexOrThrow("exerciseId")),
                        name = c.getString(c.getColumnIndexOrThrow("name"))
                    )
                )
            }
        }
        val ps = db.openHelper.readableDatabase.query("SELECT * FROM presets").use { c ->
            buildList {
                while (c.moveToNext()) add(
                    Preset(
                        id = c.getLong(c.getColumnIndexOrThrow("id")),
                        name = c.getString(c.getColumnIndexOrThrow("name"))
                    )
                )
            }
        }
        val pe = db.openHelper.readableDatabase.query("SELECT * FROM preset_exercises").use { c ->
            buildList {
                while (c.moveToNext()) add(
                    PresetExercise(
                        id = c.getLong(c.getColumnIndexOrThrow("id")),
                        presetId = c.getLong(c.getColumnIndexOrThrow("presetId")),
                        exerciseId = c.getLong(c.getColumnIndexOrThrow("exerciseId")),
                        defaultWeight = c.getDoubleOrNull("defaultWeight"),
                        defaultReps = c.getIntOrNull("defaultReps"),
                        defaultSets = c.getInt(c.getColumnIndexOrThrow("defaultSets")),
                        position = c.getInt(c.getColumnIndexOrThrow("position"))
                    )
                )
            }
        }
        val ss = db.openHelper.readableDatabase.query("SELECT * FROM sessions").use { c ->
            buildList {
                while (c.moveToNext()) add(
                    Session(
                        id = c.getLong(c.getColumnIndexOrThrow("id")),
                        date = c.getLong(c.getColumnIndexOrThrow("date")),
                        name = c.getString(c.getColumnIndexOrThrow("name")),
                        presetId = c.getLongOrNull("presetId")
                    )
                )
            }
        }
        val se = db.openHelper.readableDatabase.query("SELECT * FROM session_exercises").use { c ->
            buildList {
                while (c.moveToNext()) add(
                    SessionExercise(
                        id = c.getLong(c.getColumnIndexOrThrow("id")),
                        sessionId = c.getLong(c.getColumnIndexOrThrow("sessionId")),
                        exerciseId = c.getLong(c.getColumnIndexOrThrow("exerciseId")),
                        position = c.getInt(c.getColumnIndexOrThrow("position")),
                        notes = c.getString(c.getColumnIndexOrThrow("notes")) ?: ""
                    )
                )
            }
        }
        val st = db.openHelper.readableDatabase.query("SELECT * FROM session_sets").use { c ->
            buildList {
                while (c.moveToNext()) add(
                    SessionSet(
                        id = c.getLong(c.getColumnIndexOrThrow("id")),
                        sessionExerciseId = c.getLong(c.getColumnIndexOrThrow("sessionExerciseId")),
                        setNumber = c.getInt(c.getColumnIndexOrThrow("setNumber")),
                        reps = c.getIntOrNull("reps"),
                        weight = c.getDoubleOrNull("weight"),
                        settingsValues = c.getString(c.getColumnIndexOrThrow("settingsValues")) ?: "{}",
                        durationSeconds = c.getIntOrNull("durationSeconds"),
                        distance = c.getDoubleOrNull("distance"),
                        completed = c.getInt(c.getColumnIndexOrThrow("completed")) != 0
                    )
                )
            }
        }

        return BackupCodec.Dump(ex, sd, ps, pe, ss, se, st)
    }

    // ---------- Exports ----------
    suspend fun writeCsv(): File {
        val dump = fullDump()
        val csv = BackupCodec.toCsv(dump.sessions, dump.sessionExercises, dump.sessionSets, dump.exercises)
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val out = File(dir, "gym_log_$stamp.csv")
        out.writeText(csv)
        return out
    }

    suspend fun writeBackup(): File {
        val json = BackupCodec.toJson(fullDump())
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val out = File(dir, "gym_backup_$stamp.json")
        out.writeText(json)
        return out
    }

    /**
     * Summary returned by [importBackup]. Counts the rows successfully inserted
     * in each table — surfaced to the user in a snackbar so they have feedback
     * even on a 1000-row import.
     */
    data class ImportSummary(
        val exercises: Int,
        val settingDefs: Int,
        val presets: Int,
        val presetExercises: Int,
        val sessions: Int,
        val sessionExercises: Int,
        val sessionSets: Int
    )

    /**
     * Read a JSON backup file and insert its rows into the local database.
     *
     * Behaviour:
     *  - **Additive** — every row is inserted as a new row. Existing user data
     *    is preserved. The user is responsible for cleaning up duplicates if
     *    they re-import the same backup twice.
     *  - **ID remapping** — old IDs from the backup are mapped to the new
     *    auto-generated IDs as we go, so foreign keys (exerciseId, presetId,
     *    sessionId, sessionExerciseId) line up correctly.
     *  - **Tolerant** — malformed rows are skipped (logged as warnings by
     *    BackupCodec) rather than aborting the whole import.
     *
     * @return counts of rows inserted per table.
     */
    suspend fun importBackup(file: File): ImportSummary {
        val json = file.readText(Charsets.UTF_8)
        val dump = BackupCodec.fromJson(json)

        // 1. Exercises. Build a name+category → new-id map so child rows can
        //    resolve their exerciseId even after renumbering.
        val exIdMap = HashMap<Long, Long>(dump.exercises.size * 2)
        // If a user has an existing exercise with the same name + category,
        // dedupe to that row's ID rather than creating a duplicate. This
        // keeps the import idempotent for the common "restore to a phone
        // that's been set up" case.
        val existingExercises = exerciseDao.observeAll().first()
        val existingExByKey = existingExercises.associateBy { it.name to it.category }
        for (ex in dump.exercises) {
            val key = ex.name to ex.category
            val existing = existingExByKey[key]
            val newId = existing?.id ?: exerciseDao.insert(ex.copy(id = 0L))
            exIdMap[ex.id] = newId
        }

        // 2. Setting defs.
        val sdIdMap = HashMap<Long, Long>(dump.settingDefs.size * 2)
        for (sd in dump.settingDefs) {
            val exId = exIdMap[sd.exerciseId] ?: continue
            val newId = settingDefDao.insert(sd.copy(id = 0L, exerciseId = exId))
            sdIdMap[sd.id] = newId
        }

        // 3. Presets.
        val presetIdMap = HashMap<Long, Long>(dump.presets.size * 2)
        for (p in dump.presets) {
            val newId = presetDao.insert(p.copy(id = 0L))
            presetIdMap[p.id] = newId
        }

        // 4. Preset exercises.
        var peCount = 0
        for (pe in dump.presetExercises) {
            val presetId = presetIdMap[pe.presetId] ?: continue
            val exId = exIdMap[pe.exerciseId] ?: continue
            presetDao.insertPresetExercise(pe.copy(id = 0L, presetId = presetId, exerciseId = exId))
            peCount++
        }

        // 5. Sessions.
        val sessionIdMap = HashMap<Long, Long>(dump.sessions.size * 2)
        for (s in dump.sessions) {
            val newId = sessionDao.insertSession(s.copy(id = 0L, presetId = s.presetId?.let { presetIdMap[it] }))
            sessionIdMap[s.id] = newId
        }

        // 6. Session exercises.
        val seIdMap = HashMap<Long, Long>(dump.sessionExercises.size * 2)
        for (se in dump.sessionExercises) {
            val sessionId = sessionIdMap[se.sessionId] ?: continue
            val exId = exIdMap[se.exerciseId] ?: continue
            val newId = sessionDao.insertSessionExercise(se.copy(id = 0L, sessionId = sessionId, exerciseId = exId))
            seIdMap[se.id] = newId
        }

        // 7. Session sets.
        var setCount = 0
        for (ss in dump.sessionSets) {
            val seId = seIdMap[ss.sessionExerciseId] ?: continue
            sessionDao.insertSet(ss.copy(id = 0L, sessionExerciseId = seId))
            setCount++
        }

        return ImportSummary(
            exercises = exIdMap.size,
            settingDefs = sdIdMap.size,
            presets = presetIdMap.size,
            presetExercises = peCount,
            sessions = sessionIdMap.size,
            sessionExercises = seIdMap.size,
            sessionSets = setCount
        )
    }

    fun shareUri(file: File) = FileProvider.getUriForFile(
        context, "${context.packageName}.fileprovider", file
    )
}

private fun android.database.Cursor.getLongOrNull(column: String): Long? {
    val idx = getColumnIndexOrThrow(column)
    return if (isNull(idx)) null else getLong(idx)
}
private fun android.database.Cursor.getIntOrNull(column: String): Int? {
    val idx = getColumnIndexOrThrow(column)
    return if (isNull(idx)) null else getInt(idx)
}
private fun android.database.Cursor.getDoubleOrNull(column: String): Double? {
    val idx = getColumnIndexOrThrow(column)
    return if (isNull(idx)) null else getDouble(idx)
}
