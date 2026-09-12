package com.yy21120.skycast.data

import java.net.ConnectException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoOpportunityRepositoryTest {
    @Test
    fun `first launch remains usable when server is offline`() = runTest {
        val repository = DemoOpportunityRepository(
            delegate = object : OpportunityRepository {
                override suspend fun getWuhanOpportunities(days: Int): OpportunityResult {
                    throw ConnectException("offline")
                }
            },
            clock = { 123L },
        )

        val result = repository.getWuhanOpportunities()

        assertEquals(OpportunityDataSource.DEMO, result.source)
        assertEquals(3, result.response.opportunities.size)
        assertEquals(123L, result.cachedAtEpochMillis)
        assertTrue(result.isExpired)
    }
}
