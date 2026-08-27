package com.yy21120.skycast.data

internal fun testOpportunityResponse(): OpportunitiesResponse =
    OpportunitiesResponse(
        city = City(
            id = "wuhan",
            name = "武汉",
            latitude = 30.5928,
            longitude = 114.3055,
            timezone = "Asia/Shanghai",
        ),
        sceneType = "sunset",
        mode = "live",
        generatedAt = "2026-08-26T07:00:00+08:00",
        opportunities = listOf(testSunsetOpportunity()),
    )

internal fun testSunsetOpportunity(): SunsetOpportunity =
    SunsetOpportunity(
        sceneId = "wuhan-sunset-2026-08-26",
        date = "2026-08-26",
        sunset = "2026-08-26T18:48:00+08:00",
        coloringWindowStart = "2026-08-26T18:28:00+08:00",
        coloringWindowEnd = "2026-08-26T19:18:00+08:00",
        score = 65,
        baselineProbability = 0.65,
        probabilityStatus = "uncalibrated_baseline",
        confidence = "medium",
        recommendation = "watch",
        summary = "存在拍摄机会，但云量或降水仍有不确定性。",
        factors = listOf(
            AssessmentFactor(
                code = "cloud",
                label = "云层配置",
                value = 67.0,
                unit = "%",
                contribution = 18.0,
                effect = "favorable",
                explanation = "中高云适中，有利于染色。",
            ),
        ),
        sources = listOf(
            SourceReference(
                sourceId = "skycast:test",
                sourceUrl = "https://example.com/weather",
                sampledAt = "2026-08-26T18:00:00+08:00",
                retrievedAt = "2026-08-26T09:00:00Z",
            ),
        ),
        modelVersion = "sunset-rules-wuhan-v0.1.0",
    )
