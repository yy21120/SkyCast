package com.yy21120.skycast.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class OpportunityFormattingTest {
    @Test
    fun `formats weekday in simplified Chinese`() {
        assertEquals("8月26日 星期三", formatDate("2026-08-26"))
    }
}
