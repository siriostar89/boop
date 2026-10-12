package dev.boop.android.data.model

import kotlinx.serialization.Serializable

/** Response of `GET /health`. */
@Serializable
data class HealthStatus(val status: String)
