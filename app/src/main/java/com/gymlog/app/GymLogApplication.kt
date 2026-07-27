package com.gymlog.app

import android.app.Application
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
    }
}
