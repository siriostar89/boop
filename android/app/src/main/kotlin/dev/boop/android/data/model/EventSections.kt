package dev.boop.android.data.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull

/** Parsed sections from an event's `data` field for specialized rendering. */
data class EventSections(
    val exception: JsonObject? = null,
    val frames: List<StackFrame> = emptyList(),
    val tags: List<Pair<String, JsonElement>> = emptyList(),
    val context: List<Pair<String, JsonElement>> = emptyList(),
    val breadcrumbs: List<Breadcrumb> = emptyList(),
    val rest: List<Pair<String, JsonElement>> = emptyList(),
) {
    companion object {
        private val knownKeys = setOf("exception", "stacktrace", "tags", "context", "breadcrumbs")

        fun fromData(data: JsonObject): EventSections {
            val exception = data["exception"].let { if (it is JsonObject) it else null }

            val frames = data["stacktrace"].let {
                if (it is JsonArray) it.mapNotNull { if (it is JsonObject) StackFrame(it) else null } else emptyList()
            }

            val tags = data["tags"].let {
                if (it is JsonObject) it.entries.sortedBy { it.key }.map { it.key to it.value } else emptyList()
            }

            val context = data["context"].let {
                if (it is JsonObject) it.entries.sortedBy { it.key }.map { it.key to it.value } else emptyList()
            }

            val breadcrumbs = data["breadcrumbs"].let {
                if (it is JsonArray) it.mapNotNull { if (it is JsonObject) Breadcrumb(it) else null } else emptyList()
            }

            val rest = data.filterKeys { it !in knownKeys }
                .entries.sortedBy { it.key }
                .map { it.key to it.value }

            return EventSections(
                exception = exception,
                frames = frames,
                tags = tags,
                context = context,
                breadcrumbs = breadcrumbs,
                rest = rest,
            )
        }
    }

    val isEmpty: Boolean
        get() = exception == null && frames.isEmpty() && tags.isEmpty() && context.isEmpty() && breadcrumbs.isEmpty() && rest.isEmpty()
}

/** A single stack frame from a stack trace. */
data class StackFrame(
    val function: String,
    val file: String,
    val line: String?,
    val inApp: Boolean,
) {
    constructor(frame: JsonObject) : this(
        function = (frame["function"] as? JsonPrimitive)?.contentOrNull ?: (frame["module"] as? JsonPrimitive)?.contentOrNull ?: "—",
        file = (frame["file"] as? JsonPrimitive)?.contentOrNull ?: (frame["filename"] as? JsonPrimitive)?.contentOrNull ?: "",
        line = (frame["line"] as? JsonPrimitive)?.contentOrNull,
        inApp = (frame["in_app"] as? JsonPrimitive)?.booleanOrNull ?: false,
    )
}

/** A breadcrumb entry. */
data class Breadcrumb(
    val timestamp: String,
    val category: String,
    val message: String,
) {
    constructor(crumb: JsonObject) : this(
        timestamp = (crumb["timestamp"] as? JsonPrimitive)?.contentOrNull ?: (crumb["time"] as? JsonPrimitive)?.contentOrNull ?: "",
        category = (crumb["category"] as? JsonPrimitive)?.contentOrNull ?: (crumb["type"] as? JsonPrimitive)?.contentOrNull ?: "",
        message = (crumb["message"] as? JsonPrimitive)?.contentOrNull ?: (crumb["msg"] as? JsonPrimitive)?.contentOrNull ?: Json.encodeToString(
            JsonElement.serializer(),
            crumb,
        ),
    )
}