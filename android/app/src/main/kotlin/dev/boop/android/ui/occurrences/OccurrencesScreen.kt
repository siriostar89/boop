package dev.boop.android.ui.occurrences

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.boop.android.data.EventsRepository
import dev.boop.android.data.model.Event
import dev.boop.android.data.model.GroupInfo
import dev.boop.android.ui.formatTimestamp
import dev.boop.android.ui.levelColor
import dev.boop.android.ui.relativeTime
import dev.boop.android.ui.seenRange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OccurrencesScreen(
    repository: EventsRepository,
    projectId: String,
    fingerprint: String,
    onBack: () -> Unit,
    onEventClick: (String) -> Unit,
    viewModel: OccurrencesViewModel = viewModel(
        key = "$projectId/$fingerprint",
        factory = OccurrencesViewModel.factory(repository, projectId, fingerprint),
    ),
) {
    val listState = rememberLazyListState()
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            val total = listState.layoutInfo.totalItemsCount
            total > 0 && lastVisible >= total - 3
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) viewModel.loadMore()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Occurrences") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        val head = viewModel.head
        when {
            viewModel.events.isEmpty() && viewModel.refreshing ->
                Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { CircularProgressIndicator() }

            viewModel.error != null && viewModel.events.isEmpty() ->
                Box(Modifier.fillMaxSize().padding(padding).padding(24.dp), Alignment.Center) {
                    Text(viewModel.error!!, color = MaterialTheme.colorScheme.error)
                }

            else -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(padding),
            ) {
                item {
                    Header(
                        title = head?.title ?: "Occurrences",
                        projectName = head?.projectName.orEmpty(),
                        level = head?.level,
                        fingerprint = fingerprint,
                        group = head?.group,
                    )
                }
                items(viewModel.events, key = { it.id }) { event ->
                    OccurrenceRow(event = event, onClick = { onEventClick(event.id) })
                    HorizontalDivider()
                }
                if (viewModel.loadingMore) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(16.dp), Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(
    title: String,
    projectName: String,
    level: dev.boop.android.data.model.Level?,
    fingerprint: String,
    group: GroupInfo?,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (projectName.isNotEmpty()) {
                    Text(
                        projectName,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(8.dp))
                }
                if (level != null) {
                    Box(Modifier.size(10.dp).background(levelColor(level), CircleShape))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        level.name.lowercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = levelColor(level),
                    )
                }
            }
            Text(
                buildString {
                    append(title)
                    if (group != null && group.count > 1) append("  \u00d7${group.count}")
                },
                style = MaterialTheme.typography.titleLarge,
            )
            if (group != null) {
                Text(
                    seenRange(group.firstSeen, group.lastSeen),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                "Fingerprint: $fingerprint",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun OccurrenceRow(event: Event, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            Modifier
                .padding(top = 6.dp)
                .size(10.dp)
                .background(levelColor(event.level), CircleShape),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(event.title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
            if (event.body.isNotBlank()) {
                Text(
                    event.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                relativeTime(event.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                formatTimestamp(event.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
