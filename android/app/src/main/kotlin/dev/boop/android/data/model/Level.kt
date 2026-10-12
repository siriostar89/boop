package dev.boop.android.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Event severity, matching the server's levels. */
@Serializable
enum class Level {
    @SerialName("info")
    INFO,

    @SerialName("success")
    SUCCESS,

    @SerialName("warning")
    WARNING,

    @SerialName("error")
    ERROR,

    @SerialName("critical")
    CRITICAL,
}
