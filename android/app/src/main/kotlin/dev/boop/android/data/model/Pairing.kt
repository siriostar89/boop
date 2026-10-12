package dev.boop.android.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** The JSON the web UI encodes into the pairing QR code. */
@Serializable
data class PairingPayload(
    val version: Int,
    val server: String,
    val token: String,
)

/** Body of `POST /api/v1/pairing/exchange`. */
@Serializable
data class PairingRequest(
    val token: String,
    val name: String,
    val platform: String = "android",
)

/** Response of `POST /api/v1/pairing/exchange`. */
@Serializable
data class PairingResult(
    val device: Device,
    val credential: String,
)

/** Body of `POST /api/v1/devices`. */
@Serializable
data class DeviceRegistration(
    @SerialName("device_token") val deviceToken: String? = null,
    val name: String? = null,
    val platform: String = "android",
    @SerialName("app_bundle_id") val appBundleId: String? = null,
)
