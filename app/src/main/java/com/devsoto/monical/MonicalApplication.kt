package com.devsoto.monical

import android.app.Application

/**
 * Application entry point. Owns the [AppContainer] that supplies dependencies to ViewModels.
 * Registered via `android:name` in the manifest.
 */
class MonicalApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
