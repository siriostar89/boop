package dev.boop.android.ui.occurrences

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.boop.android.data.EventsRepository
import dev.boop.android.data.model.Event
import dev.boop.android.data.remote.toUserMessage
import kotlinx.coroutines.launch

/** Every occurrence of one fingerprint within a project, newest first. */
class OccurrencesViewModel(
    private val repository: EventsRepository,
    private val projectId: String,
    private val fingerprint: String,
) : ViewModel() {

    var events by mutableStateOf<List<Event>>(emptyList())
        private set

    /** Latest occurrence, carrying the group count and first/last seen. */
    var head by mutableStateOf<Event?>(null)
        private set
    var refreshing by mutableStateOf(false)
        private set
    var loadingMore by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    private var nextCursor: String? = null
    private var endReached = false

    init {
        refresh()
    }

    fun refresh() {
        refreshing = true
        error = null
        viewModelScope.launch {
            try {
                head = repository.page(
                    before = null,
                    limit = 1,
                    grouped = true,
                    project = projectId,
                    fingerprint = fingerprint,
                ).events.firstOrNull()
                val page = repository.page(
                    before = null,
                    project = projectId,
                    fingerprint = fingerprint,
                )
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
                    project = projectId,
                    fingerprint = fingerprint,
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
        fun factory(
            repository: EventsRepository,
            projectId: String,
            fingerprint: String,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                OccurrencesViewModel(repository, projectId, fingerprint) as T
        }
    }
}
