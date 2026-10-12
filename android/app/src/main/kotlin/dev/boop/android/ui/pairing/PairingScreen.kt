package dev.boop.android.ui.pairing

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import dev.boop.android.data.PairingRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PairingScreen(
    repository: PairingRepository,
    onPaired: () -> Unit,
    viewModel: PairingViewModel = viewModel(factory = PairingViewModel.factory(repository)),
) {
    val state = viewModel.state
    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        result.contents?.let(viewModel::onQrScanned)
    }

    LaunchedEffect(state) {
        if (state is PairingState.Success) onPaired()
    }

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Pair Boop") }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Scan the pairing code from the Boop web UI (Devices, then Pair phone).",
                style = MaterialTheme.typography.bodyMedium,
            )

            Button(
                onClick = {
                    scanLauncher.launch(
                        ScanOptions().apply {
                            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                            setPrompt("Scan the Boop pairing code")
                            setBeepEnabled(false)
                            setOrientationLocked(false)
                        },
                    )
                },
                enabled = state !is PairingState.Loading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Scan QR code")
            }

            HorizontalDivider()

            Text("Or enter details manually", style = MaterialTheme.typography.labelLarge)

            OutlinedTextField(
                value = viewModel.server,
                onValueChange = viewModel::onServerChange,
                label = { Text("Server URL") },
                placeholder = { Text("https://boop.example.com") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = viewModel.token,
                onValueChange = viewModel::onTokenChange,
                label = { Text("Pairing token") },
                placeholder = { Text("pair_...") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = viewModel::pairManually,
                enabled = state !is PairingState.Loading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Pair")
            }

            when (state) {
                PairingState.Loading -> CircularProgressIndicator()
                is PairingState.Error -> Text(
                    text = state.message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
                else -> Unit
            }
        }
    }
}
