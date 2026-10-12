package dev.boop.android.ui.inbox

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

/** Loads the event inbox with cursor pagination. */
class InboxViewModel(private val repository: EventsRepository) : ViewModel() {

    var events by mutableStateOf<List<Event>>(emptyList())
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
        if (refreshing) return
        refreshing = true
        error = null
        viewModelScope.launch {
            try {
                val page = repository.page(before = null)
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
                val page = repository.page(before = cursor)
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
        fun factory(repository: EventsRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    InboxViewModel(repository) as T
            }
    }
}
