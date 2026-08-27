package com.yy21120.skycast.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.yy21120.skycast.data.AssessmentFactor
import com.yy21120.skycast.data.City
import com.yy21120.skycast.data.OpportunitiesResponse
import com.yy21120.skycast.data.OpportunityDataSource
import com.yy21120.skycast.data.OpportunityResult
import com.yy21120.skycast.data.SourceReference
import com.yy21120.skycast.data.SunsetOpportunity
import com.yy21120.skycast.ui.theme.SkyCastTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SkyCastNavigationTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun cardOpensCompleteCachedDetailAndReturnsToList() {
        var openedUrl: String? = null
        composeRule.setContent {
            SkyCastTheme {
                SkyCastNavHost(
                    result = cachedResult(),
                    onRetry = {},
                    onOpenSource = { openedUrl = it },
                )
            }
        }

        composeRule
            .onNodeWithTag("opportunity-card-wuhan-sunset-2026-08-26")
            .performClick()

        composeRule.onNodeWithText("武汉晚霞评估详情").assertIsDisplayed()
        composeRule.onNodeWithText("离线缓存", substring = true).assertIsDisplayed()
        val detailList = composeRule.onNodeWithTag("opportunity-detail")
        detailList.performScrollToNode(hasText("日落时刻"))
        composeRule.onNodeWithText("日落时刻").assertIsDisplayed()
        detailList.performScrollToNode(hasText("未校准规则基线值"))
        composeRule.onNodeWithText("未校准规则基线值").assertIsDisplayed()
        detailList.performScrollToNode(hasText("云层配置"))
        composeRule.onNodeWithText("云层配置").assertIsDisplayed()
        detailList.performScrollToNode(hasText("降水概率"))
        composeRule.onNodeWithText("降水概率").assertIsDisplayed()
        detailList.performScrollToNode(hasText("打开来源"))
        composeRule.onNodeWithText("打开来源").performClick()
        composeRule.runOnIdle {
            assertEquals("https://example.com/weather", openedUrl)
        }

        detailList.performScrollToNode(hasText("← 返回"))
        composeRule.onNodeWithText("← 返回").performClick()
        composeRule.onNodeWithText("武汉晚霞机会").assertIsDisplayed()
    }

    @Test
    fun missingOpportunityShowsRecoverableState() {
        composeRule.setContent {
            SkyCastTheme {
                OpportunityDetailScreen(
                    result = cachedResult(),
                    opportunity = null,
                    onBack = {},
                    onRetry = {},
                    onOpenSource = {},
                )
            }
        }

        composeRule.onNodeWithText("评估不存在").assertIsDisplayed()
        composeRule.onNodeWithText("返回机会列表").assertIsDisplayed()
    }

    private fun cachedResult(): OpportunityResult = OpportunityResult(
        response = OpportunitiesResponse(
            city = City(
                id = "wuhan",
                name = "武汉",
                latitude = 30.5928,
                longitude = 114.3055,
                timezone = "Asia/Shanghai",
            ),
            sceneType = "sunset",
            mode = "live",
            generatedAt = "2026-08-26T09:00:00Z",
            opportunities = listOf(
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
                    summary = "存在拍摄机会，但仍有不确定性。",
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
                        AssessmentFactor(
                            code = "precipitation",
                            label = "降水概率",
                            value = 22.0,
                            unit = "%",
                            contribution = -8.0,
                            effect = "unfavorable",
                            explanation = "降水会降低拍摄稳定性。",
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
                ),
            ),
        ),
        source = OpportunityDataSource.CACHE,
        cachedAtEpochMillis = 1_787_698_800_000L,
    )
}
