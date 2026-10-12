package dev.boop.android.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.boop.android.AppContainer
import dev.boop.android.ui.pairing.PairingScreen

/** Shows the pairing flow until the device is paired, then the inbox. */
@Composable
fun RootScreen(container: AppContainer, pendingEventId: MutableState<String?>? = null) {
    var paired by remember { mutableStateOf(container.session.isPaired) }

    if (paired) {
        AppNavHost(
            container = container,
            onUnpair = {
                container.session.clear()
                paired = false
            },
            pendingEventId = pendingEventId,
        )
    } else {
        PairingScreen(
            repository = container.pairingRepository,
            onPaired = { paired = true },
        )
    }
}
