package dev.boop.android.ui.eventdetail

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

class EventDetailViewModel(
    private val repository: EventsRepository,
    private val eventId: String,
) : ViewModel() {

    var event by mutableStateOf<Event?>(null)
        private set
    var loading by mutableStateOf(true)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    init {
        load()
    }

    fun load() {
        loading = true
        error = null
        viewModelScope.launch {
            try {
                event = repository.event(eventId)
            } catch (e: Exception) {
                error = e.toUserMessage()
            } finally {
                loading = false
            }
        }
    }

    companion object {
        fun factory(repository: EventsRepository, eventId: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    EventDetailViewModel(repository, eventId) as T
            }
    }
}
