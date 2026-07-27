package com.gymlog.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Exercise category buckets the user described.
 *   WEIGHT_MACHINE   — tricep, hip abductor, pull down, deltoid fly
 *   CARDIO           — treadmill, stationary bike, stair master, elliptical
 *   CALISTHENICS     — pullup bar, dip bar, captain's chair, planks, situps, push ups
 *   FREE_WEIGHTS     — deadlift, bicep curls
 */
enum class ExerciseCategory(val label: String) {
    WEIGHT_MACHINE("Weight machine"),
    CARDIO("Cardio machine"),
    CALISTHENICS("Calisthenics"),
    FREE_WEIGHTS("Free weights")
}

@Entity(tableName = "exercises")
data class Exercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: ExerciseCategory,
    val notes: String = ""
)

/**
 * Persistent, predefined setting *names* attached to an exercise
 * (e.g. "Seat height", "Incline", "Speed").  Users attach a value when logging a set.
 */
@Entity(
    tableName = "machine_setting_defs",
    foreignKeys = [ForeignKey(
        entity = Exercise::class,
        parentColumns = ["id"],
        childColumns = ["exerciseId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("exerciseId")]
)
data class MachineSettingDef(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val exerciseId: Long,
    val name: String   // free-form label, e.g. "Seat height"
)

/** Named workout preset ("Push day", "Legs") */
@Entity(tableName = "presets")
data class Preset(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)

@Entity(
    tableName = "preset_exercises",
    foreignKeys = [
        ForeignKey(entity = Preset::class, parentColumns = ["id"], childColumns = ["presetId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Exercise::class, parentColumns = ["id"], childColumns = ["exerciseId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("presetId"), Index("exerciseId")]
)
data class PresetExercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val presetId: Long,
    val exerciseId: Long,
    val defaultWeight: Double? = null,   // null for body-weight calisthenics / machine-default
    val defaultReps: Int? = null,
    val defaultSets: Int = 3,
    val position: Int = 0
)

/** One logged workout (a session on a date) */
@Entity(tableName = "sessions")
data class Session(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,              // epoch millis
    val name: String,            // free-form label, "Morning push day"
    val presetId: Long? = null   // optional source preset
)

@Entity(
    tableName = "session_exercises",
    foreignKeys = [
        ForeignKey(entity = Session::class, parentColumns = ["id"], childColumns = ["sessionId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Exercise::class, parentColumns = ["id"], childColumns = ["exerciseId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("sessionId"), Index("exerciseId")]
)
data class SessionExercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val exerciseId: Long,
    val position: Int = 0,
    val notes: String = ""
)

/**
 * A single set performed during a session.
 * `settingsValues` stores JSON: {"Seat height":"5","Incline":"3"} — keeps it schema-flexible
 * so users can record arbitrary fields without DB migrations.
 */
@Entity(
    tableName = "session_sets",
    foreignKeys = [ForeignKey(
        entity = SessionExercise::class,
        parentColumns = ["id"],
        childColumns = ["sessionExerciseId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("sessionExerciseId")]
)
data class SessionSet(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionExerciseId: Long,
    val setNumber: Int,
    val reps: Int? = null,
    val weight: Double? = null,
    val settingsValues: String = "{}",   // JSON map of setting-name -> string value
    val durationSeconds: Int? = null,    // for cardio
    val distance: Double? = null,        // optional miles/km
    val completed: Boolean = true
)
