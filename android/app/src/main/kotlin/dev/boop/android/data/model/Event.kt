package dev.boop.android.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/** A stored event as returned by the server. */
@Serializable
data class Event(
    val id: String,
    val title: String,
    val body: String = "",
    val level: Level = Level.INFO,
    val source: String = "",
    val type: String = "",
    val fingerprint: String = "",
    @SerialName("external_id") val externalId: String = "",
    @SerialName("project_id") val projectId: String = "",
    @SerialName("project_name") val projectName: String = "",
    @SerialName("project_slug") val projectSlug: String = "",
    @SerialName("project_icon") val projectIcon: String = "",
    val data: JsonObject = JsonObject(emptyMap()),
    val actions: List<EventAction> = emptyList(),
    @SerialName("occurred_at") val occurredAt: String = "",
    @SerialName("created_at") val createdAt: String = "",
    val silenced: Boolean = false,
    /** Set in grouped listings: this row is the latest of [GroupInfo.count] occurrences. */
    val group: GroupInfo? = null,
) {
    val isRepeated: Boolean get() = (group?.count ?: 0) > 1
}

/** Summarises the occurrences behind a grouped row. */
@Serializable
data class GroupInfo(
    val count: Int = 0,
    @SerialName("first_seen") val firstSeen: String = "",
    @SerialName("last_seen") val lastSeen: String = "",
)

/** A button attached to an event that opens a URL. */
@Serializable
data class EventAction(
    val label: String,
    val url: String,
)

/** A page of events plus the cursor for the next page (null when exhausted). */
@Serializable
data class EventPage(
    val events: List<Event> = emptyList(),
    @SerialName("next_cursor") val nextCursor: String? = null,
)
