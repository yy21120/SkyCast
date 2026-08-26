package com.yy21120.skycast.data

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class OpportunityJsonTest {
    @Test
    fun `decodes the server opportunity contract`() {
        val response = Json.decodeFromString<OpportunitiesResponse>(API_RESPONSE)

        assertEquals("武汉", response.city.name)
        assertEquals(1, response.opportunities.size)
        assertEquals(93, response.opportunities.single().score)
        assertEquals("go", response.opportunities.single().recommendation)
        assertEquals("cloud", response.opportunities.single().factors.single().code)
    }

    private companion object {
        val API_RESPONSE =
            """
            {
              "city": {
                "id": "wuhan",
                "name": "武汉",
                "latitude": 30.5928,
                "longitude": 114.3055,
                "timezone": "Asia/Shanghai"
              },
              "scene_type": "sunset",
              "mode": "replay",
              "generated_at": "2026-08-25T10:00:00+08:00",
              "opportunities": [{
                "scene_id": "wuhan-sunset-2026-08-25",
                "date": "2026-08-25",
                "sunset": "2026-08-25T18:54:00+08:00",
                "coloring_window_start": "2026-08-25T18:34:00+08:00",
                "coloring_window_end": "2026-08-25T19:09:00+08:00",
                "score": 93,
                "baseline_probability": 0.93,
                "probability_status": "uncalibrated_baseline",
                "confidence": "medium",
                "recommendation": "go",
                "summary": "云层配置有利，值得提前到达机位。",
                "factors": [{
                  "code": "cloud",
                  "label": "云层配置",
                  "value": 68.0,
                  "unit": "%",
                  "contribution": 24.0,
                  "effect": "favorable",
                  "explanation": "中高云适中，有利于染色。"
                }],
                "sources": [{
                  "source_id": "fixture",
                  "source_url": null,
                  "sampled_at": "2026-08-25T18:00:00+08:00",
                  "retrieved_at": "2026-08-25T10:00:00+08:00"
                }],
                "model_version": "sunset-rules-v1"
              }]
            }
            """.trimIndent()
    }
}
