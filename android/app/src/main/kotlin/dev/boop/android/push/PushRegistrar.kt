package dev.boop.android.push

import com.google.firebase.messaging.FirebaseMessaging
import dev.boop.android.data.local.SessionStore
import dev.boop.android.data.model.DeviceRegistration
import dev.boop.android.data.remote.ApiFactory
import kotlinx.coroutines.tasks.await

/**
 * Registers the FCM token with the Boop server, like iOS re-registers its
 * APNs token: on every app start and whenever FCM rotates the token.
 *
 * Every method is a silent no-op when the device is not paired yet or when
 * google-services.json is missing (Firebase init fails without it).
 */
class PushRegistrar(private val session: SessionStore) {

    /** Last token sent to the server, to avoid redundant POSTs. */
    private var lastSent: String? = session.pushToken

    /** Called at app start: fetches the current token and registers it. */
    suspend fun registerAtStartup() {
        val token = runCatching { FirebaseMessaging.getInstance().token.await() }.getOrNull() ?: return
        register(token)
    }

    /** Called by the messaging service when FCM rotates the token. */
    suspend fun register(token: String) {
        if (token.isBlank() || token == lastSent) return
        val server = session.serverUrl ?: return
        val credential = session.credential ?: return
        val ok = runCatching {
            ApiFactory.create(server, credential).registerDevice(
                DeviceRegistration(
                    deviceToken = token,
                    name = session.deviceName,
                    platform = "android",
                    appBundleId = "dev.boop.android",
                ),
            )
        }.isSuccess
        if (ok) {
            lastSent = token
            session.pushToken = token
        }
    }
}
