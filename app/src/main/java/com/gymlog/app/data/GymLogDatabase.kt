package com.gymlog.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class CategoryConverters {
    @TypeConverter
    fun fromCategory(c: ExerciseCategory?): String? = c?.name

    @TypeConverter
    fun toCategory(name: String?): ExerciseCategory? = name?.let { ExerciseCategory.valueOf(it) }
}

@Database(
    entities = [
        Exercise::class,
        MachineSettingDef::class,
        Preset::class,
        PresetExercise::class,
        Session::class,
        SessionExercise::class,
        SessionSet::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(CategoryConverters::class)
abstract class GymLogDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun machineSettingDefDao(): MachineSettingDefDao
    abstract fun presetDao(): PresetDao
    abstract fun sessionDao(): SessionDao

    companion object {
        @Volatile private var INSTANCE: GymLogDatabase? = null

        /**
         * v1 → v2: add a `notes` text column to `preset_exercises` so each routine
         * entry can carry per-setting default values (JSON-encoded by the UI layer).
         * Non-destructive — existing rows keep their data with empty default notes.
         */
        private val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE preset_exercises ADD COLUMN notes TEXT NOT NULL DEFAULT ''")
            }
        }

        fun get(context: Context): GymLogDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    GymLogDatabase::class.java,
                    "gym_log.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
