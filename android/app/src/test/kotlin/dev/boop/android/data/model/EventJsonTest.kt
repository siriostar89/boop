package dev.boop.android.data.model

import dev.boop.android.data.remote.ApiFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EventJsonTest {

    @Test
    fun parsesAPageAndIgnoresUnknownFields() {
        val body = """
            {"events":[
              {"id":"evt_1","title":"KeyError","body":"key not found","level":"error",
               "source":"error_tracker","project_name":"Uini","project_slug":"uini",
               "created_at":"2026-10-11T19:57:28.948044000Z","silenced":false,
               "data":{"exception":{"type":"KeyError"}},
               "actions":[{"label":"Open","url":"https://x"}],
               "unknown_field":"ignored"}
            ],"next_cursor":"evt_1"}
        """.trimIndent()

        val page = ApiFactory.json.decodeFromString(EventPage.serializer(), body)

        assertEquals(1, page.events.size)
        assertEquals("evt_1", page.nextCursor)
        val event = page.events.first()
        assertEquals("evt_1", event.id)
        assertEquals("KeyError", event.title)
        assertEquals(Level.ERROR, event.level)
        assertEquals("Uini", event.projectName)
        assertEquals(1, event.actions.size)
        assertEquals("Open", event.actions.first().label)
    }

    @Test
    fun parsesAnEmptyPageWithoutCursor() {
        val page = ApiFactory.json.decodeFromString(EventPage.serializer(), """{"events":[]}""")

        assertEquals(0, page.events.size)
        assertNull(page.nextCursor)
    }
}
