package dev.boop.android.data.model

import dev.boop.android.data.remote.ApiFactory
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EventSectionsTest {

    @Test
    fun splitsDataIntoKnownSections() {
        val data = ApiFactory.json.parseToJsonElement(
            """
            {"exception":{"type":"KeyError","message":"oops"},
             "stacktrace":[{"function":"f","file":"x.py","line":"1","in_app":true}],
             "tags":{"env":"prod"},
             "context":{"user":"marco"},
             "breadcrumbs":[{"timestamp":"t","category":"c","message":"m"}],
             "extra":{"k":"v"}}
            """.trimIndent(),
        ).jsonObject

        val sections = EventSections.fromData(data)

        assertEquals("KeyError", sections.exception?.getValue("type").toString().trim('"'))
        assertEquals(1, sections.frames.size)
        assertEquals("f", sections.frames.first().function)
        assertTrue(sections.frames.first().inApp)
        assertEquals(1, sections.tags.size)
        assertEquals("env", sections.tags.first().first)
        assertEquals(1, sections.context.size)
        assertEquals(1, sections.breadcrumbs.size)
        assertEquals(1, sections.rest.size)
        assertEquals("extra", sections.rest.first().first)
    }

    @Test
    fun emptyDataGivesEmptySections() {
        val data = ApiFactory.json.parseToJsonElement("{}").jsonObject
        assertTrue(EventSections.fromData(data).isEmpty)
    }
}
