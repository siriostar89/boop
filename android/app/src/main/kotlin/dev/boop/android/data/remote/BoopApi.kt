package dev.boop.android.data.remote

import dev.boop.android.data.model.Device
import dev.boop.android.data.model.DeviceRegistration
import dev.boop.android.data.model.Event
import dev.boop.android.data.model.EventPage
import dev.boop.android.data.model.HealthStatus
import dev.boop.android.data.model.PairingRequest
import dev.boop.android.data.model.PairingResult
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** The subset of the Boop REST API the Android client uses. */
interface BoopApi {

    @GET("health")
    suspend fun health(): HealthStatus

    @POST("api/v1/pairing/exchange")
    suspend fun exchangePairing(@Body body: PairingRequest): PairingResult

    @POST("api/v1/devices")
    suspend fun registerDevice(@Body body: DeviceRegistration): Device

    @GET("api/v1/events")
    suspend fun listEvents(
        @Query("limit") limit: Int,
        @Query("before") before: String?,
        @Query("grouped") grouped: Boolean?,
        @Query("level") level: String?,
        @Query("project") project: String?,
        @Query("fingerprint") fingerprint: String?,
    ): EventPage

    @GET("api/v1/events/{id}")
    suspend fun getEvent(@Path("id") id: String): Event
}
