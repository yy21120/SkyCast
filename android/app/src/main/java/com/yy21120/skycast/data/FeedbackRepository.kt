package com.yy21120.skycast.data

interface FeedbackRepository {
    suspend fun submitSunsetFeedback(request: SunsetFeedbackRequest): SunsetFeedbackResponse
}
