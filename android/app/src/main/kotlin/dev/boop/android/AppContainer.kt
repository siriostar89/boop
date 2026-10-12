package dev.boop.android

import android.content.Context
import dev.boop.android.data.EventsRepository
import dev.boop.android.data.PairingRepository
import dev.boop.android.data.local.SessionStore
import dev.boop.android.push.PushRegistrar

/** Manual dependency container (no DI framework yet). */
class AppContainer(context: Context) {
    val session: SessionStore = SessionStore(context)
    val pushRegistrar: PushRegistrar = PushRegistrar(session)
    val pairingRepository: PairingRepository = PairingRepository(session, pushRegistrar)
    val eventsRepository: EventsRepository = EventsRepository(session)
}
