package dev.boop.android.data.remote

import dev.boop.android.data.model.Device
import dev.boop.android.data.model.DeviceRegistration
import dev.boop.android.data.model.HealthStatus
import dev.boop.android.data.model.PairingRequest
import dev.boop.android.data.model.PairingResult
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/** The subset of the Boop REST API the Android client uses. */
interface BoopApi {

    @GET("health")
    suspend fun health(): HealthStatus

    @POST("api/v1/pairing/exchange")
    suspend fun exchangePairing(@Body body: PairingRequest): PairingResult

    @POST("api/v1/devices")
    suspend fun registerDevice(@Body body: DeviceRegistration): Device
}
