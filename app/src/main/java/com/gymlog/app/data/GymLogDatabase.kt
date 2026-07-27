package com.gymlog.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

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
    version = 1,
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

        fun get(context: Context): GymLogDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    GymLogDatabase::class.java,
                    "gym_log.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
    }
}
