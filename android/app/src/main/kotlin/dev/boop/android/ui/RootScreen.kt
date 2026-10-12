package dev.boop.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.boop.android.AppContainer
import dev.boop.android.ui.pairing.PairingScreen

/**
 * Chooses between the pairing flow and the (placeholder) paired home screen.
 * The inbox lands in a later phase.
 */
@Composable
fun RootScreen(container: AppContainer) {
    var paired by remember { mutableStateOf(container.session.isPaired) }

    if (paired) {
        PairedPlaceholder(
            server = container.session.serverUrl.orEmpty(),
            deviceName = container.session.deviceName.orEmpty(),
            onUnpair = {
                container.session.clear()
                paired = false
            },
        )
    } else {
        PairingScreen(
            repository = container.pairingRepository,
            onPaired = { paired = true },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PairedPlaceholder(server: String, deviceName: String, onUnpair: () -> Unit) {
    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Boop") }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Paired", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(deviceName.ifBlank { "This device" }, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(4.dp))
            Text(server, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            Text(
                "Inbox and push notifications arrive in the next phases.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = onUnpair) { Text("Unpair") }
        }
    }
}
