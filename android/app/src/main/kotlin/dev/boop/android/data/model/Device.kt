package dev.boop.android.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A paired phone as returned by the server. */
@Serializable
data class Device(
    val id: String,
    val name: String,
    @SerialName("push_registered") val pushRegistered: Boolean = false,
    val platform: String = "android",
    @SerialName("app_bundle_id") val appBundleId: String = "",
    @SerialName("last_seen_at") val lastSeenAt: String? = null,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
)
