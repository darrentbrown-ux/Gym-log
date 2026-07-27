package com.gymlog.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises ORDER BY category, name")
    fun observeAll(): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE category = :c ORDER BY name")
    fun observeByCategory(c: ExerciseCategory): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun get(id: Long): Exercise?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(exercise: Exercise): Long

    @Update
    suspend fun update(exercise: Exercise)

    @Delete
    suspend fun delete(exercise: Exercise)
}

@Dao
interface MachineSettingDefDao {
    @Query("SELECT * FROM machine_setting_defs WHERE exerciseId = :exerciseId ORDER BY name")
    fun observe(exerciseId: Long): Flow<List<MachineSettingDef>>

    @Query("SELECT * FROM machine_setting_defs WHERE exerciseId = :exerciseId ORDER BY name")
    suspend fun list(exerciseId: Long): List<MachineSettingDef>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(def: MachineSettingDef): Long

    @Update
    suspend fun update(def: MachineSettingDef)

    @Delete
    suspend fun delete(def: MachineSettingDef)
}

@Dao
interface PresetDao {
    @Query("SELECT * FROM presets ORDER BY name")
    fun observeAll(): Flow<List<Preset>>

    @Query("SELECT * FROM presets WHERE id = :id")
    suspend fun get(id: Long): Preset?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(preset: Preset): Long

    @Update
    suspend fun update(preset: Preset)

    @Delete
    suspend fun delete(preset: Preset)

    // Preset exercises with joined Exercise
    @Query("""
        SELECT pe.id AS presetExerciseId, pe.presetId, pe.defaultWeight, pe.defaultReps,
               pe.defaultSets, pe.position, pe.notes AS presetNotes,
               e.id AS exerciseId, e.name AS exerciseName, e.category AS exerciseCategory,
               e.notes AS exerciseNotes
        FROM preset_exercises pe
        JOIN exercises e ON e.id = pe.exerciseId
        WHERE pe.presetId = :presetId
        ORDER BY pe.position
    """)
    fun observePresetExercises(presetId: Long): Flow<List<PresetExerciseJoined>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPresetExercise(pe: PresetExercise): Long

    @Update
    suspend fun updatePresetExercise(pe: PresetExercise)

    @Delete
    suspend fun deletePresetExercise(pe: PresetExercise)
}

data class PresetExerciseJoined(
    val presetExerciseId: Long,
    val presetId: Long,
    val defaultWeight: Double?,
    val defaultReps: Int?,
    val defaultSets: Int,
    val position: Int,
    val presetNotes: String,
    val exerciseId: Long,
    val exerciseName: String,
    val exerciseCategory: ExerciseCategory,
    val exerciseNotes: String
)

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY date DESC")
    fun observeAll(): Flow<List<Session>>

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun get(id: Long): Session?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: Session): Long

    @Update
    suspend fun updateSession(session: Session)

    @Delete
    suspend fun deleteSession(session: Session)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessionExercise(se: SessionExercise): Long

    @Update
    suspend fun updateSessionExercise(se: SessionExercise)

    @Delete
    suspend fun deleteSessionExercise(se: SessionExercise)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSet(set: SessionSet): Long

    @Update
    suspend fun updateSet(set: SessionSet)

    @Delete
    suspend fun deleteSet(set: SessionSet)

    @Query("SELECT * FROM session_exercises WHERE sessionId = :sessionId ORDER BY position")
    fun observeSessionExercises(sessionId: Long): Flow<List<SessionExercise>>

    @Query("SELECT * FROM session_sets WHERE sessionExerciseId = :sessionExerciseId ORDER BY setNumber")
    fun observeSets(sessionExerciseId: Long): Flow<List<SessionSet>>

    @Query("""
        SELECT se.id AS sessionExerciseId, se.sessionId, se.exerciseId, se.position, se.notes,
               e.name AS exerciseName, e.category AS exerciseCategory
        FROM session_exercises se
        JOIN exercises e ON e.id = se.exerciseId
        WHERE se.sessionId = :sessionId
        ORDER BY se.position
    """)
    fun observeSessionExerciseDetail(sessionId: Long): Flow<List<SessionExerciseDetail>>
}

data class SessionExerciseDetail(
    val sessionExerciseId: Long,
    val sessionId: Long,
    val exerciseId: Long,
    val position: Int,
    val notes: String,
    val exerciseName: String,
    val exerciseCategory: ExerciseCategory
)
