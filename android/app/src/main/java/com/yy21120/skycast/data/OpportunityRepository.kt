package com.yy21120.skycast.data

interface OpportunityRepository {
    suspend fun getWuhanOpportunities(days: Int = 3): OpportunityResult
}

data class OpportunityResult(
    val response: OpportunitiesResponse,
    val source: OpportunityDataSource,
    val cachedAtEpochMillis: Long,
    val isExpired: Boolean = false,
)

enum class OpportunityDataSource {
    ONLINE,
    CACHE,
}

interface OpportunityRemoteDataSource {
    suspend fun fetchWuhanOpportunities(days: Int = 3): OpportunitiesResponse
}

interface OpportunityCacheStore {
    suspend fun read(cacheKey: String): CachedOpportunityPayload?

    suspend fun write(payload: CachedOpportunityPayload)

    suspend fun delete(cacheKey: String)
}

data class CachedOpportunityPayload(
    val cacheKey: String,
    val payloadJson: String,
    val cachedAtEpochMillis: Long,
)
