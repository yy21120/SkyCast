package com.yy21120.skycast.data

import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

class CachedOpportunityRepository(
    private val remote: OpportunityRemoteDataSource,
    private val cache: OpportunityCacheStore,
    private val nowEpochMillis: () -> Long = System::currentTimeMillis,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : OpportunityRepository {
    override suspend fun getWuhanOpportunities(days: Int): OpportunityResult {
        require(days in 1..7) { "days must be between 1 and 7" }
        val cacheKey = cacheKey(days)

        return try {
            val response = remote.fetchWuhanOpportunities(days)
            val cachedAt = nowEpochMillis()
            cache.write(
                CachedOpportunityPayload(
                    cacheKey = cacheKey,
                    payloadJson = json.encodeToString(response),
                    cachedAtEpochMillis = cachedAt,
                ),
            )
            OpportunityResult(
                response = response,
                source = OpportunityDataSource.ONLINE,
                cachedAtEpochMillis = cachedAt,
            )
        } catch (exception: CancellationException) {
            throw exception
        } catch (onlineException: Exception) {
            cachedResultOrThrow(cacheKey, onlineException)
        }
    }

    private suspend fun cachedResultOrThrow(
        cacheKey: String,
        onlineException: Exception,
    ): OpportunityResult {
        val cached = cache.read(cacheKey) ?: throw onlineException
        val response = try {
            json.decodeFromString<OpportunitiesResponse>(cached.payloadJson)
        } catch (_: SerializationException) {
            cache.delete(cacheKey)
            throw onlineException
        } catch (_: IllegalArgumentException) {
            cache.delete(cacheKey)
            throw onlineException
        }
        val ageMillis = (nowEpochMillis() - cached.cachedAtEpochMillis).coerceAtLeast(0L)
        return OpportunityResult(
            response = response,
            source = OpportunityDataSource.CACHE,
            cachedAtEpochMillis = cached.cachedAtEpochMillis,
            isExpired = ageMillis > CACHE_FRESHNESS_MILLIS,
        )
    }

    private fun cacheKey(days: Int): String = "wuhan:sunset:$days"

    internal companion object {
        const val CACHE_FRESHNESS_MILLIS = 6 * 60 * 60 * 1_000L
    }
}
