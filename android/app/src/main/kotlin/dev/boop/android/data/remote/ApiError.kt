package dev.boop.android.data.remote

import java.io.IOException
import kotlinx.serialization.Serializable
import retrofit2.HttpException

@Serializable
private data class ApiErrorBody(val error: String = "", val message: String = "")

/** Maps an API failure to a short, user-facing message. */
fun Throwable.toUserMessage(): String = when (this) {
    is HttpException -> {
        val body = runCatching { response()?.errorBody()?.string() }.getOrNull()
        val parsed = body?.let {
            runCatching { ApiFactory.json.decodeFromString(ApiErrorBody.serializer(), it) }.getOrNull()
        }
        parsed?.message?.takeIf { it.isNotBlank() } ?: "Request failed (HTTP ${code()})"
    }
    is IOException -> "Cannot reach the server. Check the address."
    else -> message ?: "Unexpected error"
}
