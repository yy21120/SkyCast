package com.yy21120.skycast.data

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedbackJsonTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `encodes feedback using the server contract`() {
        val request = SunsetFeedbackRequest(
            clientFeedbackId = "5215a6f3-bace-45df-ae86-9de854f6fc64",
            sceneId = "wuhan-sunset-2026-08-27",
            outcome = SunsetOutcome.VIVID,
            shootingQuality = 5,
            notes = "明显染色",
            submittedAt = "2026-08-27T19:20:00+08:00",
        )

        val encoded = json.encodeToString(request)

        assertTrue(encoded.contains("\"client_feedback_id\""))
        assertTrue(encoded.contains("\"outcome\":\"vivid\""))
        assertTrue(encoded.contains("\"shooting_quality\":5"))
    }

    @Test
    fun `decodes accepted duplicate response`() {
        val response = json.decodeFromString<SunsetFeedbackResponse>(RESPONSE_JSON)

        assertEquals("accepted", response.status)
        assertTrue(response.duplicate)
        assertEquals(SunsetOutcome.VISIBLE, response.feedback.outcome)
        assertEquals(3, response.feedback.shootingQuality)
    }

    private companion object {
        val RESPONSE_JSON =
            """
            {
              "status": "accepted",
              "duplicate": true,
              "feedback": {
                "client_feedback_id": "5215a6f3-bace-45df-ae86-9de854f6fc64",
                "scene_id": "wuhan-sunset-2026-08-27",
                "outcome": "visible",
                "shooting_quality": 3,
                "notes": null,
                "submitted_at": "2026-08-27T19:20:00+08:00",
                "created_at": "2026-08-27T11:20:01Z"
              }
            }
            """.trimIndent()
    }
}
