package com.yy21120.skycast.data

interface OpportunityRepository {
    suspend fun getWuhanOpportunities(days: Int = 3): OpportunitiesResponse
}
