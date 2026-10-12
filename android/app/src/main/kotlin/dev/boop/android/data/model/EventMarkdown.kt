package dev.boop.android.data.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull

// Text renderings of an event for copying and sharing. `agentMarkdown` is meant
// to be pasted straight into an AI assistant (mirrors the iOS EventMarkdown).

private val compact: Json = Json
private val pretty: Json = Json { prettyPrint = true }

/** Short plain text: what happened, where, when. */
fun Event.plainText(): String {
    val lines = mutableListOf(title)
    if (body.isNotEmpty()) lines.add(body)
    lines.add("")
    val meta = mutableListOf(projectName, level.name.lowercase())
    if (source.isNotEmpty()) meta.add(source)
    meta.add(occurredAt)
    lines.add(meta.joinToString(" · "))
    actions.forEach { lines.add("${it.label}: ${it.url}") }
    return lines.joinToString("\n")
}

/** Markdown with a section per recognised part of `data`. */
fun Event.agentMarkdown(): String {
    val out = mutableListOf("# $title")
    if (body.isNotEmpty()) {
        out.add("")
        out.add(body)
    }

    val facts = mutableListOf("- Project: $projectName", "- Level: ${level.name.lowercase()}")
    if (source.isNotEmpty()) facts.add("- Source: $source")
    if (type.isNotEmpty()) facts.add("- Type: $type")
    facts.add("- Occurred: $occurredAt")
    group?.let { g ->
        if (g.count > 1) facts.add("- Occurrences: ${g.count} (first ${g.firstSeen}, last ${g.lastSeen})")
    }
    if (fingerprint.isNotEmpty()) facts.add("- Fingerprint: $fingerprint")
    if (externalId.isNotEmpty()) facts.add("- External id: $externalId")
    facts.add("- Event id: $id")
    out.add("")
    out.add("## Event")
    out.add("")
    out.addAll(facts)

    data["exception"].asObject()?.let { exc ->
        out.add("")
        out.add("## Exception")
        out.add("")
        val t = exc["type"].inlineText()
        val m = exc["message"].inlineText()
        when {
            t.isNotEmpty() && m.isNotEmpty() -> out.add("**$t**: $m")
            t.isNotEmpty() -> out.add("**$t**")
            m.isNotEmpty() -> out.add(m)
        }
        exc.keys.filter { it != "type" && it != "message" }.sorted().forEach { k ->
            out.add("- $k: ${exc[k].inlineText()}")
        }
    }

    data["tags"].asObject()?.takeIf { it.isNotEmpty() }?.let { tags ->
        val env = tags["environment"] ?: tags["env"]
        if (env != null) {
            out.add("")
            out.add("## Environment")
            out.add("")
            out.add(env.inlineText())
        }
        val others = tags.filterKeys { it != "environment" && it != "env" }
        if (others.isNotEmpty()) {
            out.add("")
            out.add("## Tags")
            out.add("")
            others.entries.sortedBy { it.key }.forEach { out.add("- ${it.key}: ${it.value.inlineText()}") }
        }
    }

    data["stacktrace"].asArray()?.takeIf { it.isNotEmpty() }?.let { frames ->
        out.add("")
        out.add("## Stack trace")
        out.add("")
        out.add("```")
        var hasInApp = false
        frames.forEach { frame ->
            val f = frame.asObject() ?: return@forEach
            var loc = f["file"].inlineText()
            val line = f["line"].inlineText().takeIf { it.isNotEmpty() && it != "null" }
            if (line != null && loc.isNotEmpty()) loc = "$loc:$line"
            val inApp = f["in_app"].asBool() ?: false
            if (inApp) hasInApp = true
            var text = if (inApp) "* " else "  "
            text += f["function"].inlineText()
            if (loc.isNotEmpty()) text += "  ($loc)"
            out.add(text)
        }
        out.add("```")
        if (hasInApp) out.add("`*` marks frames in the application's own code.")
    }

    data["context"].asObject()?.takeIf { it.isNotEmpty() }?.let { ctx ->
        out.add("")
        out.add("## Context")
        out.add("")
        ctx.entries.sortedBy { it.key }.forEach { out.add("- ${it.key}: ${it.value.inlineText()}") }
    }

    data["breadcrumbs"].asArray()?.takeIf { it.isNotEmpty() }?.let { crumbs ->
        out.add("")
        out.add("## Breadcrumbs")
        out.add("")
        crumbs.forEach { crumb ->
            val c = crumb.asObject() ?: return@forEach
            val parts = mutableListOf<String>()
            c["timestamp"].inlineText().takeIf { it.isNotEmpty() && it != "null" }?.let { parts.add(it) }
            c["category"].inlineText().takeIf { it.isNotEmpty() && it != "null" }?.let { parts.add("[$it]") }
            c["message"].inlineText().let { parts.add(it) }
            out.add("- " + parts.joinToString(" "))
        }
    }

    val known = setOf("exception", "tags", "stacktrace", "context", "breadcrumbs")
    val rest = data.filterKeys { it !in known }
    if (rest.isNotEmpty()) {
        out.add("")
        out.add("## Data")
        out.add("")
        out.add("```json")
        out.add(pretty.encodeToString(JsonObject.serializer(), JsonObject(rest)))
        out.add("```")
    }

    if (actions.isNotEmpty()) {
        out.add("")
        out.add("## Links")
        out.add("")
        actions.forEach { out.add("- [${it.label}](${it.url})") }
    }

    return out.joinToString("\n") + "\n"
}

private fun JsonElement?.asObject(): JsonObject? = this as? JsonObject
private fun JsonElement?.asArray(): JsonArray? = this as? JsonArray
private fun JsonElement?.asBool(): Boolean? = (this as? JsonPrimitive)?.booleanOrNull

/** Scalars as-is; containers as compact JSON on one line. */
private fun JsonElement?.inlineText(): String = when (this) {
    null -> "null"
    is JsonObject, is JsonArray -> compact.encodeToString(JsonElement.serializer(), this)
    else -> (this as? JsonPrimitive)?.contentOrNull ?: "null"
}
