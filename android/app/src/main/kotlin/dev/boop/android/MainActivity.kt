package dev.boop.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import dev.boop.android.ui.RootScreen
import dev.boop.android.ui.theme.BoopTheme

class MainActivity : ComponentActivity() {
    /** Event id from a notification tap, consumed by the nav host. */
    private val pendingEventId = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingEventId.value = intent.eventId()
        enableEdgeToEdge()
        setContent {
            BoopTheme {
                RootScreen((application as BoopApp).container, pendingEventId)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.eventId()?.let { pendingEventId.value = it }
    }

    private fun Intent.eventId(): String? =
        getStringExtra(EXTRA_EVENT_ID)?.takeIf { it.isNotBlank() }

    companion object {
        /** Matches the FCM data key sent by the server (see server/internal/fcm Payload). */
        const val EXTRA_EVENT_ID = "event_id"
    }
}
