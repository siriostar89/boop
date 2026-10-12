package dev.boop.android.ui.eventdetail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.boop.android.data.EventsRepository
import dev.boop.android.data.model.Event
import dev.boop.android.data.model.EventAction
import dev.boop.android.data.model.agentMarkdown
import dev.boop.android.data.model.plainText
import dev.boop.android.data.remote.ApiFactory
import dev.boop.android.ui.formatTimestamp
import dev.boop.android.ui.levelColor
import kotlinx.serialization.json.JsonObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    repository: EventsRepository,
    eventId: String,
    onBack: () -> Unit,
    viewModel: EventDetailViewModel = viewModel(
        key = eventId,
        factory = EventDetailViewModel.factory(repository, eventId),
    ),
) {
    val event = viewModel.event

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Event") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (event != null) ShareMenu(event)
                },
            )
        },
    ) { padding ->
        when {
            viewModel.loading && event == null ->
                Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                    CircularProgressIndicator()
                }

            viewModel.error != null && event == null ->
                Box(Modifier.fillMaxSize().padding(padding).padding(24.dp), Alignment.Center) {
                    Text(viewModel.error!!, color = MaterialTheme.colorScheme.error)
                }

            event != null -> EventContent(event, Modifier.padding(padding))
        }
    }
}

@Composable
private fun ShareMenu(event: Event) {
    var open by remember { mutableStateOf(false) }
    val context = LocalContext.current

    IconButton(onClick = { open = true }) {
        Icon(Icons.Filled.MoreVert, contentDescription = "Share")
    }
    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
        DropdownMenuItem(
            text = { Text("Copy") },
            onClick = {
                open = false
                copyToClipboard(context, event.plainText())
            },
        )
        DropdownMenuItem(
            text = { Text("Copy as Markdown") },
            onClick = {
                open = false
                copyToClipboard(context, event.agentMarkdown())
            },
        )
        DropdownMenuItem(
            text = { Text("Share") },
            onClick = {
                open = false
                share(context, event.agentMarkdown())
            },
        )
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    manager.setPrimaryClip(ClipData.newPlainText("Boop event", text))
}

private fun share(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share event"))
}

@Composable
private fun EventContent(event: Event, modifier: Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).background(levelColor(event.level), CircleShape))
            Spacer(Modifier.width(8.dp))
            Text(
                buildString {
                    append(event.projectName)
                    if (event.source.isNotBlank()) append(" · ${event.source}")
                    if (event.silenced) append(" · silenced")
                },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            Text(
                event.level.name.lowercase(),
                style = MaterialTheme.typography.labelMedium,
                color = levelColor(event.level),
            )
        }

        Text(event.title, style = MaterialTheme.typography.headlineSmall)

        if (event.body.isNotBlank()) {
            Text(event.body, style = MaterialTheme.typography.bodyLarge)
        }

        Text(
            formatTimestamp(event.createdAt),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (event.actions.isNotEmpty()) {
            HorizontalDivider()
            event.actions.forEach { EventActionButton(it) }
        }

        if (event.data.isNotEmpty()) {
            HorizontalDivider()
            Text("Data", style = MaterialTheme.typography.titleMedium)
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = prettyData(event.data),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }
    }
}

@Composable
private fun EventActionButton(action: EventAction) {
    val context = LocalContext.current
    Button(
        onClick = {
            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(action.url))) }
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(action.label)
    }
}

private fun prettyData(data: JsonObject): String =
    runCatching { ApiFactory.prettyJson.encodeToString(JsonObject.serializer(), data) }
        .getOrDefault(data.toString())
