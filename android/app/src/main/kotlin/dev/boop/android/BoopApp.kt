package dev.boop.android

import android.app.Application

/**
 * Application entry point. Holds the manual dependency container; push
 * registration will be wired here in a later phase.
 */
class BoopApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
