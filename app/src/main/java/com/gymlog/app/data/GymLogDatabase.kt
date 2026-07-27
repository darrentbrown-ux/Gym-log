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
    version = 3,
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
         */
        private val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE preset_exercises ADD COLUMN notes TEXT NOT NULL DEFAULT ''")
            }
        }

        /**
         * v2 → v3: add a `value` text column to `machine_setting_defs` so the user can
         * store their preferred value per setting (e.g. "Seat height: 3") that is shown
         * as a reminder when they get to the machine, and persists across routines.
         */
        private val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE machine_setting_defs ADD COLUMN value TEXT NOT NULL DEFAULT ''")
            }
        }

        fun get(context: Context): GymLogDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    GymLogDatabase::class.java,
                    "gym_log.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
