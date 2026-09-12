package com.yy21120.skycast.ui

import com.yy21120.skycast.data.AgentChatRequest
import com.yy21120.skycast.data.AgentChatResponse
import com.yy21120.skycast.data.AgentRepository
import com.yy21120.skycast.data.defaultWuhanShootingSpots
import com.yy21120.skycast.data.testSunsetOpportunity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AgentViewModelTest {
    @Test
    fun `sends grounded request and displays response`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = RecordingAgentRepository()
            val viewModel = AgentViewModel(repository)
            viewModel.selectSpot("shahu-park")
            viewModel.updateInput("今天去哪拍？")

            viewModel.send(testSunsetOpportunity())
            advanceUntilIdle()

            assertEquals("shahu-park", repository.requests.single().selectedSpotId)
            assertEquals("今天去哪拍？", repository.requests.single().message)
            assertEquals("建议去沙湖公园。", viewModel.uiState.value.messages.last().content)
            assertTrue(AgentModule.MAP in viewModel.uiState.value.revealedModules)
            assertFalse(viewModel.uiState.value.isLoading)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `questions reveal only relevant modules`() {
        assertEquals(setOf(AgentModule.MAP), modulesFor("推荐去哪拍？"))
        assertEquals(setOf(AgentModule.FORECAST), modulesFor("未来三天怎么样？"))
        assertEquals(setOf(AgentModule.DETAILS), modulesFor("查看详细云量数据"))
        assertEquals(emptySet<AgentModule>(), modulesFor("今天值得去吗？"))
        assertEquals(
            setOf(AgentModule.MAP),
            modulesFor("几点出发并打开导航地图？"),
        )
    }

    @Test
    fun `latest text-only question clears the previous visual module`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val viewModel = AgentViewModel(RecordingAgentRepository())
            viewModel.updateInput("帮我打开导航地图")
            viewModel.send(testSunsetOpportunity())
            advanceUntilIdle()
            assertEquals(setOf(AgentModule.MAP), viewModel.uiState.value.revealedModules)

            viewModel.updateInput("今天值得去吗？")
            viewModel.send(testSunsetOpportunity())
            advanceUntilIdle()
            assertEquals(emptySet<AgentModule>(), viewModel.uiState.value.revealedModules)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `network failure returns useful offline answer`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val viewModel = AgentViewModel(
                object : AgentRepository {
                    override suspend fun chat(request: AgentChatRequest): AgentChatResponse {
                        error("offline")
                    }
                },
            )
            viewModel.updateInput("带什么镜头？")

            viewModel.send(testSunsetOpportunity())
            advanceUntilIdle()

            val answer = viewModel.uiState.value.messages.last()
            assertTrue(answer.content.contains("广角镜头"))
            assertTrue(answer.isFallback)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private class RecordingAgentRepository : AgentRepository {
        val requests = mutableListOf<AgentChatRequest>()

        override suspend fun chat(request: AgentChatRequest): AgentChatResponse {
            requests += request
            return AgentChatResponse(
                reply = "建议去沙湖公园。",
                sceneId = "wuhan-sunset-2026-08-26",
                score = 65,
                confidence = "medium",
                recommendedSpots = defaultWuhanShootingSpots(),
                usedTools = listOf("get_sunset_assessment", "list_shooting_spots"),
                provider = "test",
                model = "test-model",
                generatedAt = "2026-08-26T09:00:00Z",
                safetyNotice = "注意安全",
            )
        }
    }
}
