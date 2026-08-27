package com.yy21120.skycast.ui

import com.yy21120.skycast.data.OpportunityDataSource
import com.yy21120.skycast.data.OpportunityResult
import com.yy21120.skycast.data.testOpportunityResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OpportunityNavigationModelTest {
    private val result = OpportunityResult(
        response = testOpportunityResponse(),
        source = OpportunityDataSource.ONLINE,
        cachedAtEpochMillis = 1_000L,
    )

    @Test
    fun `finds detail by stable scene id`() {
        assertEquals(
            "wuhan-sunset-2026-08-26",
            findOpportunity(result, "wuhan-sunset-2026-08-26")?.sceneId,
        )
    }

    @Test
    fun `returns null for an opportunity no longer present`() {
        assertNull(findOpportunity(result, "missing-scene"))
    }
}
