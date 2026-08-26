package com.yy21120.skycast.data

import java.net.ConnectException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class CachedOpportunityRepositoryTest {
    @Test
    fun `online success writes cache and returns online result`() = runTest {
        val cache = FakeOpportunityCacheStore()
        val response = testOpportunityResponse()
        val repository = repository(
            remoteResult = Result.success(response),
            cache = cache,
            now = 10_000L,
        )

        val result = repository.getWuhanOpportunities()

        assertEquals(OpportunityDataSource.ONLINE, result.source)
        assertEquals(response, result.response)
        assertEquals(10_000L, result.cachedAtEpochMillis)
        assertNotNull(cache.payload)
        assertEquals("wuhan:sunset:3", cache.payload?.cacheKey)
    }

    @Test
    fun `network failure returns fresh cached response`() = runTest {
        val cachedAt = 20_000L
        val cache = FakeOpportunityCacheStore(
            payload = cachedPayload(cachedAt),
        )
        val repository = repository(
            remoteResult = Result.failure(ConnectException("offline")),
            cache = cache,
            now = cachedAt + 60_000L,
        )

        val result = repository.getWuhanOpportunities()

        assertEquals(OpportunityDataSource.CACHE, result.source)
        assertEquals(testOpportunityResponse(), result.response)
        assertFalse(result.isExpired)
    }

    @Test
    fun `network failure without cache retains original error`() = runTest {
        val networkError = ConnectException("offline")
        val repository = repository(
            remoteResult = Result.failure(networkError),
            cache = FakeOpportunityCacheStore(),
            now = 20_000L,
        )

        try {
            repository.getWuhanOpportunities()
            fail("Expected the original network error")
        } catch (exception: ConnectException) {
            assertEquals(networkError, exception)
        }
    }

    @Test
    fun `cache older than six hours is marked expired`() = runTest {
        val cachedAt = 20_000L
        val cache = FakeOpportunityCacheStore(payload = cachedPayload(cachedAt))
        val repository = repository(
            remoteResult = Result.failure(ConnectException("offline")),
            cache = cache,
            now = cachedAt + CachedOpportunityRepository.CACHE_FRESHNESS_MILLIS + 1L,
        )

        val result = repository.getWuhanOpportunities()

        assertTrue(result.isExpired)
    }

    @Test
    fun `corrupt cache is deleted and original network error is retained`() = runTest {
        val networkError = ConnectException("offline")
        val cache = FakeOpportunityCacheStore(
            payload = CachedOpportunityPayload(
                cacheKey = "wuhan:sunset:3",
                payloadJson = "not-json",
                cachedAtEpochMillis = 1_000L,
            ),
        )
        val repository = repository(
            remoteResult = Result.failure(networkError),
            cache = cache,
            now = 2_000L,
        )

        try {
            repository.getWuhanOpportunities()
            fail("Expected the original network error")
        } catch (exception: ConnectException) {
            assertEquals(networkError, exception)
        }
        assertEquals("wuhan:sunset:3", cache.deletedKey)
    }

    private fun repository(
        remoteResult: Result<OpportunitiesResponse>,
        cache: FakeOpportunityCacheStore,
        now: Long,
    ): CachedOpportunityRepository =
        CachedOpportunityRepository(
            remote = object : OpportunityRemoteDataSource {
                override suspend fun fetchWuhanOpportunities(days: Int): OpportunitiesResponse =
                    remoteResult.getOrThrow()
            },
            cache = cache,
            nowEpochMillis = { now },
        )

    private fun cachedPayload(cachedAt: Long): CachedOpportunityPayload =
        CachedOpportunityPayload(
            cacheKey = "wuhan:sunset:3",
            payloadJson = Json.encodeToString(testOpportunityResponse()),
            cachedAtEpochMillis = cachedAt,
        )
}

private class FakeOpportunityCacheStore(
    var payload: CachedOpportunityPayload? = null,
) : OpportunityCacheStore {
    var deletedKey: String? = null

    override suspend fun read(cacheKey: String): CachedOpportunityPayload? = payload

    override suspend fun write(payload: CachedOpportunityPayload) {
        this.payload = payload
    }

    override suspend fun delete(cacheKey: String) {
        deletedKey = cacheKey
        payload = null
    }
}
