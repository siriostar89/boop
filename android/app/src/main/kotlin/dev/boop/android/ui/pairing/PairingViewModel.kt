package dev.boop.android.ui.pairing

import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.boop.android.data.PairingRepository
import dev.boop.android.data.model.PairingPayload
import dev.boop.android.data.remote.ApiFactory
import java.io.IOException
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import retrofit2.HttpException

sealed interface PairingState {
    data object Idle : PairingState
    data object Loading : PairingState
    data class Error(val message: String) : PairingState
    data class Success(val deviceName: String) : PairingState
}

/** Drives the pairing screen: scan a QR or type the details, then exchange the token. */
class PairingViewModel(private val repository: PairingRepository) : ViewModel() {

    var server by mutableStateOf("")
        private set
    var token by mutableStateOf("")
        private set
    var state by mutableStateOf<PairingState>(PairingState.Idle)
        private set

    fun onServerChange(value: String) { server = value }
    fun onTokenChange(value: String) { token = value }

    /** Handles the raw contents of a scanned pairing QR code. */
    fun onQrScanned(contents: String) {
        val payload = runCatching {
            ApiFactory.json.decodeFromString(PairingPayload.serializer(), contents)
        }.getOrNull()
        if (payload == null) {
            state = PairingState.Error("That QR code is not a Boop pairing code")
            return
        }
        pair(payload.server, payload.token)
    }

    fun pairManually() {
        if (server.isBlank() || token.isBlank()) {
            state = PairingState.Error("Enter both the server address and the pairing token")
            return
        }
        pair(server, token)
    }

    private fun pair(server: String, token: String) {
        state = PairingState.Loading
        viewModelScope.launch {
            try {
                val device = repository.pair(server, token, deviceName())
                state = PairingState.Success(device.name)
            } catch (e: Exception) {
                state = PairingState.Error(e.toUserMessage())
            }
        }
    }

    private fun deviceName(): String = Build.MODEL?.takeIf { it.isNotBlank() } ?: "Android device"

    private fun Throwable.toUserMessage(): String = when (this) {
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

    companion object {
        fun factory(repository: PairingRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    PairingViewModel(repository) as T
            }
    }
}

@Serializable
private data class ApiErrorBody(val error: String = "", val message: String = "")
