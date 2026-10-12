package dev.boop.android.data

import dev.boop.android.data.local.SessionStore
import dev.boop.android.data.model.Event
import dev.boop.android.data.model.EventPage
import dev.boop.android.data.model.Level
import dev.boop.android.data.remote.ApiFactory
import dev.boop.android.data.remote.BoopApi

/** Reads events for the paired device. */
class EventsRepository(private val session: SessionStore) {

    private fun api(): BoopApi {
        val server = session.serverUrl ?: error("This device is not paired")
        return ApiFactory.create(server, session.credential)
    }

    suspend fun page(
        before: String?,
        limit: Int = 50,
        grouped: Boolean = false,
        level: Level? = null,
        project: String? = null,
        fingerprint: String? = null,
    ): EventPage = api().listEvents(
        limit = limit,
        before = before,
        grouped = if (grouped) true else null,
        level = level?.name?.lowercase(),
        project = project,
        fingerprint = fingerprint,
    )

    suspend fun event(id: String): Event = api().getEvent(id)
}
