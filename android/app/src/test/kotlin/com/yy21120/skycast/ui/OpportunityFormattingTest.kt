package com.yy21120.skycast.ui

import java.time.ZoneId
import org.junit.Assert.assertEquals
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
}
