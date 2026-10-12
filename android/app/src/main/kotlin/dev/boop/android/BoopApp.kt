package dev.boop.android

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Application entry point. Holds the manual dependency container and
 * re-registers the FCM push token on every launch (mirrors iOS).
 */
class BoopApp : Application() {
    lateinit var container: AppContainer
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        scope.launch { container.pushRegistrar.registerAtStartup() }
    }
}
