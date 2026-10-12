package dev.boop.android.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.boop.android.AppContainer
import dev.boop.android.ui.eventdetail.EventDetailScreen
import dev.boop.android.ui.inbox.InboxScreen

/** Inbox and event detail; shown once the device is paired. */
@Composable
fun AppNavHost(container: AppContainer, onUnpair: () -> Unit) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "inbox") {
        composable("inbox") {
            InboxScreen(
                repository = container.eventsRepository,
                onEventClick = { id -> navController.navigate("event/$id") },
                onUnpair = onUnpair,
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
