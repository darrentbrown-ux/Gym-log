package com.gymlog.app.data

import android.content.Context
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
