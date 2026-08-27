package com.yy21120.skycast.ui

import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OpportunityFormattingTest {
    @Test
    fun `formats weekday in simplified Chinese`() {
        assertEquals("8月26日 星期三", formatDate("2026-08-26"))
    }

    @Test
    fun `formats server and cache timestamps`() {
        val wuhanZone = ZoneId.of("Asia/Shanghai")
        assertEquals(
            "8月26日 07:00",
            formatTimestamp("2026-08-25T23:00:00Z", wuhanZone),
        )
        assertEquals(
            "8月26日 07:00",
            formatCacheTime(
                epochMillis = 1_787_698_800_000L,
                zoneId = wuhanZone,
            ),
        )
    }

    @Test
    fun `converts opportunity time into the city timezone`() {
        assertEquals(
            "18:28",
            formatOpportunityTime(
                value = "2026-08-26T10:28:00Z",
                zoneId = ZoneId.of("Asia/Shanghai"),
            ),
        )
    }

    @Test
    fun `formats baseline and factor values without implying probability`() {
        assertEquals("0.650", formatBaselineValue(0.65))
        assertEquals("67", formatMetricNumber(67.0))
        assertEquals("+18", formatContribution(18.0))
        assertEquals("-8.5", formatContribution(-8.5))
    }

    @Test
    fun `allows only valid http and https source links`() {
        assertEquals("https://example.com/source", validHttpUrl("https://example.com/source"))
        assertEquals("http://example.com", validHttpUrl(" http://example.com "))
        assertNull(validHttpUrl("javascript:alert(1)"))
        assertNull(validHttpUrl("file:///data/local/source.json"))
        assertNull(validHttpUrl("https:///missing-host"))
        assertNull(validHttpUrl(null))
    }
}
