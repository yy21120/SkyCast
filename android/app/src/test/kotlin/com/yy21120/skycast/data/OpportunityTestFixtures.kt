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
        opportunities = emptyList(),
    )
