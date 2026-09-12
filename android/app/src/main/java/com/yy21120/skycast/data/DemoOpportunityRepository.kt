package com.yy21120.skycast.data

import kotlinx.coroutines.CancellationException

class DemoOpportunityRepository(
    private val delegate: OpportunityRepository,
    private val clock: () -> Long = System::currentTimeMillis,
) : OpportunityRepository {
    override suspend fun getWuhanOpportunities(days: Int): OpportunityResult =
        try {
            delegate.getWuhanOpportunities(days)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            OpportunityResult(
                response = demoWuhanOpportunities(days),
                source = OpportunityDataSource.DEMO,
                cachedAtEpochMillis = clock(),
                isExpired = true,
            )
        }
}

private fun demoWuhanOpportunities(days: Int): OpportunitiesResponse {
    val opportunities = listOf(
        demoOpportunity("2026-08-26", "18:52", "18:28", "19:18", 65, "watch"),
        demoOpportunity("2026-08-27", "18:51", "18:27", "19:17", 64, "watch"),
        demoOpportunity("2026-08-28", "18:50", "18:26", "19:16", 63, "watch"),
    ).take(days)
    return OpportunitiesResponse(
        city = City("wuhan", "武汉", 30.5928, 114.3055, "Asia/Shanghai"),
        sceneType = "sunset",
        mode = "replay",
        generatedAt = "2026-08-26T09:00:00Z",
        opportunities = opportunities,
    )
}

private fun demoOpportunity(
    date: String,
    sunsetTime: String,
    startTime: String,
    endTime: String,
    score: Int,
    recommendation: String,
) = SunsetOpportunity(
    sceneId = "wuhan-sunset-$date-demo",
    date = date,
    sunset = "${date}T$sunsetTime:00+08:00",
    coloringWindowStart = "${date}T$startTime:00+08:00",
    coloringWindowEnd = "${date}T$endTime:00+08:00",
    score = score,
    baselineProbability = score / 100.0,
    probabilityStatus = "uncalibrated_baseline",
    confidence = "medium",
    recommendation = recommendation,
    summary = "存在拍摄机会，但云量和降水仍有不确定性，建议临近时复核。",
    factors = listOf(
        AssessmentFactor(
            code = "high_cloud",
            label = "高云量",
            value = 58.0,
            unit = "%",
            contribution = 14.0,
            effect = "favorable",
            explanation = "中高云适中，可能提供较好的染色载体。",
        ),
        AssessmentFactor(
            code = "precipitation",
            label = "降水概率",
            value = 22.0,
            unit = "%",
            contribution = -6.0,
            effect = "limiting",
            explanation = "仍有降水不确定性，出发前需要复核。",
        ),
        AssessmentFactor(
            code = "visibility",
            label = "能见度",
            value = 16.0,
            unit = "km",
            contribution = 8.0,
            effect = "favorable",
            explanation = "能见度条件适合观察远处云层与天际线。",
        ),
    ),
    sources = listOf(
        SourceReference(
            sourceId = "skycast:bundled-demo",
            sampledAt = "${date}T17:00:00+08:00",
            retrievedAt = "2026-08-26T09:00:00Z",
        ),
    ),
    modelVersion = "sunset-rules-wuhan-v0.1.0-demo",
)
