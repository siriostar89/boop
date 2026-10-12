package dev.boop.android.data.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertTrue
import org.junit.Test

class EventMarkdownTest {

    private fun event(dataJson: String, actions: List<EventAction> = emptyList()) = Event(
        id = "evt_1",
        title = "KeyError",
        body = "key :can_palette? not found",
        level = Level.ERROR,
        source = "error_tracker",
        type = "exception",
        fingerprint = "uini-keyerror",
        externalId = "ext-9",
        projectId = "prj_1",
        projectName = "Uini",
        data = Json.parseToJsonElement(dataJson).jsonObject,
        actions = actions,
        occurredAt = "2026-10-11T19:57:28Z",
        createdAt = "2026-10-11T19:57:28Z",
        group = GroupInfo(count = 47, firstSeen = "2026-10-11T09:31:00Z", lastSeen = "2026-10-11T10:42:00Z"),
    )

    @Test
    fun rendersEveryRecognisedSection() {
        val markdown = event(
            dataJson = """
                {
                  "exception": {"type": "KeyError", "message": "key :can_palette? not found", "module": "Uini.Widget"},
                  "tags": {"environment": "production", "region": "eu"},
                  "stacktrace": [
                    {"file": "lib/uini/widget.ex", "line": 49, "function": "handle_event/3", "in_app": true},
                    {"file": "lib/phoenix/router.ex", "line": 12, "function": "call/2", "in_app": false}
                  ],
                  "context": {"user_id": "123"},
                  "breadcrumbs": [{"timestamp": "19:57:20", "category": "http", "message": "POST /widgets"}],
                  "custom": {"k": 1}
                }
            """.trimIndent(),
            actions = listOf(EventAction("Open in ErrorTracker", "https://errors.example.com/1")),
        ).agentMarkdown()

        assertTrue(markdown.startsWith("# KeyError"))
        assertTrue(markdown.contains("- Level: error"))
        assertTrue(markdown.contains("- Occurrences: 47"))
        assertTrue(markdown.contains("## Exception"))
        assertTrue(markdown.contains("**KeyError**: key :can_palette? not found"))
        assertTrue(markdown.contains("## Environment"))
        assertTrue(markdown.contains("production"))
        assertTrue(markdown.contains("## Tags"))
        assertTrue(markdown.contains("- region: eu"))
        assertTrue(markdown.contains("## Stack trace"))
        assertTrue(markdown.contains("* handle_event/3  (lib/uini/widget.ex:49)"))
        assertTrue(markdown.contains("## Context"))
        assertTrue(markdown.contains("- user_id: 123"))
        assertTrue(markdown.contains("## Breadcrumbs"))
        assertTrue(markdown.contains("- 19:57:20 [http] POST /widgets"))
        assertTrue(markdown.contains("## Data"))
        assertTrue(markdown.contains("\"custom\""))
        assertTrue(markdown.contains("## Links"))
        assertTrue(markdown.contains("- [Open in ErrorTracker](https://errors.example.com/1)"))
    }

    @Test
    fun plainTextHasNoMarkdown() {
        val text = event("{}").plainText()

        assertTrue(text.startsWith("KeyError"))
        assertTrue(text.contains("Uini · error · error_tracker"))
        assertTrue(!text.contains("## "))
    }
}
