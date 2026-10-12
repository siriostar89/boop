package dev.boop.android.ui

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.boop.android.AppContainer
import dev.boop.android.ui.eventdetail.EventDetailScreen
import dev.boop.android.ui.inbox.InboxScreen
import dev.boop.android.ui.occurrences.OccurrencesScreen

/** Inbox, occurrences and event detail; shown once the device is paired. */
@Composable
fun AppNavHost(container: AppContainer, onUnpair: () -> Unit) {
    val navController = rememberNavController()

    fun openEvent(id: String) = navController.navigate("event/${Uri.encode(id)}")

    NavHost(navController = navController, startDestination = "inbox") {
        composable("inbox") {
            InboxScreen(
                repository = container.eventsRepository,
                session = container.session,
                onEventClick = { id -> openEvent(id) },
                onOccurrencesClick = { projectId, fingerprint ->
                    navController.navigate(
                        "occurrences/${Uri.encode(projectId)}/${Uri.encode(fingerprint)}",
                    )
                },
                onUnpair = onUnpair,
            )
        }
        composable("occurrences/{projectId}/{fingerprint}") { entry ->
            OccurrencesScreen(
                repository = container.eventsRepository,
                projectId = entry.arguments?.getString("projectId").orEmpty(),
                fingerprint = entry.arguments?.getString("fingerprint").orEmpty(),
                onBack = { navController.popBackStack() },
                onEventClick = { id -> openEvent(id) },
            )
        }
        composable("event/{eventId}") { entry ->
            EventDetailScreen(
                repository = container.eventsRepository,
                eventId = entry.arguments?.getString("eventId").orEmpty(),
                onBack = { navController.popBackStack() },
            )
        }
    }
}
