package dev.boop.android.ui.inbox

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.boop.android.data.EventsRepository
import dev.boop.android.data.local.SessionStore
import dev.boop.android.data.model.Event
import dev.boop.android.data.model.Level
import dev.boop.android.data.remote.toUserMessage
import kotlinx.coroutines.launch

/** A project option derived from the events already loaded (the device credential cannot list projects). */
data class ProjectOption(val id: String, val name: String)

/** Loads the event inbox with cursor pagination and level/project/grouping filters. */
class InboxViewModel(
    private val repository: EventsRepository,
    private val session: SessionStore,
) : ViewModel() {

    var events by mutableStateOf<List<Event>>(emptyList())
        private set
    var refreshing by mutableStateOf(false)
        private set
    var loadingMore by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    var grouped by mutableStateOf(session.inboxGrouped)
        private set
    var level by mutableStateOf<Level?>(null)
        private set
    var project by mutableStateOf<String?>(null)
        private set

    private var nextCursor: String? = null
    private var endReached = false

    /** Projects seen so far, for the filter picker (mirrors the iOS inbox). */
    val projects: List<ProjectOption>
        get() {
            val seen = mutableSetOf<String>()
            return events
                .filter { it.projectId.isNotEmpty() && seen.add(it.projectId) }
                .map { ProjectOption(it.projectId, it.projectName) }
                .sortedBy { it.name }
        }

    val hasFilters: Boolean get() = level != null || project != null

    init {
        refresh()
    }

    fun onGroupedChange(value: Boolean) {
        if (value == grouped) return
        grouped = value
        session.inboxGrouped = value
        refresh()
    }

    fun onLevelChange(value: Level?) {
        if (value == level) return
        level = value
        refresh()
    }

    fun onProjectChange(value: String?) {
        if (value == project) return
        project = value
        refresh()
    }

    fun clearFilters() {
        level = null
        project = null
        refresh()
    }

    fun refresh() {
        refreshing = true
        error = null
        viewModelScope.launch {
            try {
                val page = repository.page(before = null, grouped = grouped, level = level, project = project)
                events = page.events
                nextCursor = page.nextCursor
                endReached = page.nextCursor == null
            } catch (e: Exception) {
                error = e.toUserMessage()
            } finally {
                refreshing = false
            }
        }
    }

    fun loadMore() {
        val cursor = nextCursor
        if (loadingMore || refreshing || endReached || cursor == null) return
        loadingMore = true
        viewModelScope.launch {
            try {
                val page = repository.page(
                    before = cursor,
                    grouped = grouped,
                    level = level,
                    project = project,
                )
                events = events + page.events
                nextCursor = page.nextCursor
                endReached = page.nextCursor == null
            } catch (e: Exception) {
                error = e.toUserMessage()
            } finally {
                loadingMore = false
            }
        }
    }

    companion object {
        fun factory(repository: EventsRepository, session: SessionStore): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    InboxViewModel(repository, session) as T
            }
    }
}
