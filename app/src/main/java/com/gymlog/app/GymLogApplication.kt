package com.gymlog.app

import android.app.Application

class GymLogApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppContext.context = this
        // Seed Darren's 07/26/2026 reference workout on first run only.
        SampleWorkoutSeeder.runIfNeeded(this)
    }
}
