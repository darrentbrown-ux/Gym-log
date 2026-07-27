package com.gymlog.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gymlog.app.data.Exercise
import com.gymlog.app.data.ExerciseCategory
import com.gymlog.app.data.MachineSettingDef
import com.gymlog.app.data.Preset
import com.gymlog.app.data.PresetExercise
import com.gymlog.app.data.Repository
import com.gymlog.app.data.Session
import com.gymlog.app.data.SessionExercise
import com.gymlog.app.data.SessionSet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class GymLogViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = Repository(app)

    // ---- Exercise ----
    val exercises: Flow<List<Exercise>> = repo.exercises()

    fun exercisesByCategory(c: ExerciseCategory): Flow<List<Exercise>> =
        repo.exercisesByCategory(c)

    suspend fun addExercise(name: String, category: ExerciseCategory, notes: String, settings: List<String>): Long {
        val id = repo.addExercise(Exercise(name = name, category = category, notes = notes))
        settings.filter { it.isNotBlank() }.forEach { def ->
            repo.addSettingDef(MachineSettingDef(exerciseId = id, name = def.trim()))
        }
        return id
    }

    suspend fun updateExercise(e: Exercise, settings: List<String>) {
        repo.updateExercise(e)
        // Replace-setting-defs-by-name strategy for simplicity
        val current = repo.settingsSnapshot(e.id).map { it.name }.toSet()
        val desired = settings.map { it.trim() }.filter { it.isNotBlank() }.toSet()
        val toRemove = repo.settingsSnapshot(e.id).filter { it.name !in desired }
        toRemove.forEach { repo.deleteSettingDef(it) }
        desired.filterNot { it in current }.forEach { repo.addSettingDef(MachineSettingDef(exerciseId = e.id, name = it)) }
    }

    suspend fun deleteExercise(e: Exercise) = repo.deleteExercise(e)

    fun settingsFor(exerciseId: Long) = repo.settingsFor(exerciseId)
    suspend fun settingsSnapshot(exerciseId: Long) = repo.settingsSnapshot(exerciseId)

    suspend fun getExercise(id: Long) = repo.exerciseDao.get(id)

    // ---- Presets ----
    val presets = repo.presets()

    suspend fun addPreset(name: String): Long = repo.addPreset(Preset(name = name))
    suspend fun deletePreset(p: Preset) = repo.deletePreset(p)
    suspend fun getPreset(id: Long) = repo.getPreset(id)

    fun presetExercises(presetId: Long) = repo.presetExercises(presetId)

    suspend fun addPresetExercise(presetId: Long, exerciseId: Long,
                                  defaultWeight: Double?, defaultReps: Int?, defaultSets: Int) {
        val nextPos = repo.presetExercises(presetId).first().size
        repo.addPresetExercise(
            PresetExercise(
                presetId = presetId,
                exerciseId = exerciseId,
                defaultWeight = defaultWeight,
                defaultReps = defaultReps,
                defaultSets = defaultSets,
                position = nextPos
            )
        )
    }

    suspend fun removePresetExercise(pe: PresetExercise) = repo.deletePresetExercise(pe)

    // ---- Sessions ----
    val sessions = repo.sessions()

    suspend fun createSession(date: Long, name: String, presetId: Long?): Long =
        repo.createSession(Session(date = date, name = name, presetId = presetId))

    suspend fun deleteSession(s: Session) = repo.deleteSession(s)
    suspend fun getSession(id: Long) = repo.getSession(id)

    fun sessionDetail(sessionId: Long) = repo.sessionExercises(sessionId)
    fun setsOf(sessionExerciseId: Long) = repo.sets(sessionExerciseId)

    /**
     * Pre-fill a session from a preset (or build the scaffold with no exercises).
     * Returns the new session id and the list of created SessionExercise rows
     * so the UI can navigate to the new session.
     */
    suspend fun buildSessionFromPreset(name: String, presetId: Long?): Long {
        val now = System.currentTimeMillis()
        val sessionId = createSession(now, name, presetId)
        if (presetId != null) {
            // read preset_exercises via a one-shot direct query — presetExercises() is a Flow.
            // Easiest path: fetch via the DAO directly through the repository's preset field.
            val pe = presetExercisesList(presetId)
            pe.forEachIndexed { idx, item ->
                repo.addSessionExercise(
                    SessionExercise(
                        sessionId = sessionId,
                        exerciseId = item.exerciseId,
                        position = idx
                    )
                )
            }
        }
        return sessionId
    }

    suspend fun presetExercisesList(presetId: Long): List<com.gymlog.app.data.PresetExerciseJoined> =
        repo.presetExercises(presetId).first()

    suspend fun addSessionExercise(sessionId: Long, exerciseId: Long) {
        val nextPos = (repo.sessionExercises(sessionId).first().maxOfOrNull { it.position } ?: -1) + 1
        repo.addSessionExercise(SessionExercise(sessionId = sessionId, exerciseId = exerciseId, position = nextPos))
    }

    suspend fun deleteSessionExercise(se: SessionExercise) = repo.deleteSessionExercise(se)

    suspend fun addSet(sessionExerciseId: Long, set: SessionSet): Long = repo.addSet(set)
    suspend fun updateSet(set: SessionSet) = repo.updateSet(set)
    suspend fun deleteSet(set: SessionSet) = repo.deleteSet(set)

    /** Read sets one-shot for a session exercise. */
    suspend fun setsFor(sessionExerciseId: Long): List<SessionSet> =
        repo.sets(sessionExerciseId).first()

    /** All session exercises for a session, one-shot. */
    suspend fun sessionExercisesFor(sessionId: Long): List<com.gymlog.app.data.SessionExerciseDetail> =
        repo.sessionExercises(sessionId).first()

    /** Write a complete workout log CSV (one row per set). */
    suspend fun exportCsv(): java.io.File = repo.writeCsv()

    /** Write a complete JSON backup of all user data. */
    suspend fun backupJson(): java.io.File = repo.writeBackup()
}
