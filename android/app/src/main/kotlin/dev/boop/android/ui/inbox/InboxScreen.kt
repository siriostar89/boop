package dev.boop.android.ui.inbox

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.boop.android.data.EventsRepository
import dev.boop.android.data.local.SessionStore
import dev.boop.android.data.model.Event
import dev.boop.android.data.model.Level
import dev.boop.android.ui.levelColor
import dev.boop.android.ui.relativeTime
import dev.boop.android.ui.seenRange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(
    repository: EventsRepository,
    session: SessionStore,
    onEventClick: (String) -> Unit,
    onOccurrencesClick: (projectId: String, fingerprint: String) -> Unit,
    onUnpair: () -> Unit,
    viewModel: InboxViewModel = viewModel(factory = InboxViewModel.factory(repository, session)),
) {
    var menuOpen by remember { mutableStateOf(false) }
    var filtersOpen by remember { mutableStateOf(false) }
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
                title = { Text("Boop") },
                actions = {
                    TextButton(onClick = { filtersOpen = true }) { Text("Filters") }
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Unpair") },
                            onClick = {
                                menuOpen = false
                                onUnpair()
                            },
                        )
                    }
                },
            )
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = viewModel.refreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                viewModel.events.isEmpty() && viewModel.refreshing ->
                    Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }

                viewModel.events.isEmpty() && viewModel.error != null ->
                    CenteredMessage(
                        message = viewModel.error!!,
                        actionLabel = "Retry",
                        onAction = { viewModel.refresh() },
                        isError = true,
                    )

                viewModel.events.isEmpty() ->
                    CenteredMessage(
                        message = if (viewModel.hasFilters) "Nothing matches" else "No events yet",
                        actionLabel = if (viewModel.hasFilters) "Clear filters" else null,
                        onAction = { viewModel.clearFilters() },
                        isError = false,
                    )

                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 12.dp),
                ) {
                    items(viewModel.events, key = { it.id }) { event ->
                        EventRow(
                            event = event,
                            onClick = {
                                if (event.isRepeated) {
                                    onOccurrencesClick(event.projectId, event.fingerprint)
                                } else {
                                    onEventClick(event.id)
                                }
                            },
                        )
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

    if (filtersOpen) {
        FilterSheet(
            grouped = viewModel.grouped,
            onGroupedChange = viewModel::onGroupedChange,
            level = viewModel.level,
            onLevelChange = viewModel::onLevelChange,
            projects = viewModel.projects,
            project = viewModel.project,
            onProjectChange = viewModel::onProjectChange,
            onDismiss = { filtersOpen = false },
        )
    }
}

@Composable
private fun EventRow(event: Event, onClick: () -> Unit) {
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
            Text(
                buildString {
                    append(event.projectName)
                    if (event.source.isNotBlank()) append(" · ${event.source}")
                    if (event.silenced) append(" · silenced")
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    event.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                event.group?.let { g ->
                    if (g.count > 1) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "\u00d7${g.count}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            val group = event.group
            if (group != null && group.count > 1) {
                Text(
                    seenRange(group.firstSeen, group.lastSeen),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            } else if (event.body.isNotBlank()) {
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
        Text(
            relativeTime(event.createdAt),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSheet(
    grouped: Boolean,
    onGroupedChange: (Boolean) -> Unit,
    level: Level?,
    onLevelChange: (Level?) -> Unit,
    projects: List<ProjectOption>,
    project: String?,
    onProjectChange: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Group repeats", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Switch(checked = grouped, onCheckedChange = onGroupedChange)
            }

            SectionTitle("Level")
            FilterRadio("All levels", level == null) { onLevelChange(null) }
            Level.entries.forEach { l ->
                FilterRadio(l.name.lowercase(), level == l) { onLevelChange(l) }
            }

            if (projects.isNotEmpty()) {
                SectionTitle("Project")
                FilterRadio("All projects", project == null) { onProjectChange(null) }
                projects.forEach { p ->
                    FilterRadio(p.name, project == p.id) { onProjectChange(p.id) }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Spacer(Modifier.width(0.dp))
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
    )
}

@Composable
private fun FilterRadio(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun CenteredMessage(
    message: String,
    actionLabel: String?,
    onAction: () -> Unit,
    isError: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            message,
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (actionLabel != null) {
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}
