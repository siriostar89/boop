package dev.boop.android.data

import dev.boop.android.data.local.SessionStore
import dev.boop.android.data.model.Device
import dev.boop.android.data.model.PairingRequest
import dev.boop.android.data.remote.ApiFactory

/** Exchanges a pairing token for a device credential and persists the session. */
class PairingRepository(private val session: SessionStore) {

    suspend fun pair(server: String, token: String, deviceName: String): Device {
        val baseUrl = server.trim().trimEnd('/')
        val api = ApiFactory.create(baseUrl)
        val result = api.exchangePairing(
            PairingRequest(token = token.trim(), name = deviceName, platform = "android"),
        )
        session.save(server = baseUrl, credential = result.credential, device = result.device)
        return result.device
    }
}
