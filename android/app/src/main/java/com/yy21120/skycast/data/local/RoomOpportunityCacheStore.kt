package com.yy21120.skycast.data.local

import com.yy21120.skycast.data.CachedOpportunityPayload
import com.yy21120.skycast.data.OpportunityCacheStore

class RoomOpportunityCacheStore(
    private val dao: OpportunityCacheDao,
) : OpportunityCacheStore {
    override suspend fun read(cacheKey: String): CachedOpportunityPayload? =
        dao.read(cacheKey)?.let { entity ->
            CachedOpportunityPayload(
                cacheKey = entity.cacheKey,
                payloadJson = entity.payloadJson,
                cachedAtEpochMillis = entity.cachedAtEpochMillis,
            )
        }

    override suspend fun write(payload: CachedOpportunityPayload) {
        dao.upsert(
            OpportunityCacheEntity(
                cacheKey = payload.cacheKey,
                payloadJson = payload.payloadJson,
                cachedAtEpochMillis = payload.cachedAtEpochMillis,
            ),
        )
    }

    override suspend fun delete(cacheKey: String) {
        dao.delete(cacheKey)
    }
}
