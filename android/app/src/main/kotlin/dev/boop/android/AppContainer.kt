package dev.boop.android

import android.content.Context
import dev.boop.android.data.EventsRepository
import dev.boop.android.data.PairingRepository
import dev.boop.android.data.local.SessionStore

/** Manual dependency container (no DI framework yet). */
class AppContainer(context: Context) {
    val session: SessionStore = SessionStore(context)
    val pairingRepository: PairingRepository = PairingRepository(session)
    val eventsRepository: EventsRepository = EventsRepository(session)
}
