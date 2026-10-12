package dev.boop.android.ui.eventdetail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import dev.boop.android.data.model.EventSections
import dev.boop.android.data.model.StackFrame
import dev.boop.android.data.model.agentMarkdown
import dev.boop.android.data.model.plainText
import dev.boop.android.data.remote.ApiFactory
import dev.boop.android.ui.formatTimestamp
import dev.boop.android.ui.levelColor
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

private fun JsonObject.contentOrNull(key: String): String? =
    (this[key] as? JsonPrimitive)?.contentOrNull

private fun JsonElement.inlineText(): String =
    (this as? JsonPrimitive)?.contentOrNull ?: toString()

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
    val sections = remember(event.data) { EventSections.fromData(event.data) }
    var showRaw by remember { mutableStateOf(false) }

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

        // Exception section
        sections.exception?.let { exc ->
            HorizontalDivider()
            EventSectionCard(title = "Exception") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    exc.contentOrNull("type")?.let { t ->
                        Text(t, style = MaterialTheme.typography.titleMedium)
                    }
                    exc.contentOrNull("message")?.let { m ->
                        Text(m, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    exc.keys.filter { it != "type" && it != "message" }.sorted().forEach { k ->
                        Text("$k: ${exc[k]?.inlineText() ?: "null"}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        // Stacktrace section
        if (sections.frames.isNotEmpty()) {
            HorizontalDivider()
            EventSectionCard(title = "Stacktrace") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    sections.frames.forEach { frame ->
                        StackFrameRow(frame)
                    }
                }
            }
        }

        // Tags section
        if (sections.tags.isNotEmpty()) {
            HorizontalDivider()
            EventSectionCard(title = "Tags") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    sections.tags.forEach { (key, value) ->
                        Text("$key: ${value.inlineText()}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        // Context section
        if (sections.context.isNotEmpty()) {
            HorizontalDivider()
            EventSectionCard(title = "Context") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    sections.context.forEach { (key, value) ->
                        if (value is JsonObject || value is JsonArray) {
                            Text(key, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(value.toString(), style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                        } else {
                            Text("$key: ${value.inlineText()}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        // Breadcrumbs section
        if (sections.breadcrumbs.isNotEmpty()) {
            HorizontalDivider()
            EventSectionCard(title = "Breadcrumbs") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    sections.breadcrumbs.forEach { crumb ->
                        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(crumb.timestamp, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(crumb.category, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(crumb.message, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }

        // Rest / Raw JSON section
        if (sections.rest.isNotEmpty()) {
            HorizontalDivider()
            EventSectionCard(title = "Data") {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    sections.rest.forEach { (key, value) ->
                        Text("$key: ${value.toString()}", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        // Full raw JSON toggle
        if (event.data.isNotEmpty()) {
            HorizontalDivider()
            EventSectionCard(title = "Raw JSON") {
                Text(
                    if (showRaw) "Hide" else "Show",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showRaw = !showRaw }
                        .padding(vertical = 8.dp),
                )
                if (showRaw) {
                    Text(
                        text = prettyData(event.data),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun EventSectionCard(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth().padding(12.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun StackFrameRow(frame: StackFrame) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (frame.inApp) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(frame.function, style = MaterialTheme.typography.bodyMedium)
            if (frame.file.isNotBlank()) {
                Text(
                    buildString {
                        append(frame.file)
                        frame.line?.let { append(":$it") }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
        if (frame.inApp) {
            Text("in-app", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
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
