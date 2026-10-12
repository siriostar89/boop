package dev.boop.android.data

import dev.boop.android.data.local.SessionStore
import dev.boop.android.data.model.Device
import dev.boop.android.data.model.PairingRequest
import dev.boop.android.data.remote.ApiFactory
import dev.boop.android.push.PushRegistrar

/**
 * Exchanges a pairing token for a device credential and persists the session.
 * Right after pairing, the FCM token (if Firebase can provide one) is
 * registered too — mirroring iOS, which re-sends a held APNs token on pair —
 * so the server never sits on "no token" waiting for the next cold start.
 */
class PairingRepository(
    private val session: SessionStore,
    private val pushRegistrar: PushRegistrar? = null,
) {

    suspend fun pair(server: String, token: String, deviceName: String): Device {
        val baseUrl = server.trim().trimEnd('/')
        val api = ApiFactory.create(baseUrl)
        val result = api.exchangePairing(
            PairingRequest(token = token.trim(), name = deviceName, platform = "android"),
        )
        session.save(server = baseUrl, credential = result.credential, device = result.device)
        pushRegistrar?.registerAtStartup()
        return result.device
    }
}
