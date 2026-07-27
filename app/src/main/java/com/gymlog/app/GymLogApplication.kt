package com.gymlog.app

import android.app.Application
import android.util.Log
import com.gymlog.app.data.Repository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class GymLogApplication : Application() {
    /**
     * App-scoped background scope. The seeder used to run inside `onCreate` via
     * `runBlocking`, which trips strict-mode on Android 12+ test beds (where the
     * process is configured to throw on main-thread disk I/O). Moving it to a
     * coroutine on `Dispatchers.IO` removes the trip wire.
     */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        AppContext.context = this
        // Fire-and-forget seeder on a background dispatcher so we never block the
        // main thread. The seeder is idempotent (gated by SharedPreferences flags)
        // so it's safe to launch from any process state.
        appScope.launch { SampleWorkoutSeeder.runIfNeeded(this@GymLogApplication) }
        // Fire-and-forget data migrations (e.g. exercise name renames). Each
        // migration must be idempotent on its own — see runMigrations() below.
        appScope.launch { runMigrations() }
    }

    /**
     * One-shot data migrations. Each migration is gated by its own
     * SharedPreferences flag so it runs at most once per install.
     */
    private suspend fun runMigrations() {
        val prefs = getSharedPreferences("gym_log_migrations", MODE_PRIVATE)

        // v1.5.3: "Captain chair" → "Captain's chair" (added apostrophe).
        // The seeder already uses the corrected name; this updates any user data
        // carried over from v1.5.0/1.5.1/1.5.2.
        if (!prefs.getBoolean("migrated_v153_captain_chair_rename", false)) {
            val repo = Repository(this)
            val n = repo.renameExerciseByName("Captain chair", "Captain's chair")
            if (n > 0) Log.i("GymLogApp", "Migration v1.5.3: renamed $n 'Captain chair' → 'Captain's chair'")
            prefs.edit().putBoolean("migrated_v153_captain_chair_rename", true).apply()
        }
    }
}
