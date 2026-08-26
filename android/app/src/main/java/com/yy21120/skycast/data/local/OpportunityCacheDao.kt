package com.yy21120.skycast.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface OpportunityCacheDao {
    @Query("SELECT * FROM opportunity_cache WHERE cache_key = :cacheKey LIMIT 1")
    suspend fun read(cacheKey: String): OpportunityCacheEntity?

    @Upsert
    suspend fun upsert(entity: OpportunityCacheEntity)

    @Query("DELETE FROM opportunity_cache WHERE cache_key = :cacheKey")
    suspend fun delete(cacheKey: String)
}
